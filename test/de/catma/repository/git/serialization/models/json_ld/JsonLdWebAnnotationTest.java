package de.catma.repository.git.serialization.models.json_ld;

import de.catma.document.Range;
import de.catma.document.source.*;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.repository.git.ProjectHandler;
import de.catma.repository.git.TagsetHandler;
import de.catma.repository.git.managers.JGitRepoManager;
import de.catma.repository.git.managers.GitLabServerManager;
import de.catma.repository.git.managers.GitLabServerManagerTest;
import de.catma.repository.git.serialization.SerializationHelper;
import de.catma.repository.git.serialization.model_wrappers.GitSourceDocumentInfo;
import de.catma.tag.*;
import de.catma.util.IDGenerator;
import helpers.Randomizer;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.submodule.SubmoduleWalk;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.gitlab4j.api.models.User;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.Assert.*;

public class JsonLdWebAnnotationTest {
	// this string needs to be formatted with the following 8 pieces of information:
	// projectRootRepositoryName
	// tagsetDefinitionUuid
	// tagDefinitionUuid
	// userPropertyDefinitionUuid
	// systemPropertyDefinitionUuid
	// userMarkupCollectionUuid
	// tagInstanceUuid
	// sourceDocumentUuid
	private final String expectedSerializedRepresentation = "" +
			"{\n" +
			"\t\"body\":{\n" +
			"\t\t\"@context\":{\n" +
			"\t\t\t\"UPROP_DEF\":\"http://catma.de/gitlab/%1$s/tagsets/%2$s/%3$s/propertydefs.json/%4$s\",\n" +
			"\t\t\t\"catma_displaycolor\":\"http://catma.de/gitlab/%1$s/tagsets/%2$s/%3$s/propertydefs.json/%5$s\",\n" +
			"\t\t\t\"tag\":\"http://catma.de/portal/tag\",\n" +
			"\t\t\t\"tagset\":\"http://catma.de/portal/tagset\"\n" +
			"\t\t},\n" +
			"\t\t\"properties\":{\n" +
			"\t\t\t\"system\":{\n" +
			"\t\t\t\t\"catma_displaycolor\":[\"SYSPROP_VAL_1\"]\n" +
			"\t\t\t},\n" +
			"\t\t\t\"user\":{\n" +
			"\t\t\t\t\"UPROP_DEF\":[\"UPROP_VAL_2\"]\n" +
			"\t\t\t}\n" +
			"\t\t},\n" +
			"\t\t\"tag\":\"http://catma.de/gitlab/%1$s/tagsets/%2$s/%3$s\",\n" +
			"\t\t\"tagset\":\"http://catma.de/gitlab/%1$s/tagsets/%2$s\",\n" +
			"\t\t\"type\":\"Dataset\"\n" +
			"\t},\n" +
			"\t\"@context\":\"http://www.w3.org/ns/anno.jsonld\",\n" +
			"\t\"id\":\"http://catma.de/gitlab/%1$s/collections/%6$s/annotations/%7$s.json\",\n" +
			"\t\"target\":{\n" +
			"\t\t\"items\":[{\n" +
			"\t\t\t\"selector\":{\n" +
			"\t\t\t\t\"end\":18,\n" +
			"\t\t\t\t\"start\":12,\n" +
			"\t\t\t\t\"type\":\"TextPositionSelector\"\n" +
			"\t\t\t},\n" +
			"\t\t\t\"source\":\"http://catma.de/gitlab/%1$s/documents/%8$s\"\n" +
			"\t\t},\n" +
			"\t\t{\n" +
			"\t\t\t\"selector\":{\n" +
			"\t\t\t\t\"end\":47,\n" +
			"\t\t\t\t\"start\":41,\n" +
			"\t\t\t\t\"type\":\"TextPositionSelector\"\n" +
			"\t\t\t},\n" +
			"\t\t\t\"source\":\"http://catma.de/gitlab/%1$s/documents/%8$s\"\n" +
			"\t\t}],\n" +
			"\t\t\"type\":\"List\"\n" +
			"\t},\n" +
			"\t\"type\":\"Annotation\"\n" +
			"}";

	private Properties catmaProperties;
	private de.catma.user.User catmaUser;
	private GitLabServerManager gitLabServerManager;

	private ArrayList<String> projectsToDeleteOnTearDown = new ArrayList<>();

