package de.catma.api.pre.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.google.gson.reflect.TypeToken;

import de.catma.api.pre.PreApplication;
import de.catma.api.pre.backend.interfaces.RemoteGitManagerRestrictedFactory;
import de.catma.api.pre.fixture.ProjectFixtures;
import de.catma.api.pre.serialization.model_wrappers.PreApiAnnotation;
import de.catma.api.pre.serialization.model_wrappers.PreApiAnnotationProperty;
import de.catma.api.pre.serialization.models.Export;
import de.catma.api.pre.serialization.models.ExportDocument;
import de.catma.project.ProjectReference;
import de.catma.properties.CATMAProperties;
import de.catma.properties.CATMAPropertyKey;
import de.catma.repository.git.serialization.SerializationHelper;
import de.catma.util.IDGenerator;

public class PreProjectServiceTest extends JerseyTest {

	private RemoteGitManagerRestrictedFactory remoteGitManagerRestrictedFactoryMock = Mockito.mock(RemoteGitManagerRestrictedFactory.class);
	
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
		
		CATMAProperties.INSTANCE.setProperties(properties);
	}
	
	@Test
	void shouldProduce403UnauthorizedWithoutAuthArgs() throws Exception {
		Response response = target("project").request(MediaType.APPLICATION_JSON).get();
		
		assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
	}
	
	@Test
	void shouldProduceProjectListWithJwtAccessTokenQueryParam() throws Exception {
		String personalAccessToken = "my_personal_token";
		List<ProjectReference> expectedList = ProjectFixtures.setUpProjectList(remoteGitManagerRestrictedFactoryMock, personalAccessToken);

		Response authResponse = target("auth").queryParam("accesstoken", personalAccessToken).request(MediaType.APPLICATION_JSON).get();
		
		String token = IOUtils.toString((InputStream)authResponse.getEntity(), StandardCharsets.UTF_8);

		Response response = target("project").queryParam("accesstoken", token).request(MediaType.APPLICATION_JSON).get();
		
		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		
		String serializedList = IOUtils.toString((InputStream)response.getEntity(), StandardCharsets.UTF_8);
		Type listType = new TypeToken<ArrayList<ProjectReference>>(){}.getType();
		ArrayList<ProjectReference> resultList = new SerializationHelper<ArrayList<ProjectReference>>().deserialize(serializedList, listType);

		assertEquals(expectedList, resultList);
	}
	
	@Test
	void shouldProduceProjectListWithJwtAccessTokenInBearerHeader() throws Exception {
		String personalAccessToken = "my_personal_token";
		List<ProjectReference> expectedList = ProjectFixtures.setUpProjectList(remoteGitManagerRestrictedFactoryMock, personalAccessToken);

		Response authResponse = target("auth").queryParam("accesstoken", personalAccessToken).request(MediaType.APPLICATION_JSON).get();
		
		String token = IOUtils.toString((InputStream)authResponse.getEntity(), StandardCharsets.UTF_8);

		Response response = 
				target("project")
				.request(MediaType.APPLICATION_JSON)
				.header("Authorization", String.format("Bearer %s", new String(Base64.getEncoder().encode(token.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8)))
				.get();
		
		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		
		String serializedList = IOUtils.toString((InputStream)response.getEntity(), StandardCharsets.UTF_8);
		Type listType = new TypeToken<ArrayList<ProjectReference>>(){}.getType();
		ArrayList<ProjectReference> resultList = new SerializationHelper<ArrayList<ProjectReference>>().deserialize(serializedList, listType);

		assertEquals(expectedList, resultList);
	}
	
	@Test
	void shouldProduceProjectListWithBackendAccessTokenInBearerHeader() throws Exception {
		String personalAccessToken = "my_personal_token";
		List<ProjectReference> expectedList = ProjectFixtures.setUpProjectList(remoteGitManagerRestrictedFactoryMock, personalAccessToken);

		Response response = 
				target("project")
				.request(MediaType.APPLICATION_JSON)
				.header("Authorization", String.format("Bearer %s", new String(Base64.getEncoder().encode(personalAccessToken.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8)))
				.get();
		
		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		
		String serializedList = IOUtils.toString((InputStream)response.getEntity(), StandardCharsets.UTF_8);
		Type listType = new TypeToken<ArrayList<ProjectReference>>(){}.getType();
		ArrayList<ProjectReference> resultList = new SerializationHelper<ArrayList<ProjectReference>>().deserialize(serializedList, listType);

		assertEquals(expectedList, resultList);
	}
	
	@Test
	void shouldProduceProjectListWithBackendAccessTokenQueryParam() throws Exception {
		String personalAccessToken = "my_personal_token";
		List<ProjectReference> expectedList = ProjectFixtures.setUpProjectList(remoteGitManagerRestrictedFactoryMock, personalAccessToken);

		Response response = target("project").queryParam("accesstoken", personalAccessToken).request(MediaType.APPLICATION_JSON).get();
		
		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		
		String serializedList = IOUtils.toString((InputStream)response.getEntity(), StandardCharsets.UTF_8);
		Type listType = new TypeToken<ArrayList<ProjectReference>>(){}.getType();
		ArrayList<ProjectReference> resultList = new SerializationHelper<ArrayList<ProjectReference>>().deserialize(serializedList, listType);

		assertEquals(expectedList, resultList);
	}

	@Test
	void shouldProduce403UnauthorizedWithWrongBackendAccessTokenQueryParam() throws Exception {
		ProjectFixtures.setUpProjectList(remoteGitManagerRestrictedFactoryMock, "my_personal_token");

		Response response = target("project").queryParam("accesstoken", "wrong_access_token").request(MediaType.APPLICATION_JSON).get();
		
		assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
	}
	
	@Test
	void shouldProduce404OnProjectNotFound() throws Exception {
		IDGenerator idGenerator = new IDGenerator();
		
		String namespace = "test_namespace";
		String projectName = "test_project";
		String projectId = idGenerator.generate(projectName);

		ProjectFixtures.setUpRemoteGitManagerThrowing404(remoteGitManagerRestrictedFactoryMock);
		Response authResponse = target("auth").queryParam("accesstoken", "my personal token").request(MediaType.APPLICATION_JSON).get();
		String token = IOUtils.toString((InputStream)authResponse.getEntity(), StandardCharsets.UTF_8);

		Response response = target("project/"+namespace+"/"+projectId).queryParam("accesstoken", token).request(MediaType.APPLICATION_JSON).get();
		assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
	}
	
	@Test
	void shouldProduceProjectExportWithJwtAccessTokenQueryParam() throws Exception {
		
		IDGenerator idGenerator = new IDGenerator();

		String namespace = "test_namespace";
		String projectName = "test_project";
		String projectId = idGenerator.generate(projectName);
		String sourceDocumentUuid = idGenerator.generateDocumentId();
		String tagId = idGenerator.generate();
		String tagName = "my tag";
		String annotationId = idGenerator.generate();
		String propertyName = "my property";
		String propertyValue = "value1";
		String tagsetId = idGenerator.generateTagsetId();
		String tagsetName = "my tagset";

		
		
		String annotatedPhrase = 
			ProjectFixtures.setUpFullProject(
					remoteGitManagerRestrictedFactoryMock, 
					namespace, projectId, projectName, 
					sourceDocumentUuid, 
					tagsetId, tagsetName,
					tagId, tagName, 
					annotationId, propertyName, propertyValue);
		
		
		Response authResponse = target("auth").queryParam("accesstoken", "my personal token").request(MediaType.APPLICATION_JSON).get();
		
		String token = IOUtils.toString((InputStream)authResponse.getEntity(), StandardCharsets.UTF_8);

		Response response = target("project/"+namespace+"/"+projectId).queryParam("accesstoken", token).request(MediaType.APPLICATION_JSON).get();
		
		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		
		
		Export export = new SerializationHelper<Export>().deserialize(IOUtils.toString((InputStream)response.getEntity(), StandardCharsets.UTF_8), Export.class);
		
		List<ExportDocument> exportDocuments = export.getExportDocuments();
		assertTrue(exportDocuments.size() == 1);

		ExportDocument exportDocument = export.getExportDocuments().get(0);
		
		
		
		assertEquals(sourceDocumentUuid, exportDocument.getSourceDocument().getId());
		assertTrue(exportDocument.getAnnotations().size() == 1);
		PreApiAnnotation annotation = exportDocument.getAnnotations().get(0);
		assertEquals(annotatedPhrase, annotation.getPhrase());
		assertEquals(annotationId, annotation.getId());
		assertEquals(tagId, annotation.getTagId());
		assertEquals(sourceDocumentUuid, annotation.getSourceDocumentId());
		assertEquals(tagName, annotation.getTagName());
		assertTrue(annotation.getProperties().size() == 1);
		PreApiAnnotationProperty property = annotation.getProperties().get(0);
		assertEquals(propertyName, property.getName());
		assertTrue(property.getValues().size() == 1);
		assertEquals(propertyValue, property.getValues().get(0));
	}
	
	@Test
	void shouldProduceDocumentContentWithJwtAccessTokenQueryParam() throws Exception {
		
		IDGenerator idGenerator = new IDGenerator();

		String namespace = "test_namespace";
		String projectName = "test_project";
		String projectId = idGenerator.generate(projectName);
		String sourceDocumentUuid = idGenerator.generateDocumentId();
		
		String expectedContent = 
			ProjectFixtures.setUpProjectWithDocument(
					remoteGitManagerRestrictedFactoryMock, 
					namespace, projectId, projectName, 
					sourceDocumentUuid);
		
		
		Response authResponse = target("auth").queryParam("accesstoken", "my personal token").request(MediaType.APPLICATION_JSON).get();
		
		String token = IOUtils.toString((InputStream)authResponse.getEntity(), StandardCharsets.UTF_8);

		Response response = target("project/"+namespace+"/"+projectId+"/doc/"+sourceDocumentUuid).queryParam("accesstoken", token).request(MediaType.APPLICATION_JSON).get();
		
		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		
		String content = IOUtils.toString((InputStream)response.getEntity(), StandardCharsets.UTF_8);
		
		assertEquals(expectedContent.substring(0, 10), content.substring(0,10));
	}
	
}
