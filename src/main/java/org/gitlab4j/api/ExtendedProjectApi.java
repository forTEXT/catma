package org.gitlab4j.api;

import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;

import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.GitLabApiForm;
import org.gitlab4j.api.ProjectApi;

/**
 * Extends {@link ProjectApi} with a variant of <code>getProject</code> that deserializes into an {@link ExtendedProject}, so that the
 * <code>import_error</code> field is available.
 * <p>
 * NB: this class lives in gitlab4j's own package only to keep CATMA's patches to gitlab4j in one place - it doesn't use anything that isn't accessible
 * from outside the package.
 */
public class ExtendedProjectApi extends ProjectApi {

	public ExtendedProjectApi(GitLabApi gitLabApi) {
		super(gitLabApi);
	}
	

	public ExtendedProject getExtendedProject(Object projectIdOrPath) throws GitLabApiException {
        Form formData = new GitLabApiForm()
	        	.withParam("statistics", (Boolean)null)
	        	.withParam("license", (Boolean)null)
	        	.withParam("with_custom_attributes", (Boolean)null);
        Response response = get(Response.Status.OK, formData.asMap(),
	        	"projects", getProjectIdOrPath(projectIdOrPath));
	        
        return response.readEntity(ExtendedProject.class);
	}
}
