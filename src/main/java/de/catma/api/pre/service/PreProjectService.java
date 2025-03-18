package de.catma.api.pre.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.lang.model.type.NullType;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.jgit.lib.Constants;

import javax.ws.rs.core.SecurityContext;

import de.catma.api.pre.PreProject;
import de.catma.api.pre.backend.interfaces.RemoteGitManagerRestrictedProvider;
import de.catma.api.pre.cache.AnnotationCountCache;
import de.catma.api.pre.cache.ProjectCache;
import de.catma.api.pre.cache.ProjectCache.CacheKey;
import de.catma.api.pre.cache.RemoteGitManagerRestrictedProviderCache;
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

@Path("/projects")
public class PreProjectService {
	
    private final Logger logger = Logger.getLogger(PreProjectService.class.getName());

	@Inject
	private RemoteGitManagerRestrictedProviderCache remoteGitManagerRestrictedProviderCache;

	@Inject
	private ProjectCache projectCache;
	
	@Inject
	private AnnotationCountCache annotationCountCache;
	
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProjects(@Context SecurityContext securityContext) throws IOException {
    	Principal principal = securityContext.getUserPrincipal();
    	if (principal == null) {
    		return Response.status(Status.UNAUTHORIZED).build();
    	}
    	try {
			RemoteGitManagerRestrictedProvider remoteGitManagerRestrictedProvider = remoteGitManagerRestrictedProviderCache.get(principal.getName());
			if (remoteGitManagerRestrictedProvider == null) { // can happen if the server is restarted
				return Response.status(Status.UNAUTHORIZED.getStatusCode(), "Please re-authenticate").build();
			}
	    	RemoteGitManagerRestricted remoteGitManagerRestricted = remoteGitManagerRestrictedProvider.createRemoteGitManagerRestricted();
	    	if (remoteGitManagerRestricted == null) {
	    		return Response.status(Status.UNAUTHORIZED.getStatusCode(), "Token expired").build();
	    	}
	    	
	    	return Response.ok(new SerializationHelper<ProjectReference>().serialize(remoteGitManagerRestricted.getProjectReferences())).build();
    	}
    	catch (Exception e) {
    		logger.log(Level.SEVERE, "API: Failed to deliver list of projects", e);
    		return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    	}
    }
    
