package de.catma.oauth;

/**
 * Constants shared by the OAuth handlers ({@link GitLabOauthHandler} and {@link GoogleOauthHandler}).
 * <p>
 * Both providers redirect back to the same URL, so the session also carries the name of the provider that a flow was started for, allowing the callback to be
 * dispatched to the right handler.
 */
public class OauthConstants {
    public static final String OAUTH_CSRF_TOKEN_SESSION_ATTRIBUTE_NAME = "OAUTH_CSRF_TOKEN";
    public static final String OAUTH_NONCE_SESSION_ATTRIBUTE_NAME = "OAUTH_NONCE";
    public static final String OAUTH_PROVIDER_SESSION_ATTRIBUTE_NAME = "OAUTH_PROVIDER";

    public static final String CSRF_TOKEN_STATE_PARAMETER_NAME = "csrf_token";

    public enum OauthProvider {
        GITLAB,
        GOOGLE,
    }
}
