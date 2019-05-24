package de.catma.repository.git.managers.gitlab4j_api_custom;

import java.util.List;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.gitlab4j.api.AbstractApi;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Member;

public class CustomProjectApi extends AbstractApi {

	public CustomProjectApi(GitLabApi gitLabApi) {
		super(gitLabApi);
	}

    public List<Member> getAllMembers(Integer projectId) throws GitLabApiException {
        Response response = get(Response.Status.OK, this.getDefaultPerPageParam(), "projects", projectId, "members","all");
        return (response.readEntity(new GenericType<List<Member>>() {}));
    }
}
