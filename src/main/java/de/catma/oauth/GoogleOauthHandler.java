package de.catma.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.catma.properties.CATMAPropertyKey;
import de.catma.util.Pair;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;

import javax.validation.constraints.NotNull;
import javax.ws.rs.core.UriBuilder;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Based on: <a href="https://developers.google.com/identity/openid-connect/openid-connect#authenticatingtheuser">Google's OpenID Connect Documentation</a>
 * <p>
 * The basic flow is as follows:
 * 1. <code>getOauthAuthorizationRequestUri</code> is called and the client is redirected to the resultant URI (Google login/consent page).
 * 2. We receive a callback request from Google at the <code>redirectUrl</code> with parameters "code", "state" and "error".
 *    These should be supplied to <code>handleCallbackAndGetIdentity</code> to produce an <a href="#{@link}">{@link OauthIdentity}</a>
 *    that can then be used to authenticate the user.
 */
public class GoogleOauthHandler {
    public static final String OAUTH_CSRF_TOKEN_SESSION_ATTRIBUTE_NAME = "OAUTH_CSRF_TOKEN";
    private static final String OAUTH_NONCE_SESSION_ATTRIBUTE_NAME = "OAUTH_NONCE";

    public static final String CSRF_TOKEN_STATE_PARAMETER_NAME = "csrf_token";

    private static final Logger logger = Logger.getLogger(GoogleOauthHandler.class.getName());

    /**
     * Handles steps 1 & 2 (create an anti-forgery state token & send an authentication request to Google) as per
     * <a href="https://developers.google.com/identity/openid-connect/openid-connect#authenticatingtheuser">Google's OpenID Connect Documentation</a>.
     * <p>
     * Note that we only build the URI here - the caller is responsible for redirecting the client.
     *
     * @param redirectUrl the HTTP endpoint where we will receive the response from Google. This must be exactly as configured for the OAuth client in
     *                    <a href="https://console.cloud.google.com/auth/clients">Google Cloud Console</a>!
     * @param sessionSetAttributeFn the function that will be used to store some items in the relevant server-side session for later verification
     * @param optionalStateParams an optional map of parameters that can be used to recover the context when the user returns to our application
     * @return the {@link URI} that the client should be redirected to
     */
    public static URI getOauthAuthorizationRequestUri(
            @NotNull String redirectUrl, @NotNull BiConsumer<String, Object> sessionSetAttributeFn,
            Map<String, String> optionalStateParams
    ) {
        String csrfToken = new BigInteger(130, new SecureRandom()).toString(32);
        String nonce = new BigInteger(130, new SecureRandom()).toString(32);

        // add csrfToken and nonce to the session - these are verified later in the flow (handleCallbackAndGetIdentity)
        sessionSetAttributeFn.accept(OAUTH_CSRF_TOKEN_SESSION_ATTRIBUTE_NAME, csrfToken);
        sessionSetAttributeFn.accept(OAUTH_NONCE_SESSION_ATTRIBUTE_NAME, nonce);

        String state = String.format("%s=%s", CSRF_TOKEN_STATE_PARAMETER_NAME, csrfToken);

        // extra parameters that allow us to recover the context when the user returns to our application (eg: action and token for invitations)
        if (optionalStateParams != null && !optionalStateParams.isEmpty()) {
            state += "&" + optionalStateParams.entrySet().stream()
                    .map(e -> String.format("%s=%s", e.getKey(), e.getValue()))
                    .collect(Collectors.joining("&"));
        }

        logger.info("Google OAuth redirect_uri is: " + redirectUrl);
//        logger.info("Google OAuth state is: " + state);
//        logger.info("Google OAuth nonce is: " + nonce);

        UriBuilder authorizationUriBuilder = UriBuilder.fromUri(CATMAPropertyKey.GOOGLE_OAUTH_AUTHORIZATION_CODE_REQUEST_URL.getValue());
        authorizationUriBuilder.queryParam("client_id", CATMAPropertyKey.GOOGLE_OAUTH_CLIENT_ID.getValue());
        authorizationUriBuilder.queryParam("response_type", "code");
        authorizationUriBuilder.queryParam("scope", "openid email");
        // NB: the redirect_uri must be exactly as configured for the OAuth client in Google Cloud Console! (https://console.cloud.google.com/auth/clients)
        // it can't contain dynamic path-parts, URL fragments, relative paths or wildcards, and can't be a public IP address
        authorizationUriBuilder.queryParam("redirect_uri", redirectUrl);
        authorizationUriBuilder.queryParam("state", state);
        authorizationUriBuilder.queryParam("nonce", nonce);

        return authorizationUriBuilder.build();
    }

