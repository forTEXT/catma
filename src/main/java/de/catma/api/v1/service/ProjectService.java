package de.catma.api.v1.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.lang.model.type.NullType;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.lib.Constants;

import de.catma.api.v1.backend.interfaces.RemoteGitManagerRestrictedProvider;
import de.catma.api.v1.cache.CollectionAnnotationCountCache;
import de.catma.api.v1.cache.ProjectExportSerializerCache;
import de.catma.api.v1.cache.ProjectExportSerializerCache.CacheKey;
import de.catma.api.v1.cache.RemoteGitManagerRestrictedProviderCache;
import de.catma.api.v1.serialization.ProjectExportSerializer;
import de.catma.api.v1.serialization.models.ProjectExport;
import de.catma.backgroundservice.DefaultBackgroundService;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.backgroundservice.ProgressListener;
import de.catma.document.annotation.AnnotationCollection;
import de.catma.project.ProjectReference;
import de.catma.properties.CATMAPropertyKey;
import de.catma.repository.git.GitProjectHandler;
import de.catma.repository.git.graph.interfaces.CollectionProvider;
import de.catma.repository.git.graph.interfaces.CollectionsProvider;
import de.catma.repository.git.graph.lazy.LazyGraphProjectHandler;
import de.catma.repository.git.managers.JGitCredentialsManager;
import de.catma.repository.git.managers.JGitRepoManager;
import de.catma.repository.git.managers.interfaces.LocalGitRepositoryManager;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerRestricted;
import de.catma.repository.git.serialization.SerializationHelper;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagManager;
import de.catma.user.User;
import de.catma.util.ExceptionUtil;

@Path("/projects")
// swagger:
@SecurityRequirement(name = "BearerAuth")
public class ProjectService {
	
    private final Logger logger = Logger.getLogger(ProjectService.class.getName());

	@Inject
	private RemoteGitManagerRestrictedProviderCache remoteGitManagerRestrictedProviderCache;

	@Inject
	private ProjectExportSerializerCache projectExportSerializerCache;
	
	@Inject
	private CollectionAnnotationCountCache collectionAnnotationCountCache;

	@Context
	private UriInfo uriInfo;
	@Context
	private SecurityContext securityContext;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
	// swagger:
	@Tag(name = "Projects", description = "List the projects you have access to")
	@Operation(
			responses = {
					@ApiResponse(
							responseCode = "200",
							content = @Content(
									array = @ArraySchema(schema = @Schema(implementation = ProjectReference.class))
							)
					)
			}
	)
    public Response getProjects() throws IOException {
    	try {
			RemoteGitManagerRestrictedProvider remoteGitManagerRestrictedProvider = remoteGitManagerRestrictedProviderCache.get(
					securityContext.getUserPrincipal().getName()
			);
			// TODO: the following will throw `java.io.IOException: org.gitlab4j.api.GitLabApiException` with a message indicating invalid credentials if
			//       the cache contains a provider with backend credentials that were valid when AuthService.authenticate was called, but no longer are.
			//       Currently this results in a 500 - see how AuthService.authenticate handles invalid credentials and consider doing something like that here
			//       (eg: return a 401 with "Please re-authenticate", as we do for a special case in AuthorizationRequestFilter), although the window for this
			//       to occur is relatively small (because our JWTs are short-lived).
	    	RemoteGitManagerRestricted remoteGitManagerRestricted = remoteGitManagerRestrictedProvider.createRemoteGitManagerRestricted();

	    	return Response.ok(new SerializationHelper<ProjectReference>().serialize(remoteGitManagerRestricted.getProjectReferences())).build();
    	}
    	catch (Exception e) {
    		logger.log(Level.SEVERE, "API: Failed to deliver list of projects", e);
    		return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    	}
    }

