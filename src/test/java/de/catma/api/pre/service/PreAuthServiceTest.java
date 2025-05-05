package de.catma.api.pre.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.gitlab4j.api.GitLabApiException;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.SignedJWT;

import de.catma.api.pre.AuthConstants;
import de.catma.api.pre.PreApplication;
import de.catma.api.pre.backend.interfaces.RemoteGitManagerPrivilegedFactory;
import de.catma.api.pre.backend.interfaces.RemoteGitManagerRestrictedFactory;
import de.catma.api.pre.fixture.AuthFixtures;
import de.catma.api.pre.oauth.HashMapSessionStorageHandler;
import de.catma.api.pre.oauth.interfaces.HttpClientFactory;
import de.catma.api.pre.oauth.interfaces.SessionStorageHandler;
import de.catma.oauth.GoogleOauthHandler;
import de.catma.properties.CATMAProperties;
import de.catma.properties.CATMAPropertyKey;

class PreAuthServiceTest extends JerseyTest {
	private static final String AUTH_TARGET = AuthConstants.AUTH_SERVICE_PATH.substring(1);
	private static final String DUMMY_USER_IDENTIFIER = "dummyUserIdentifier";
	private static final String DUMMY_PERSONAL_ACCESS_TOKEN = "dummy_personal_access_token";

	private RemoteGitManagerRestrictedFactory remoteGitManagerRestrictedFactoryMock = Mockito.mock(RemoteGitManagerRestrictedFactory.class);
	private RemoteGitManagerPrivilegedFactory remoteGitManagerPrivilegedFactoryMock = Mockito.mock(RemoteGitManagerPrivilegedFactory.class);
	
	private HttpClientFactory httpClientFactoryMock = Mockito.mock(HttpClientFactory.class); 

	// if HttpServletRequest is injected into the service under test with the @Context annotation, then it will be null because the tests don't run in a servlet
	// environment (see this file @ cc6a16d6 for how to set up a servlet environment)
	@Override
	protected Application configure() {
		PreApplication app = new PreApplication();

		// try to make sure that the configured package to scan is as expected
		assertEquals("de.catma.api.pre", PreApplication.class.getPackage().getName());

		app.packages("de.catma.api.pre"); // the corresponding configuration for the production code is in the web.xml
		app.register(new AbstractBinder() {
			@Override
			protected void configure() {
				bind(remoteGitManagerRestrictedFactoryMock).to(RemoteGitManagerRestrictedFactory.class).ranked(2);
				bind(remoteGitManagerPrivilegedFactoryMock).to(RemoteGitManagerPrivilegedFactory.class).ranked(2);
				bind(httpClientFactoryMock).to(HttpClientFactory.class).ranked(2);
				bind(HashMapSessionStorageHandler.class).to(SessionStorageHandler.class).ranked(2);
			}
		});

		return app;
	}
	
	@BeforeAll
	static void setup() {
		Properties properties = new Properties();
		properties.setProperty(CATMAPropertyKey.API_HMAC_SECRET.name(), "dummy_hmac_secret".repeat(2));
		properties.setProperty(CATMAPropertyKey.API_GIT_REPOSITORY_BASE_PATH.name(), System.getProperty("java.io.tmpdir"));
		properties.setProperty(CATMAPropertyKey.GOOGLE_OAUTH_AUTHORIZATION_CODE_REQUEST_URL.name(), "http://oauthprovider.local/auth");
		properties.setProperty(CATMAPropertyKey.GOOGLE_OAUTH_ACCESS_TOKEN_REQUEST_URL.name(), "http://oauthprovider.local/token");
		properties.setProperty(CATMAPropertyKey.GOOGLE_OAUTH_CLIENT_ID.name(), "dummy_client_id");
		properties.setProperty(CATMAPropertyKey.GOOGLE_OAUTH_CLIENT_SECRET.name(), "dummy_client_secret");
		
		CATMAProperties.INSTANCE.setProperties(properties);
	}
	

