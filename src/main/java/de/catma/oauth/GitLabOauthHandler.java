package de.catma.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.catma.properties.CATMAPropertyKey;
import de.catma.util.Pair;
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
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Based on: <a href="https://docs.gitlab.com/api/oauth2/">GitLab's OAuth 2.0 identity provider documentation</a>
 * <p>
 * This replaces the resource owner password credentials (ROPC) grant, which GitLab removed in version 19.0. The basic flow is as follows:
 * 1. <code>getOauthAuthorizationRequestUri</code> is called and the client is redirected to the resultant URI (GitLab login/consent page - the consent step is
 *    skipped because our application is registered as "trusted").
 * 2. We receive a callback request from GitLab at the <code>redirectUrl</code> with parameters "code", "state" and "error".
 *    These should be supplied to <code>handleCallbackAndGetTokens</code> to produce a {@link GitLabOauthTokens} record, which is then usually wrapped in a
 *    {@link GitLabOauthTokenProvider} so that the access token can be refreshed for the lifetime of the session.
 * <p>
 * Unlike {@link GoogleOauthHandler} we don't request the "openid" scope and therefore don't receive an ID token, so there is no nonce to verify - the identity
 * of the user is established by calling GitLab's /user endpoint with the access token (see
 * {@link de.catma.repository.git.managers.GitlabManagerRestricted}).
 */
public class GitLabOauthHandler {
    // 'api' grants access to the REST API, 'write_repository' grants access to Git over HTTPS
    // (see https://docs.gitlab.com/integration/oauth_provider/#view-all-authorized-applications for the full list of scopes)
    private static final String SCOPES = "api write_repository";

    private static final Logger logger = Logger.getLogger(GitLabOauthHandler.class.getName());

    /**
     * Creates an anti-forgery state token and builds the authorization request URI.
     * <p>
     * Note that we only build the URI here - the caller is responsible for redirecting the client.
     *
     * @param redirectUrl the HTTP endpoint where we will receive the response from GitLab. This must be one of the redirect URIs configured for the OAuth
     *                    application on the GitLab server!
     * @param sessionSetAttributeFn the function that will be used to store some items in the relevant server-side session for later verification
     * @param optionalStateParams an optional map of parameters that can be used to recover the context when the user returns to our application
     * @return the {@link URI} that the client should be redirected to
     */
    public static URI getOauthAuthorizationRequestUri(
            @NotNull String redirectUrl, @NotNull BiConsumer<String, Object> sessionSetAttributeFn,
            Map<String, String> optionalStateParams
    ) {
        String csrfToken = new BigInteger(130, new SecureRandom()).toString(32);

        // add the csrfToken to the session - it is verified later in the flow (handleCallbackAndGetTokens)
        // the provider allows the callback to be dispatched to this handler rather than GoogleOauthHandler
        sessionSetAttributeFn.accept(OauthConstants.OAUTH_CSRF_TOKEN_SESSION_ATTRIBUTE_NAME, csrfToken);
        sessionSetAttributeFn.accept(OauthConstants.OAUTH_PROVIDER_SESSION_ATTRIBUTE_NAME, OauthConstants.OauthProvider.GITLAB.name());

        String state = String.format("%s=%s", OauthConstants.CSRF_TOKEN_STATE_PARAMETER_NAME, csrfToken);

        // extra parameters that allow us to recover the context when the user returns to our application (eg: action and token for invitations)
        if (optionalStateParams != null && !optionalStateParams.isEmpty()) {
            state += "&" + optionalStateParams.entrySet().stream()
                    .map(e -> String.format("%s=%s", e.getKey(), e.getValue()))
                    .collect(Collectors.joining("&"));
        }

        logger.info("GitLab OAuth redirect_uri is: " + redirectUrl);

        UriBuilder authorizationUriBuilder = UriBuilder.fromUri(getAuthorizationUrl());
        authorizationUriBuilder.queryParam("client_id", CATMAPropertyKey.GITLAB_OAUTH_CLIENT_ID.getValue());
        authorizationUriBuilder.queryParam("response_type", "code");
        authorizationUriBuilder.queryParam("scope", SCOPES);
        // NB: the redirect_uri must be one of those configured for the OAuth application on the GitLab server!
        authorizationUriBuilder.queryParam("redirect_uri", redirectUrl);
        authorizationUriBuilder.queryParam("state", state);

        return authorizationUriBuilder.build();
    }

