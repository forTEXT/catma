package de.catma.api.pre.fixture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.mockito.Mockito;

import de.catma.api.pre.backend.interfaces.RemoteGitManagerPrivilegedFactory;
import de.catma.api.pre.backend.interfaces.RemoteGitManagerRestrictedFactory;
import de.catma.api.pre.oauth.interfaces.HttpClientFactory;
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

	public static void setUpValidUsernamePasswordAuth(String dummyIdent,
			RemoteGitManagerRestrictedFactory remoteGitManagerRestrictedFactoryMock) throws IOException {

		RemoteGitManagerRestricted remoteGitManagerRestrictedMock = Mockito.mock(RemoteGitManagerRestricted.class);
		User userMock = Mockito.mock(User.class);
		
		when(remoteGitManagerRestrictedFactoryMock.create(anyString(), anyString())).thenReturn(remoteGitManagerRestrictedMock);

		when(remoteGitManagerRestrictedMock.getUser()).thenReturn(userMock);
		when(remoteGitManagerRestrictedMock.getUsername()).thenReturn(dummyIdent);
		
		when(userMock.getIdentifier()).thenReturn(dummyIdent);		
	}

	public static void setUpValidThirdPartyOauth(
			String dummyIdent, 
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
					"email": "dummy@dummy.org"
				}
				""".formatted(dummyIdent);
		
		byte[] encodedPayload = Base64.getEncoder().encode(payLoad.getBytes(StandardCharsets.UTF_8));
		
		when(entityMock.getContent()).thenReturn(
				new ByteArrayInputStream(
						"""
						{
							"id_token": "1234.%s"
						}
						"""
						.formatted(new String(encodedPayload, StandardCharsets.UTF_8))
						.getBytes(StandardCharsets.UTF_8)));
		
		RemoteGitManagerPrivileged remoteGitManagerPrivilegedMock = Mockito.mock(RemoteGitManagerPrivileged.class);
		when(remoteGitManagerPrivilegedFactoryMock.create()).thenReturn(remoteGitManagerPrivilegedMock);
		when(remoteGitManagerPrivilegedMock.acquireImpersonationToken(any(), any(), any(), any())).thenReturn(new Pair<>(null, "my_impersonation_token"));
		
		RemoteGitManagerRestricted remoteGitManagerRestrictedMock = Mockito.mock(RemoteGitManagerRestricted.class);
		User userMock = Mockito.mock(User.class);
		
		when(remoteGitManagerRestrictedFactoryMock.create(anyString())).thenReturn(remoteGitManagerRestrictedMock);

		when(remoteGitManagerRestrictedMock.getUser()).thenReturn(userMock);
		when(remoteGitManagerRestrictedMock.getUsername()).thenReturn(dummyIdent);
		
		when(userMock.getIdentifier()).thenReturn(dummyIdent);		
	}
}