	@Test
	void authentificationWithTokenParamShouldReturnJwtWithIdentPayload() throws Exception {
		AuthFixtures.setUpValidTokenAuth(DUMMY_USER_IDENTIFIER, remoteGitManagerRestrictedFactoryMock);
		
		Response response = target(AUTH_TARGET)
				.queryParam(AuthConstants.AUTH_ENDPOINT_TOKEN_PARAMETER_NAME, DUMMY_PERSONAL_ACCESS_TOKEN)
				.request().get();
		
		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		
		String token = IOUtils.toString((InputStream)response.getEntity(), StandardCharsets.UTF_8);
		
		SignedJWT signedJWT = SignedJWT.parse(token);
		JWSVerifier verifier = new MACVerifier(CATMAPropertyKey.API_HMAC_SECRET.getValue());
		
		assertTrue(signedJWT.verify(verifier));
		
		String userIdentifier = signedJWT.getJWTClaimsSet().getSubject();
		
		assertEquals(DUMMY_USER_IDENTIFIER, userIdentifier);
	}
	
	@Test
	void authentificationWithBearerAuthHeaderShouldReturnJwtWithIdentPayload() throws Exception {
		AuthFixtures.setUpValidTokenAuth(DUMMY_USER_IDENTIFIER, remoteGitManagerRestrictedFactoryMock);
		
		Response response = target(AUTH_TARGET)
				.request()
				.header(AuthConstants.AUTHORIZATION_HEADER_NAME, AuthConstants.AUTHENTICATION_SCHEME_BEARER_PREFIX + DUMMY_PERSONAL_ACCESS_TOKEN)
				.get();
		
		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		
		String token = IOUtils.toString((InputStream)response.getEntity(), StandardCharsets.UTF_8);
		
		SignedJWT signedJWT = SignedJWT.parse(token);
		JWSVerifier verifier = new MACVerifier(CATMAPropertyKey.API_HMAC_SECRET.getValue());
		
		assertTrue(signedJWT.verify(verifier));
		
		String userIdentifier = signedJWT.getJWTClaimsSet().getSubject();
		
		assertEquals(DUMMY_USER_IDENTIFIER, userIdentifier);
	}
	