	private ArrayList<File> directoriesToDeleteOnTearDown = new ArrayList<>();

	public JsonLdWebAnnotationTest() throws Exception {
		String propertiesFile = System.getProperties().containsKey("prop") ?
				System.getProperties().getProperty("prop") : "catma.properties";

		this.catmaProperties = new Properties();
		this.catmaProperties.load(new FileInputStream(propertiesFile));
	}

	@Before
	public void setUp() throws Exception {
		// create a fake CATMA user which we'll use to instantiate the GitLabServerManager & JGitRepoManager
		this.catmaUser = Randomizer.getDbUser();

		this.gitLabServerManager = new GitLabServerManager(
			this.catmaProperties, catmaUser
		);
		this.gitLabServerManager.replaceGitLabServerUrl = true;
	}

	@After
	public void tearDown() throws Exception {
		if (this.projectsToDeleteOnTearDown.size() > 0) {
			try (JGitRepoManager jGitRepoManager = new JGitRepoManager(this.catmaProperties, this.catmaUser)) {
				ProjectHandler projectHandler = new ProjectHandler(jGitRepoManager, this.gitLabServerManager);

				for (String projectId : this.projectsToDeleteOnTearDown) {
					projectHandler.delete(projectId);
				}
				this.projectsToDeleteOnTearDown.clear();
			}
		}

		if (this.directoriesToDeleteOnTearDown.size() > 0) {
			for (File dir : this.directoriesToDeleteOnTearDown) {
				FileUtils.deleteDirectory(dir);
			}
			this.directoriesToDeleteOnTearDown.clear();
		}

		// delete the GitLab user that the GitLabServerManager constructor in setUp would have
		// created - see GitLabServerManagerTest tearDown() for more info
		User user = this.gitLabServerManager.getGitLabUser();
		this.gitLabServerManager.getAdminGitLabApi().getUserApi().deleteUser(user.getId());
		GitLabServerManagerTest.awaitUserDeleted(
			this.gitLabServerManager.getAdminGitLabApi().getUserApi(), user.getId()
		);
	}

	// TODO: remove this (use getJsonLdWebAnnotation instead)
	public static TagInstance getFakeTagInstance() {
		PropertyPossibleValueList systemPropertyPossibleValues = new PropertyPossibleValueList(
			Arrays.asList("SYSPROP_VAL_1", "SYSPROP_VAL_2"), true
		);
		PropertyDefinition systemPropertyDefinition = new PropertyDefinition(
			1, "CATMA_SYSPROP_DEF", PropertyDefinition.SystemPropertyName.catma_markupauthor.toString(),
			systemPropertyPossibleValues
		);

		PropertyPossibleValueList userPropertyPossibleValues = new PropertyPossibleValueList(
			Arrays.asList("UPROP_VAL_1", "UPROP_VAL_2"), true
		);
		PropertyDefinition userPropertyDefinition = new PropertyDefinition(
			2, "CATMA_UPROP_DEF", "UPROP_DEF", userPropertyPossibleValues
		);

		TagDefinition tagDefinition = new TagDefinition(
			1, "CATMA_TAG_DEF", "TAG_DEF", new Version(), null, null,
			"CATMA_TAGSET_DEF"
		);
		tagDefinition.addSystemPropertyDefinition(systemPropertyDefinition);
		tagDefinition.addUserDefinedPropertyDefinition(userPropertyDefinition);

		Property systemProperty = new Property(systemPropertyDefinition, new PropertyValueList("SYSPROP_VAL_1"));
		Property userProperty = new Property(userPropertyDefinition, new PropertyValueList("UPROP_VAL_2"));

		TagInstance tagInstance = new TagInstance("CATMA_TAG_INST", tagDefinition);
		tagInstance.addSystemProperty(systemProperty);
		tagInstance.addUserDefinedProperty(userProperty);

		return tagInstance;
	}

