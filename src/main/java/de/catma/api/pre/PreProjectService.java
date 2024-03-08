package de.catma.api.pre;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.List;
import java.util.logging.Logger;

import javax.cache.Cache;
import javax.cache.Caching;
import javax.lang.model.type.NullType;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import de.catma.api.pre.serialization.ProjectSerializer;
import de.catma.backgroundservice.DefaultBackgroundService;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.backgroundservice.ProgressListener;
import de.catma.document.annotation.AnnotationCollection;
import de.catma.hazelcast.HazelcastConfiguration;
import de.catma.project.ProjectReference;
import de.catma.properties.CATMAPropertyKey;
import de.catma.repository.git.GitProjectHandler;
import de.catma.repository.git.graph.interfaces.CollectionProvider;
import de.catma.repository.git.graph.interfaces.CollectionsProvider;
import de.catma.repository.git.graph.lazy.LazyGraphProjectHandler;
import de.catma.repository.git.managers.GitlabManagerRestricted;
import de.catma.repository.git.managers.JGitCredentialsManager;
import de.catma.repository.git.managers.JGitRepoManager;
import de.catma.repository.git.managers.interfaces.LocalGitRepositoryManager;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerRestricted;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagManager;
import de.catma.user.User;

@Path("/project")
public class PreProjectService {
	
    public static final String API_PACKAGE = "pre"; // project resource export
    public static final String API_VERSION = "v1";

	private final static Logger logger = Logger.getLogger(PreProjectService.class.getName());
	
	private final Cache<String, RemoteGitManagerRestrictedProvider> authenticatedUsersCache = 
    		Caching.getCachingProvider().getCacheManager().getCache(
    				HazelcastConfiguration.CacheKeyName.API_AUTH.name());

	
	@Context
	private UriInfo info;

	@Context
	private HttpServletRequest servletRequest;    

	@Context 
	private ServletContext servletContext;
	
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<ProjectReference> getProjects(@QueryParam("token") String token) throws IOException {
    	RemoteGitManagerRestricted remoteGitManagerRestricted = new GitlabManagerRestricted(token);
    	return remoteGitManagerRestricted.getProjectReferences();
    }
    
    @GET
    @Path("/{namespace}/{catmaProjectId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProject(@Context SecurityContext securityContex, @PathParam("namespace") String namespace, @PathParam("catmaProjectId") String catmaProjecId) {
    	try {
	    	Principal principal = securityContex.getUserPrincipal();
	    	if (principal == null) {
	    		return Response.status(Status.UNAUTHORIZED).build();
	    	}
	    	
	    	RemoteGitManagerRestricted remoteGitManagerRestricted = authenticatedUsersCache.get(principal.getName()).createRemoteGitManagerRestricted();
	    	if (remoteGitManagerRestricted == null) {
	    		return Response.status(Status.UNAUTHORIZED.getStatusCode(), "token expired").build();
	    	}
	    	
	    	
	    	User user = remoteGitManagerRestricted.getUser();
	    	ProjectReference projectReference = remoteGitManagerRestricted.getProjectReference(namespace, catmaProjecId);
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

			ProjectSerializer projectSerializer = new ProjectSerializer();
			
			return Response.ok(projectSerializer.serializeProjectResources(graphProjectHandler), MediaType.APPLICATION_JSON).build();
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    		return Response.status(Status.INTERNAL_SERVER_ERROR.getStatusCode(), e.getMessage()).build();
    	}
    }
    
    
}