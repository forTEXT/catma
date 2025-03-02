package de.catma.api.pre.oauth;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.jboss.aerogear.security.otp.Totp;
import org.jboss.aerogear.security.otp.api.Clock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.catma.api.pre.PreApplication;
import de.catma.api.pre.oauth.interfaces.HttpClientFactory;
import de.catma.api.pre.oauth.interfaces.OauthHandler;
import de.catma.api.pre.oauth.interfaces.SessionStorageHandler;
import de.catma.properties.CATMAPropertyKey;

public class GoogleOauthHandler implements OauthHandler {
	private final static String OAUTH_TOKEN_SESION_ATTRIBUTE_NAME = "OAUTHTOKEN";

	private final Logger logger = Logger.getLogger(GoogleOauthHandler.class.getName());
	
	@Inject
	private HttpClientFactory httpClientProvider;
	
	@Inject 
	private SessionStorageHandler sessionStorageHandler;

	@Override
	public URI getAuthorizationUri(String oauthToken) throws Exception {
        sessionStorageHandler.put(OAUTH_TOKEN_SESION_ATTRIBUTE_NAME, oauthToken);

        Totp totp = new Totp(
                CATMAPropertyKey.OTP_SECRET.getValue() + oauthToken,
                new Clock(CATMAPropertyKey.OTP_DURATION.getIntValue())
        );
        
        String state = totp.now();
        
        String redirectUrl = String.format("%s%s/%s/%s/", 
        		CATMAPropertyKey.API_BASE_URL.getValue(), 
        		PreApplication.API_PACKAGE, 
        		PreApplication.API_VERSION, 
        		"auth");
        		
        String authorizationUrl = String.format(
                // note %% escape, otherwise an exception is thrown
                "%s?client_id=%s&response_type=code&scope=openid%%20email&redirect_uri=%s&state=%s",
                CATMAPropertyKey.GOOGLE_OAUTH_AUTHORIZATION_CODE_REQUEST_URL.getValue(),
                CATMAPropertyKey.GOOGLE_OAUTH_CLIENT_ID.getValue(),
                URLEncoder.encode(redirectUrl, "UTF-8"),
                state
        );
		return new URI(authorizationUrl);
	}
	
	@Override
	public OauthIdentity getIdentity(String oauthAuthorizationCode, String otpTimestamp) throws IOException {
		
		String oauthToken = (String) sessionStorageHandler.get(OAUTH_TOKEN_SESION_ATTRIBUTE_NAME);
		
		if (oauthToken == null) {
			throw new IllegalStateException("session does not contain an oauth token");
		}

		// validate the oauthToken
		Totp totp = new Totp(
				CATMAPropertyKey.OTP_SECRET.getValue() + oauthToken,
				new Clock(CATMAPropertyKey.OTP_DURATION.getIntValue())
		);

		if (!totp.verify(otpTimestamp)) {
			throw new IllegalStateException("state token verification failed");
		}

		// make access token request which provides the identity as a payload
		try (CloseableHttpClient httpclient = httpClientProvider.create()) {
			
			HttpPost httpPost = new HttpPost(CATMAPropertyKey.GOOGLE_OAUTH_ACCESS_TOKEN_REQUEST_URL.getValue());
			List <NameValuePair> data = new ArrayList<>();
			data.add(new BasicNameValuePair("code", oauthAuthorizationCode));
			data.add(new BasicNameValuePair("grant_type", "authorization_code"));
			data.add(new BasicNameValuePair("client_id", CATMAPropertyKey.GOOGLE_OAUTH_CLIENT_ID.getValue()));
			data.add(new BasicNameValuePair("client_secret", CATMAPropertyKey.GOOGLE_OAUTH_CLIENT_SECRET.getValue()));
			
			// Note: although there is no redirect happening in access token request, this parameter needs to be added as it is mandatory
			// and it needs to match the 'redirect_uri' from the preceding authorization code request (see getAuthorizationUri above)
	        String redirectUrl = String.format("%s%s/%s/%s/", 
	        		CATMAPropertyKey.API_BASE_URL.getValue(), 
	        		PreApplication.API_PACKAGE, 
	        		PreApplication.API_VERSION, 
	        		"auth");
			data.add(new BasicNameValuePair("redirect_uri", redirectUrl));
			httpPost.setEntity(new UrlEncodedFormEntity(data));
	
			CloseableHttpResponse tokenRequestResponse = httpclient.execute(httpPost);
	
			HttpEntity entity = tokenRequestResponse.getEntity();
			InputStream content = entity.getContent();
			ByteArrayOutputStream bodyBuffer = new ByteArrayOutputStream();
			IOUtils.copy(content, bodyBuffer);
	
			logger.info("Google OAuth access token request result: " + bodyBuffer.toString(StandardCharsets.UTF_8.name()));
	
			ObjectMapper mapper = new ObjectMapper();
	
			ObjectNode accessTokenResponseJson = mapper.readValue(bodyBuffer.toString(), ObjectNode.class);
			String idToken = accessTokenResponseJson.get("id_token").asText();
			String[] pieces = idToken.split("\\.");
			// we skip the header and go ahead with the payload
			String payload = pieces[1];
			String decodedPayload = new String(Base64.getDecoder().decode(payload), StandardCharsets.UTF_8);
			logger.info("Google OAuth access token request result (decoded): " + decodedPayload);
	
			sessionStorageHandler.put(OAUTH_TOKEN_SESION_ATTRIBUTE_NAME, null);
			
			ObjectNode payloadJson = mapper.readValue(decodedPayload, ObjectNode.class);
			String identifier = payloadJson.get("sub").asText();
			String provider = "google_com";
			String email = payloadJson.get("email").asText();
			String name = email.substring(0, email.indexOf("@")) + "@catma" + new Random().nextInt(); 
			return new OauthIdentity(identifier, provider, email, name);
		}
	}
}
