package org.gitlab4j.api;

import java.util.Date;

import javax.ws.rs.core.Form;

import org.gitlab4j.api.models.Commit;
import org.gitlab4j.api.utils.ISO8601;

public class ExtendedCommitsApi extends CommitsApi {

	public ExtendedCommitsApi(GitLabApi gitLabApi) {
		super(gitLabApi);
	}
	
    public EnhancedPager<Commit> getCommitsWithEnhancedPager(Object projectIdOrPath, String ref, Date since, Date until, String author, int itemsPerPage) throws GitLabApiException {
        Form formData = new GitLabApiForm()
                .withParam("ref_name", ref)
                .withParam("since", ISO8601.toString(since, false))
                .withParam("until", ISO8601.toString(until, false))
                .withParam("author", author);
        
        return (new EnhancedPager<Commit>(this, Commit.class, itemsPerPage, formData.asMap(),  "projects", getProjectIdOrPath(projectIdOrPath), "repository", "commits"));
    }

}
