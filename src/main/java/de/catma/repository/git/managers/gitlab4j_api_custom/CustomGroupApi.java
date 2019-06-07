package de.catma.repository.git.managers.gitlab4j_api_custom;

import java.util.List;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.gitlab4j.api.AbstractApi;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.AccessLevel;
import org.gitlab4j.api.models.Group;

public class CustomGroupApi extends AbstractApi {

	public CustomGroupApi(GitLabApi gitLabApi) {
		super(gitLabApi);
	}

    public List<Group> getGroups() throws GitLabApiException {
    	MultivaluedMap<String, String> params = CustomGroupApi.this.getDefaultPerPageParam();
    	params.add("min_access_level", AccessLevel.GUEST.value.toString());
        Response response = get(Response.Status.OK, params, "groups");
        return (response.readEntity(new GenericType<List<Group>>() {}));
    }
}
