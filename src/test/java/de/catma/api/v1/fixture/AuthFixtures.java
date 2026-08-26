package de.catma.api.v1.fixture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import de.catma.api.v1.backend.interfaces.RemoteGitManagerPrivilegedFactory;
import de.catma.api.v1.backend.interfaces.RemoteGitManagerRestrictedFactory;
import de.catma.api.v1.oauth.interfaces.HttpClientFactory;
import de.catma.oauth.GitLabOauthTokenProvider;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerPrivileged;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerRestricted;
import de.catma.user.User;
import de.catma.util.Pair;

public class AuthFixtures {

	public static void setUpValidTokenAuth(String dummyIdent,
			RemoteGitManagerRestrictedFactory remoteGitManagerRestrictedFactoryMock) throws IOException {
		
		RemoteGitManagerRestricted remoteGitManagerRestrictedMock = Mockito.mock(RemoteGitManagerRestricted.class);
		User userMock = Mockito.mock(User.class);
		
		when(remoteGitManagerRestrictedFactoryMock.create(anyString())).thenReturn(remoteGitManagerRestrictedMock);

		when(remoteGitManagerRestrictedMock.getUser()).thenReturn(userMock);
		when(remoteGitManagerRestrictedMock.getUsername()).thenReturn(dummyIdent);
		
		when(userMock.getIdentifier()).thenReturn(dummyIdent);
		
	}

	public static void setUpValidGitLabOauth(
			String dummyIdent,
			RemoteGitManagerRestrictedFactory remoteGitManagerRestrictedFactoryMock,
			HttpClientFactory httpClientFactoryMock) throws IOException {

		// GitLab's token endpoint response - unlike the Google flow there is no ID token, the identity comes from the RemoteGitManagerRestricted
		mockHttpResponseBody(
				httpClientFactoryMock,
				"""
				{
					"access_token": "dummy_access_token",
					"token_type": "bearer",
					"expires_in": 7200,
					"refresh_token": "dummy_refresh_token",
					"created_at": 1607635748
				}
				"""
		);

		RemoteGitManagerRestricted remoteGitManagerRestrictedMock = Mockito.mock(RemoteGitManagerRestricted.class);
		User userMock = Mockito.mock(User.class);

		when(remoteGitManagerRestrictedFactoryMock.create(any(GitLabOauthTokenProvider.class))).thenReturn(remoteGitManagerRestrictedMock);

		when(remoteGitManagerRestrictedMock.getUser()).thenReturn(userMock);
		when(remoteGitManagerRestrictedMock.getUsername()).thenReturn(dummyIdent);

		when(userMock.getIdentifier()).thenReturn(dummyIdent);
	}

	/**
	 * Mocks the {@link HttpClientFactory} so that any request made through it responds with <code>responseBody</code>.
	 */
	private static void mockHttpResponseBody(HttpClientFactory httpClientFactoryMock, String responseBody) throws IOException {
		CloseableHttpClient httpClientMock = Mockito.mock(CloseableHttpClient.class);

		when(httpClientFactoryMock.create()).thenReturn(httpClientMock);

		CloseableHttpResponse responseMock = Mockito.mock(CloseableHttpResponse.class);
		when(httpClientMock.execute(any())).thenReturn(responseMock);

		HttpEntity entityMock = Mockito.mock(HttpEntity.class);
		when(responseMock.getEntity()).thenReturn(entityMock);

		ByteArrayInputStream responseBodyInputStream = new ByteArrayInputStream(responseBody.getBytes(StandardCharsets.UTF_8));

		// if HttpEntity.getContent() is called
		when(entityMock.getContent()).thenReturn(responseBodyInputStream);

		// if HttpEntity.writeTo() is called (needs to be mocked differently because it is a void method, see
		// https://javadoc.io/doc/org.mockito/mockito-core/5.17.0/org.mockito/org/mockito/Mockito.html#12)
		doAnswer(new Answer() {
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				OutputStream os = (OutputStream) args[0];
				os.write(responseBodyInputStream.readAllBytes());
				return null;
			}
		}).when(entityMock).writeTo(any());
	}

	public static void setUpValidThirdPartyOauth(
			String dummyIdent,
			String nonce,
			RemoteGitManagerPrivilegedFactory remoteGitManagerPrivilegedFactoryMock, RemoteGitManagerRestrictedFactory remoteGitManagerRestrictedFactoryMock,
			HttpClientFactory httpClientFactoryMock) throws IOException {
		CloseableHttpClient httpClientMock = Mockito.mock(CloseableHttpClient.class);
		
		when(httpClientFactoryMock.create()).thenReturn(httpClientMock);
		
		CloseableHttpResponse responseMock = Mockito.mock(CloseableHttpResponse.class);
		when(httpClientMock.execute(any())).thenReturn(responseMock);
		
		HttpEntity entityMock = Mockito.mock(HttpEntity.class);
		when(responseMock.getEntity()).thenReturn(entityMock);
		
		String payLoad = 
				"""
				{
					"sub": "%s",
					"email": "dummy@dummy.org",
					"nonce": "%s"
				}
				""".formatted(dummyIdent, nonce);
		
		byte[] encodedPayload = Base64.getEncoder().encode(payLoad.getBytes(StandardCharsets.UTF_8));

		ByteArrayInputStream idTokenInputStream = new ByteArrayInputStream(
				"""
				{
					"id_token": "dummyHeader.%s.dummySignature"
				}
				""".formatted(new String(encodedPayload, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8)
		);

		// if HttpEntity.getContent() is called
		when(entityMock.getContent()).thenReturn(idTokenInputStream);

		// if HttpEntity.writeTo() is called (needs to be mocked differently because it is a void method, see
		// https://javadoc.io/doc/org.mockito/mockito-core/5.17.0/org.mockito/org/mockito/Mockito.html#12)
		doAnswer(new Answer() {
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				OutputStream os = (OutputStream) args[0];
				os.write(idTokenInputStream.readAllBytes());
				return null;
			}
		}).when(entityMock).writeTo(any());

		RemoteGitManagerPrivileged remoteGitManagerPrivilegedMock = Mockito.mock(RemoteGitManagerPrivileged.class);
		when(remoteGitManagerPrivilegedFactoryMock.create()).thenReturn(remoteGitManagerPrivilegedMock);
		when(remoteGitManagerPrivilegedMock.acquireImpersonationToken(any(), any(), any(), any())).thenReturn(new Pair<>(null, "dummy_impersonation_token"));
		
		RemoteGitManagerRestricted remoteGitManagerRestrictedMock = Mockito.mock(RemoteGitManagerRestricted.class);
		User userMock = Mockito.mock(User.class);
		
		when(remoteGitManagerRestrictedFactoryMock.create(anyString())).thenReturn(remoteGitManagerRestrictedMock);

		when(remoteGitManagerRestrictedMock.getUser()).thenReturn(userMock);
		when(remoteGitManagerRestrictedMock.getUsername()).thenReturn(dummyIdent);
		
		when(userMock.getIdentifier()).thenReturn(dummyIdent);		
	}
}
