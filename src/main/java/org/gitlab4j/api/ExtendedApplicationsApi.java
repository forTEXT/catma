package org.gitlab4j.api;

import java.util.List;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

/**
 * Extends {@link ApplicationsApi} with a variant of <code>getApplications</code> that deserializes into an {@link ExtendedApplication}, so that the
 * <code>confidential</code> field is available.
 * <p>
 * NB: this class lives in gitlab4j's own package only to keep CATMA's patches to gitlab4j in one place - it doesn't use anything that isn't accessible
 * from outside the package.
 */
public class ExtendedApplicationsApi extends ApplicationsApi {

	public ExtendedApplicationsApi(GitLabApi gitLabApi) {
		super(gitLabApi);
	}

	/**
	 * Gets all instance-wide applications (GitLab doesn't list applications that belong to a user or group here).
	 *
	 * @return the applications
	 * @throws GitLabApiException if the request failed
	 */
	public List<ExtendedApplication> getExtendedApplications() throws GitLabApiException {
		// ref: https://docs.gitlab.com/api/applications/#list-all-applications
		// the endpoint isn't paginated - it returns all applications at once, without any paging headers - so this doesn't use a pager
		Response response = get(Response.Status.OK, null, "applications");
		return response.readEntity(new GenericType<List<ExtendedApplication>>() {});
	}
}
