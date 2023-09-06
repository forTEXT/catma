package de.catma.repository.git;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.eventbus.EventBus;
import com.vaadin.ui.UI;
import de.catma.backgroundservice.BackgroundService;
import de.catma.backgroundservice.DefaultProgressCallable;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.backgroundservice.ProgressListener;
import de.catma.document.annotation.AnnotationCollection;
import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.annotation.TagReference;
import de.catma.document.comment.Comment;
import de.catma.document.comment.Reply;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentReference;
import de.catma.document.source.contenthandler.StandardContentHandler;
import de.catma.indexer.IndexedProject;
import de.catma.indexer.Indexer;
import de.catma.indexer.TermExtractor;
import de.catma.indexer.TermInfo;
import de.catma.project.OpenProjectListener;
import de.catma.project.ProjectReference;
import de.catma.project.event.*;
import de.catma.properties.CATMAPropertyKey;
import de.catma.rbac.RBACPermission;
import de.catma.rbac.RBACRole;
import de.catma.rbac.RBACSubject;
import de.catma.repository.git.graph.interfaces.*;
import de.catma.repository.git.graph.lazy.LazyGraphProjectHandler;
import de.catma.repository.git.managers.JGitCredentialsManager;
import de.catma.repository.git.managers.interfaces.LocalGitRepositoryManager;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerRestricted;
import de.catma.repository.git.resource.provider.LatestContribution;
import de.catma.repository.git.resource.provider.LatestContributionsResourceProvider;
import de.catma.repository.git.resource.provider.SynchronizedResourceProvider;
import de.catma.repository.git.resource.provider.interfaces.GitProjectResourceProvider;
import de.catma.repository.git.resource.provider.interfaces.GitProjectResourceProviderFactory;
import de.catma.serialization.TagLibrarySerializationHandler;
import de.catma.serialization.TagsetDefinitionImportStatus;
import de.catma.serialization.tei.TeiSerializationHandlerFactory;
import de.catma.serialization.tei.TeiTagLibrarySerializationHandler;
import de.catma.serialization.tei.TeiUserMarkupCollectionDeserializer;
import de.catma.tag.*;
import de.catma.tag.TagManager.TagManagerEvent;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.user.Member;
import de.catma.user.User;
import de.catma.util.IDGenerator;
import de.catma.util.Pair;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.mime.MediaType;