	/**
	 * @return a HashMap<String, Object> with these keys:
	 *         'jsonLdWebAnnotation' - for the JsonLdWebAnnotation object
	 *         'projectUuid'
	 *         - following additional keys which are to be used when formatting this.expectedSerializedRepresentation -:
	 *         projectRootRepositoryName, tagsetDefinitionUuid, tagDefinitionUuid, userPropertyDefinitionUuid,
	 *         systemPropertyDefinitionUuid, userMarkupCollectionUuid, tagInstanceUuid, sourceDocumentUuid
	 */
	private HashMap<String, Object> getJsonLdWebAnnotation() throws Exception {
		try (JGitRepoManager jGitRepoManager = new JGitRepoManager(this.catmaProperties, this.catmaUser)) {
			this.directoriesToDeleteOnTearDown.add(jGitRepoManager.getRepositoryBasePath());

			// create project
			ProjectHandler projectHandler = new ProjectHandler(jGitRepoManager, this.gitLabServerManager);

			String projectId = projectHandler.create(
					"Test CATMA Project", "This is a test CATMA project"
			);
			this.projectsToDeleteOnTearDown.add(projectId);

			// add new tagset to project
			String tagsetId = projectHandler.createTagset(
					projectId, null, "Test Tagset", null
			);

			// add new source document to project
			File originalSourceDocument = new File("testdocs/rose_for_emily.pdf");
			File convertedSourceDocument = new File("testdocs/rose_for_emily.txt");

			FileInputStream originalSourceDocumentStream = new FileInputStream(originalSourceDocument);
			FileInputStream convertedSourceDocumentStream = new FileInputStream(convertedSourceDocument);

			IndexInfoSet indexInfoSet = new IndexInfoSet();
			indexInfoSet.setLocale(Locale.ENGLISH);

			ContentInfoSet contentInfoSet = new ContentInfoSet(
					"William Faulkner",
					"",
					"",
					"A Rose for Emily"
			);

			TechInfoSet techInfoSet = new TechInfoSet(
					FileType.TEXT,
					StandardCharsets.UTF_8,
					FileOSType.DOS,
					705211438L,
					null
			);

			SourceDocumentInfo sourceDocumentInfo = new SourceDocumentInfo(
					indexInfoSet, contentInfoSet, techInfoSet
			);

			GitSourceDocumentInfo gitSourceDocumentInfo = new GitSourceDocumentInfo(sourceDocumentInfo);

			String sourceDocumentId = projectHandler.createSourceDocument(
					projectId,
					null, originalSourceDocumentStream, originalSourceDocument.getName(),
					convertedSourceDocumentStream, convertedSourceDocument.getName(),
					gitSourceDocumentInfo
			);

			// add new markup collection to project
			String markupCollectionId = projectHandler.createMarkupCollection(
					projectId, null, "Test Markup Collection", null,
					sourceDocumentId, "fakeSourceDocumentVersion"
			);

			// commit the changes to the project root repo (addition of tagset, source document and markup collection
			// submodules)
			String projectRootRepositoryName = ProjectHandler.getProjectRootRepositoryName(projectId);
			jGitRepoManager.open(projectRootRepositoryName);
			jGitRepoManager.commit(
					String.format(
							"Adding new tagset %s, source document %s and markup collection %s",
							tagsetId,
							sourceDocumentId,
							markupCollectionId
					),
					"Test Committer",
					"testcommitter@catma.de"
			);
			jGitRepoManager.detach();  // can't call open on an attached instance

			// construct TagDefinition object
			IDGenerator idGenerator = new IDGenerator();

			PropertyPossibleValueList systemPropertyPossibleValues = new PropertyPossibleValueList(
					Arrays.asList("SYSPROP_VAL_1", "SYSPROP_VAL_2"), true
			);
			String systemPropertyDefinitionUuid = idGenerator.generate();
			PropertyDefinition systemPropertyDefinition = new PropertyDefinition(
					null, systemPropertyDefinitionUuid,
					PropertyDefinition.SystemPropertyName.catma_displaycolor.toString(),
					systemPropertyPossibleValues
			);

			PropertyPossibleValueList userPropertyPossibleValues = new PropertyPossibleValueList(
					Arrays.asList("UPROP_VAL_1", "UPROP_VAL_2"), true
			);
			String userPropertyDefinitionUuid = idGenerator.generate();
			PropertyDefinition userPropertyDefinition = new PropertyDefinition(
					null, userPropertyDefinitionUuid, "UPROP_DEF", userPropertyPossibleValues
			);

			String tagDefinitionUuid = idGenerator.generate();
			TagDefinition tagDefinition = new TagDefinition(
					null, tagDefinitionUuid, "TAG_DEF", new Version(), null, null,
					tagsetId
			);
			tagDefinition.addSystemPropertyDefinition(systemPropertyDefinition);
			tagDefinition.addUserDefinedPropertyDefinition(userPropertyDefinition);

			// call createTagDefinition
			// NB: in this case we know that the tagset submodule is on the master branch tip, ie: not in a detached
			// head state, so it's safe to make changes to the submodule and commit them
			// TODO: createTagDefinition should probably do some validation and fail fast if the tagset submodule is in
			// a detached head state - in that case the submodule would need to be updated first
			// see the "Updating a submodule in-place in the container" scenario at
			// https://medium.com/@porteneuve/mastering-git-submodules-34c65e940407
			TagsetHandler tagsetHandler = new TagsetHandler(jGitRepoManager, this.gitLabServerManager);
			String returnedTagDefinitionId = tagsetHandler.createTagDefinition(projectId, tagsetId, tagDefinition);

			assertNotNull(returnedTagDefinitionId);
			assert returnedTagDefinitionId.startsWith("CATMA_");

			// the JGitRepoManager instance should always be in a detached state after TagsetHandler calls return
			assertFalse(jGitRepoManager.isAttached());

			assertEquals(tagDefinitionUuid, returnedTagDefinitionId);

			// commit and push submodule changes (creation of tag definition)
			// TODO: add methods to JGitRepoManager to do this
			jGitRepoManager.open(projectRootRepositoryName);

			Repository projectRootRepository = jGitRepoManager.getGitApi().getRepository();
			String tagsetSubmodulePath = String.format(
					"%s/%s", ProjectHandler.TAGSET_SUBMODULES_DIRECTORY_NAME, tagsetId
			);
			Repository tagsetSubmoduleRepository = SubmoduleWalk.getSubmoduleRepository(
					projectRootRepository, tagsetSubmodulePath
			);
			Git submoduleGit = new Git(tagsetSubmoduleRepository);
			submoduleGit.add().addFilepattern(tagDefinitionUuid).call();
			submoduleGit.commit().setMessage(
					String.format("Adding tag definition %s", tagDefinitionUuid)
			).setCommitter("Test Committer", "testcommitter@catma.de").call();
			submoduleGit.push().setCredentialsProvider(
					new UsernamePasswordCredentialsProvider(
							this.gitLabServerManager.getGitLabUser().getUsername(),
							this.gitLabServerManager.getGitLabUserImpersonationToken()
					)
			).call();
			tagsetSubmoduleRepository.close();
			submoduleGit.close();

			// commit and push project root repo changes (update of tagset submodule)
			jGitRepoManager.getGitApi().add().addFilepattern(tagsetSubmodulePath).call();
			jGitRepoManager.commit(
					String.format("Updating tagset %s", tagsetId),
					"Test Committer",
					"testcommitter@catma.de"
			);

			jGitRepoManager.detach();  // can't call open on an attached instance

			// construct TagInstance object
			Property systemProperty = new Property(systemPropertyDefinition, new PropertyValueList("SYSPROP_VAL_1"));
			Property userProperty = new Property(userPropertyDefinition, new PropertyValueList("UPROP_VAL_2"));

			String tagInstanceUuid = idGenerator.generate();
			TagInstance tagInstance = new TagInstance(tagInstanceUuid, tagDefinition);
			tagInstance.addSystemProperty(systemProperty);
			tagInstance.addUserDefinedProperty(userProperty);

			// construct JsonLdWebAnnotation object
			String sourceDocumentUri = String.format(
					"http://catma.de/gitlab/%s/documents/%s", projectRootRepositoryName, sourceDocumentId
			);

			Range range1 = new Range(12, 18);
			Range range2 = new Range(41, 47);

			List<TagReference> tagReferences = new ArrayList<>(
					Arrays.asList(
							new TagReference(
									tagInstance, sourceDocumentUri, range1, markupCollectionId
							),
							new TagReference(
									tagInstance, sourceDocumentUri, range2, markupCollectionId
							)
					)
			);

			JsonLdWebAnnotation jsonLdWebAnnotation = new JsonLdWebAnnotation(
					"http://catma.de/gitlab", projectId, tagReferences
			);

			HashMap<String, Object> returnValue = new HashMap<>();
			returnValue.put("jsonLdWebAnnotation", jsonLdWebAnnotation);
			returnValue.put("projectRootRepositoryName", projectRootRepositoryName);
			returnValue.put("projectUuid", projectId);
			returnValue.put("tagsetDefinitionUuid", tagsetId);
			returnValue.put("tagDefinitionUuid", tagDefinitionUuid);
			returnValue.put("userPropertyDefinitionUuid", userPropertyDefinitionUuid);
			returnValue.put("systemPropertyDefinitionUuid", systemPropertyDefinitionUuid);
			returnValue.put("userMarkupCollectionUuid", markupCollectionId);
			returnValue.put("tagInstanceUuid", tagInstanceUuid);
			returnValue.put("sourceDocumentUuid", sourceDocumentId);

			return returnValue;
		}
	}