	@Test
	void authentificationWithBasicAuthHeaderShouldReturnJwtWithIdentPayload() throws Exception {
		AuthFixtures.setUpValidUsernamePasswordAuth(DUMMY_USER_IDENTIFIER, remoteGitManagerRestrictedFactoryMock);
		
		Response response = target(AUTH_TARGET)
				.request()
				.header(
						AuthConstants.AUTHORIZATION_HEADER_NAME,
						AuthConstants.AUTHENTICATION_SCHEME_BASIC_PREFIX
								+ new String(Base64.getEncoder().encode("dummyUsername:dummyPassword".getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8)
				)
				.get();
		
		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		
		String token = IOUtils.toString((InputStream)response.getEntity(), StandardCharsets.UTF_8);
		
		SignedJWT signedJWT = SignedJWT.parse(token);
		JWSVerifier verifier = new MACVerifier(CATMAPropertyKey.API_HMAC_SECRET.getValue());
		
		assertTrue(signedJWT.verify(verifier));
		
		String userIdentifier = signedJWT.getJWTClaimsSet().getSubject();
		
		assertEquals(DUMMY_USER_IDENTIFIER, userIdentifier);
	}
	
	@Test
	void failedAuthentificationWithTokenShouldReturn401Unauthorized() throws Exception {
		when(remoteGitManagerRestrictedFactoryMock.create(anyString())).thenThrow(
				new IOException(new GitLabApiException("401 Unauthorized")));

		Response response = target(AUTH_TARGET)
				.queryParam(AuthConstants.AUTH_ENDPOINT_TOKEN_PARAMETER_NAME, DUMMY_PERSONAL_ACCESS_TOKEN)
				.request().get();
		
		assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
	}
	
	@Test
	void authentificationWithUserPasswordParamsShouldReturnJwtWithIdentPayload() throws Exception {
		AuthFixtures.setUpValidUsernamePasswordAuth(DUMMY_USER_IDENTIFIER, remoteGitManagerRestrictedFactoryMock);
		
		Response response = target(AUTH_TARGET)
				.queryParam("username", "dummyUsername")
				.queryParam("password", "dummyPassword")
				.request().get();
		
		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		
		String token = IOUtils.toString((InputStream)response.getEntity(), StandardCharsets.UTF_8);
		
		SignedJWT signedJWT = SignedJWT.parse(token);
		JWSVerifier verifier = new MACVerifier(CATMAPropertyKey.API_HMAC_SECRET.getValue());
		
		assertTrue(signedJWT.verify(verifier));
		
		String userIdentifier = signedJWT.getJWTClaimsSet().getSubject();
		
		assertEquals(DUMMY_USER_IDENTIFIER, userIdentifier);
	}
	
	@Test
	void successfulGoogleOauthAuthentificationShouldReturnJwtWithIdentPayload() throws Exception {
		Response authRedirectResponse = target(AUTH_TARGET + "/google").request().get();
		assertEquals(Status.TEMPORARY_REDIRECT.getStatusCode(), authRedirectResponse.getStatus());
		assertTrue(authRedirectResponse.getLocation().toString().startsWith(CATMAPropertyKey.GOOGLE_OAUTH_AUTHORIZATION_CODE_REQUEST_URL.getValue()));

		String query = authRedirectResponse.getLocation().getQuery();
		List<NameValuePair> queryParams = URLEncodedUtils.parse(URLDecoder.decode(query, StandardCharsets.UTF_8), StandardCharsets.UTF_8);
		Map<String, String> queryParamsMap = queryParams.stream().collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
		String state = queryParamsMap.get("state");
		String nonce = queryParamsMap.get("nonce");

		AuthFixtures.setUpValidThirdPartyOauth(
				DUMMY_USER_IDENTIFIER,
				nonce,
				remoteGitManagerPrivilegedFactoryMock,
				remoteGitManagerRestrictedFactoryMock,
				httpClientFactoryMock
		);

		Response authResponse = target(AUTH_TARGET + "/google/callback")
				.queryParam("code", "dummy_code")
				.queryParam("state", state)
				.request().get();
		assertEquals(Status.OK.getStatusCode(), authResponse.getStatus());
		
		String token = IOUtils.toString((InputStream)authResponse.getEntity(), StandardCharsets.UTF_8);
		
		SignedJWT signedJWT = SignedJWT.parse(token);
		JWSVerifier verifier = new MACVerifier(CATMAPropertyKey.API_HMAC_SECRET.getValue());
		
		assertTrue(signedJWT.verify(verifier));
		
		String userIdentifier = signedJWT.getJWTClaimsSet().getSubject();
		
		assertEquals(DUMMY_USER_IDENTIFIER, userIdentifier);
	}

	@Test
	void googleOauthCallbackWithoutRequiredParamsShouldReturn400BadRequest() {
		Response authResponse = target(AUTH_TARGET + "/google/callback").request().get(); // no 'code' or 'state' params
		assertEquals(Status.BAD_REQUEST.getStatusCode(), authResponse.getStatus());
	}

	@Test
	void googleOauthCallbackWithErrorFromGoogleShouldReturn500InternalServerError() {
		// unfortunately doesn't seem to work, or somehow incompatible with our logging setup
//		enable(TestProperties.RECORD_LOG_LEVEL); // not sure if needed
//		set(TestProperties.RECORD_LOG_LEVEL, Level.SEVERE.intValue());

		// need to do this so that the error is not due to missing session attributes (CSRF token and nonce)
		Response authRedirectResponse = target(AUTH_TARGET + "/google").request().get();

		Response response = target(AUTH_TARGET + "/google/callback")
				.queryParam("code", "dummy_code")
				.queryParam("state", "dummy_state")
				.queryParam("error", "Dummy error from Google")
				.request().get();
		assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());

		// would like to assert log output here...
//		List<LogRecord> logRecords = getLoggedRecords();
		// ...

		// any failure produces a 500 response, and GoogleOauthHandler purposefully doesn't provide the caller with the exact reason (2nd exception below)
		// the following exceptions should be logged:
		// 1. SEVERE: Google OAuth: External error: An error from Google
		// 2. SEVERE: Failed to process OAuth callback
		//    de.catma.oauth.OauthException: Authentication failed, inspect logs
	}