import javax.lang.model.type.NullType;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class GraphWorktreeProject implements IndexedProject {
	private static final String UTF8_CONVERSION_FILE_EXTENSION = "txt";
	private static final String ORIG_INFIX = "_orig";
	private static final String TOKENIZED_FILE_EXTENSION = "json";

	private final Logger logger = Logger.getLogger(GraphWorktreeProject.class.getName());

	private final User user;
	private final GitProjectHandler gitProjectHandler;
	private final ProjectReference projectReference;
	private final TagManager tagManager;
	private final BackgroundService backgroundService;

	private final GraphProjectHandler graphProjectHandler;
	private final Indexer indexer;

	private final String tempDir;
	private final IDGenerator idGenerator;

	private EventBus eventBus;
	private PropertyChangeSupport propertyChangeSupport;

	private boolean tagManagerListenersEnabled = true;
	private String rootRevisionHash;

	public GraphWorktreeProject(
			User user,
			GitProjectHandler gitProjectHandler,
			ProjectReference projectReference,
			TagManager tagManager,
			BackgroundService backgroundService,
			EventBus eventBus
	) {
		this.user = user;
		this.gitProjectHandler = gitProjectHandler;
		this.projectReference = projectReference;
		this.tagManager = tagManager;
		this.backgroundService = backgroundService;
		this.eventBus = eventBus;

		this.graphProjectHandler = new LazyGraphProjectHandler(
				this.projectReference,
				this.user,
				this.tagManager,
				new TagsetsProvider() {
					@Override
					public List<TagsetDefinition> getTagsets() {
						return GraphWorktreeProject.this.gitProjectHandler.getTagsets();
					}
				},
				new DocumentsProvider() {
					@Override
					public List<SourceDocument> getDocuments() {
						return GraphWorktreeProject.this.gitProjectHandler.getDocuments();
					}
				},
				new DocumentProvider() {
					@Override
					public SourceDocument getDocument(String documentId) throws IOException {
						return GraphWorktreeProject.this.gitProjectHandler.getDocument(documentId);
					}
				},
				new DocumentIndexProvider() {
					@Override
					public Map getDocumentIndex(String documentId) throws IOException {
						return GraphWorktreeProject.this.gitProjectHandler.getDocumentIndex(documentId);
					}
				},
				new CommentsProvider() {
					@Override
					public List<Comment> getComments(List<String> documentIds) throws Exception {
						return GraphWorktreeProject.this.gitProjectHandler.getCommentsWithReplies(documentIds);
					}
				},
				new CollectionProvider() {
					@Override
					public AnnotationCollection getCollection(String collectionId, TagLibrary tagLibrary) throws IOException {
						return GraphWorktreeProject.this.gitProjectHandler.getCollection(collectionId, tagLibrary);
					}
				}
		);

		this.indexer = this.graphProjectHandler.createIndexer();

		this.tempDir = CATMAPropertyKey.TEMP_DIR.getValue();
		this.idGenerator = new IDGenerator();

		this.propertyChangeSupport = new PropertyChangeSupport(this);
	}

	@Override
	public void addEventListener(ProjectEvent projectEvent, PropertyChangeListener propertyChangeListener) {
		propertyChangeSupport.addPropertyChangeListener(projectEvent.name(), propertyChangeListener);
	}

	@Override
	public void removeEventListener(ProjectEvent projectEvent, PropertyChangeListener propertyChangeListener) {
		if (propertyChangeSupport != null) {
			propertyChangeSupport.removePropertyChangeListener(projectEvent.name(), propertyChangeListener);
		}
	}


	@Override
	public String getId() {
		return projectReference.getProjectId();
	}

	@Override
	public String getName() {
		return projectReference.getName();
	}

	@Override
	public String toString() {
		return this.projectReference.toString();
	}

	@Override
	public String getDescription() {
		return projectReference.getDescription();
	}

	@Override
	public String getVersion() {
		return rootRevisionHash;
	}

	@Override
	public User getCurrentUser() {
		return user;
	}

	@Override
	public TagManager getTagManager() {
		return tagManager;
	}

	@Override
	public Indexer getIndexer() {
		return indexer;
	}


	@Override
	public boolean isReadOnly() {
		return gitProjectHandler.isReadOnly();
	}

	@Override
	public void setLatestContributionsView(boolean enabled, OpenProjectListener openProjectListener) throws Exception {
		if (hasUncommittedChanges() || hasUntrackedChanges()) {
			throw new IllegalStateException("There are uncommitted changes that need to be committed first!");
		}

		logger.info(
				String.format(
						"Switching view mode for project \"%1$s\" with ID %2$s to '%3$s'",
						projectReference.getName(),
						projectReference.getProjectId(),
						enabled ? "latest contributions" : "synchronized"
				)
		);

		try {
			logger.info(
					String.format("Checking for conflicts in project \"%s\" with ID %s", projectReference.getName(), projectReference.getProjectId())
			);
			if (gitProjectHandler.hasConflicts()) {
				openProjectListener.failure(new IllegalStateException(
						String.format(
								"There are conflicts in project \"%s\" with ID %s. Please contact support.",
								projectReference.getName(),
								projectReference.getProjectId()
						)
				));
				return;
			}

			gitProjectHandler.ensureUserBranch();

			if (enabled) {
				Set<Member> members = gitProjectHandler.getProjectMembers();
				List<String> possibleBranches = members.stream()
						.filter(member -> !member.getIdentifier().equals(getCurrentUser().getIdentifier()))
						.map(member -> "refs/remotes/origin/" + member.getIdentifier())
						.collect(Collectors.toList());

				Set<LatestContribution> latestContributions = gitProjectHandler.getLatestContributions(possibleBranches);

				gitProjectHandler.setResourceProvider(new GitProjectResourceProviderFactory() {
					@Override
					public GitProjectResourceProvider createResourceProvider(
							String projectId,
							ProjectReference projectReference,
							File projectPath,
							LocalGitRepositoryManager localGitRepositoryManager,
							RemoteGitManagerRestricted remoteGitServerManager,
							JGitCredentialsManager jGitCredentialsManager
					) {
						return new LatestContributionsResourceProvider(
								projectId,
								projectReference,
								projectPath,
								localGitRepositoryManager,
								remoteGitServerManager,
								latestContributions
						);
					}
				});
			}
			else {
				gitProjectHandler.setResourceProvider(new GitProjectResourceProviderFactory() {
					@Override
					public GitProjectResourceProvider createResourceProvider(
							String projectId,
							ProjectReference projectReference,
							File projectPath,
							LocalGitRepositoryManager localGitRepositoryManager,
							RemoteGitManagerRestricted remoteGitServerManager,
							JGitCredentialsManager jGitCredentialsManager
					) {
						return new SynchronizedResourceProvider(
								projectId,
								projectReference,
								projectPath,
								localGitRepositoryManager,
								remoteGitServerManager,
								jGitCredentialsManager
						);
					}
				});
			}

			ProgressListener progressListener = new ProgressListener() {
				@Override
				public void setProgress(String value, Object... args) {
					openProjectListener.progress(value, args);
				}
			};

			graphProjectHandler.ensureProjectRevisionIsLoaded(
					rootRevisionHash,
					true, // forceGraphReload
					// TODO: unfortunately we can't pass the CollectionsProvider into the LazyGraphProjectHandler ctor (yet) because of the ProgressListener
					new CollectionsProvider() {
						@Override
						public List<AnnotationCollection> getCollections(TagLibrary tagLibrary) throws IOException {
							return gitProjectHandler.getCollections(tagLibrary, progressListener, true);
						}
					},
					backgroundService,
					new ExecutionListener<NullType>() {
						@Override
						public void error(Throwable t) {
							openProjectListener.failure(t);
						}

						@Override
						public void done(NullType result) {
							logger.info(
									String.format(
											"Project \"%s\" with ID %s has been re-opened after switching view mode",
											projectReference.getName(),
											projectReference.getProjectId()
									)
							);
							openProjectListener.ready(GraphWorktreeProject.this);
						}
					},
					progressListener
			);
		}
		catch (Exception e) {
			openProjectListener.failure(e);
		}
	}


	private void initTagManagerListeners() {
		PropertyChangeListener tagsetDefinitionChangedListener = new PropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent evt) {
				if (!tagManagerListenersEnabled) {
					return;
				}

				try {
					if (evt.getOldValue() == null) { // TagsetDefinition was added
						final TagsetDefinition tagsetDefinition = (TagsetDefinition) evt.getNewValue();
						addTagsetDefinition(tagsetDefinition);
					}
					else if (evt.getNewValue() == null) { // TagsetDefinition was deleted
						final TagsetDefinition tagsetDefinition = (TagsetDefinition) evt.getOldValue();
						removeTagsetDefinition(tagsetDefinition);
					}
					else { // TagsetDefinition was updated
						final TagsetDefinition tagsetDefinition = (TagsetDefinition) evt.getNewValue();
						updateTagsetDefinition(tagsetDefinition);
					}
				}
				catch (Exception e) {
					propertyChangeSupport.firePropertyChange(
							ProjectEvent.exceptionOccurred.name(),
							null,
							e
					);
				}
			}
		};
		tagManager.addPropertyChangeListener(TagManagerEvent.tagsetDefinitionChanged, tagsetDefinitionChangedListener);

		PropertyChangeListener tagDefinitionChangedListener = new PropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent evt) {
				if (!tagManagerListenersEnabled) {
					return;
				}

				try {
					if (evt.getOldValue() == null) { // TagDefinition was added
						@SuppressWarnings("unchecked")
						final Pair<TagsetDefinition, TagDefinition> args = (Pair<TagsetDefinition, TagDefinition>) evt.getNewValue();
						addTagDefinition(args.getSecond(), args.getFirst());
					}
					else if (evt.getNewValue() == null) { // TagDefinition was deleted
						@SuppressWarnings("unchecked")
						final Pair<TagsetDefinition, TagDefinition> args = (Pair<TagsetDefinition, TagDefinition>) evt.getOldValue();
						removeTagDefinition(args.getSecond(), args.getFirst());
					}
					else { // TagDefinition was updated
						final TagDefinition tag = (TagDefinition) evt.getNewValue();
						final TagsetDefinition tagset = (TagsetDefinition) evt.getOldValue();
						updateTagDefinition(tag, tagset);
					}
				}
				catch (Exception e) {
					propertyChangeSupport.firePropertyChange(
							ProjectEvent.exceptionOccurred.name(),
							null,
							e
					);
				}
			}
		};
		tagManager.addPropertyChangeListener(TagManagerEvent.tagDefinitionChanged, tagDefinitionChangedListener);

		PropertyChangeListener userDefinedPropertyChangedListener = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				if (!tagManagerListenersEnabled) {
					return;
				}

				try {
					if (evt.getOldValue() == null) { // PropertyDefinition was added
						@SuppressWarnings("unchecked")
						Pair<PropertyDefinition, TagDefinition> args = (Pair<PropertyDefinition, TagDefinition>) evt.getNewValue();
						addPropertyDefinition(args.getFirst(), args.getSecond());
					}
					else if (evt.getNewValue() == null) { // PropertyDefinition was deleted
						@SuppressWarnings("unchecked")
						Pair<PropertyDefinition, Pair<TagDefinition, TagsetDefinition>> args =
								(Pair<PropertyDefinition, Pair<TagDefinition, TagsetDefinition>>) evt.getOldValue();
						removePropertyDefinition(args.getFirst(), args.getSecond().getFirst(), args.getSecond().getSecond());
					}
					else { // PropertyDefinition was updated
						PropertyDefinition propertyDefinition = (PropertyDefinition) evt.getNewValue();
						TagDefinition tagDefinition = (TagDefinition) evt.getOldValue();
						updatePropertyDefinition(propertyDefinition, tagDefinition);
					}
				}
				catch (Exception e) {
					propertyChangeSupport.firePropertyChange(
							ProjectEvent.exceptionOccurred.name(),
							null,
							e
					);
				}
			}
		};
		tagManager.addPropertyChangeListener(TagManagerEvent.userPropertyDefinitionChanged, userDefinedPropertyChangedListener);
	}

	@Override
	public void open(OpenProjectListener openProjectListener) {
		try {
			logger.info(
					String.format("Opening project \"%s\" with ID %s", projectReference.getName(), projectReference.getProjectId())
			);

			rootRevisionHash = gitProjectHandler.getRootRevisionHash();
			logger.info(
					String.format(
							"Revision hash for project \"%1$s\" with ID %2$s is: %3$s",
							projectReference.getName(),
							projectReference.getProjectId(),
							rootRevisionHash
					)
			);

			logger.info(
					String.format("Checking for conflicts in project \"%s\" with ID %s", projectReference.getName(), projectReference.getProjectId())
			);
			if (gitProjectHandler.hasConflicts()) {
				openProjectListener.failure(
						new IllegalStateException(
								String.format(
										"There are conflicts in project \"%s\" with ID %s. Please contact support.",
										projectReference.getName(),
										projectReference.getProjectId()
								)
						)
				);
				return;
			}

			gitProjectHandler.ensureUserBranch();

			boolean forceGraphReload = false;
			if (gitProjectHandler.hasUncommittedChanges() || gitProjectHandler.hasUntrackedChanges()) {
				commitAndPushChanges("Auto-committing changes on project open");
				// calling commitAndPushChanges at this stage causes the project revision to be updated, so GraphProjectHandler.ensureProjectRevisionIsLoaded
				// won't do anything unless we force it
				forceGraphReload = true;
			}

			gitProjectHandler.verifyCollections();

			ProgressListener progressListener = new ProgressListener() {
				@Override
				public void setProgress(String value, Object... args) {
					logger.info(String.format(value, args));
					openProjectListener.progress(value, args);
				}
			};

			graphProjectHandler.ensureProjectRevisionIsLoaded(
					rootRevisionHash,
					forceGraphReload,
					// TODO: unfortunately we can't pass the CollectionsProvider into the LazyGraphProjectHandler ctor (yet) because of the ProgressListener
					new CollectionsProvider() {
						@Override
						public List<AnnotationCollection> getCollections(TagLibrary tagLibrary) throws IOException {
							return gitProjectHandler.getCollections(tagLibrary, progressListener, true);
						}
					},
					backgroundService,
					new ExecutionListener<NullType>() {
						@Override
						public void error(Throwable t) {
							openProjectListener.failure(t);
						}

						@Override
						public void done(NullType result) {
							initTagManagerListeners();

							logger.info(
									String.format(
											"Project \"%s\" with ID %s has been opened",
											projectReference.getName(),
											projectReference.getProjectId()
									)
							);
							openProjectListener.ready(GraphWorktreeProject.this);
						}
					},
					progressListener
			);
		}
		catch (Exception e) {
			openProjectListener.failure(e);
		}
	}

	@Override
	public void close() {
		try {
			if (gitProjectHandler.hasConflicts()) {
				logger.warning(
						String.format(
								"Project \"%s\" with ID %s has conflicts on closing, skipping auto-commit and -push",
								projectReference.getName(),
								projectReference.getProjectId()
						)
				);
			}
			else {
				commitAndPushChanges("Auto-committing changes on project close");
			}
		}
		catch (IOException e) {
			((ErrorHandler) UI.getCurrent()).showAndLogError("Error closing project", e);
		}

		try {
			for (PropertyChangeListener listener : propertyChangeSupport.getPropertyChangeListeners()) {
				propertyChangeSupport.removePropertyChangeListener(listener);
			}
			propertyChangeSupport = null;
			eventBus = null;
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "Error closing project", e);
		}
	}


	// tagset & tag operations
	@Override
	public Collection<TagsetDefinition> getTagsets() {
		return tagManager.getTagLibrary().getTagsetDefinitions();
	}

	private void addTagsetDefinition(TagsetDefinition tagsetDefinition) throws Exception {
		if (isReadOnly()) {
			throw new IllegalStateException(
					String.format(
							"Project \"%s\" is in read-only mode! Cannot create tagset \"%s\".",
							projectReference.getName(),
							tagsetDefinition.getName()
					)
			);
		}

		tagsetDefinition.setResponsibleUser(user.getIdentifier());

		String oldRootRevisionHash = rootRevisionHash;

		// create tagset in repo and commit
		rootRevisionHash = gitProjectHandler.createTagset(
				tagsetDefinition.getUuid(),
				tagsetDefinition.getName(),
				tagsetDefinition.getDescription(),
				tagsetDefinition.getForkedFromCommitURL()
		);

		// update revision hash on GraphProjectHandler
		graphProjectHandler.updateProjectRevision(oldRootRevisionHash, rootRevisionHash);
	}

	private void updateTagsetDefinition(TagsetDefinition tagsetDefinition) throws Exception {
		if (isReadOnly()) {
			throw new IllegalStateException(
					String.format(
							"Project \"%1$s\" is in read-only mode! Cannot update tagset \"%2$s\" with ID %3$s.",
							projectReference.getName(),
							tagsetDefinition.getName(),
							tagsetDefinition.getUuid()
					)
			);
		}

		String oldRootRevisionHash = rootRevisionHash;

		// update tagset in repo and commit
		rootRevisionHash = gitProjectHandler.updateTagset(tagsetDefinition);

		// update revision hash on GraphProjectHandler
		graphProjectHandler.updateProjectRevision(oldRootRevisionHash, rootRevisionHash);
	}

	private void removeTagsetDefinition(TagsetDefinition tagsetDefinition) throws Exception {
		if (isReadOnly()) {
			throw new IllegalStateException(
					String.format(
							"Project \"%s\" is in read-only mode! Cannot delete tagset \"%s\".",
							projectReference.getName(),
							tagsetDefinition.getName()
					)
			);
		}

		String oldRootRevisionHash = rootRevisionHash;

		// collect corresponding annotations
		// TODO: this always produces empty multimaps, because the tagset has already been removed from the tag library at this stage
		//       the annotations are however deleted the next time we check for orphans (eg: on project open, sync or view mode change)
		Multimap<String, TagReference> tagReferencesByCollectionId = graphProjectHandler.getTagReferencesByCollectionId(tagsetDefinition);
		Multimap<String, TagInstance> tagInstancesByCollectionId = Multimaps.transformValues(
				tagReferencesByCollectionId, TagReference::getTagInstance
		);

		// delete tagset and corresponding annotations from repo and commit
		rootRevisionHash = gitProjectHandler.removeTagset(tagsetDefinition, tagInstancesByCollectionId);

		// update revision hash on GraphProjectHandler
		graphProjectHandler.updateProjectRevision(oldRootRevisionHash, rootRevisionHash);
	}

	private void addTagDefinition(TagDefinition tagDefinition, TagsetDefinition tagsetDefinition) throws Exception {
		if (isReadOnly()) {
			throw new IllegalStateException(
					String.format(
							"Project \"%1$s\" is in read-only mode! Cannot add tag \"%2$s\" to tagset \"%3$s\".",
							projectReference.getName(),
							tagDefinition.getName(),
							tagsetDefinition.getName()
					)
			);
		}

		String oldRootRevisionHash = rootRevisionHash;

		// create tag in repo and commit
		rootRevisionHash = gitProjectHandler.createOrUpdateTag(
				tagsetDefinition.getUuid(),
				tagDefinition,
				String.format(
						"Added tag \"%1$s\" with ID %2$s to tagset \"%3$s\" with ID %4$s",
						tagDefinition.getName(),
						tagDefinition.getUuid(),
						tagsetDefinition.getName(),
						tagsetDefinition.getUuid()
				)
		);

		// update revision hash on GraphProjectHandler
		graphProjectHandler.updateProjectRevision(oldRootRevisionHash, rootRevisionHash);
	}

	private void updateTagDefinition(TagDefinition tagDefinition, TagsetDefinition tagsetDefinition) throws Exception {
		if (isReadOnly()) {
			throw new IllegalStateException(
					String.format(
							"Project \"%1$s\" is in read-only mode! Cannot update tag \"%2$s\" with ID %3$s in tagset \"%4$s\".",
							projectReference.getName(),
							tagDefinition.getName(),
							tagDefinition.getUuid(),
							tagsetDefinition.getName()
					)
			);
		}

		String oldRootRevisionHash = rootRevisionHash;

		// update tag in repo and commit
		rootRevisionHash = gitProjectHandler.createOrUpdateTag(
				tagsetDefinition.getUuid(),
				tagDefinition,
				String.format(
						"Updated tag \"%1$s\" with ID %2$s in tagset \"%3$s\" with ID %4$s",
						tagDefinition.getName(),
						tagDefinition.getUuid(),
						tagsetDefinition.getName(),
						tagsetDefinition.getUuid()
				)
		);

		// update revision hash on GraphProjectHandler
		graphProjectHandler.updateProjectRevision(oldRootRevisionHash, rootRevisionHash);
	}

	private void removeTagDefinition(TagDefinition tagDefinition, TagsetDefinition tagsetDefinition) throws Exception {
		if (isReadOnly()) {
			throw new IllegalStateException(
					String.format(
							"Project \"%1$s\" is in read-only mode! Cannot delete tag \"%2$s\" from tagset \"%3$s\".",
							projectReference.getName(),
							tagDefinition.getName(),
							tagsetDefinition.getName()
					)
			);
		}

		String oldRootRevisionHash = rootRevisionHash;

		// collect corresponding annotations
		Multimap<String, TagReference> tagReferencesByCollectionId = graphProjectHandler.getTagReferencesByCollectionId(tagDefinition);
		Multimap<String, TagInstance> tagInstancesByCollectionId = Multimaps.transformValues(
				tagReferencesByCollectionId, TagReference::getTagInstance
		);

		// delete tag and corresponding annotations from repo and commit
		rootRevisionHash = gitProjectHandler.removeTagAndAnnotations(tagDefinition, tagInstancesByCollectionId);

		// update revision hash on GraphProjectHandler
		graphProjectHandler.updateProjectRevision(oldRootRevisionHash, rootRevisionHash);

		// fire annotation change events for each collection
		for (String collectionId : tagInstancesByCollectionId.keySet()) {
			Collection<String> deletedTagInstanceIds = tagInstancesByCollectionId.get(collectionId).stream()
					.map(TagInstance::getUuid).collect(Collectors.toList());

			propertyChangeSupport.firePropertyChange(
					ProjectEvent.tagReferencesChanged.name(),
					new Pair<>(collectionId, deletedTagInstanceIds),
					null
			);
		}
	}

	private void addPropertyDefinition(PropertyDefinition propertyDefinition, TagDefinition tagDefinition) throws Exception {
		if (isReadOnly()) {
			throw new IllegalStateException(
					String.format(
							"Project \"%1$s\" is in read-only mode! Cannot add property \"%2$s\" to tag \"%3$s\" in tagset with ID %4$s.",
							projectReference.getName(),
							propertyDefinition.getName(),
							tagDefinition.getName(),
							tagDefinition.getTagsetDefinitionUuid()
					)
			);
		}

		String oldRootRevisionHash = rootRevisionHash;

		// update tag in repo and commit
		TagsetDefinition tagsetDefinition = tagManager.getTagLibrary().getTagsetDefinition(tagDefinition);

		rootRevisionHash = gitProjectHandler.createOrUpdateTag(
				tagsetDefinition.getUuid(),
				tagDefinition,
				String.format(
						"Added property \"%1$s\" with ID %2$s to tag \"%3$s\" with ID %4$s in tagset \"%5$s\" with ID %6$s",
						propertyDefinition.getName(),
						propertyDefinition.getUuid(),
						tagDefinition.getName(),
						tagDefinition.getUuid(),
						tagsetDefinition.getName(),
						tagsetDefinition.getUuid()
				)
		);

		// update revision hash on GraphProjectHandler
		graphProjectHandler.updateProjectRevision(oldRootRevisionHash, rootRevisionHash);
	}

	private void updatePropertyDefinition(PropertyDefinition propertyDefinition, TagDefinition tagDefinition) throws Exception {
		if (isReadOnly()) {
			throw new IllegalStateException(
					String.format(
							"Project \"%1$s\" is in read-only mode! Cannot update property \"%2$s\" with ID %3$s for tag \"%4$s\" in tagset with ID %5$s.",
							projectReference.getName(),
							propertyDefinition.getName(),
							propertyDefinition.getUuid(),
							tagDefinition.getName(),
							tagDefinition.getTagsetDefinitionUuid()
					)
			);
		}

		String oldRootRevisionHash = rootRevisionHash;

		// update tag in repo and commit
		TagsetDefinition tagsetDefinition = tagManager.getTagLibrary().getTagsetDefinition(tagDefinition);

		rootRevisionHash = gitProjectHandler.createOrUpdateTag(
				tagsetDefinition.getUuid(),
				tagDefinition,
				String.format(
						"Updated property \"%1$s\" with ID %2$s for tag \"%3$s\" with ID %4$s in tagset \"%5$s\" with ID %6$s",
						propertyDefinition.getName(),
						propertyDefinition.getUuid(),
						tagDefinition.getName(),
						tagDefinition.getUuid(),
						tagsetDefinition.getName(),
						tagsetDefinition.getUuid()
				)
		);

		// update revision hash on GraphProjectHandler
		graphProjectHandler.updateProjectRevision(oldRootRevisionHash, rootRevisionHash);
	}

	private void removePropertyDefinition(
			PropertyDefinition propertyDefinition,
			TagDefinition tagDefinition,
			TagsetDefinition tagsetDefinition
	) throws Exception {
		if (isReadOnly()) {
			throw new IllegalStateException(
					String.format(
							"Project \"%1$s\" is in read-only mode! Cannot delete property \"%2$s\" from tag \"%3$s\" in tagset \"%4$s\".",
							projectReference.getName(),
							propertyDefinition.getName(),
							tagDefinition.getName(),
							tagsetDefinition.getName()
					)
			);
		}

		// collect corresponding annotations
		Multimap<String, TagReference> tagReferencesByCollectionId = graphProjectHandler.getTagReferencesByCollectionId(tagDefinition);

		// auto-commit affected collections before proceeding (in case they have any uncommitted changes)
		gitProjectHandler.addCollectionsToStagedAndCommit(
				tagReferencesByCollectionId.keySet(),
				String.format(
						"Auto-committing changes before performing an update of annotations "
								+ "as part of the deletion of the property \"%s\" with ID %s",
						propertyDefinition.getName(),
						propertyDefinition.getUuid()
				),
				false, // don't force
				false // don't push now, this happens when the property definition is deleted below
		);

		// delete properties from corresponding annotations in repo (no commit)
		for (String collectionId : tagReferencesByCollectionId.keySet()) {
			Collection<TagReference> tagReferences = tagReferencesByCollectionId.get(collectionId);
			Set<TagInstance> tagInstances = tagReferences.stream()
					.map(TagReference::getTagInstance)
					.collect(Collectors.toSet());

			tagInstances.forEach(
					tagInstance -> tagInstance.removeUserDefinedProperty(propertyDefinition.getUuid())
			);

			for (TagInstance tagInstance : tagInstances) {
				gitProjectHandler.updateTagInstance(
						collectionId,
						tagInstance,
						tagReferences.stream()
								.filter(tagRef -> tagRef.getTagInstanceId().equals(tagInstance.getUuid()))
								.collect(Collectors.toList()),
						tagManager.getTagLibrary()
				);
			}
		}

		String oldRootRevisionHash = rootRevisionHash;

		// delete property definition from repo and commit
		rootRevisionHash = gitProjectHandler.removePropertyDefinition(
				propertyDefinition,
				tagDefinition,
				tagsetDefinition,
				tagReferencesByCollectionId.keySet()
		);

		// update revision hash on GraphProjectHandler
		graphProjectHandler.updateProjectRevision(oldRootRevisionHash, rootRevisionHash);
	}

	@Override
	public List<TagsetDefinitionImportStatus> prepareTagLibraryForImport(InputStream inputStream) throws IOException {
		TeiSerializationHandlerFactory factory = new TeiSerializationHandlerFactory(rootRevisionHash);
		factory.setTagManager(new TagManager(new TagLibrary()));

		TagLibrarySerializationHandler tagLibrarySerializationHandler = factory.getTagLibrarySerializationHandler();
		TagLibrary tagLibraryToImport = tagLibrarySerializationHandler.deserialize(null, inputStream);

		List<TagsetDefinitionImportStatus> tagsetDefinitionImportStatuses = new ArrayList<>();

		for (TagsetDefinition tagsetDefinition : tagLibraryToImport) {
			boolean tagsetAlreadyInProject = tagManager.getTagLibrary().getTagsetDefinition(tagsetDefinition.getUuid()) != null;
			tagsetDefinitionImportStatuses.add(new TagsetDefinitionImportStatus(tagsetDefinition, tagsetAlreadyInProject));
		}

		return tagsetDefinitionImportStatuses;
	}

	private void importTagHierarchy(TagDefinition tag, TagsetDefinition tagset, TagsetDefinition targetTagset) {
		for (TagDefinition childTag : tagset.getDirectChildren(tag)) {
			tagManager.addTagDefinition(targetTagset, childTag);
			importTagHierarchy(childTag, tagset, targetTagset);
		}
	}

	@Override
	public void importTagsets(List<TagsetDefinitionImportStatus> tagsetDefinitionImportStatuses) throws IOException {
		if (isReadOnly()) {
			throw new IllegalStateException(
					String.format("Project \"%s\" is in read-only mode! Cannot import tagsets.", projectReference.getName())
			);
		}

		for (TagsetDefinitionImportStatus tagsetDefinitionImportStatus : tagsetDefinitionImportStatuses) {
			if (!tagsetDefinitionImportStatus.isDoImport()) {
				continue;
			}

			TagsetDefinition tagset = tagsetDefinitionImportStatus.getTagset();

			if (!tagsetDefinitionImportStatus.isCurrent()) { // new tagset
				try {
					// disable listeners that would otherwise interfere with the import process
					tagManagerListenersEnabled = false;

					addTagsetDefinition(tagset);
					tagManager.addTagsetDefinition(tagset);
				}
				catch (Exception e) {
					throw new IOException(
							String.format("Failed to import tagset \"%s\"! The import has been aborted.", tagset.getName()),
							e
					);
				}
				finally {
					tagManagerListenersEnabled = true;
				}

				for (TagDefinition tag : tagset.getRootTagDefinitions()) {
					// TODO: not clear what these are doing (other than raising events) as source and destination tagsets are the same, test
					tagManager.addTagDefinition(tagset, tag);
					importTagHierarchy(tag, tagset, tagset);
				}
			}
			else { // tagset already exists in project
				try {
					TagsetDefinition existingTagset = tagManager.getTagLibrary().getTagsetDefinition(tagset.getUuid());

					for (TagDefinition incomingTag : tagset) {
						if (!existingTagset.hasTagDefinition(incomingTag.getUuid())) { // new tag
							tagManager.addTagDefinition(existingTagset, incomingTag);
						}
						else { // tag already exists in tagset
							if (!tagsetDefinitionImportStatus.passesUpdateFilter(incomingTag.getUuid())) {
								continue;
							}

							TagDefinition existingTag = existingTagset.getTagDefinition(incomingTag.getUuid());

							for (PropertyDefinition incomingPropertyDef : incomingTag.getUserDefinedPropertyDefinitions()) {
								PropertyDefinition existingPropertyDef = existingTag.getPropertyDefinitionByUuid(incomingPropertyDef.getUuid());

								if (existingPropertyDef == null) { // new property
									existingTag.addUserDefinedPropertyDefinition(incomingPropertyDef);
								}
								else { // property already exists in tag
									for (String value : incomingPropertyDef.getPossibleValueList()) {
										if (!existingPropertyDef.getPossibleValueList().contains(value)) {
											existingPropertyDef.addValue(value);
										}
									}

									existingPropertyDef.setName(incomingPropertyDef.getName());
									updatePropertyDefinition(existingPropertyDef, existingTag);
								}

								existingTag.setName(incomingTag.getName());
								existingTag.setColor(incomingTag.getColor());
								updateTagDefinition(existingTag, existingTagset);
							}
						}
					}

					existingTagset.setName(tagset.getName());
					updateTagsetDefinition(existingTagset);
				}
				catch (Exception e) {
					throw new IOException(
							String.format("Failed to import tagset \"%s\"! The import has been aborted.", tagset.getName()),
							e
					);
				}
			}
		}
	}

	// collection operations
	@Override
	public AnnotationCollection getAnnotationCollection(AnnotationCollectionReference annotationCollectionRef) throws IOException {
		try {
			return graphProjectHandler.getAnnotationCollection(annotationCollectionRef);
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}

	@Override
	public void createAnnotationCollection(String name, SourceDocumentReference sourceDocumentRef) {
		if (isReadOnly()) {
			throw new IllegalStateException(
					String.format(
							"Project \"%s\" is in read-only mode! Cannot create collection \"%s\".",
							projectReference.getName(),
							name
					)
			);
		}

		try {
			String collectionId = idGenerator.generateCollectionId();
			String oldRootRevisionHash = rootRevisionHash;

			rootRevisionHash = gitProjectHandler.createAnnotationCollection(
					collectionId,
					name,
					null, // description
					sourceDocumentRef.getUuid(),
					null, // not originated from a fork
					true // with push
			);

			graphProjectHandler.addAnnotationCollection(
					collectionId,
					name,
					sourceDocumentRef,
					tagManager.getTagLibrary(),
					oldRootRevisionHash,
					rootRevisionHash
			);

			eventBus.post(
					new CollectionChangeEvent(
							sourceDocumentRef.getUserMarkupCollectionReference(collectionId),
							sourceDocumentRef,
							ChangeType.CREATED
					)
			);
		}
		catch (Exception e) {
			propertyChangeSupport.firePropertyChange(
					ProjectEvent.exceptionOccurred.name(),
					null,
					e
			);
		}
	}

	@Override
	public void updateAnnotationCollectionMetadata(AnnotationCollectionReference annotationCollectionRef) throws IOException {
		if (isReadOnly()) {
			throw new IllegalStateException(
					String.format(
							"Project \"%1$s\" is in read-only mode! Cannot update metadata for collection \"%2$s\" with ID %3$s.",
							projectReference.getName(),
							annotationCollectionRef.getName(),
							annotationCollectionRef.getId()
					)
			);
		}

		String oldRootRevisionHash = rootRevisionHash;

		// update collection in repo and commit
		rootRevisionHash = gitProjectHandler.updateCollection(annotationCollectionRef);

		// update revision hash on GraphProjectHandler
		graphProjectHandler.updateProjectRevision(oldRootRevisionHash, rootRevisionHash);

		// fire collection change event
		SourceDocumentReference sourceDocumentRef = getSourceDocumentReference(annotationCollectionRef.getSourceDocumentId());
		eventBus.post(
				new CollectionChangeEvent(
						annotationCollectionRef,
						sourceDocumentRef,
						ChangeType.UPDATED
				)
		);
	}

	@Override
	public void deleteAnnotationCollection(AnnotationCollectionReference annotationCollectionRef) throws IOException {
		if (isReadOnly()) {
			throw new IllegalStateException(
					String.format(
							"Project \"%s\" is in read-only mode! Cannot delete collection \"%s\".",
							projectReference.getName(),
							annotationCollectionRef.getName()
					)
			);
		}

		String oldRootRevisionHash = rootRevisionHash;

		rootRevisionHash = gitProjectHandler.removeCollection(annotationCollectionRef);

		graphProjectHandler.removeAnnotationCollection(annotationCollectionRef, oldRootRevisionHash, rootRevisionHash);

		SourceDocumentReference sourceDocumentRef = getSourceDocumentReference(annotationCollectionRef.getSourceDocumentId());
		sourceDocumentRef.removeUserMarkupCollectionReference(annotationCollectionRef);

		eventBus.post(
				new CollectionChangeEvent(
						annotationCollectionRef,
						sourceDocumentRef,
						ChangeType.DELETED
				)
		);
	}

	@Override
	public void addTagReferencesToCollection(AnnotationCollection annotationCollection, List<TagReference> tagReferences) {
		if (isReadOnly()) {
			throw new IllegalStateException(
					String.format(
							"Project \"%s\" is in read-only mode! Cannot add tag references to collection \"%s\".",
							projectReference.getName(),
							annotationCollection.getName()
					)
			);
		}

		try {
			String collectionTarget = annotationCollection.getSourceDocumentId();
			Set<String> annotationTargets = tagReferences.stream().map(TagReference::getSourceDocumentId).collect(Collectors.toSet());

			if (!annotationTargets.stream().allMatch(annotationTarget -> annotationTarget.equals(collectionTarget))) {
				throw new IllegalStateException("One or more annotations don't reference the same document as the collection!");
			}

			// add annotations to repo (no commit - annotations are committed in bulk later on)
			gitProjectHandler.addTagReferencesToCollection(annotationCollection.getUuid(), tagReferences, tagManager.getTagLibrary());

			// fire annotation change event for the collection
			propertyChangeSupport.firePropertyChange(
					ProjectEvent.tagReferencesChanged.name(),
					null,
					new Pair<>(annotationCollection, tagReferences)
			);
		}
		catch (Exception e) {
			propertyChangeSupport.firePropertyChange(
					ProjectEvent.exceptionOccurred.name(),
					null,
					e
			);
		}
	}

	@Override
	public void removeTagReferencesFromCollection(AnnotationCollection annotationCollection, List<TagReference> tagReferences) {
		if (isReadOnly()) {
			throw new IllegalStateException(
					String.format(
							"Project \"%s\" is in read-only mode! Cannot delete tag references from collection \"%s\".",
							projectReference.getName(),
							annotationCollection.getName()
					)
			);
		}

		try {
			String collectionTarget = annotationCollection.getSourceDocumentId();
			Set<String> annotationTargets = tagReferences.stream().map(TagReference::getSourceDocumentId).collect(Collectors.toSet());

			if (!annotationTargets.stream().allMatch(annotationTarget -> annotationTarget.equals(collectionTarget))) {
				throw new IllegalStateException("One or more annotations don't reference the same document as the collection!");
			}

			// delete annotations from repo (no commit - annotations are committed in bulk later on)
			Collection<TagInstance> tagInstances = tagReferences.stream().map(TagReference::getTagInstance).collect(Collectors.toSet());
			gitProjectHandler.removeTagInstances(annotationCollection.getUuid(), tagInstances);

			// fire annotation change event for the collection
			Collection<String> tagInstanceIds = tagInstances.stream().map(TagInstance::getUuid).collect(Collectors.toList());
			propertyChangeSupport.firePropertyChange(
					ProjectEvent.tagReferencesChanged.name(),
					new Pair<>(annotationCollection.getUuid(), tagInstanceIds),
					null
			);
		}
		catch (Exception e) {
			propertyChangeSupport.firePropertyChange(
					ProjectEvent.exceptionOccurred.name(),
					null,
					e
			);
		}
	}

	@Override
	public void updateTagInstanceProperties(AnnotationCollection annotationCollection, TagInstance tagInstance, Collection<Property> properties) {
		if (isReadOnly()) {
			throw new IllegalStateException(
					String.format(
							"Project \"%1$s\" is in read-only mode! Cannot update tag instance properties for tag with ID %2$s in collection \"%3$s\".",
							projectReference.getName(),
							tagInstance.getUuid(),
							annotationCollection.getName()
					)
			);
		}

		try {
			for (Property property : properties) {
				tagInstance.addUserDefinedProperty(property);
			}

			// update annotation in repo (no commit - annotations are committed in bulk later on)
			gitProjectHandler.updateTagInstance(
					annotationCollection.getUuid(),
					tagInstance,
					annotationCollection.getTagReferences(tagInstance),
					tagManager.getTagLibrary()
			);

			// fire property change event for the annotation
			propertyChangeSupport.firePropertyChange(
					ProjectEvent.propertyValueChanged.name(),
					tagInstance,
					properties
			);
		}
		catch (Exception e) {
			propertyChangeSupport.firePropertyChange(
					ProjectEvent.exceptionOccurred.name(),
					null,
					e
			);
		}
	}

	@Override
	public Pair<AnnotationCollection, List<TagsetDefinitionImportStatus>> prepareAnnotationCollectionForImport(
			InputStream inputStream,
			SourceDocumentReference sourceDocumentRef
	) throws IOException {
		TagManager tagManager = new TagManager(new TagLibrary());

		TeiTagLibrarySerializationHandler teiTagLibrarySerializationHandler = new TeiTagLibrarySerializationHandler(
				tagManager, rootRevisionHash
		);
		TagLibrary tagLibraryToImport = teiTagLibrarySerializationHandler.deserialize(null, inputStream);

		List<TagsetDefinitionImportStatus> tagsetDefinitionImportStatuses = new ArrayList<>();

		for (TagsetDefinition tagsetDefinition : tagLibraryToImport) {
			boolean tagsetAlreadyInProject = getTagManager().getTagLibrary().getTagsetDefinition(tagsetDefinition.getUuid()) != null;
			tagsetDefinitionImportStatuses.add(new TagsetDefinitionImportStatus(tagsetDefinition, tagsetAlreadyInProject));
		}

		String collectionId = idGenerator.generateCollectionId();

		TeiUserMarkupCollectionDeserializer deserializer = new TeiUserMarkupCollectionDeserializer(
				teiTagLibrarySerializationHandler.getTeiDocument(),
				tagManager.getTagLibrary(),
				collectionId
		);

		AnnotationCollection annotationCollection = new AnnotationCollection(
				collectionId,
				teiTagLibrarySerializationHandler.getTeiDocument().getContentInfoSet(),
				tagManager.getTagLibrary(),
				deserializer.getTagReferences().stream().peek(tr -> tr.setSourceDocumentId(sourceDocumentRef.getUuid())).collect(Collectors.toList()),
				sourceDocumentRef.getUuid(),
				null,
				user.getIdentifier()
		);

		return new Pair<>(annotationCollection, tagsetDefinitionImportStatuses);
	}

	@Override
	public void importAnnotationCollection(
			List<TagsetDefinitionImportStatus> tagsetDefinitionImportStatuses,
			AnnotationCollection annotationCollection
	) throws IOException {
		if (isReadOnly()) {
			throw new IllegalStateException(
					String.format(
							"Project \"%s\" is in read-only mode! Cannot import collection \"%s\".",
							projectReference.getName(),
							annotationCollection.getName()
					)
			);
		}

		// if updates to existing tagsets are needed, only update the tags that are actually referenced in the collection
		Set<String> tagDefinitionIds = annotationCollection.getTagReferences().stream()
				.map(TagReference::getTagDefinitionId)
				.collect(Collectors.toSet());

		for (TagsetDefinitionImportStatus tagsetDefinitionImportStatus : tagsetDefinitionImportStatuses) {
			tagsetDefinitionImportStatus.setUpdateFilter(tagDefinitionIds);
		}

		importTagsets(tagsetDefinitionImportStatuses);

		annotationCollection.setTagLibrary(tagManager.getTagLibrary());

		try {
			SourceDocumentReference sourceDocumentRef = getSourceDocumentReference(annotationCollection.getSourceDocumentId());

			String oldRootRevisionHash = rootRevisionHash;

			rootRevisionHash = gitProjectHandler.createAnnotationCollection(
					annotationCollection.getId(),
					annotationCollection.getName(),
					annotationCollection.getContentInfoSet().getDescription(),
					annotationCollection.getSourceDocumentId(),
					null, // not originated from a fork
					false // no push, because we push as part of the commit down the line after adding the annotations
			);

			graphProjectHandler.addAnnotationCollection(
					annotationCollection.getId(),
					annotationCollection.getName(),
					sourceDocumentRef,
					tagManager.getTagLibrary(),
					oldRootRevisionHash,
					rootRevisionHash
			);

			AnnotationCollectionReference annotationCollectionRef = sourceDocumentRef.getUserMarkupCollectionReference(annotationCollection.getId());

			eventBus.post(
					new CollectionChangeEvent(
							annotationCollectionRef,
							sourceDocumentRef,
							ChangeType.CREATED
					)
			);

			AnnotationCollection createdAnnotationCollection = getAnnotationCollection(annotationCollectionRef);
			createdAnnotationCollection.addTagReferences(annotationCollection.getTagReferences());
			addTagReferencesToCollection(createdAnnotationCollection, annotationCollection.getTagReferences());

			commitAndPushChanges(
					String.format(
							"Imported annotations from collection \"%s\" with ID %s",
							createdAnnotationCollection.getName(),
							createdAnnotationCollection.getId()
					)
			);
		}
		catch (Exception e) {
			throw new IOException(
					String.format(
							"Failed to import collection \"%s\"! The import has been aborted.",
							annotationCollection.getName()
					),
					e
			);
		}
	}

	// document operations
	@Override
	public boolean hasSourceDocument(String sourceDocumentId) {
		return graphProjectHandler.hasSourceDocument(sourceDocumentId);
	}

	@Override
	public Collection<SourceDocumentReference> getSourceDocumentReferences() throws Exception {
		return graphProjectHandler.getSourceDocumentReferences();
	}

	@Override
	public SourceDocumentReference getSourceDocumentReference(String sourceDocumentId) {
		return graphProjectHandler.getSourceDocumentReference(sourceDocumentId);
	}

	@Override
	public SourceDocument getSourceDocument(String sourceDocumentId) throws Exception {
		return graphProjectHandler.getSourceDocument(sourceDocumentId);
	}

	@Override
	public void addSourceDocument(SourceDocument sourceDocument) throws Exception {
		addSourceDocument(sourceDocument, true);
	}

	@Override
	public void addSourceDocument(SourceDocument sourceDocument, boolean deleteTempFile) throws Exception {
		if (isReadOnly()) {
			throw new IllegalStateException(
					String.format(
							"Project \"%s\" is in read-only mode! Cannot create document \"%s\".",
							projectReference.getName(),
							sourceDocument
					)
			);
		}

		try {
			logger.info(String.format(
					"Starting tokenization of document \"%s\" with ID %s",
					sourceDocument,
					sourceDocument.getUuid()
			));

			List<String> unseparableCharacterSequences = sourceDocument.getSourceContentHandler()
					.getSourceDocumentInfo().getIndexInfoSet().getUnseparableCharacterSequences();
			List<Character> userDefinedSeparatingCharacters = sourceDocument.getSourceContentHandler()
					.getSourceDocumentInfo().getIndexInfoSet().getUserDefinedSeparatingCharacters();
			Locale locale = sourceDocument.getSourceContentHandler()
					.getSourceDocumentInfo().getIndexInfoSet().getLocale();

			TermExtractor termExtractor = new TermExtractor(
					sourceDocument.getContent(),
					unseparableCharacterSequences,
					userDefinedSeparatingCharacters,
					locale
			);

			final Map<String, List<TermInfo>> terms = termExtractor.getTerms();

			logger.info(String.format(
					"Finished tokenization of document \"%s\" with ID %s",
					sourceDocument,
					sourceDocument.getUuid()
			));

			String oldRootRevisionHash = rootRevisionHash;

			File documentTempFile = Paths.get(new File(tempDir).toURI())
					.resolve(sourceDocument.getUuid())
					.toFile();

			String convertedFilename = sourceDocument.getUuid() + "." + UTF8_CONVERSION_FILE_EXTENSION;

			try (FileInputStream documentFileInputStream = new FileInputStream(documentTempFile)) {
				MediaType mediaType = MediaType.parse(
						sourceDocument.getSourceContentHandler().getSourceDocumentInfo().getTechInfoSet().getMimeType()
				);
				String extension = mediaType.getBaseType().getType();

				if (StringUtils.isBlank(extension)) {
					extension = "unknown";
				}

				rootRevisionHash = gitProjectHandler.createSourceDocument(
						sourceDocument.getUuid(),
						documentFileInputStream,
						sourceDocument.getUuid() + ORIG_INFIX + "." + extension,
						new ByteArrayInputStream(sourceDocument.getContent().getBytes(StandardCharsets.UTF_8)),
						convertedFilename,
						terms,
						sourceDocument.getUuid() + "." + TOKENIZED_FILE_EXTENSION,
						sourceDocument.getSourceContentHandler().getSourceDocumentInfo()
				);

				sourceDocument.unload();
				StandardContentHandler standardContentHandler = new StandardContentHandler();
				standardContentHandler.setSourceDocumentInfo(sourceDocument.getSourceContentHandler().getSourceDocumentInfo());
				sourceDocument.setSourceContentHandler(standardContentHandler);

				graphProjectHandler.addSourceDocument(
						sourceDocument,
						oldRootRevisionHash,
						rootRevisionHash
				);
			}

			if (deleteTempFile) {
				documentTempFile.delete();
			}

			eventBus.post(
					new DocumentChangeEvent(
							new SourceDocumentReference(sourceDocument.getUuid(), sourceDocument.getSourceContentHandler()),
							ChangeType.CREATED
					)
			);
		}
		catch (Exception e) {
			propertyChangeSupport.firePropertyChange(
					ProjectEvent.exceptionOccurred.name(),
					null,
					e
			);
		}
	}

	@Override
	public void updateSourceDocumentMetadata(SourceDocumentReference sourceDocumentRef) throws IOException {
		if (isReadOnly()) {
			throw new IllegalStateException(
					String.format(
							"Project \"%1$s\" is in read-only mode! Cannot update document \"%2$s\" with ID %3$s.",
							projectReference.getName(),
							sourceDocumentRef,
							sourceDocumentRef.getUuid()
					)
			);
		}

		String oldRootRevisionHash = rootRevisionHash;

		// update document metadata in repo and commit
		rootRevisionHash = gitProjectHandler.updateSourceDocument(sourceDocumentRef);

		// update revision hash on GraphProjectHandler
		graphProjectHandler.updateProjectRevision(oldRootRevisionHash, rootRevisionHash);

		// fire document change event
		eventBus.post(new DocumentChangeEvent(sourceDocumentRef, ChangeType.UPDATED));
	}

	@Override
	public void deleteSourceDocument(SourceDocumentReference sourceDocumentRef) throws Exception {
		if (isReadOnly()) {
			throw new IllegalStateException(
					String.format(
							"Project \"%s\" is in read-only mode! Cannot delete document \"%s\".",
							projectReference.getName(),
							sourceDocumentRef
					)
			);
		}

		String oldRootRevisionHash = rootRevisionHash;

		rootRevisionHash = gitProjectHandler.removeDocument(sourceDocumentRef);
		// TODO: delete/close all corresponding comments?

		for (AnnotationCollectionReference annotationCollectionRef : sourceDocumentRef.getUserMarkupCollectionRefs()) {
			graphProjectHandler.removeAnnotationCollection(annotationCollectionRef, oldRootRevisionHash, rootRevisionHash);

			eventBus.post(new CollectionChangeEvent(annotationCollectionRef, sourceDocumentRef, ChangeType.DELETED));
		}

		graphProjectHandler.removeSourceDocument(sourceDocumentRef, oldRootRevisionHash, rootRevisionHash);

		eventBus.post(new DocumentChangeEvent(sourceDocumentRef, ChangeType.DELETED));
	}

	// comment operations
	@Override
	public List<Comment> getComments(String sourceDocumentId) throws IOException {
		return gitProjectHandler.getComments(sourceDocumentId);
	}

	@Override
	public void addComment(Comment comment) throws IOException {
		gitProjectHandler.addComment(comment);
		eventBus.post(new CommentChangeEvent(ChangeType.CREATED, comment));
	}

	@Override
	public void updateComment(Comment comment) throws IOException {
		gitProjectHandler.updateComment(comment);
		eventBus.post(new CommentChangeEvent(ChangeType.UPDATED, comment));
	}

	@Override
	public void removeComment(Comment comment) throws IOException {
		gitProjectHandler.removeComment(comment);
		eventBus.post(new CommentChangeEvent(ChangeType.DELETED, comment));
	}

	@Override
	public List<Reply> getCommentReplies(Comment comment) throws IOException {
		return gitProjectHandler.getCommentReplies(comment);
	}

	@Override
	public void addCommentReply(Comment comment, Reply reply) throws IOException {
		gitProjectHandler.addReply(comment, reply);
		eventBus.post(new ReplyChangeEvent(ChangeType.CREATED, comment, reply));
	}

	@Override
	public void updateCommentReply(Comment comment, Reply reply) throws IOException {
		gitProjectHandler.updateReply(comment, reply);
		eventBus.post(new ReplyChangeEvent(ChangeType.UPDATED, comment, reply));
	}

	@Override
	public void deleteCommentReply(Comment comment, Reply reply) throws IOException {
		gitProjectHandler.removeReply(comment, reply);
		eventBus.post(new ReplyChangeEvent(ChangeType.DELETED, comment, reply));
	}

	// member, role and permissions related things
	@Override
	public List<User> findUser(String usernameOrEmail) throws IOException {
		return gitProjectHandler.findUser(usernameOrEmail);
	}

	@Override
	public boolean hasPermission(RBACRole role, RBACPermission permission) {
		return gitProjectHandler.hasPermission(role, permission);
	}

	@Override
	public RBACRole getCurrentUserProjectRole() throws IOException {
		return gitProjectHandler.getRoleOnProject(user);
	}

	@Override
	public Set<Member> getProjectMembers() throws IOException {
		return gitProjectHandler.getProjectMembers();
	}

	@Override
	public RBACSubject assignRoleToSubject(RBACSubject subject, RBACRole role) throws IOException {
		return gitProjectHandler.assignOnProject(subject, role);
	}

	@Override
	public void removeSubject(RBACSubject subject) throws IOException {
		gitProjectHandler.unassignFromProject(subject);
	}

	// synchronization related things
	@Override
	public boolean hasUntrackedChanges() throws IOException {
		return gitProjectHandler.hasUntrackedChanges();
	}

	@Override
	public boolean hasUncommittedChanges() throws Exception {
		return gitProjectHandler.hasUncommittedChanges();
	}

	@Override
	public void commitAndPushChanges(String commitMessage) throws IOException {
		if (isReadOnly()) {
			return;
		}

		logger.info(
				String.format(
						"Committing and pushing changes in project \"%s\" with ID %s",
						projectReference.getName(),
						projectReference.getProjectId()
				)
		);

		String oldRootRevisionHash = rootRevisionHash;

		rootRevisionHash = gitProjectHandler.commitProject(commitMessage);

		graphProjectHandler.updateProjectRevision(oldRootRevisionHash, rootRevisionHash);
	}

	@Override
	public void synchronizeWithRemote(OpenProjectListener openProjectListener) throws Exception {
		if (isReadOnly()) {
			throw new IllegalStateException(
					String.format("Project \"%s\" is in read-only mode! Cannot synchronize with remote.", projectReference.getName())
			);
		}

		logger.info(
				String.format("Synchronizing project \"%s\" with ID %s", projectReference.getName(), projectReference.getProjectId())
		);

		final ProgressListener progressListener = new ProgressListener() {
			@Override
			public void setProgress(String value, Object... args) {
				openProjectListener.progress(value, args);
			}
		};

		// TODO: graphProjectHandler.ensureProjectRevisionIsLoaded uses the BackgroundService anyway, do we need to sync in the background too?
		backgroundService.submit(
				new DefaultProgressCallable<Boolean>() {
					@Override
					public Boolean call() throws Exception {
						try {
							progressListener.setProgress("Synchronizing...");
							boolean success = gitProjectHandler.synchronizeWithRemote();
							progressListener.setProgress("Synchronization " + (success ? "completed" : "failed"));
							return success;
						}
						catch (IOException e) {
							logger.log(
									Level.SEVERE,
									String.format(
											"Failed to synchronize project \"%1$s\" with ID %2$s for user \"%3$s\"",
											projectReference.getName(),
											projectReference.getProjectId(),
											user.getIdentifier()
									),
									e
							);
							return false;
						}
					}
				},
				new ExecutionListener<Boolean>() {
					@Override
					public void done(Boolean result) {
						if (!result) {
							openProjectListener.ready(null);
							return;
						}

						try {
							logger.info(
									String.format(
											"Checking for conflicts in project \"%s\" with ID %s",
											projectReference.getName(),
											projectReference.getProjectId()
									)
							);
							if (gitProjectHandler.hasConflicts()) {
								openProjectListener.failure(new IllegalStateException(
										String.format(
												"There are conflicts in project \"%1$s\" with ID %2$s for user \"%3$s\"",
												projectReference.getName(),
												projectReference.getProjectId(),
												user.getIdentifier()
										)
								));
								return;
							}

							gitProjectHandler.ensureUserBranch();
							rootRevisionHash = gitProjectHandler.getRootRevisionHash();

							graphProjectHandler.ensureProjectRevisionIsLoaded(
									rootRevisionHash,
									false, // forceGraphReload
									// TODO: unfortunately we can't pass the CollectionsProvider into the LazyGraphProjectHandler ctor (yet) because of the
									//       ProgressListener
									new CollectionsProvider() {
										@Override
										public List<AnnotationCollection> getCollections(TagLibrary tagLibrary) throws IOException {
											return gitProjectHandler.getCollections(tagLibrary, progressListener, true);
										}
									},
									backgroundService,
									new ExecutionListener<NullType>() {
										@Override
										public void error(Throwable t) {
											openProjectListener.failure(t);
										}

										@Override
										public void done(NullType result) {
											logger.info(
													String.format(
															"Project \"%s\" with ID %s has been re-opened after synchronizing",
															projectReference.getName(),
															projectReference.getProjectId()
													)
											);
											openProjectListener.ready(GraphWorktreeProject.this);
										}
									},
									progressListener
							);
						}
						catch (Exception e) {
							openProjectListener.failure(e);
						}
					}

					@Override
					public void error(Throwable t) {
						openProjectListener.failure(t);
					}
				},
				progressListener
		);
	}

	@Override
	public void addAndCommitCollections(Collection<AnnotationCollectionReference> annotationCollectionRefs, String commitMessage) throws IOException {
		if (isReadOnly()) {
			throw new IllegalStateException(
					String.format("Project \"%s\" is in read-only mode! Cannot add and commit collections.", projectReference.getName())
			);
		}

		String oldRootRevisionHash = rootRevisionHash;

		rootRevisionHash = gitProjectHandler.addCollectionsToStagedAndCommit(
				annotationCollectionRefs.stream().map(AnnotationCollectionReference::getId).collect(Collectors.toSet()),
				commitMessage,
				false, // don't force
				true // withPush
		);

		graphProjectHandler.updateProjectRevision(oldRootRevisionHash, rootRevisionHash);
	}
}