    @GET
    @Path("/{namespace}/{catmaProjectId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProject(
    		@Context SecurityContext securityContext, 
    		@PathParam("namespace") String namespace, @PathParam("catmaProjectId") String catmaProjectId, 
    		@QueryParam("includeExtendedMetadata") Boolean includeExtendedMetadata, @QueryParam("page") Integer page, @QueryParam("pageSize") Integer pageSize,
    		@QueryParam("forcePull") Boolean forcePull) {
    	try {
    		Principal principal = securityContext.getUserPrincipal();
	    	if (principal == null) {
	    		return Response.status(Status.UNAUTHORIZED).build();
	    	}

			RemoteGitManagerRestrictedProvider remoteGitManagerRestrictedProvider = remoteGitManagerRestrictedProviderCache.get(principal.getName());
			if (remoteGitManagerRestrictedProvider == null) { // can happen if the server is restarted
				return Response.status(Status.UNAUTHORIZED.getStatusCode(), "Please re-authenticate").build();
			}
	    	RemoteGitManagerRestricted remoteGitManagerRestricted = remoteGitManagerRestrictedProvider.createRemoteGitManagerRestricted();
	    	if (remoteGitManagerRestricted == null) {
	    		return Response.status(Status.UNAUTHORIZED.getStatusCode(), "Token expired").build();
	    	}
	    	try {
	    		CacheKey key = new CacheKey(remoteGitManagerRestricted.getUsername(), namespace, catmaProjectId);
	    		if (forcePull != null && forcePull) {
	    			projectCache.invalidate(key);
	    		}
		    	PreProject project = projectCache.get(
		    			new CacheKey(remoteGitManagerRestricted.getUsername(), namespace, catmaProjectId), 
		    			createPreProjectLoader(remoteGitManagerRestricted, namespace, catmaProjectId));
		    	return Response.ok(project.serializeProjectResources(includeExtendedMetadata==null?true:includeExtendedMetadata, page==null?1:page, pageSize==null?PreProject.DEFAULT_PAGE_SIZE:pageSize), MediaType.APPLICATION_JSON).build();
	    	}
	    	catch (ExecutionException ee) {
	    		if (ee.getMessage().contains("404")) {
	    			return Response.status(Status.NOT_FOUND).build();
	    		}
	    		else {
	    			throw new Exception(ee);
	    		}
	    	}
    	}
    	catch (Exception e) {
    		logger.log(Level.SEVERE, String.format("API: Failed to deliver project export for project %s/%s", namespace, catmaProjectId), e);
    		return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    	}
    }
    
    
    @GET
    @Path("/{namespace}/{catmaProjectId}/doc/{documentId}")    
    public Response getDocument(@Context SecurityContext securityContext, @PathParam("namespace") String namespace, @PathParam("catmaProjectId") String catmaProjectId, @PathParam("documentId") String documentId) {
    	try {
	    	Principal principal = securityContext.getUserPrincipal();
	    	if (principal == null) {
	    		return Response.status(Status.UNAUTHORIZED).build();
	    	}

			RemoteGitManagerRestrictedProvider remoteGitManagerRestrictedProvider = remoteGitManagerRestrictedProviderCache.get(principal.getName());
			if (remoteGitManagerRestrictedProvider == null) { // can happen if the server is restarted
				return Response.status(Status.UNAUTHORIZED.getStatusCode(), "Please re-authenticate").build();
			}
	    	RemoteGitManagerRestricted remoteGitManagerRestricted = remoteGitManagerRestrictedProvider.createRemoteGitManagerRestricted();
	    	if (remoteGitManagerRestricted == null) {
	    		return Response.status(Status.UNAUTHORIZED.getStatusCode(), "Token expired").build();
	    	}
	    	
	    	PreProject project = projectCache.get(
	    			new CacheKey(remoteGitManagerRestricted.getUsername(), namespace, catmaProjectId), 
	    			createPreProjectLoader(remoteGitManagerRestricted, namespace, catmaProjectId));
	    	

	    	File plainTextFile = new File(project.getFileUri(documentId));
	    	
			return Response.ok(plainTextFile, MediaType.TEXT_PLAIN_TYPE.withCharset(StandardCharsets.UTF_8.name())).build();
    	}
    	catch (Exception e) {
    		logger.log(Level.SEVERE, String.format("API: Failed to deliver document %s for project %s/%s", documentId, namespace, catmaProjectId), e);
    		return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    	}    	
    }
    
    private Callable<PreProject> createPreProjectLoader(final RemoteGitManagerRestricted remoteGitManagerRestricted, final String namespace, final String catmaProjectId) {
    	return () -> {    	
	    	User user = remoteGitManagerRestricted.getUser();

	    	ProjectReference projectReference = remoteGitManagerRestricted.getProjectReference(namespace, catmaProjectId);

	    	JGitRepoManager localGitRepositoryManager = new JGitRepoManager(CATMAPropertyKey.API_GIT_REPOSITORY_BASE_PATH.getValue(), user);
	    	JGitCredentialsManager jGitCredentialsManager = new JGitCredentialsManager(remoteGitManagerRestricted);
	    	
			if (!Paths.get(new File(CATMAPropertyKey.API_GIT_REPOSITORY_BASE_PATH.getValue()).toURI())
					.resolve(user.getIdentifier())
					.resolve(projectReference.getNamespace())
					.resolve(projectReference.getProjectId())
					.toFile()
					.exists()
			) {
				try (LocalGitRepositoryManager localRepoManager = localGitRepositoryManager) {
	
					// clone the repository locally
					localRepoManager.clone(
							projectReference.getNamespace(),
							projectReference.getProjectId(),
							remoteGitManagerRestricted.getProjectRepositoryUrl(projectReference),
							jGitCredentialsManager
					);
				}
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
					Paths.get(new File(CATMAPropertyKey.API_GIT_REPOSITORY_BASE_PATH.getValue()).toURI())
							.resolve(user.getIdentifier())
							.resolve(projectReference.getNamespace())
							.resolve(projectReference.getProjectId())
							.toFile(),
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
	        return new PreProject(user.getIdentifier(), namespace, catmaProjectId, tagManager, gitProjectHandler, graphProjectHandler, annotationCountCache);
	    };
    }
    
}