package de.catma.repository.git.managers;

import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;

import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.GitLabApiForm;
import org.gitlab4j.api.ProjectApi;

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
