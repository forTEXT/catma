package de.catma.repository.git.managers;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.GitLabApiForm;
import org.gitlab4j.api.IssuesApi;
import org.gitlab4j.api.models.Issue;
import org.gitlab4j.api.models.IssueFilter;

public class IssuesExtApi extends IssuesApi {

	public IssuesExtApi(GitLabApi gitLabApi) {
		super(gitLabApi);
	}
	
    public IssuesPager<Issue> getGroupIssues(Object groupIdOrPath, IssueFilter filter) throws GitLabApiException {
        GitLabApiForm formData = filter.getQueryParams();
        return (new IssuesPager<Issue>(this, Issue.class, getDefaultPerPage(), formData.asMap(), "groups", getGroupIdOrPath(groupIdOrPath), "issues"));
    }


    public Response get(Status expectedStatus, MultivaluedMap<String, String> queryParams, Object... pathArgs)
    		throws GitLabApiException {
    	return super.get(expectedStatus, queryParams, pathArgs);
    }
}