    /**
     * Handles steps 3-5 (confirm anti-forgery state token [and nonce], exchange code for access token and ID token & obtain user information from the ID token)
     * as per <a href="https://developers.google.com/identity/openid-connect/openid-connect#authenticatingtheuser">Google's OpenID Connect Documentation</a>.
     *
     * @param authorizationCode the <code>code</code> parameter from the response from Google
     * @param state the <code>state</code> parameter from the response from Google
     * @param error the <code>error</code> parameter from the response from Google
     * @param redirectUrl this must be the same URL that was passed to <a href="#getOauthAuthorizationRequestUri">getOauthAuthorizationRequestUri</a>
     * @param httpClient a {@link CloseableHttpClient} instance that will be used to make an HTTP request to Google
     * @param sessionGetAttributeFn the function that will be used to retrieve items from the relevant server-side session
     * @param sessionSetAttributeFn the function that will be used to remove previously stored items from the relevant server-side session
     * @return a {@link Pair} containing an {@link OauthIdentity} record and a map of the optional state parameters that were passed to
     *         <code>getOauthAuthorizationRequestUri</code>
     * @throws OauthException if we receive an error from Google or if anything goes wrong while processing the callback
     */
    public static Pair<OauthIdentity, Map<String, String>> handleCallbackAndGetIdentity(
            @NotNull String authorizationCode, @NotNull String state, @NotNull String error,
            @NotNull String redirectUrl, @NotNull CloseableHttpClient httpClient,
            @NotNull Function<String, Object> sessionGetAttributeFn, @NotNull BiConsumer<String, Object> sessionSetAttributeFn
    ) throws OauthException {
        try {
            Object expectedCsrfToken = sessionGetAttributeFn.apply(OAUTH_CSRF_TOKEN_SESSION_ATTRIBUTE_NAME);
            Object expectedNonce = sessionGetAttributeFn.apply(OAUTH_NONCE_SESSION_ATTRIBUTE_NAME);

            if (expectedCsrfToken == null || expectedNonce == null) {
                throw new OauthException("Internal error: Either one of or both CSRF token and nonce were not present in the session");
            }

            if (error != null && !error.isEmpty()) {
                throw new OauthException("External error: " + error);
            }

            // yes, URLEncodedUtils.parse does not decode, strange but true
            List<NameValuePair> stateParams = URLEncodedUtils.parse(URLDecoder.decode(state, StandardCharsets.UTF_8), StandardCharsets.UTF_8);
            Map<String, String> stateParamsMap = stateParams.stream().collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));

            // verify csrf token
            // this is the only state parameter that we care about in this context, any others are not related to the OAuth flow
            // we remove it from the parameters map so that the remaining ones can be returned to the caller
            String receivedCsrfToken = stateParamsMap.remove(CSRF_TOKEN_STATE_PARAMETER_NAME);
            if (!receivedCsrfToken.equals(expectedCsrfToken)) {
                throw new OauthException("Internal error: CSRF token verification failed");
            }

            // exchange code for access token and ID token
            HttpPost httpPost = new HttpPost(CATMAPropertyKey.GOOGLE_OAUTH_ACCESS_TOKEN_REQUEST_URL.getValue());
            List <NameValuePair> requestData = new ArrayList<>();
            requestData.add(new BasicNameValuePair("code", authorizationCode));
            requestData.add(new BasicNameValuePair("grant_type", "authorization_code"));
            requestData.add(new BasicNameValuePair("client_id", CATMAPropertyKey.GOOGLE_OAUTH_CLIENT_ID.getValue()));
            requestData.add(new BasicNameValuePair("client_secret", CATMAPropertyKey.GOOGLE_OAUTH_CLIENT_SECRET.getValue()));
            // although there is no redirect happening as part of the access token request, this parameter needs to be added as it is mandatory,
            // and it needs to match the redirect_uri parameter of the preceding authentication request (see getOauthAuthorizationRequestUri)
            requestData.add(new BasicNameValuePair("redirect_uri", redirectUrl));
            httpPost.setEntity(new UrlEncodedFormEntity(requestData));

            CloseableHttpResponse response = httpClient.execute(httpPost);

            HttpEntity entity = response.getEntity();
            ByteArrayOutputStream bodyBuffer = new ByteArrayOutputStream();
            entity.writeTo(bodyBuffer);
//            logger.info("Google OAuth: Access token request result:\n" + bodyBuffer.toString(StandardCharsets.UTF_8));

            ObjectMapper mapper = new ObjectMapper();

            ObjectNode responseJson = mapper.readValue(bodyBuffer.toString(), ObjectNode.class);
            String idToken = responseJson.get("id_token").asText();
            String[] pieces = idToken.split("\\.");
            // we skip the header and go ahead with the payload (see the note under step 5 in the docs for why we don't need to validate the id_token here)
            String payload = pieces[1];
            String decodedPayload = new String(Base64.decodeBase64(payload), StandardCharsets.UTF_8);
//            logger.info("Google OAuth: Access token request result (decoded):\n" + decodedPayload);

            ObjectNode payloadJson = mapper.readValue(decodedPayload, ObjectNode.class);
            // verify nonce
            String receivedNonce = payloadJson.get("nonce").asText();
            if (!receivedNonce.equals(expectedNonce)) {
                throw new OauthException("Internal error: Nonce verification failed");
            }

            String identifier = payloadJson.get("sub").asText();
            String provider = "google_com";
            String email = payloadJson.get("email").asText();
            // here we generate a public name that does not reveal the user's identifier
            // TODO: review - this is nice in theory but doesn't do much in practice, as the identifier forms part of the username and we have to use this
            //       elsewhere (e.g. commits, backend files), so it is ultimately revealed anyway
            //       it's also pretty easy to guess someone's email address based on the public name in most cases, as this pattern is only used for Google
            //       logins
            //       lastly, we should probably allow users to choose their own username and change it if they wish (also because of Mattermost SSO)
            String name = String.format("%s@catma%s", email.substring(0, email.indexOf("@")), new Random().nextInt());

            return new Pair<>(new OauthIdentity(identifier, provider, email, name), stateParamsMap);
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, "Google OAuth: " + e.getMessage(), e);
            // purposefully not providing the caller with the exact reason to prevent error details being returned to the client
            throw new OauthException("Authentication failed, inspect logs");
        }
        finally {
            // clear the session attributes (prevents replay attacks)
            sessionSetAttributeFn.accept(OAUTH_CSRF_TOKEN_SESSION_ATTRIBUTE_NAME, null);
            sessionSetAttributeFn.accept(OAUTH_NONCE_SESSION_ATTRIBUTE_NAME, null);
        }
    }
}
