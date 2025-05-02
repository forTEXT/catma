package de.catma.api.pre.fixture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.gitlab4j.api.GitLabApiException;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import de.catma.api.pre.PreProject;
import de.catma.api.pre.backend.interfaces.RemoteGitManagerRestrictedFactory;
import de.catma.document.Range;
import de.catma.document.annotation.TagReference;
import de.catma.document.source.ContentInfoSet;
import de.catma.document.source.FileOSType;
import de.catma.document.source.FileType;
import de.catma.document.source.IndexInfoSet;
import de.catma.document.source.SourceDocumentInfo;
import de.catma.document.source.TechInfoSet;
import de.catma.indexer.TermExtractor;
import de.catma.indexer.TermInfo;
import de.catma.project.ProjectReference;
import de.catma.properties.CATMAPropertyKey;
import de.catma.repository.git.GitProjectHandler;
import de.catma.repository.git.managers.JGitRepoManager;
import de.catma.repository.git.managers.interfaces.LocalGitRepositoryManager;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerRestricted;
import de.catma.repository.git.managers.jgit.JGitCommandFactory;
import de.catma.repository.git.managers.jgit.RelativeJGitCommandFactory;
import de.catma.tag.Property;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagManager;
import de.catma.tag.TagsetDefinition;
import de.catma.tag.Version;
import de.catma.user.User;
import de.catma.util.ColorConverter;
import de.catma.util.IDGenerator;

public class ProjectFixtures {
	private static final String DUMMY_USER_IDENTIFIER = "dummyUserIdentifier";
	private static final String DUMMY_USER_EMAIL_ADDRESS = "dummy@dummy.org";
	private static final String DUMMY_USER_PASSWORD = "dummyPassword";

	private static final Logger LOGGER = Logger.getLogger(ProjectFixtures.class.getName()); 

	public static List<ProjectReference> setUpProjectList(RemoteGitManagerRestrictedFactory remoteGitManagerRestrictedFactoryMock, String personalAccessToken) throws IOException {
		RemoteGitManagerRestricted remoteGitManagerRestrictedMock = Mockito.mock(RemoteGitManagerRestricted.class);
		
		when(remoteGitManagerRestrictedFactoryMock.create(anyString())).thenAnswer(new Answer<RemoteGitManagerRestricted>() {
			@Override
			public RemoteGitManagerRestricted answer(InvocationOnMock invocation) throws Throwable {
				if (invocation.getArguments().length > 0 && invocation.getArgument(0).equals(personalAccessToken)) {
					return remoteGitManagerRestrictedMock;
				}
				throw new GitLabApiException("401 Unauthorized");
			}
		});
		
		User userMock = Mockito.mock(User.class);
		
		when(remoteGitManagerRestrictedMock.getUser()).thenReturn(userMock);
		when(remoteGitManagerRestrictedMock.getUsername()).thenReturn(DUMMY_USER_IDENTIFIER);
		
		when(userMock.getIdentifier()).thenReturn(DUMMY_USER_IDENTIFIER);
		

		ProjectReference pr1 = new ProjectReference("project1", DUMMY_USER_IDENTIFIER, "First project", "First description");
		ProjectReference pr2 = new ProjectReference("project2", DUMMY_USER_IDENTIFIER, "Second project", "Second description");
		List<ProjectReference> prList = List.of(pr1, pr2);
		
		when(remoteGitManagerRestrictedMock.getProjectReferences()).thenReturn(prList);
		return prList;
	}
	
	public static void setUpRemoteGitManagerThrowing404(RemoteGitManagerRestrictedFactory remoteGitManagerRestrictedFactoryMock) throws Exception {
		RemoteGitManagerRestricted remoteGitManagerRestrictedMock = Mockito.mock(RemoteGitManagerRestricted.class);
		User userMock = Mockito.mock(User.class);
		
		when(remoteGitManagerRestrictedFactoryMock.create(anyString())).thenReturn(remoteGitManagerRestrictedMock);

		when(remoteGitManagerRestrictedMock.getUser()).thenReturn(userMock);
		when(remoteGitManagerRestrictedMock.getUsername()).thenReturn(DUMMY_USER_IDENTIFIER);
		when(remoteGitManagerRestrictedMock.getEmail()).thenReturn(DUMMY_USER_EMAIL_ADDRESS);
		when(remoteGitManagerRestrictedMock.getPassword()).thenReturn(DUMMY_USER_PASSWORD);
		when(userMock.getIdentifier()).thenReturn(DUMMY_USER_IDENTIFIER);

		when(remoteGitManagerRestrictedMock.getProjectReference(any(), any())).thenAnswer(new Answer<ProjectReference>() {
			@Override
			public ProjectReference answer(InvocationOnMock invocation) throws Throwable {
				throw new GitLabApiException("404 Project Not Found");
			}
		});
	}
	
