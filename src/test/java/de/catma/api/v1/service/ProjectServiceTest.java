package de.catma.api.v1.service;

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

import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.google.gson.reflect.TypeToken;

import de.catma.api.v1.AuthConstants;
import de.catma.api.v1.ApiApplication;
import de.catma.api.v1.backend.interfaces.RemoteGitManagerRestrictedFactory;
import de.catma.api.v1.fixture.ProjectFixtures;
import de.catma.api.v1.serialization.model_wrappers.ProjectExportAnnotation;
import de.catma.api.v1.serialization.model_wrappers.ProjectExportAnnotationProperty;
import de.catma.api.v1.serialization.models.ProjectExport;
import de.catma.api.v1.serialization.models.ProjectExportDocument;
import de.catma.project.ProjectReference;
import de.catma.properties.CATMAProperties;
import de.catma.properties.CATMAPropertyKey;
import de.catma.repository.git.serialization.SerializationHelper;
import de.catma.util.IDGenerator;

public class ProjectServiceTest extends JerseyTest {
	private static final String AUTH_TARGET = AuthConstants.AUTH_SERVICE_PATH.substring(1);
	private static final String DUMMY_PERSONAL_ACCESS_TOKEN = "dummy_personal_access_token";

	private RemoteGitManagerRestrictedFactory remoteGitManagerRestrictedFactoryMock = Mockito.mock(RemoteGitManagerRestrictedFactory.class);

	// if HttpServletRequest is injected into the service under test with the @Context annotation, then it will be null because the tests don't run in a servlet
	// environment (see this file @ ffe07daf for how to set up a servlet environment)
	@Override
	protected Application configure() {
		ApiApplication app = new ApiApplication();
		
		// try to make sure that the configured package to scan is as expected
		assertEquals("de.catma.api.v1", ApiApplication.class.getPackage().getName());
		
		app.packages("de.catma.api.v1"); // the corresponding configuration for the production code is in the web.xml
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
		properties.setProperty(CATMAPropertyKey.API_HMAC_SECRET.name(), "dummy_hmac_secret".repeat(2));
		properties.setProperty(CATMAPropertyKey.API_GIT_REPOSITORY_BASE_PATH.name(), System.getProperty("java.io.tmpdir"));
		
		CATMAProperties.INSTANCE.setProperties(properties);
	}
	
	@Test
	void shouldProduce403UnauthorizedWithoutAuthArgs() throws Exception {
		Response response = target("projects").request(MediaType.APPLICATION_JSON).get();
		
		assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
	}

	@Test
	void shouldProduceProjectListWithJwtAccessTokenInBearerHeader() throws Exception {
		List<ProjectReference> expectedList = ProjectFixtures.setUpProjectList(remoteGitManagerRestrictedFactoryMock, DUMMY_PERSONAL_ACCESS_TOKEN);

		Response authResponse = target(AUTH_TARGET)
				.request()
				.header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
				.header(HttpHeaders.AUTHORIZATION, AuthConstants.AUTHENTICATION_SCHEME_BEARER_PREFIX + DUMMY_PERSONAL_ACCESS_TOKEN)
				.post(null);
		
		String apiToken = IOUtils.toString((InputStream)authResponse.getEntity(), StandardCharsets.UTF_8);

		Response response = 
				target("projects")
				.request(MediaType.APPLICATION_JSON)
				.header(HttpHeaders.AUTHORIZATION, AuthConstants.AUTHENTICATION_SCHEME_BEARER_PREFIX + apiToken)
				.get();
		
		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		
		String serializedList = IOUtils.toString((InputStream)response.getEntity(), StandardCharsets.UTF_8);
		Type listType = new TypeToken<ArrayList<ProjectReference>>(){}.getType();
		ArrayList<ProjectReference> resultList = new SerializationHelper<ArrayList<ProjectReference>>().deserialize(serializedList, listType);

		assertEquals(expectedList, resultList);
	}
	
	@Test
	void shouldProduceProjectListWithBackendAccessTokenInBearerHeader() throws Exception {
		List<ProjectReference> expectedList = ProjectFixtures.setUpProjectList(remoteGitManagerRestrictedFactoryMock, DUMMY_PERSONAL_ACCESS_TOKEN);

		Response response = 
				target("projects")
				.request(MediaType.APPLICATION_JSON)
				.header(HttpHeaders.AUTHORIZATION, AuthConstants.AUTHENTICATION_SCHEME_BEARER_PREFIX + DUMMY_PERSONAL_ACCESS_TOKEN)
				.get();
		
		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		
		String serializedList = IOUtils.toString((InputStream)response.getEntity(), StandardCharsets.UTF_8);
		Type listType = new TypeToken<ArrayList<ProjectReference>>(){}.getType();
		ArrayList<ProjectReference> resultList = new SerializationHelper<ArrayList<ProjectReference>>().deserialize(serializedList, listType);

		assertEquals(expectedList, resultList);
	}