    /**
     * Confirms the anti-forgery state token and exchanges the authorization code for an access token and a refresh token.
     *
     * @param authorizationCode the <code>code</code> parameter from the response from GitLab
     * @param state the <code>state</code> parameter from the response from GitLab
     * @param error the <code>error</code> parameter from the response from GitLab
     * @param redirectUrl this must be the same URL that was passed to <a href="#getOauthAuthorizationRequestUri">getOauthAuthorizationRequestUri</a>
     * @param httpClient a {@link CloseableHttpClient} instance that will be used to make an HTTP request to GitLab
     * @param sessionGetAttributeFn the function that will be used to retrieve items from the relevant server-side session
     * @param sessionSetAttributeFn the function that will be used to remove previously stored items from the relevant server-side session
     * @return a {@link Pair} containing a {@link GitLabOauthTokens} record and a map of the optional state parameters that were passed to
     *         <code>getOauthAuthorizationRequestUri</code>
     * @throws OauthException if we receive an error from GitLab or if anything goes wrong while processing the callback
     */
    public static Pair<GitLabOauthTokens, Map<String, String>> handleCallbackAndGetTokens(
            @NotNull String authorizationCode, @NotNull String state, String error,
            @NotNull String redirectUrl, @NotNull CloseableHttpClient httpClient,
            @NotNull Function<String, Object> sessionGetAttributeFn, @NotNull BiConsumer<String, Object> sessionSetAttributeFn
    ) throws OauthException {
        try {
            Object expectedCsrfToken = sessionGetAttributeFn.apply(OauthConstants.OAUTH_CSRF_TOKEN_SESSION_ATTRIBUTE_NAME);

            if (expectedCsrfToken == null) {
                throw new OauthException("Internal error: CSRF token was not present in the session");
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
            String receivedCsrfToken = stateParamsMap.remove(OauthConstants.CSRF_TOKEN_STATE_PARAMETER_NAME);
            if (!expectedCsrfToken.equals(receivedCsrfToken)) {
                throw new OauthException("Internal error: CSRF token verification failed");
            }

            List<NameValuePair> requestData = new ArrayList<>();
            requestData.add(new BasicNameValuePair("grant_type", "authorization_code"));
            requestData.add(new BasicNameValuePair("code", authorizationCode));
            requestData.add(new BasicNameValuePair("redirect_uri", redirectUrl));

            return new Pair<>(requestTokens(requestData, httpClient), stateParamsMap);
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, "GitLab OAuth: " + e.getMessage(), e);
            // purposefully not providing the caller with the exact reason to prevent error details being returned to the client
            throw new OauthException("Authentication failed, inspect logs");
        }
        finally {
            // clear the session attributes (prevents replay attacks)
            sessionSetAttributeFn.accept(OauthConstants.OAUTH_CSRF_TOKEN_SESSION_ATTRIBUTE_NAME, null);
            sessionSetAttributeFn.accept(OauthConstants.OAUTH_PROVIDER_SESSION_ATTRIBUTE_NAME, null);
        }
    }

    /**
     * Exchanges a refresh token for a new pair of tokens.
     * <p>
     * NB: GitLab invalidates both the old access token and the old refresh token in the process, so callers must ensure that this doesn't happen concurrently
     * for the same session (see {@link GitLabOauthTokenProvider}).
     *
     * @param refreshToken the refresh token
     * @param redirectUrl the redirect URL that was used when the tokens were originally obtained
     * @param httpClient a {@link CloseableHttpClient} instance that will be used to make an HTTP request to GitLab
     * @return a new {@link GitLabOauthTokens} record
     * @throws IOException if the tokens couldn't be refreshed
     */
    public static GitLabOauthTokens refreshTokens(
            @NotNull String refreshToken, @NotNull String redirectUrl, @NotNull CloseableHttpClient httpClient
    ) throws IOException {
        List<NameValuePair> requestData = new ArrayList<>();
        requestData.add(new BasicNameValuePair("grant_type", "refresh_token"));
        requestData.add(new BasicNameValuePair("refresh_token", refreshToken));
        requestData.add(new BasicNameValuePair("redirect_uri", redirectUrl));

        return requestTokens(requestData, httpClient);
    }

    private static GitLabOauthTokens requestTokens(List<NameValuePair> requestData, CloseableHttpClient httpClient) throws IOException {
        HttpPost httpPost = new HttpPost(getTokenUrl());

        requestData.add(new BasicNameValuePair("client_id", CATMAPropertyKey.GITLAB_OAUTH_CLIENT_ID.getValue()));
        requestData.add(new BasicNameValuePair("client_secret", CATMAPropertyKey.GITLAB_OAUTH_CLIENT_SECRET.getValue()));
        httpPost.setEntity(new UrlEncodedFormEntity(requestData));

        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            HttpEntity entity = response.getEntity();
            ByteArrayOutputStream bodyBuffer = new ByteArrayOutputStream();
            entity.writeTo(bodyBuffer);

            ObjectNode responseJson = new ObjectMapper().readValue(bodyBuffer.toString(StandardCharsets.UTF_8), ObjectNode.class);

            if (!responseJson.has("access_token")) {
                // don't log the response body, it could contain sensitive information
                throw new IOException(
                        String.format(
                                "GitLab's token endpoint did not return an access token (HTTP status %d, error: %s)",
                                response.getStatusLine().getStatusCode(),
                                responseJson.has("error") ? responseJson.get("error").asText() : "n/a"
                        )
                );
            }

            return new GitLabOauthTokens(
                    responseJson.get("access_token").asText(),
                    responseJson.has("refresh_token") ? responseJson.get("refresh_token").asText() : null,
                    // GitLab's default access token lifetime is 2 hours, but instance administrators can configure it (GitLab 19.1 and later)
                    Instant.now().plusSeconds(responseJson.has("expires_in") ? responseJson.get("expires_in").asLong() : 7200L)
            );
        }
    }

    private static String getGitLabBaseUrl() {
        String gitLabServerUrl = CATMAPropertyKey.GITLAB_SERVER_URL.getValue();
        // the property may or may not have a trailing slash, depending on the deployment
        return gitLabServerUrl.endsWith("/") ? gitLabServerUrl.substring(0, gitLabServerUrl.length() - 1) : gitLabServerUrl;
    }

    private static String getAuthorizationUrl() {
        return getGitLabBaseUrl() + "/oauth/authorize";
    }

    private static String getTokenUrl() {
        return getGitLabBaseUrl() + "/oauth/token";
    }
}