	public static String setUpProjectWithDocument(
			RemoteGitManagerRestrictedFactory remoteGitManagerRestrictedFactoryMock, 
			String namespace, String projectId, String projectName, 
			String sourceDocumentUuid) throws Exception {
		
		LOGGER.info("setting up a 'fake' remote project");
		
		// set up a fake remote git project
		InitCommand init = Git.init();
		File remoteGitDir = Paths.get(CATMAPropertyKey.API_GIT_REPOSITORY_BASE_PATH.getValue(), "remote", namespace, projectId).toFile();
		
		if (remoteGitDir.exists()) {
			FileUtils.deleteDirectory(remoteGitDir);
		}
		remoteGitDir.mkdirs();
		init.setDirectory(remoteGitDir);
		try (Git gitApi = init.call()) {
			
			gitApi.commit()
					.setAllowEmpty(true)
					.setMessage("Created Project my_remote_project")
					.setCommitter(DUMMY_USER_IDENTIFIER, DUMMY_USER_EMAIL_ADDRESS)
					.call();
			gitApi.getRepository().close();
		}
		
		LOGGER.info("cloning the 'fake' remote project to a local project");

		// clone the fake remote git project to a local git project
		File localProjectGitDir = Paths.get(CATMAPropertyKey.API_GIT_REPOSITORY_BASE_PATH.getValue(), DUMMY_USER_IDENTIFIER, namespace, projectId).toFile();
		
		if (localProjectGitDir.exists()) {
			FileUtils.deleteDirectory(localProjectGitDir);
		}
		localProjectGitDir.mkdirs();
	
		JGitCommandFactory jGitCommandFactory = new RelativeJGitCommandFactory();
		CloneCommand cloneCommand = jGitCommandFactory.newCloneCommand();
		// setURI actually takes a plain path string and using a URI leads to a corrupt 'remote' configuration
		try (Git gitApi = cloneCommand.setURI(remoteGitDir.getAbsolutePath()).setDirectory(localProjectGitDir).call()) {
			gitApi.getRepository().close();			
		}
		
		// mock gitlab
		RemoteGitManagerRestricted remoteGitManagerRestrictedMock = Mockito.mock(RemoteGitManagerRestricted.class);
		User userMock = Mockito.mock(User.class);
		
		when(remoteGitManagerRestrictedFactoryMock.create(anyString())).thenReturn(remoteGitManagerRestrictedMock);

		when(remoteGitManagerRestrictedMock.getUser()).thenReturn(userMock);
		when(remoteGitManagerRestrictedMock.getUsername()).thenReturn(DUMMY_USER_IDENTIFIER);
		when(remoteGitManagerRestrictedMock.getEmail()).thenReturn(DUMMY_USER_EMAIL_ADDRESS);
		when(remoteGitManagerRestrictedMock.getPassword()).thenReturn(DUMMY_USER_PASSWORD);
		when(userMock.getIdentifier()).thenReturn(DUMMY_USER_IDENTIFIER);

		ZonedDateTime now = ZonedDateTime.now();
		ProjectReference projectReference = new ProjectReference(projectId, namespace, projectName, "a project description", now, now);
		when(remoteGitManagerRestrictedMock.getProjectReference(eq(namespace), eq(projectId))).thenReturn(projectReference);
		
		LOGGER.info("adding a SourceDocument to the project");
		
		// add SourceDocument
		File originalSourceDocument = new File("testdocs/rose_for_emily.pdf");
		File convertedSourceDocument = new File("testdocs/rose_for_emily_wobom.txt");

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
			705211438L
		);

		SourceDocumentInfo sourceDocumentInfo = new SourceDocumentInfo(
			indexInfoSet, contentInfoSet, techInfoSet
		);