	@Test
	void shouldProduce403UnauthorizedWithWrongBackendAccessTokenInBearerHeader() throws Exception {
		ProjectFixtures.setUpProjectList(remoteGitManagerRestrictedFactoryMock, DUMMY_PERSONAL_ACCESS_TOKEN);

		Response response = target("projects")
				.request(MediaType.APPLICATION_JSON)
				.header(HttpHeaders.AUTHORIZATION, AuthConstants.AUTHENTICATION_SCHEME_BEARER_PREFIX + "wrong_personal_access_token")
				.get();
		
		assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
	}
	
	@Test
	void shouldProduce404OnProjectNotFound() throws Exception {
		IDGenerator idGenerator = new IDGenerator();
		
		String namespace = "test_namespace";
		String projectName = "test_project";
		String projectId = idGenerator.generate(projectName);

		ProjectFixtures.setUpRemoteGitManagerThrowing404(remoteGitManagerRestrictedFactoryMock);
		Response authResponse = target(AUTH_TARGET)
				.request()
				.header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
				.header(HttpHeaders.AUTHORIZATION, AuthConstants.AUTHENTICATION_SCHEME_BEARER_PREFIX + DUMMY_PERSONAL_ACCESS_TOKEN)
				.post(null);
		String apiToken = IOUtils.toString((InputStream)authResponse.getEntity(), StandardCharsets.UTF_8);

		Response response = target("projects/"+namespace+"/"+projectId+"/export")
				.request(MediaType.APPLICATION_JSON)
				.header(HttpHeaders.AUTHORIZATION, AuthConstants.AUTHENTICATION_SCHEME_BEARER_PREFIX + apiToken)
				.get();
		assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
	}
	
	@Test
	void shouldProduceProjectExportWithJwtAccessTokenInBearerHeader() throws Exception {
		
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
		
		
		Response authResponse = target(AUTH_TARGET)
				.request()
				.header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
				.header(HttpHeaders.AUTHORIZATION, AuthConstants.AUTHENTICATION_SCHEME_BEARER_PREFIX + DUMMY_PERSONAL_ACCESS_TOKEN)
				.post(null);
		
		String apiToken = IOUtils.toString((InputStream)authResponse.getEntity(), StandardCharsets.UTF_8);

		Response response = target("projects/"+namespace+"/"+projectId+"/export")
				.request(MediaType.APPLICATION_JSON)
				.header(HttpHeaders.AUTHORIZATION, AuthConstants.AUTHENTICATION_SCHEME_BEARER_PREFIX + apiToken)
				.get();
		
		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		
		
		ProjectExport projectExport = new SerializationHelper<ProjectExport>().deserialize(IOUtils.toString((InputStream)response.getEntity(), StandardCharsets.UTF_8), ProjectExport.class);
		assertDoesNotThrow(() -> UUID.fromString(projectExport.getExportId().substring(2)));
		assertTrue(ChronoUnit.SECONDS.between(projectExport.getExportCreatedAt(), ZonedDateTime.now()) <= 5);
		assertEquals(1, projectExport.getTotalPages());
		assertEquals(1, projectExport.getPageNo());
		assertEquals(100, projectExport.getPageSize());
		assertNull(projectExport.getPrevPage());
		assertNull(projectExport.getNextPage());
		// page 1, so we should also get extended metadata by default
		assertNotNull(projectExport.getExtendedMetadata());
		assertEquals(1, projectExport.getExtendedMetadata().documents().size());
		assertEquals(sourceDocumentUuid, projectExport.getExtendedMetadata().documents().get(sourceDocumentUuid).getId());

		List<ProjectExportDocument> projectExportDocuments = projectExport.getDocuments();
		assertTrue(projectExportDocuments.size() == 1);

		ProjectExportDocument projectExportDocument = projectExport.getDocuments().get(0);
		

		assertEquals(sourceDocumentUuid, projectExportDocument.getId());
		assertTrue(projectExportDocument.getAnnotations().size() == annotatedPhrasesSortedByAnnotationId.size());
		ProjectExportAnnotation annotation1 = projectExportDocument.getAnnotations().stream().filter(a -> a.getId().equals(annotationId1)).findFirst().get();
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
		assertTrue(ChronoUnit.SECONDS.between(annotation1.getCreatedAt(), ZonedDateTime.now()) <= 5);
		assertTrue(annotation1.getUserProperties().size() == 1);
		ProjectExportAnnotationProperty property = annotation1.getUserProperties().get(0);
		assertEquals(propertyName, property.getName());
		assertTrue(property.getValues().size() == 1);
		assertEquals(propertyValue, property.getValues().get(0));

		// explicitly NOT requesting extended metadata should work
		Response response2 = target("projects/"+namespace+"/"+projectId+"/export")
				.queryParam("includeExtendedMetadata", false)
				.request(MediaType.APPLICATION_JSON)
				.header(HttpHeaders.AUTHORIZATION, AuthConstants.AUTHENTICATION_SCHEME_BEARER_PREFIX + apiToken)
				.get();
		assertEquals(Status.OK.getStatusCode(), response2.getStatus());

		ProjectExport projectExport2 = new SerializationHelper<ProjectExport>().deserialize(
				IOUtils.toString((InputStream) response2.getEntity(), StandardCharsets.UTF_8),
				ProjectExport.class
		);
		assertNull(projectExport2.getExtendedMetadata());
	}
	
