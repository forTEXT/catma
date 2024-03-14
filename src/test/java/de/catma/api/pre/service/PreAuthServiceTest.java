package de.catma.api.pre.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.gitlab4j.api.GitLabApiException;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.SignedJWT;

import de.catma.api.pre.PreApplication;
import de.catma.api.pre.backend.interfaces.RemoteGitManagerPrivilegedFactory;
import de.catma.api.pre.backend.interfaces.RemoteGitManagerRestrictedFactory;
import de.catma.api.pre.fixture.AuthFixtures;
import de.catma.api.pre.oauth.HashMapSessionStorageHandler;
import de.catma.api.pre.oauth.interfaces.HttpClientFactory;
import de.catma.api.pre.oauth.interfaces.SessionStorageHandler;
import de.catma.properties.CATMAProperties;
import de.catma.properties.CATMAPropertyKey;

class PreAuthServiceTest extends JerseyTest {
	
	private RemoteGitManagerRestrictedFactory remoteGitManagerRestrictedFactoryMock = Mockito.mock(RemoteGitManagerRestrictedFactory.class);
	private RemoteGitManagerPrivilegedFactory remoteGitManagerPrivilegedFactoryMock = Mockito.mock(RemoteGitManagerPrivilegedFactory.class);
	
	private HttpClientFactory httpClientFactoryMock = Mockito.mock(HttpClientFactory.class); 
	
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
		properties.setProperty(CATMAPropertyKey.API_HMAC_SECRET.name(), "mySecret".repeat(4));
		properties.setProperty(CATMAPropertyKey.API_BASE_URL.name(), "http://test.local/api");
		properties.setProperty(CATMAPropertyKey.API_GIT_REPOSITORY_BASE_PATH.name(), System.getProperty("java.io.tmpdir"));
		properties.setProperty(CATMAPropertyKey.OTP_SECRET.name(), "the_otp_secret");
		properties.setProperty(CATMAPropertyKey.OTP_DURATION.name(), "600");
		properties.setProperty(CATMAPropertyKey.GOOGLE_OAUTH_AUTHORIZATION_CODE_REQUEST_URL.name(), "http://oauthprovider.local/authorize");
		properties.setProperty(CATMAPropertyKey.GOOGLE_OAUTH_CLIENT_ID.name(), "4711");
		properties.setProperty(CATMAPropertyKey.GOOGLE_OAUTH_ACCESS_TOKEN_REQUEST_URL.name(), "http://oauthprovider.local/access");
		properties.setProperty(CATMAPropertyKey.GOOGLE_OAUTH_CLIENT_SECRET.name(), "the_client_secret");
		