	@Test
	public void serialize() throws Exception {
		HashMap<String, Object> getJsonLdWebAnnotationResult = this.getJsonLdWebAnnotation();
		JsonLdWebAnnotation jsonLdWebAnnotation = (JsonLdWebAnnotation)getJsonLdWebAnnotationResult.get(
				"jsonLdWebAnnotation"
		);

		String serialized = new SerializationHelper<JsonLdWebAnnotation>().serialize(jsonLdWebAnnotation);

		String expectedSerializedRepresentation = this.expectedSerializedRepresentation.replaceAll(
				"[\n\t]", ""
		);
		expectedSerializedRepresentation = String.format(
				expectedSerializedRepresentation,
				getJsonLdWebAnnotationResult.get("projectRootRepositoryName"),
				getJsonLdWebAnnotationResult.get("tagsetDefinitionUuid"),
				getJsonLdWebAnnotationResult.get("tagDefinitionUuid"),
				getJsonLdWebAnnotationResult.get("userPropertyDefinitionUuid"),
				getJsonLdWebAnnotationResult.get("systemPropertyDefinitionUuid"),
				getJsonLdWebAnnotationResult.get("userMarkupCollectionUuid"),
				getJsonLdWebAnnotationResult.get("tagInstanceUuid"),
				getJsonLdWebAnnotationResult.get("sourceDocumentUuid")
		);

		assertEquals(expectedSerializedRepresentation, serialized);
	}