		Map<String, List<TermInfo>> terms = new TermExtractor(
				IOUtils.toString(convertedSourceDocumentStream, techInfoSet.getCharset()),
				new ArrayList<>(),
				new ArrayList<>(),
				indexInfoSet.getLocale()
		).getTerms();
		

		String tokenizedSourceDocumentFileName = sourceDocumentUuid + "." + "json"; // GraphWorktreeProject.TOKENIZED_FILE_EXTENSION
		String convertedSourceDocumentFileName = sourceDocumentUuid + "." + "txt"; // GraphWorktreeProject.UTF8_CONVERSION_FILE_EXTENSION
		String originalSourceDcoumentFileName = sourceDocumentUuid + "_orig" + ".pdf"; // GraphWorktreeProject.ORIG_INFIX


		LocalGitRepositoryManager localGitRepositoryManager = new JGitRepoManager(CATMAPropertyKey.API_GIT_REPOSITORY_BASE_PATH.getValue(), userMock);
		
		GitProjectHandler gitProjectHandler =
				new GitProjectHandler(userMock, projectReference, localProjectGitDir, localGitRepositoryManager, remoteGitManagerRestrictedMock);
		gitProjectHandler.ensureUserBranch();

		gitProjectHandler.createSourceDocument(
				sourceDocumentUuid, 
				originalSourceDocumentStream, 
				originalSourceDcoumentFileName, 
				new FileInputStream(convertedSourceDocument), 
				convertedSourceDocumentFileName, 
				terms, 
				tokenizedSourceDocumentFileName, 
				sourceDocumentInfo);