	@Test
	void shouldProduceProjectExportPage2WithJwtAccessTokenInBearerHeader() throws Exception {
		
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
		
		
		Response authResponse = target(AUTH_TARGET)
				.request()
				.header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
				.header(HttpHeaders.AUTHORIZATION, AuthConstants.AUTHENTICATION_SCHEME_BEARER_PREFIX + DUMMY_PERSONAL_ACCESS_TOKEN)
				.post(null);
		
		String apiToken = IOUtils.toString((InputStream)authResponse.getEntity(), StandardCharsets.UTF_8);

		Response response = target("projects/"+namespace+"/"+projectId+"/export")
				.queryParam("page", 2)
				.queryParam("pageSize", 2)
				.request(MediaType.APPLICATION_JSON)
				.header(HttpHeaders.AUTHORIZATION, AuthConstants.AUTHENTICATION_SCHEME_BEARER_PREFIX + apiToken)
				.get();
		
		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		
		
		ProjectExport projectExport = new SerializationHelper<ProjectExport>().deserialize(IOUtils.toString((InputStream)response.getEntity(), StandardCharsets.UTF_8), ProjectExport.class);
		assertDoesNotThrow(() -> UUID.fromString(projectExport.getExportId().substring(2)));
		assertTrue(ChronoUnit.SECONDS.between(projectExport.getExportCreatedAt(), ZonedDateTime.now()) <= 5);
		assertEquals(2, projectExport.getTotalPages());
		assertEquals(2, projectExport.getPageNo());
		assertEquals(2, projectExport.getPageSize());
		assertTrue(projectExport.getPrevPage().contains("page=1"));
		assertNull(projectExport.getNextPage());
		// page 2, so we should NOT get extended metadata by default
		assertNull(projectExport.getExtendedMetadata());

		ProjectExportDocument document = projectExport.getDocuments().get(0);
		ProjectExportAnnotation annotation = document.getAnnotations().get(0);

		
		assertTrue(annotatedPhrasesSortedByAnnotationId.subList(2,3).contains(annotation.getPhrases().get(0).getPhrase()));

		// explicitly requesting extended metadata should work
		Response response2 = target("projects/"+namespace+"/"+projectId+"/export")
				.queryParam("page", 2)
				.queryParam("pageSize", 2)
				.queryParam("includeExtendedMetadata", true)
				.request(MediaType.APPLICATION_JSON)
				.header(HttpHeaders.AUTHORIZATION, AuthConstants.AUTHENTICATION_SCHEME_BEARER_PREFIX + apiToken)
				.get();
		assertEquals(Status.OK.getStatusCode(), response2.getStatus());

		ProjectExport projectExport2 = new SerializationHelper<ProjectExport>().deserialize(
				IOUtils.toString((InputStream) response2.getEntity(), StandardCharsets.UTF_8),
				ProjectExport.class
		);
		assertNotNull(projectExport2.getExtendedMetadata());
		// check that the explicitly provided includeExtendedMetadata query param value is preserved
		assertTrue(projectExport2.getPrevPage().contains("includeExtendedMetadata=true"));
	}
	
	@Test
	void shouldProduceDocumentContentWithJwtAccessTokenInBearerHeader() throws Exception {
		
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
		
		
		Response authResponse = target(AUTH_TARGET)
				.request()
				.header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
				.header(HttpHeaders.AUTHORIZATION, AuthConstants.AUTHENTICATION_SCHEME_BEARER_PREFIX + DUMMY_PERSONAL_ACCESS_TOKEN)
				.post(null);
		
		String apiToken = IOUtils.toString((InputStream)authResponse.getEntity(), StandardCharsets.UTF_8);

		Response response = target("projects/"+namespace+"/"+projectId+"/export/doc/"+sourceDocumentUuid)
				.request(MediaType.APPLICATION_JSON)
				.header(HttpHeaders.AUTHORIZATION, AuthConstants.AUTHENTICATION_SCHEME_BEARER_PREFIX + apiToken)
				.get();
		
		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		
		String content = IOUtils.toString((InputStream)response.getEntity(), StandardCharsets.UTF_8);
		
		assertEquals(expectedContent.substring(0, 10), content.substring(0,10));
	}
	
}