	@Test
	public void deserialize() throws Exception {
		String toDeserialize = this.expectedSerializedRepresentation.replaceAll("[\n\t]", "");
		toDeserialize = String.format(
				toDeserialize,
				"fakeProjectRootRepositoryName",
				"fakeTagsetDefinitionUuid",
				"fakeTagDefinitionUuid",
				"fakeUserPropertyDefinitionUuid",
				"fakeSystemPropertyDefinitionUuid",
				"fakeUserMarkupCollectionUuid",
				"fakeTagInstanceUuid",
				"fakeSourceDocumentUuid"
		);

		JsonLdWebAnnotation jsonLdWebAnnotation = new SerializationHelper<JsonLdWebAnnotation>().deserialize(
				toDeserialize, JsonLdWebAnnotation.class
		);

		assertNotNull(jsonLdWebAnnotation);

		// re-serialize and assert that what comes out is what went in
		String serialized = new SerializationHelper<JsonLdWebAnnotation>().serialize(jsonLdWebAnnotation);

		assertEquals(toDeserialize, serialized);
	}

	@Test
	public void toTagReferenceList() throws Exception {
		try (JGitRepoManager jGitRepoManager = new JGitRepoManager(this.catmaProperties, this.catmaUser)) {
			this.directoriesToDeleteOnTearDown.add(jGitRepoManager.getRepositoryBasePath());

			// TODO: test with a hierarchy of tag definitions
			HashMap<String, Object> getJsonLdWebAnnotationResult = this.getJsonLdWebAnnotation();
			JsonLdWebAnnotation jsonLdWebAnnotation = (JsonLdWebAnnotation)getJsonLdWebAnnotationResult.get(
					"jsonLdWebAnnotation"
			);

			assertNotNull(jsonLdWebAnnotation);List<TagReference> tagReferences = jsonLdWebAnnotation.toTagReferenceList(
					(String)getJsonLdWebAnnotationResult.get("projectUuid"),
					(String)getJsonLdWebAnnotationResult.get("userMarkupCollectionUuid"),
					jGitRepoManager, this.gitLabServerManager
			);

			assertEquals(2, tagReferences.size());

			for (TagReference tagReference : tagReferences) {
				TagDefinition tagDefinition = tagReference.getTagDefinition();
				TagInstance tagInstance = tagReference.getTagInstance();

				assertEquals(
						getJsonLdWebAnnotationResult.get("tagsetDefinitionUuid"),
						tagDefinition.getTagsetDefinitionUuid()
				);
				assertEquals(getJsonLdWebAnnotationResult.get("tagDefinitionUuid"), tagDefinition.getUuid());
				assertEquals("TAG_DEF", tagDefinition.getName());
				assertEquals("", tagDefinition.getParentUuid());

				PropertyDefinition[] systemPropertyDefinitions = tagDefinition.getSystemPropertyDefinitions()
						.toArray(new PropertyDefinition[0]);
				assertEquals(1, systemPropertyDefinitions.length);
				assertEquals(
						getJsonLdWebAnnotationResult.get("systemPropertyDefinitionUuid"),
						systemPropertyDefinitions[0].getUuid()
				);
				assertEquals("catma_displaycolor", systemPropertyDefinitions[0].getName());
				List<String> possibleSystemPropertyValues = systemPropertyDefinitions[0].getPossibleValueList()
						.getPropertyValueList().getValues();
				assertEquals(2, possibleSystemPropertyValues.size());
				assertArrayEquals(
					new String[]{"SYSPROP_VAL_1", "SYSPROP_VAL_2"}, possibleSystemPropertyValues.toArray(new String[0])
				);

				PropertyDefinition[] userPropertyDefinitions = tagDefinition.getUserDefinedPropertyDefinitions()
						.toArray(new PropertyDefinition[0]);
				assertEquals(1, userPropertyDefinitions.length);
				assertEquals(
						getJsonLdWebAnnotationResult.get("userPropertyDefinitionUuid"),
						userPropertyDefinitions[0].getUuid()
				);
				assertEquals("UPROP_DEF", userPropertyDefinitions[0].getName());
				List<String> possibleUserPropertyValues = userPropertyDefinitions[0].getPossibleValueList()
						.getPropertyValueList().getValues();
				assertEquals(2, possibleUserPropertyValues.size());
				assertArrayEquals(
					new String[]{"UPROP_VAL_1", "UPROP_VAL_2"}, possibleUserPropertyValues.toArray(new String[0])
				);

				assertEquals(getJsonLdWebAnnotationResult.get("tagInstanceUuid"), tagInstance.getUuid());

				Property[] systemProperties = tagInstance.getSystemProperties().toArray(new Property[0]);
				assertEquals(1, systemProperties.length);
				assertEquals(systemPropertyDefinitions[0], systemProperties[0].getPropertyDefinition());
				List<String> systemPropertyValues = systemProperties[0].getPropertyValueList().getValues();
				assertEquals(1, systemPropertyValues.size());
				assertEquals("SYSPROP_VAL_1", systemPropertyValues.get(0));

				Property[] userProperties = tagInstance.getUserDefinedProperties().toArray(new Property[0]);
				assertEquals(1, userProperties.length);
				assertEquals(userPropertyDefinitions[0], userProperties[0].getPropertyDefinition());
				List<String> userPropertyValues = userProperties[0].getPropertyValueList().getValues();
				assertEquals(1, userPropertyValues.size());
				assertEquals("UPROP_VAL_2", userPropertyValues.get(0));
			}

			assertEquals(
					new URI(
							String.format(
									"http://catma.de/gitlab/%s/documents/%s",
									getJsonLdWebAnnotationResult.get("projectRootRepositoryName"),
									getJsonLdWebAnnotationResult.get("sourceDocumentUuid")
							)
					),
					tagReferences.get(0).getTarget()
			);
			assertEquals(new Range(12, 18), tagReferences.get(0).getRange());

			assertEquals(
					new URI(
							String.format(
									"http://catma.de/gitlab/%s/documents/%s",
									getJsonLdWebAnnotationResult.get("projectRootRepositoryName"),
									getJsonLdWebAnnotationResult.get("sourceDocumentUuid")
							)
					),
					tagReferences.get(1).getTarget()
			);
			assertEquals(new Range(41, 47), tagReferences.get(1).getRange());
		}
	}
}