		return gitProjectHandler.getDocument(sourceDocumentUuid).getContent();
	}


	public static List<String> setUpFullProject(
			RemoteGitManagerRestrictedFactory remoteGitManagerRestrictedFactoryMock,
			String namespace, String projectId, String projectName, 
			String sourceDocumentUuid, 
			String tagsetId, String tagsetName,
			String tagId, String tagName, 
			String annotationId1, String propertyName, String propertyValue) throws Exception {
		IDGenerator idGenerator = new IDGenerator();
		return setUpFullProject(
				remoteGitManagerRestrictedFactoryMock, namespace, projectId, projectName, sourceDocumentUuid, 
				tagsetId, tagsetName, tagId, tagName, String.format("#%s", ColorConverter.randomHex()),
				idGenerator.generateCollectionId(), "my collection",
				annotationId1, propertyName, propertyValue, 
				idGenerator.generate(),idGenerator.generate(),idGenerator.generate());
	}
	
	public static List<String> setUpFullProject(
			RemoteGitManagerRestrictedFactory remoteGitManagerRestrictedFactoryMock,
			String namespace, String projectId, String projectName, 
			String sourceDocumentUuid, 
			String tagsetId, String tagsetName,
			String tagId, String tagName, String tagHexColor,
			String annotationCollectionId, String annotationCollectionName,
			String annotationId1, String propertyName, String propertyValue,
			String annotationId2, String annotationId3, String annotationId4) throws Exception {
		
		LOGGER.info("setting up a 'fake' remote project");
		
		// set up a fake remote git project
		InitCommand init = Git.init();
		File remoteGitDir = Paths.get(CATMAPropertyKey.API_GIT_REPOSITORY_BASE_PATH.getValue(), "remote", namespace, projectId).toFile();
		
		LOGGER.info(String.format("'Fake' remote git dir is %s", remoteGitDir.toString()));
		
		if (remoteGitDir.exists()) {
			FileUtils.deleteDirectory(remoteGitDir);
		}
		remoteGitDir.mkdirs();
		init.setDirectory(remoteGitDir);
		try (Git gitApi = init.call()) {
			
			gitApi.commit()
					.setAllowEmpty(true)
					.setMessage("Created Project my_remote_project")
					.setCommitter(DUMMY_USER_IDENTIFIER, DUMMY_USER_EMAIL_ADDRESS)
					.call();
			gitApi.getRepository().close();
		}
		
		LOGGER.info("cloning the 'fake' remote project to a local project");

		// clone the fake remote git project to a local git project
		File localProjectGitDir = Paths.get(CATMAPropertyKey.API_GIT_REPOSITORY_BASE_PATH.getValue(), DUMMY_USER_IDENTIFIER, namespace, projectId).toFile();
		
		LOGGER.info(String.format("Local git dir is %s", localProjectGitDir.toString()));
		
		if (localProjectGitDir.exists()) {
			FileUtils.deleteDirectory(localProjectGitDir);
		}
		localProjectGitDir.mkdirs();
	
		JGitCommandFactory jGitCommandFactory = new RelativeJGitCommandFactory();
		CloneCommand cloneCommand = jGitCommandFactory.newCloneCommand();
		// setURI actually takes a plain path string and using a URI leads to a corrupt 'remote' configuration
		try (Git gitApi = cloneCommand.setURI(remoteGitDir.getAbsolutePath()).setDirectory(localProjectGitDir).call()) {
			gitApi.getRepository().close();			
		}
		
		// mock gitlab
		RemoteGitManagerRestricted remoteGitManagerRestrictedMock = Mockito.mock(RemoteGitManagerRestricted.class);
		User userMock = Mockito.mock(User.class);
		
		when(remoteGitManagerRestrictedFactoryMock.create(anyString())).thenReturn(remoteGitManagerRestrictedMock);

		when(remoteGitManagerRestrictedMock.getUser()).thenReturn(userMock);
		when(remoteGitManagerRestrictedMock.getUsername()).thenReturn(DUMMY_USER_IDENTIFIER);
		when(remoteGitManagerRestrictedMock.getEmail()).thenReturn(DUMMY_USER_EMAIL_ADDRESS);
		when(remoteGitManagerRestrictedMock.getPassword()).thenReturn(DUMMY_USER_PASSWORD);
		when(userMock.getIdentifier()).thenReturn(DUMMY_USER_IDENTIFIER);

		ZonedDateTime now = ZonedDateTime.now();
		ProjectReference projectReference = new ProjectReference(projectId, namespace, projectName, "a project description", now, now);
		when(remoteGitManagerRestrictedMock.getProjectReference(eq(namespace), eq(projectId))).thenReturn(projectReference);
		
		LOGGER.info("adding a SourceDocument to the project");
		
		// add SourceDocument
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
			705211438L
		);

		SourceDocumentInfo sourceDocumentInfo = new SourceDocumentInfo(
			indexInfoSet, contentInfoSet, techInfoSet
		);

		Map<String, List<TermInfo>> terms = new TermExtractor(
				IOUtils.toString(convertedSourceDocumentStream, techInfoSet.getCharset()),
				new ArrayList<>(),
				new ArrayList<>(),
				indexInfoSet.getLocale()
		).getTerms();
		

		String tokenizedSourceDocumentFileName = sourceDocumentUuid + "." + "json"; // GraphWorktreeProject.TOKENIZED_FILE_EXTENSION
		String convertedSourceDocumentFileName = sourceDocumentUuid + "." + "txt"; // GraphWorktreeProject.UTF8_CONVERSION_FILE_EXTENSION
		String originalSourceDcoumentFileName = sourceDocumentUuid + "_orig" + ".pdf"; // GraphWorktreeProject.ORIG_INFIX


		LocalGitRepositoryManager localGitRepositoryManager = new JGitRepoManager(CATMAPropertyKey.API_GIT_REPOSITORY_BASE_PATH.getValue(), userMock);
		
		GitProjectHandler gitProjectHandler =
				new GitProjectHandler(userMock, projectReference, localProjectGitDir, localGitRepositoryManager, remoteGitManagerRestrictedMock);
		gitProjectHandler.ensureUserBranch();

		gitProjectHandler.createSourceDocument(
				sourceDocumentUuid, 
				originalSourceDocumentStream, 
				originalSourceDcoumentFileName, 
				new FileInputStream(convertedSourceDocument), 
				convertedSourceDocumentFileName, 
				terms, 
				tokenizedSourceDocumentFileName, 
				sourceDocumentInfo);

		LOGGER.info("adding a Tagset to the project");

		// add Tagset
		gitProjectHandler.createTagset(tagsetId, tagsetName, "all the tags", null);
		TagsetDefinition tagset = new TagsetDefinition(tagsetId, tagsetName);
		
		TagLibrary tagLibrary = new TagLibrary();
		TagManager tagManager = new TagManager(tagLibrary);
		tagManager.addTagsetDefinition(tagset);
		
		IDGenerator idGenerator = new IDGenerator();
		
		// add Tag
		TagDefinition tag = new TagDefinition(tagId, tagName, null, tagsetId);
		tag.addSystemPropertyDefinition(
				new PropertyDefinition(
					idGenerator.generate(PropertyDefinition.SystemPropertyName.catma_displaycolor.name()), 
					PropertyDefinition.SystemPropertyName.catma_displaycolor.name(), 
					Collections.singletonList(ColorConverter.toRGBIntAsString(tagHexColor.substring(1))) // strip leading #
				)
		);


		
		tagManager.addTagDefinition(tagset, tag);

		PropertyDefinition propertyDef = new PropertyDefinition(idGenerator.generate(), propertyName, List.of("value1, value2"));
		tagManager.addUserDefinedPropertyDefinition(tag, propertyDef);
		
		gitProjectHandler.createOrUpdateTag(tagsetId, tag, "created 'my tag'");
		
		LOGGER.info("adding an AnnotationCollection to the project");

		// add AnnotationCollection
		gitProjectHandler.createAnnotationCollection(annotationCollectionId, annotationCollectionName, "all the annotations", sourceDocumentUuid, null, true);
		
		
		LOGGER.info("adding Annotations to the project");
		// add Annotations
		TagInstance tagInstance1 = 
				new TagInstance(
						annotationId1, 
						tagId,
						DUMMY_USER_IDENTIFIER,
						ZonedDateTime.now().format(DateTimeFormatter.ofPattern(Version.DATETIMEPATTERN)),
						tag.getUserDefinedPropertyDefinitions(),
						tagsetId);
		
		tagInstance1.addUserDefinedProperty(new Property(propertyDef.getUuid(), Collections.singletonList(propertyValue)));

		TagInstance tagInstance2 = 
				new TagInstance(
						annotationId2, 
						tagId,
						DUMMY_USER_IDENTIFIER,
						ZonedDateTime.now().format(DateTimeFormatter.ofPattern(Version.DATETIMEPATTERN)),
						tag.getUserDefinedPropertyDefinitions(),
						tagsetId);

		TagInstance tagInstance3 = 
				new TagInstance(
						annotationId3, 
						tagId,
						DUMMY_USER_IDENTIFIER,
						ZonedDateTime.now().format(DateTimeFormatter.ofPattern(Version.DATETIMEPATTERN)),
						tag.getUserDefinedPropertyDefinitions(),
						tagsetId);

		TagInstance tagInstance4 = 
				new TagInstance(
						annotationId4, 
						tagId,
						DUMMY_USER_IDENTIFIER,
						ZonedDateTime.now().format(DateTimeFormatter.ofPattern(Version.DATETIMEPATTERN)),
						tag.getUserDefinedPropertyDefinitions(),
						tagsetId);

		tagInstance1.addUserDefinedProperty(new Property(propertyDef.getUuid(), Collections.singletonList(propertyValue)));

		Range range1 = new Range(2, 10);
		TagReference tagReference1 = new TagReference(annotationCollectionId, tagInstance1, sourceDocumentUuid, range1);
		
		Range range2 = new Range(15, 20);
		TagReference tagReference2 = new TagReference(annotationCollectionId, tagInstance2, sourceDocumentUuid, range2);

		Range range3 = new Range(25, 30);
		TagReference tagReference3 = new TagReference(annotationCollectionId, tagInstance3, sourceDocumentUuid, range3);

		Range range4 = new Range(35, 40);
		TagReference tagReference4 = new TagReference(annotationCollectionId, tagInstance4, sourceDocumentUuid, range4);
		
		List<TagReference> refs = List.of(tagReference1, tagReference2, tagReference3, tagReference4);
		gitProjectHandler.addTagReferencesToCollection(annotationCollectionId, refs, tagLibrary);

		gitProjectHandler.addCollectionsToStagedAndCommit(Collections.singleton(annotationCollectionId), "some nice annotations", false, true);
		
		// return annotated phrases for validation
		return refs.stream().sorted(PreProject.TAG_REFERENCE_COMPARATOR).map(tr -> {
			try {
				return gitProjectHandler.getDocument(sourceDocumentUuid).getContent(tr.getRange());
			}
			catch(IOException e) {
				throw new RuntimeException(e);
			}
		
		}).toList();
	}
}
