package org.gitlab4j.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;

import org.gitlab4j.api.models.PersonalAccessToken;
import org.gitlab4j.api.utils.JacksonJson;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Extends {@link PersonalAccessTokenApi} with a way to read the scopes of the token that is being used to authenticate, as the strings that GitLab actually
 * returns.
 * <p>
 * Upstream's {@link PersonalAccessToken#getScopes} is typed as a list of {@link Constants.ProjectAccessTokenScope} (as of 5.8.1), and that enum has no
 * constant for <code>sudo</code> or <code>admin_mode</code> - both of which a personal access token can perfectly well have. Worse, unmapped values don't
 * fail: <code>JacksonJsonEnumHelper.forValue</code> is a map lookup that returns <code>null</code>, so an admin token with
 * <code>["api", "sudo", "admin_mode"]</code> deserializes to <code>[API, null, null]</code>. Anything checking that list for <code>sudo</code> would
 * silently conclude it isn't there.
 * <p>
 * NB: this class lives in gitlab4j's own package because it needs the package-private {@link AbstractApi#AbstractApi} constructor chain and
 * {@link AbstractApi#get}.
 */
public class ExtendedPersonalAccessTokenApi extends PersonalAccessTokenApi {

	public ExtendedPersonalAccessTokenApi(GitLabApi gitLabApi) {
		super(gitLabApi);
	}

	/**
	 * Gets the scopes of the token that this API instance authenticates with, as returned by GitLab.
	 *
	 * @return the scope strings, e.g. <code>api</code>, <code>sudo</code>
	 * @throws GitLabApiException if the request failed
	 */
	public List<String> getCurrentTokenScopes() throws GitLabApiException {
		// ref: https://docs.gitlab.com/api/personal_access_tokens/#get-single-personal-access-token
		Response response = get(Response.Status.OK, null, "personal_access_tokens", "self");

		try {
			JsonNode scopes = JacksonJson.toJsonNode(response.readEntity(String.class)).get("scopes");

			List<String> result = new ArrayList<>();
			if (scopes != null) {
				scopes.forEach(scope -> result.add(scope.asText()));
			}
			return result;
		}
		catch (IOException e) {
			throw new GitLabApiException(e);
		}
	}
}
