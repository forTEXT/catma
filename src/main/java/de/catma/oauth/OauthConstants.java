package de.catma.oauth;

/**
 * Constants used by {@link GitLabOauthHandler}, shared with the callers that start a flow and handle the callback
 * ({@link de.catma.ui.CatmaApplication} for the web UI, {@link de.catma.api.v1.service.AuthService} for the REST API).
 */
public class OauthConstants {
    public static final String OAUTH_CSRF_TOKEN_SESSION_ATTRIBUTE_NAME = "OAUTH_CSRF_TOKEN";

    public static final String CSRF_TOKEN_STATE_PARAMETER_NAME = "csrf_token";
}
