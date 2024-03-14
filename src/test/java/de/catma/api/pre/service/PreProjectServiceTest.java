package de.catma.api.pre.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.google.gson.reflect.TypeToken;

import de.catma.api.pre.PreApplication;
import de.catma.api.pre.backend.interfaces.RemoteGitManagerRestrictedFactory;
import de.catma.api.pre.fixture.ProjectFixtures;
import de.catma.project.ProjectReference;
import de.catma.properties.CATMAProperties;
import de.catma.properties.CATMAPropertyKey;
import de.catma.repository.git.serialization.SerializationHelper;
import de.catma.util.IDGenerator;

public class PreProjectServiceTest extends JerseyTest {

	private RemoteGitManagerRestrictedFactory remoteGitManagerRestrictedFactoryMock = Mockito.mock(RemoteGitManagerRestrictedFactory.class);
//	private RemoteGitManagerPrivilegedFactory remoteGitManagerPrivilegedFactoryMock = Mockito.mock(RemoteGitManagerPrivilegedFactory.class);
//	
//	private HttpClientFactory httpClientFactoryMock = Mockito.mock(HttpClientFactory.class); 
	
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
//				bind(remoteGitManagerPrivilegedFactoryMock).to(RemoteGitManagerPrivilegedFactory.class).ranked(2);
//				bind(httpClientFactoryMock).to(HttpClientFactory.class).ranked(2);
//				bind(HashMapSessionStorageHandler.class).to(SessionStorageHandler.class).ranked(2);
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
//		properties.setProperty(CATMAPropertyKey.OTP_SECRET.name(), "the_otp_secret");
//		properties.setProperty(CATMAPropertyKey.OTP_DURATION.name(), "600");
//		properties.setProperty(CATMAPropertyKey.GOOGLE_OAUTH_AUTHORIZATION_CODE_REQUEST_URL.name(), "http://oauthprovider.local/authorize");
//		properties.setProperty(CATMAPropertyKey.GOOGLE_OAUTH_CLIENT_ID.name(), "4711");
//		properties.setProperty(CATMAPropertyKey.GOOGLE_OAUTH_ACCESS_TOKEN_REQUEST_URL.name(), "http://oauthprovider.local/access");
//		properties.setProperty(CATMAPropertyKey.GOOGLE_OAUTH_CLIENT_SECRET.name(), "the_client_secret");
		
		CATMAProperties.INSTANCE.setProperties(properties);
	}
	
	
	@Test
	void shouldProduceProjectListWithJwtAccessTokenQueryParam() throws Exception {
		List<ProjectReference> expectedList = ProjectFixtures.setUpProjectList(remoteGitManagerRestrictedFactoryMock);

		Response authResponse = target("auth").queryParam("accesstoken", "my personal token").request(MediaType.APPLICATION_JSON).get();
		
		String token = IOUtils.toString((InputStream)authResponse.getEntity(), StandardCharsets.UTF_8);

		Response response = target("project").queryParam("accesstoken", token).request(MediaType.APPLICATION_JSON).get();
		
		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		
		String serializedList = IOUtils.toString((InputStream)response.getEntity(), StandardCharsets.UTF_8);
		Type listType = new TypeToken<ArrayList<ProjectReference>>(){}.getType();
		ArrayList<ProjectReference> resultList = new SerializationHelper<ArrayList<ProjectReference>>().deserialize(serializedList, listType);

		assertEquals(expectedList, resultList);
	}
	
	@Test
	void shouldProduceProjectExportWithJwtAccessTokenQueryParam() throws Exception {
		String projectName = "test_project";
		String namespace = "test_namespace";
		
		IDGenerator idGenerator = new IDGenerator();
		String projectId = idGenerator.generate(projectName);
		
		InitCommand init = Git.init();
		File remoteGitDir = Paths.get(CATMAPropertyKey.API_GIT_REPOSITORY_BASE_PATH.getValue(), "remote", namespace, projectId).toFile();
		System.out.println(remoteGitDir.getAbsolutePath());
		if (remoteGitDir.exists()) {
			FileUtils.deleteDirectory(remoteGitDir);
		}
		remoteGitDir.mkdirs();
		init.setDirectory(remoteGitDir);
		try (Git gitApi = init.call()) {
			
			gitApi.commit().setAllowEmpty(true).setMessage("Created Project my_remote_project").setCommitter("dummyIdent", "dummy@dummy.org").call();
			
			
			
			gitApi.getRepository().close();
		}
		
		File localGitDir = Paths.get(CATMAPropertyKey.API_GIT_REPOSITORY_BASE_PATH.getValue(), "local", namespace, projectId).toFile();
		System.out.println(localGitDir.getAbsolutePath());
		if (localGitDir.exists()) {
			FileUtils.deleteDirectory(localGitDir);
		}
		localGitDir.mkdirs();
	
		CloneCommand cloneCommand = Git.cloneRepository();
		try (Git gitApi = cloneCommand.setURI(localGitDir.toURI().toString()).setDirectory(localGitDir).call()) {
			
		}
		
	}
	
	

}
