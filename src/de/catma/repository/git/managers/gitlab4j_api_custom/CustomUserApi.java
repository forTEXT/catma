package de.catma.repository.git.managers.gitlab4j_api_custom;

import org.gitlab4j.api.AbstractApi;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;

import javax.annotation.Nullable;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class CustomUserApi extends AbstractApi {
	public CustomUserApi(GitLabApi gitLabApi) {
		super(gitLabApi);
	}

	public CreateImpersonationTokenResponse createImpersonationToken(int userId, String name,
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

		CreateImpersonationTokenResponse createImpersonationTokenResponse =
				response.readEntity(CreateImpersonationTokenResponse.class);

		return createImpersonationTokenResponse;
	}
}
