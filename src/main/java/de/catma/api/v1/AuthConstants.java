package de.catma.api.v1;

import com.google.common.collect.ImmutableList;
import com.nimbusds.jose.JWSAlgorithm;
import de.catma.properties.CATMAPropertyKey;

public class AuthConstants {
    public static final String AUTH_SERVICE_PATH = "/auth";

    // the browser-based authentication endpoint and its OAuth callback, relative to AUTH_SERVICE_PATH
    public static final String GITLAB_OAUTH_PATH = "/gitlab";
    public static final String GITLAB_OAUTH_CALLBACK_PATH = GITLAB_OAUTH_PATH + "/callback";

    public static final ImmutableList<JWSAlgorithm> PERMISSIBLE_JWS_ALGORITHMS = ImmutableList.of(
            JWSAlgorithm.HS256 // the default, iteration order is guaranteed with ImmutableList
    );

    public static final String AUTHENTICATION_SCHEME_BEARER_PREFIX = "Bearer ";

    // form parameters - if Authorization header is not used
    // initial auth only, GitLab impersonation or personal access token, swapped for a JWT
    public static final String AUTH_ENDPOINT_TOKEN_FORM_PARAMETER_NAME = "access_token";

    /**
     * Gets the public URL of the browser-based authentication endpoint.
     * <p>
     * This is built from {@link CATMAPropertyKey#BASE_URL} rather than from the incoming request, because behind a reverse proxy the URL that reaches the
     * servlet container need not be the public one.
     *
     * @return the URL
     */
    public static String getGitLabOauthUrl() {
        // the API is mapped at /api/v1/* in web.xml
        return CATMAPropertyKey.BASE_URL.getValue() + "api/v1" + AUTH_SERVICE_PATH + GITLAB_OAUTH_PATH;
    }

    /**
     * Gets the redirect URI of the browser-based authentication flow. It has to be registered for the OAuth application on the GitLab server, which
     * {@link de.catma.servlet.GitLabCapabilitiesCheckServlet} verifies at startup.
     *
     * @return the URL
     */
    public static String getGitLabOauthRedirectUrl() {
        return CATMAPropertyKey.BASE_URL.getValue() + "api/v1" + AUTH_SERVICE_PATH + GITLAB_OAUTH_CALLBACK_PATH;
    }
}