		CATMAProperties.INSTANCE.setProperties(properties);
	}
	

	@Test
	void authentificationWithTokenParamShouldReturnJwtWithIdentPayload() throws Exception {
		String dummyIdent = "dummyIdent";
		
		AuthFixtures.setUpValidTokenAuth(dummyIdent, remoteGitManagerRestrictedFactoryMock);
		
		Response response = target("auth").queryParam("accesstoken", "my personal token").request(MediaType.APPLICATION_JSON).get();
		
		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		
		String token = IOUtils.toString((InputStream)response.getEntity(), StandardCharsets.UTF_8);
		
		SignedJWT signedJWT = SignedJWT.parse(token);
		JWSVerifier verifier = new MACVerifier(CATMAPropertyKey.API_HMAC_SECRET.getValue());
		
		assertTrue(signedJWT.verify(verifier));
		
		String userIdentifier = signedJWT.getJWTClaimsSet().getSubject();
		
		assertEquals(dummyIdent, userIdentifier);
	}
	
	@Test
	void authentificationWithBearerAuthHeaderShouldReturnJwtWithIdentPayload() throws Exception {
		String dummyIdent = "dummyIdent";
		
		AuthFixtures.setUpValidTokenAuth(dummyIdent, remoteGitManagerRestrictedFactoryMock);
		
		Response response = target("auth")
				.request(MediaType.APPLICATION_JSON)
				.header("Authorization", String.format("Bearer %s", new String(Base64.getEncoder().encode("my_personal_token".getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8)))
				.get();
		
		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		
		String token = IOUtils.toString((InputStream)response.getEntity(), StandardCharsets.UTF_8);
		
		SignedJWT signedJWT = SignedJWT.parse(token);
		JWSVerifier verifier = new MACVerifier(CATMAPropertyKey.API_HMAC_SECRET.getValue());
		
		assertTrue(signedJWT.verify(verifier));
		
		String userIdentifier = signedJWT.getJWTClaimsSet().getSubject();
		
		assertEquals(dummyIdent, userIdentifier);
	}
	
	@Test
	void authentificationWithBasicAuthHeaderShouldReturnJwtWithIdentPayload() throws Exception {
		String dummyIdent = "dummyIdent";
		
		AuthFixtures.setUpValidUsernamePasswordAuth(dummyIdent, remoteGitManagerRestrictedFactoryMock);
		
		Response response = target("auth")
				.request(MediaType.APPLICATION_JSON)
				.header("Authorization", String.format("Basic %s", new String(Base64.getEncoder().encode("theusername:1234".getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8)))
				.get();
		
		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		
		String token = IOUtils.toString((InputStream)response.getEntity(), StandardCharsets.UTF_8);
		
		SignedJWT signedJWT = SignedJWT.parse(token);
		JWSVerifier verifier = new MACVerifier(CATMAPropertyKey.API_HMAC_SECRET.getValue());
		
		assertTrue(signedJWT.verify(verifier));
		
		String userIdentifier = signedJWT.getJWTClaimsSet().getSubject();
		
		assertEquals(dummyIdent, userIdentifier);
	}
	
	@Test
	void failedAuthentificationWithTokenShouldReturn403Forbidden() throws Exception {
		when(remoteGitManagerRestrictedFactoryMock.create(anyString())).thenThrow(
				new IOException(new GitLabApiException("unknown backend token")));

		Response response = target("auth").queryParam("accesstoken", "my personal token").request(MediaType.APPLICATION_JSON).get();
		
		assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
	}
	
	@Test
	void authentificationWithUserPasswordParamsShouldReturnJwtWithIdentPayload() throws Exception {
		String dummyIdent = "dummyIdent";

		AuthFixtures.setUpValidUsernamePasswordAuth(dummyIdent, remoteGitManagerRestrictedFactoryMock);
		
		Response response = target("auth").queryParam("username", "theusername").queryParam("password", "1234").request(MediaType.APPLICATION_JSON).get();
		
		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		
		String token = IOUtils.toString((InputStream)response.getEntity(), StandardCharsets.UTF_8);
		
		SignedJWT signedJWT = SignedJWT.parse(token);
		JWSVerifier verifier = new MACVerifier(CATMAPropertyKey.API_HMAC_SECRET.getValue());
		
		assertTrue(signedJWT.verify(verifier));
		
		String userIdentifier = signedJWT.getJWTClaimsSet().getSubject();
		
		assertEquals(dummyIdent, userIdentifier);
	}
	
	@Test
	void authentificationWithThirdPartyOauthShouldReturnJwtWithIdentPayload() throws Exception {
		String dummyIdent = "dummyIdent";
		
		AuthFixtures.setUpValidThirdPartyOauth(dummyIdent, remoteGitManagerPrivilegedFactoryMock, remoteGitManagerRestrictedFactoryMock, httpClientFactoryMock);
		
		Response authRedirectResponse = target("auth/google").request().get();
		assertEquals(Status.TEMPORARY_REDIRECT.getStatusCode(), authRedirectResponse.getStatus());
		assertTrue(authRedirectResponse.getLocation().toString().startsWith(CATMAPropertyKey.GOOGLE_OAUTH_AUTHORIZATION_CODE_REQUEST_URL.getValue()));
		String query = authRedirectResponse.getLocation().getQuery();
		String state = query.substring(query.indexOf("state=")+6);

		Response authResponse = target("auth").queryParam("code", "1234").queryParam("state", state).request().get();
		assertEquals(Status.OK.getStatusCode(), authResponse.getStatus());
		
		String token = IOUtils.toString((InputStream)authResponse.getEntity(), StandardCharsets.UTF_8);
		
		SignedJWT signedJWT = SignedJWT.parse(token);
		JWSVerifier verifier = new MACVerifier(CATMAPropertyKey.API_HMAC_SECRET.getValue());
		
		assertTrue(signedJWT.verify(verifier));
		
		String userIdentifier = signedJWT.getJWTClaimsSet().getSubject();
		
		assertEquals(dummyIdent, userIdentifier);
	}
	
	@Test
	void failedAuthentificationWithThirdPartyOauthShouldReturn403Forbidden() throws Exception {
		Response response = target("auth").queryParam("error", "authentication failed").request().get();
		
		assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
	}

	@Test
	void failedAuthentificationWithThirdPartyOauthShouldReturn403ForbiddenEvenWithAuthCode() throws Exception {
		Response response = target("auth").queryParam("code", "4711").queryParam("error", "authentication failed").request().get();
		
		assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
	}
	
	@Test
	void wrongStateWithAuthentificationWithThirdPartyOauthShouldReturn403Forbidden() throws Exception {
		String dummyIdent = "dummyIdent";
		
		AuthFixtures.setUpValidThirdPartyOauth(dummyIdent, remoteGitManagerPrivilegedFactoryMock, remoteGitManagerRestrictedFactoryMock, httpClientFactoryMock);
		
		Response authRedirectResponse = target("auth/google").request().get();
		assertEquals(Status.TEMPORARY_REDIRECT.getStatusCode(), authRedirectResponse.getStatus());
		assertTrue(authRedirectResponse.getLocation().toString().startsWith(CATMAPropertyKey.GOOGLE_OAUTH_AUTHORIZATION_CODE_REQUEST_URL.getValue()));
		
		// wrong state
		String state = "666";

		Response authResponse = target("auth").queryParam("code", "1234").queryParam("state", state).request().get();
		assertEquals(Status.FORBIDDEN.getStatusCode(), authResponse.getStatus());		
	}

}
