package de.catma.oauth;

import org.apache.http.impl.client.CloseableHttpClient;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.time.Instant;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Holds the {@link GitLabOauthTokens} for a session and transparently refreshes the access token before it expires.
 * <p>
 * GitLab access tokens expire after 2 hours by default, which is shorter than a CATMA session can live (see the session timeout in web.xml), so the access
 * token has to be refreshed. All access is synchronized because refreshing invalidates both of the old tokens - two concurrent refreshes for the same session
 * (which is entirely possible, as background threads perform Git operations while the UI thread makes API calls) would leave us holding a dead refresh token.
 */
public class GitLabOauthTokenProvider {
	// refresh a little before the token actually expires, so that we don't hand out a token that expires while it is in flight
	private static final long REFRESH_LEEWAY_SECONDS = 60;

	private final Logger logger = Logger.getLogger(GitLabOauthTokenProvider.class.getName());

	private final String redirectUrl;
	private final Supplier<CloseableHttpClient> httpClientSupplier;

	private GitLabOauthTokens tokens;

	/**
	 * @param tokens the tokens as obtained from {@link GitLabOauthHandler#handleCallbackAndGetTokens}
	 * @param redirectUrl the redirect URL that was used to obtain <code>tokens</code>, which is repeated when refreshing them
	 * @param httpClientSupplier supplies the {@link CloseableHttpClient} used for refresh requests (refreshes happen long after the initial request, and
	 *                           potentially on a background thread, so we can't hold on to the client that was used for the initial token request)
	 */
	public GitLabOauthTokenProvider(
			@NotNull GitLabOauthTokens tokens, @NotNull String redirectUrl, @NotNull Supplier<CloseableHttpClient> httpClientSupplier
	) {
		this.tokens = tokens;
		this.redirectUrl = redirectUrl;
		this.httpClientSupplier = httpClientSupplier;
	}

	/**
	 * Returns a currently valid access token, refreshing it first if it is about to expire.
	 * <p>
	 * This is used as the auth token supplier for gitlab4j-api and as the password for Git over HTTPS, neither of which can handle a checked exception, so a
	 * failed refresh is logged and the existing (probably expired) token is returned - the resulting API or Git failure is handled by the caller.
	 *
	 * @return the access token
	 */
	public synchronized String getAccessToken() {
		if (Instant.now().plusSeconds(REFRESH_LEEWAY_SECONDS).isAfter(tokens.expiresAt())) {
			try {
				refresh();
			}
			catch (IOException e) {
				logger.log(Level.WARNING, "Failed to refresh the GitLab OAuth access token, returning the existing one", e);
			}
		}

		return tokens.accessToken();
	}

	/**
	 * Refreshes the access token irrespective of whether it has expired.
	 *
	 * @throws IOException if the tokens couldn't be refreshed
	 */
	public synchronized void forceRefresh() throws IOException {
		refresh();
	}

	private void refresh() throws IOException {
		if (tokens.refreshToken() == null) {
			throw new IOException("Can't refresh the GitLab OAuth access token, no refresh token is available");
		}

		logger.info("Refreshing the GitLab OAuth access token...");

		try (CloseableHttpClient httpClient = httpClientSupplier.get()) {
			tokens = GitLabOauthHandler.refreshTokens(tokens.refreshToken(), redirectUrl, httpClient);
		}
	}
}