	@GET
	@Path("/{namespace}/{projectId}")
	@Produces(MediaType.APPLICATION_JSON)
	// swagger:
	@Tag(name = "Projects")
	@Hidden
	public Response getProject(@PathParam("namespace") String namespace, @PathParam("projectId") String projectId) {
		return Response.status(Status.NOT_IMPLEMENTED).build();
	}
    
    @GET
    @Path("/{namespace}/{projectId}/export")
    @Produces(MediaType.APPLICATION_JSON)
	// swagger:
	@Tag(name = "Project Export", description = "Export project resources in an easy-to-use JSON format")
	@Operation(
			description = "Namespace is a username, as projects are always owned by a particular user. Use the projects endpoint to list available projects " +
					"with their namespace and ID. Output is paginated and 'pageSize' defaults to 100 (the no. of annotations returned per page). Links to " +
					"previous and next pages are also returned. Extended metadata is only returned with the first page by default, unless specified with " +
					"'includeExtendedMetadata'. Use 'forcePull' to force the server to update its copy of the project.",
			responses = {
					@ApiResponse(
							responseCode = "200",
							// too tedious to define a completely accurate response schema here, it's good enough as is:
							description = "Note that where you see 'additionalPropN' keys in the sample output, this would actually be a resource ID in most " +
									"cases, allowing for direct lookups by ID in the 'extendedMetadata'. Tag properties are the exception and are not " +
									"accurately represented here. See the link to our website at the top of the page for an accurate example.",
							content = @Content(schema = @Schema(implementation = ProjectExport.class))
					),
					@ApiResponse(responseCode = "404", description = "Project not found")
			}
	)
    public Response getProjectExport(
    		@PathParam("namespace") String namespace, @PathParam("projectId") String projectId,
    		@QueryParam("includeExtendedMetadata") Boolean includeExtendedMetadata, @QueryParam("page") Integer page, @QueryParam("pageSize") Integer pageSize,
    		@QueryParam("forcePull") Boolean forcePull) {
    	try {
    		RemoteGitManagerRestrictedProvider remoteGitManagerRestrictedProvider = remoteGitManagerRestrictedProviderCache.get(
					securityContext.getUserPrincipal().getName()
			);
			// TODO: see TODO in getProjects, same applies here
			RemoteGitManagerRestricted remoteGitManagerRestricted = remoteGitManagerRestrictedProvider.createRemoteGitManagerRestricted();

	    	CacheKey key = new CacheKey(remoteGitManagerRestricted.getUsername(), namespace, projectId);
	    	if (forcePull != null && forcePull) {
	    		projectExportSerializerCache.invalidate(key);
	    	}
		    ProjectExportSerializer serializer = projectExportSerializerCache.get(
					new CacheKey(remoteGitManagerRestricted.getUsername(), namespace, projectId),
					getProjectExportSerializerCacheLoader(remoteGitManagerRestricted, namespace, projectId));
			return Response.ok(
					serializer.serializeProjectResources(
							// strips any query params that should not be present in URLs built based on this one
							uriInfo.getRequestUriBuilder()
									.replaceQueryParam("forcePull")
									.build(),
							// only include extended metadata on the first page by default
							includeExtendedMetadata == null ? (page == null || page == 1) : includeExtendedMetadata,
							page == null ? 1 : page,
							pageSize == null ? ProjectExportSerializer.DEFAULT_PAGE_SIZE : pageSize
					),
					MediaType.APPLICATION_JSON
			).build();
    	}
    	catch (Exception e) {
    		logger.log(Level.SEVERE, String.format("API: Failed to deliver project export for project %s/%s", namespace, projectId), e);

			// check for a 404 from GitLab
			String message = ExceptionUtil.getMessageFor("org.gitlab4j.api.GitLabApiException", e);
			if (message != null && message.contains("404")) {
				return Response.status(Status.NOT_FOUND).build();
			}

    		return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    	}
    }
    
    
    @GET
    @Path("/{namespace}/{projectId}/export/doc/{documentId}")
	@Produces(MediaType.TEXT_PLAIN)
	// swagger:
	@Tag(name = "Project Export")
	@Operation(
			description = "Returns the content of the specified document. Note that document URLs are returned as part of the 'extendedMetadata' of the " +
					"project export endpoint.",
			responses = {
					@ApiResponse(responseCode = "200", description = "The document content as UTF-8 plain text"),
					@ApiResponse(responseCode = "404", description = "Project or document not found")
			}
	)
    public Response getProjectExportDocument(@PathParam("namespace") String namespace, @PathParam("projectId") String projectId, @PathParam("documentId") String documentId) {
    	try {
	    	RemoteGitManagerRestrictedProvider remoteGitManagerRestrictedProvider = remoteGitManagerRestrictedProviderCache.get(
					securityContext.getUserPrincipal().getName()
			);
			// TODO: see TODO in getProjects, same applies here
			RemoteGitManagerRestricted remoteGitManagerRestricted = remoteGitManagerRestrictedProvider.createRemoteGitManagerRestricted();

	    	ProjectExportSerializer serializer = projectExportSerializerCache.get(
					new CacheKey(remoteGitManagerRestricted.getUsername(), namespace, projectId),
					getProjectExportSerializerCacheLoader(remoteGitManagerRestricted, namespace, projectId));
	    	

	    	File plainTextFile = new File(serializer.getFileUri(documentId));
	    	
			return Response.ok(plainTextFile, MediaType.TEXT_PLAIN_TYPE.withCharset(StandardCharsets.UTF_8.name())).build();
    	}
    	catch (Exception e) {
    		logger.log(Level.SEVERE, String.format("API: Failed to deliver document %s for project %s/%s", documentId, namespace, projectId), e);

			// check for a 404 from GitLab
			String message = ExceptionUtil.getMessageFor("org.gitlab4j.api.GitLabApiException", e);
			if (message != null && message.contains("404")) {
				return Response.status(Status.NOT_FOUND).build();
			}

			// check for invalid document ID
			if (e instanceof NullPointerException && e.getMessage().matches(
					"^(?i)cannot invoke.*getSourceDocumentInfo.*because the return value of.*getSourceDocumentReference.*is null$")) {
				return Response.status(Status.NOT_FOUND).build();
			}

    		return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    	}    	
    }
    
