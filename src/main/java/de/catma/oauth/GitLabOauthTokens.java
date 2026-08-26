package de.catma.oauth;

import java.time.Instant;

/**
 * The tokens returned by GitLab's token endpoint.
 *
 * @param accessToken the access token, used both as the GitLab API auth token and as the password for Git over HTTPS (with the username "oauth2")
 * @param refreshToken the refresh token, which can be exchanged for a new pair of tokens (see {@link GitLabOauthTokenProvider})
 * @param expiresAt the point in time at which <code>accessToken</code> expires (GitLab's default lifetime is 2 hours)
 */
public record GitLabOauthTokens(String accessToken, String refreshToken, Instant expiresAt) {
}
