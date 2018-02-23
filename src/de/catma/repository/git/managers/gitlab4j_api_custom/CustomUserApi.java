package de.catma.repository.git.managers.gitlab4j_api_custom;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.annotation.Nullable;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.gitlab4j.api.AbstractApi;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.GitLabApiForm;
import org.gitlab4j.api.Pager;
import org.gitlab4j.api.models.User;

import de.catma.repository.git.managers.gitlab4j_api_custom.models.ImpersonationToken;

public class CustomUserApi extends AbstractApi {
	public CustomUserApi(GitLabApi gitLabApi) {
		super(gitLabApi);
	}

	public ImpersonationToken createImpersonationToken(int userId, String name,
													   @Nullable Date expiresAt,
													   @Nullable String[] scopes)
			throws GitLabApiException {
		MultivaluedHashMap<String, String> parameters = new MultivaluedHashMap<>();

		parameters.add("name", name);

		if (expiresAt != null) {
			TimeZone tz = TimeZone.getTimeZone("UTC");
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
			df.setTimeZone(tz);
			String expiresAtIso = df.format(expiresAt);

			parameters.add("expires_at", expiresAtIso);
		}

		if (scopes != null) {
			parameters.addAll("scopes[]", scopes);
		}
		else {
			parameters.add("scopes[]", "api");
		}

		Response response = post(
			Response.Status.CREATED, parameters, "users", userId, "impersonation_tokens"
		);

		return response.readEntity(ImpersonationToken.class);
	}

	public List<ImpersonationToken> getImpersonationTokens(int userId, @Nullable String state)
			throws GitLabApiException {
		MultivaluedMap<String, String> parameters = getDefaultPerPageParam();

		if (state != null) {
			parameters.add("state", state);
		}

		Response response = get(
			Response.Status.OK, parameters, "users", userId, "impersonation_tokens"
		);

		return response.readEntity(new GenericType<List<ImpersonationToken>>() {});
	}

	// tentatively added should paging be needed at a later stage
	// see UserApi.getUsers methods for reference
	public List<ImpersonationToken> getImpersonationTokens(int userId, @Nullable String state,
														   int page, int perPage)
			throws GitLabApiException {
		throw new GitLabApiException("Not implemented");
	}

	public Pager<ImpersonationToken> getImpersonationTokens(int userId, @Nullable String state,
															int itemsPerPage)
			throws GitLabApiException {
		throw new GitLabApiException("Not implemented");
	}
	
	
    public User getUser(String externUid, String provider) throws GitLabApiException {
        GitLabApiForm formData = new GitLabApiForm();
        formData.param("extern_uid", externUid);
        formData.param("provider", provider);
        
        Response response = get(Response.Status.OK, formData.asMap(), "users");
        List<User> users = response.readEntity(new GenericType<List<User>>() {});
        return (users.isEmpty() ? null : users.get(0));
    }
}