	@Test
	void googleOauthCallbackWithInvalidStateShouldReturn500InternalServerError() throws Exception {
		// need to do this so that the error is not due to missing session attributes (CSRF token and nonce)
		Response authRedirectResponse = target(AUTH_TARGET + "/google").request().get();

		String query = authRedirectResponse.getLocation().getQuery();
		List<NameValuePair> queryParams = URLEncodedUtils.parse(URLDecoder.decode(query, StandardCharsets.UTF_8), StandardCharsets.UTF_8);
		Map<String, String> queryParamsMap = queryParams.stream().collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
		String nonce = queryParamsMap.get("nonce");

		AuthFixtures.setUpValidThirdPartyOauth(
				DUMMY_USER_IDENTIFIER,
				nonce,
				remoteGitManagerPrivilegedFactoryMock,
				remoteGitManagerRestrictedFactoryMock,
				httpClientFactoryMock
		);

		String invalidState = String.format("%s=%s", GoogleOauthHandler.CSRF_TOKEN_STATE_PARAMETER_NAME, "invalid_state");

		Response authResponse = target(AUTH_TARGET + "/google/callback")
				.queryParam("code", "dummy_code")
				.queryParam("state", invalidState)
				.request().get();
		assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), authResponse.getStatus());

		// any failure produces a 500 response, and GoogleOauthHandler purposefully doesn't provide the caller with the exact reason (2nd exception below)
		// the following exceptions should be logged:
		// 1. SEVERE: Google OAuth: Internal error: CSRF token verification failed
		// 2. SEVERE: Failed to process OAuth callback
		//    de.catma.oauth.OauthException: Authentication failed, inspect logs
	}

	@Test
	void googleOauthCallbackWithInvalidNonceShouldReturn500InternalServerError() throws Exception {
		// need to do this so that the error is not due to missing session attributes (CSRF token and nonce)
		Response authRedirectResponse = target(AUTH_TARGET + "/google").request().get();

		String query = authRedirectResponse.getLocation().getQuery();
		List<NameValuePair> queryParams = URLEncodedUtils.parse(URLDecoder.decode(query, StandardCharsets.UTF_8), StandardCharsets.UTF_8);
		Map<String, String> queryParamsMap = queryParams.stream().collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
		String state = queryParamsMap.get("state");

		String invalidNonce = "invalid_nonce";
		AuthFixtures.setUpValidThirdPartyOauth(
				DUMMY_USER_IDENTIFIER,
				invalidNonce,
				remoteGitManagerPrivilegedFactoryMock,
				remoteGitManagerRestrictedFactoryMock,
				httpClientFactoryMock
		);

		Response authResponse = target(AUTH_TARGET + "/google/callback")
				.queryParam("code", "dummy_code")
				.queryParam("state", state)
				.request().get();
		assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), authResponse.getStatus());

		// any failure produces a 500 response, and GoogleOauthHandler purposefully doesn't provide the caller with the exact reason (2nd exception below)
		// the following exceptions should be logged:
		// 1. SEVERE: Google OAuth: Internal error: Nonce verification failed
		// 2. SEVERE: Failed to process OAuth callback
		//    de.catma.oauth.OauthException: Authentication failed, inspect logs
	}
}
