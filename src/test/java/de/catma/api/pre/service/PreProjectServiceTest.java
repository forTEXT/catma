package de.catma.api.pre.service;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.ServletDeploymentContext;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerFactory;
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

	// set up a servlet environment, otherwise the HttpServletRequest that is injected into the service under test with the @Context annotation will be null
	// ref: https://stackoverflow.com/a/29387230/207981
	// if a servlet environment is not needed you can simply override configure() (see file history)
	@Override
	protected TestContainerFactory getTestContainerFactory() {
		// couldn't get the Jetty test container to work as a servlet environment
		// it appears to need additional configuration (ref: https://github.com/eclipse-ee4j/jersey/issues/4625), but there is almost no documentation,
		// so I gave up and switched to Grizzly (the exact container technology shouldn't matter anyway)
		// if you want to give it a shot, uncomment/add the 'jersey-test-framework-provider-jetty' artifact in pom.xml, review the linked issue above,
		// and also see what little documentation does exist (it was entirely unhelpful at the time of writing this):
		//   1. the official docs: https://eclipse-ee4j.github.io/jersey.github.io/documentation/latest/test-framework.html
		//   2. the sample tests: https://github.com/eclipse-ee4j/jersey/tree/2.x/test-framework/providers/jetty/src/test/java/org/glassfish/jersey/test/jetty
//		return new JettyTestContainerFactory();

		return new GrizzlyWebTestContainerFactory();
	}

	@Override
	protected DeploymentContext configureDeployment() {
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

		return ServletDeploymentContext.forServlet(new ServletContainer(app)).build();
	}

	@BeforeAll
	static void setup() {
		Properties properties = new Properties();
		properties.setProperty(CATMAPropertyKey.API_HMAC_SECRET.name(), "mySecret".repeat(4));
		properties.setProperty(CATMAPropertyKey.API_GIT_REPOSITORY_BASE_PATH.name(), System.getProperty("java.io.tmpdir"));
		
		CATMAProperties.INSTANCE.setProperties(properties);
	}
	
	@Test
	void shouldProduce403UnauthorizedWithoutAuthArgs() throws Exception {
		Response response = target("projects").request(MediaType.APPLICATION_JSON).get();
		
		assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
	}
	
	@Test
	void shouldProduceProjectListWithJwtAccessTokenQueryParam() throws Exception {
		String personalAccessToken = "my_personal_token";
		List<ProjectReference> expectedList = ProjectFixtures.setUpProjectList(remoteGitManagerRestrictedFactoryMock, personalAccessToken);

		Response authResponse = target("auth").queryParam("accesstoken", personalAccessToken).request().get();
		
		String token = IOUtils.toString((InputStream)authResponse.getEntity(), StandardCharsets.UTF_8);

		Response response = target("projects").queryParam("accesstoken", token).request(MediaType.APPLICATION_JSON).get();
		
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

		Response authResponse = target("auth").queryParam("accesstoken", personalAccessToken).request().get();
		
		String token = IOUtils.toString((InputStream)authResponse.getEntity(), StandardCharsets.UTF_8);

		Response response = 
				target("projects")
				.request(MediaType.APPLICATION_JSON)
				.header("Authorization", String.format("Bearer %s", token))
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
				target("projects")
				.request(MediaType.APPLICATION_JSON)
				.header("Authorization", String.format("Bearer %s", personalAccessToken))
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

		Response response = target("projects").queryParam("accesstoken", personalAccessToken).request(MediaType.APPLICATION_JSON).get();
		
		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		
		String serializedList = IOUtils.toString((InputStream)response.getEntity(), StandardCharsets.UTF_8);
		Type listType = new TypeToken<ArrayList<ProjectReference>>(){}.getType();
		ArrayList<ProjectReference> resultList = new SerializationHelper<ArrayList<ProjectReference>>().deserialize(serializedList, listType);

		assertEquals(expectedList, resultList);
	}

	@Test
	void shouldProduce403UnauthorizedWithWrongBackendAccessTokenQueryParam() throws Exception {
		ProjectFixtures.setUpProjectList(remoteGitManagerRestrictedFactoryMock, "my_personal_token");

		Response response = target("projects").queryParam("accesstoken", "wrong_access_token").request(MediaType.APPLICATION_JSON).get();
		
		assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
	}
	
	@Test
	void shouldProduce404OnProjectNotFound() throws Exception {
		IDGenerator idGenerator = new IDGenerator();
		
		String namespace = "test_namespace";
		String projectName = "test_project";
		String projectId = idGenerator.generate(projectName);

		ProjectFixtures.setUpRemoteGitManagerThrowing404(remoteGitManagerRestrictedFactoryMock);
		Response authResponse = target("auth").queryParam("accesstoken", "my personal token").request().get();
		String token = IOUtils.toString((InputStream)authResponse.getEntity(), StandardCharsets.UTF_8);

		Response response = target("projects/"+namespace+"/"+projectId).queryParam("accesstoken", token).request(MediaType.APPLICATION_JSON).get();
		assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
	}
	
	@Test
	void shouldProduceProjectExportWithJwtAccessTokenQueryParam() throws Exception {
		
		IDGenerator idGenerator = new IDGenerator();

		String namespace = "test_namespace";
		String projectName = "test_project_ForProjectExport";
		String projectId = idGenerator.generate(projectName);
		String sourceDocumentUuid = idGenerator.generateDocumentId();
		String tagId = idGenerator.generate();
		String tagName = "my tag";
		String tagHexColor = "#ff0000";
		String annotationCollectionId = idGenerator.generateCollectionId();
		String annotationCollectionName = "my collection";
		String annotationId1 = idGenerator.generate();
		String annotationId2 = idGenerator.generate();
		String annotationId3 = idGenerator.generate();
		String annotationId4 = idGenerator.generate();
		String propertyName = "my property";
		String propertyValue = "value1";
		String tagsetId = idGenerator.generateTagsetId();
		String tagsetName = "my tagset";

		List<String> annotatedPhrasesSortedByAnnotationId = 
			ProjectFixtures.setUpFullProject(
					remoteGitManagerRestrictedFactoryMock, 
					namespace, projectId, projectName, 
					sourceDocumentUuid, 
					tagsetId, tagsetName,
					tagId, tagName, tagHexColor,
					annotationCollectionId, annotationCollectionName,
					annotationId1, propertyName, propertyValue,
					annotationId2, annotationId3, annotationId4);
		
		
		Response authResponse = target("auth").queryParam("accesstoken", "my personal token").request().get();
		
		String token = IOUtils.toString((InputStream)authResponse.getEntity(), StandardCharsets.UTF_8);

		Response response = target("projects/"+namespace+"/"+projectId).queryParam("accesstoken", token).request(MediaType.APPLICATION_JSON).get();
		
		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		
		
		Export export = new SerializationHelper<Export>().deserialize(IOUtils.toString((InputStream)response.getEntity(), StandardCharsets.UTF_8), Export.class);
		assertDoesNotThrow(() -> UUID.fromString(export.getExportId().substring(2)));
		assertTrue(ChronoUnit.SECONDS.between(export.getExportCreatedAt(), ZonedDateTime.now()) <= 5);
		assertEquals(1, export.getTotalPages());
		assertEquals(1, export.getPageNo());
		assertEquals(100, export.getPageSize());
		assertNull(export.getPrevPage());
		assertNull(export.getNextPage());
		// page 1, so we should also get extended metadata by default
		assertNotNull(export.getExtendedMetadata());
		assertEquals(1, export.getExtendedMetadata().documents().size());
		assertEquals(sourceDocumentUuid, export.getExtendedMetadata().documents().get(sourceDocumentUuid).getId());

		List<ExportDocument> exportDocuments = export.getDocuments();
		assertTrue(exportDocuments.size() == 1);

		ExportDocument exportDocument = export.getDocuments().get(0);
		

		assertEquals(sourceDocumentUuid, exportDocument.getId());
		assertTrue(exportDocument.getAnnotations().size() == annotatedPhrasesSortedByAnnotationId.size());
		PreApiAnnotation annotation1 = exportDocument.getAnnotations().stream().filter(a -> a.getId().equals(annotationId1)).findFirst().get();
		assertEquals(annotationId1, annotation1.getId());
		assertEquals(
				annotatedPhrasesSortedByAnnotationId.get(
						List.of(annotationId1, annotationId2, annotationId3, annotationId4).stream().sorted().toList().indexOf(annotationId1)),
				annotation1.getPhrases().get(0).getPhrase());
		assertEquals(annotationCollectionId, annotation1.getAnnotationCollectionId());
		assertEquals(annotationCollectionName, annotation1.getAnnotationCollectionName());
		assertEquals(tagId, annotation1.getTagId());
		assertEquals(tagName, annotation1.getTagName());
		assertEquals(tagHexColor, annotation1.getTagColor());
		assertEquals(String.format("/%s", tagName), annotation1.getTagPath());
		assertEquals(namespace, annotation1.getAuthor());
		assertTrue(ChronoUnit.SECONDS.between(annotation1.getCreatedAt(), ZonedDateTime.now()) <= 5);
		assertTrue(annotation1.getUserProperties().size() == 1);
		PreApiAnnotationProperty property = annotation1.getUserProperties().get(0);
		assertEquals(propertyName, property.getName());
		assertTrue(property.getValues().size() == 1);
		assertEquals(propertyValue, property.getValues().get(0));

		// explicitly NOT requesting extended metadata should work
		Response response2 = target("projects/"+namespace+"/"+projectId)
				.queryParam("accesstoken", token)
				.queryParam("includeExtendedMetadata", false)
				.request(MediaType.APPLICATION_JSON).get();
		assertEquals(Status.OK.getStatusCode(), response2.getStatus());

		Export export2 = new SerializationHelper<Export>().deserialize(
				IOUtils.toString((InputStream) response2.getEntity(), StandardCharsets.UTF_8),
				Export.class
		);
		assertNull(export2.getExtendedMetadata());
	}
	
	@Test
	void shouldProduceProjectExportPage2WithJwtAccessTokenQueryParam() throws Exception {
		
		IDGenerator idGenerator = new IDGenerator();

		String namespace = "test_namespace";
		String projectName = "test_project_ForExportPage2";
		String projectId = idGenerator.generate(projectName);
		String sourceDocumentUuid = idGenerator.generateDocumentId();
		String tagId = idGenerator.generate();
		String tagName = "my tag";
		String annotationId = idGenerator.generate();
		String propertyName = "my property";
		String propertyValue = "value1";
		String tagsetId = idGenerator.generateTagsetId();
		String tagsetName = "my tagset";

		
		
		List<String> annotatedPhrasesSortedByAnnotationId = 
			ProjectFixtures.setUpFullProject(
					remoteGitManagerRestrictedFactoryMock, 
					namespace, projectId, projectName, 
					sourceDocumentUuid, 
					tagsetId, tagsetName,
					tagId, tagName, 
					annotationId, propertyName, propertyValue);
		
		
		Response authResponse = target("auth").queryParam("accesstoken", "my personal token").request().get();
		
		String token = IOUtils.toString((InputStream)authResponse.getEntity(), StandardCharsets.UTF_8);

		Response response = target("projects/"+namespace+"/"+projectId).queryParam("accesstoken", token).queryParam("page", 2).queryParam("pageSize", 2).request(MediaType.APPLICATION_JSON).get();
		
		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		
		
		Export export = new SerializationHelper<Export>().deserialize(IOUtils.toString((InputStream)response.getEntity(), StandardCharsets.UTF_8), Export.class);
		assertDoesNotThrow(() -> UUID.fromString(export.getExportId().substring(2)));
		assertTrue(ChronoUnit.SECONDS.between(export.getExportCreatedAt(), ZonedDateTime.now()) <= 5);
		assertEquals(2, export.getTotalPages());
		assertEquals(2, export.getPageNo());
		assertEquals(2, export.getPageSize());
		assertTrue(export.getPrevPage().contains("page=1"));
		assertNull(export.getNextPage());
		// page 2, so we should NOT get extended metadata by default
		assertNull(export.getExtendedMetadata());

		ExportDocument exportDocument = export.getDocuments().get(0);
		PreApiAnnotation annotation = exportDocument.getAnnotations().get(0);

		
		assertTrue(annotatedPhrasesSortedByAnnotationId.subList(2,3).contains(annotation.getPhrases().get(0).getPhrase()));

		// explicitly requesting extended metadata should work
		Response response2 = target("projects/"+namespace+"/"+projectId)
				.queryParam("accesstoken", token)
				.queryParam("page", 2)
				.queryParam("pageSize", 2)
				.queryParam("includeExtendedMetadata", true)
				.request(MediaType.APPLICATION_JSON).get();
		assertEquals(Status.OK.getStatusCode(), response2.getStatus());

		Export export2 = new SerializationHelper<Export>().deserialize(
				IOUtils.toString((InputStream) response2.getEntity(), StandardCharsets.UTF_8),
				Export.class
		);
		assertNotNull(export2.getExtendedMetadata());
	}
	
	@Test
	void shouldProduceDocumentContentWithJwtAccessTokenQueryParam() throws Exception {
		
		IDGenerator idGenerator = new IDGenerator();

		String namespace = "test_namespace";
		String projectName = "test_project_ForDocumentContent";
		String projectId = idGenerator.generate(projectName);
		String sourceDocumentUuid = idGenerator.generateDocumentId();
		
		String expectedContent = 
			ProjectFixtures.setUpProjectWithDocument(
					remoteGitManagerRestrictedFactoryMock, 
					namespace, projectId, projectName, 
					sourceDocumentUuid);
		
		
		Response authResponse = target("auth").queryParam("accesstoken", "my personal token").request().get();
		
		String token = IOUtils.toString((InputStream)authResponse.getEntity(), StandardCharsets.UTF_8);

		Response response = target("projects/"+namespace+"/"+projectId+"/doc/"+sourceDocumentUuid).queryParam("accesstoken", token).request(MediaType.APPLICATION_JSON).get();
		
		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		
		String content = IOUtils.toString((InputStream)response.getEntity(), StandardCharsets.UTF_8);
		
		assertEquals(expectedContent.substring(0, 10), content.substring(0,10));
	}
	
}