    private Callable<ProjectExportSerializer> getProjectExportSerializerCacheLoader(final RemoteGitManagerRestricted remoteGitManagerRestricted, final String namespace, final String projectId) {
    	return () -> {    	
	    	User user = remoteGitManagerRestricted.getUser();

	    	ProjectReference projectReference = remoteGitManagerRestricted.getProjectReference(namespace, projectId);

	    	JGitRepoManager localGitRepositoryManager = new JGitRepoManager(CATMAPropertyKey.API_GIT_REPOSITORY_BASE_PATH.getValue(), user);
	    	JGitCredentialsManager jGitCredentialsManager = new JGitCredentialsManager(remoteGitManagerRestricted);

			File targetPath = Paths.get(new File(CATMAPropertyKey.API_GIT_REPOSITORY_BASE_PATH.getValue()).toURI())
					.resolve(user.getIdentifier())
					.resolve(projectReference.getNamespace())
					.resolve(projectReference.getProjectId())
					.toFile();

			if (!targetPath.exists()) {
				// TODO: currently fresh project clones (project exists remotely but not locally) are not initialised properly -> user branch does not match
				//       remote (see notes)
				//       therefore, copy the repo from GIT_REPOSITORY_BASE_PATH for now (hopefully it exists there and is in a clean state)
//				try (LocalGitRepositoryManager localRepoManager = localGitRepositoryManager) {
//
//					// clone the repository locally
//					localRepoManager.clone(
//							projectReference.getNamespace(),
//							projectReference.getProjectId(),
//							remoteGitManagerRestricted.getProjectRepositoryUrl(projectReference),
//							jGitCredentialsManager
//					);
//				}

				File sourcePath = Paths.get(new File(CATMAPropertyKey.GIT_REPOSITORY_BASE_PATH.getValue()).toURI())
						.resolve(user.getIdentifier())
						.resolve(projectReference.getNamespace())
						.resolve(projectReference.getProjectId())
						.toFile();

				if (!sourcePath.exists()) {
					throw new IOException(String.format("Failed to find project to copy at source path %s", sourcePath.getAbsolutePath()));
				}

				FileUtils.copyDirectory(sourcePath, targetPath);
			}
			else {
				logger.info(
						String.format(
								"Project \"%1$s\" with ID %2$s already cloned, pulling changes...",
								projectReference.getName(),
								projectReference.getProjectId()
						)
				);
				try (LocalGitRepositoryManager localRepoManager = localGitRepositoryManager) {
					localRepoManager.open(projectReference.getNamespace(), projectReference.getProjectId());
					localRepoManager.checkout(user.getIdentifier(), true);
					localGitRepositoryManager.fetch(jGitCredentialsManager);
					localGitRepositoryManager.merge(Constants.DEFAULT_REMOTE_NAME + "/" + user.getIdentifier());
				}
			}
	    	
			GitProjectHandler gitProjectHandler = new GitProjectHandler(
					user,
					projectReference,
					targetPath,
					localGitRepositoryManager,
					remoteGitManagerRestricted
			);
			
			String rootRevisionHash = gitProjectHandler.getRootRevisionHash();
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
				throw new IllegalStateException(
						String.format(
								"There are conflicts in project '%s' with ID %s. Please contact support.",
								projectReference.getName(),
								projectReference.getProjectId()
						)
				);
			}
	
			gitProjectHandler.ensureUserBranch();
	
			if (gitProjectHandler.hasUncommittedChanges() || gitProjectHandler.hasUntrackedChanges()) {
				throw new IllegalStateException(
						String.format(
								"There are uncommited changes in project '%s' with ID %s. Please contact support.",
								projectReference.getName(),
								projectReference.getProjectId()
						)
				);
			}
			
			gitProjectHandler.verifyCollections();
			
	        
	        TagManager tagManager = new TagManager(new TagLibrary());
	        
	        LazyGraphProjectHandler graphProjectHandler = 
	        		new LazyGraphProjectHandler(
	        				projectReference, 
	        				user, 
	        				tagManager, 
	        				() -> gitProjectHandler.getTagsets(), 
	        				() -> gitProjectHandler.getDocuments(), 
	        				documentId -> gitProjectHandler.getDocument(documentId),
	        				documentId -> gitProjectHandler.getDocumentIndex(documentId), 
	        				documentIds -> gitProjectHandler.getCommentsWithReplies(documentIds), 
	        				new CollectionProvider() {
	        					@Override
	        					public AnnotationCollection getCollection(String collectionId, TagLibrary tagLibrary)
	        							throws IOException {
	        						return gitProjectHandler.getCollection(collectionId, tagLibrary);
	        					}
							});
	        
			ProgressListener progressListener = new ProgressListener() {
				@Override
				public void setProgress(String value, Object... args) {
					logger.info(String.format(value, args));
				}
			};
	
	        graphProjectHandler.ensureProjectRevisionIsLoaded(
	        		gitProjectHandler.getRootRevisionHash(), 
	        		false, 
	        		new CollectionsProvider() {
						
						@Override
						public List<AnnotationCollection> getCollections(TagLibrary tagLibrary) throws IOException {
							return gitProjectHandler.getCollections(tagLibrary, progressListener, false);
						}
					}, 
	        		new DefaultBackgroundService(null, false),
	        		new ExecutionListener<NullType>() {
						public void done(NullType result) {};
						public void error(Throwable t) {};
					},
					progressListener);
	        return new ProjectExportSerializer(user.getIdentifier(), namespace, projectId, tagManager, gitProjectHandler, graphProjectHandler, collectionAnnotationCountCache);
	    };
    }
    
}