package org.gitlab4j.api;

import java.util.Date;

import javax.ws.rs.core.Form;

import org.gitlab4j.api.models.Commit;
import org.gitlab4j.api.utils.ISO8601;

/**
 * Extends {@link CommitsApi} with support for the <code>author</code> query parameter, which is absent from all of upstream's <code>getCommits</code>
 * overloads (as of 5.8.1), and returns an {@link EnhancedPager} rather than a {@link Pager}.
 * <p>
 * NB: this class lives in gitlab4j's own package only to keep CATMA's patches to gitlab4j in one place - it doesn't use anything that isn't accessible
 * from outside the package.
 */
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
