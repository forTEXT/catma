package de.catma.repository.git;

import de.catma.properties.CATMAPropertyKey;

import java.io.IOException;
import java.net.URL;

/**
 * GitLab utility functions
 *
 */
public class GitLabUtils {
	public static final char[] PWD_CHARS = (
			"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789~`!@#$%^&*()-_=+[{]}\\|;:\'\",<.>/?"
	).toCharArray();

	/**
	 * Rewrites the protocol, host and port of URLs returned by the GitLab API.
	 * <p>
	 * This is necessary when the internal and external hostname and port of the GitLab server don't match, as can be the case
	 * when running GitLab as a Docker container, for example.
	 *
	 * @param url the URL to rewrite
	 * @return the rewritten URL
	 */
	public static String rewriteGitLabServerUrl(String url) {
		try {
			URL currentUrl = new URL(url);
			URL gitLabServerUrl = new URL(CATMAPropertyKey.GITLAB_SERVER_URL.getValue());
			URL newUrl = new URL(
					gitLabServerUrl.getProtocol(), gitLabServerUrl.getHost(), gitLabServerUrl.getPort(),
					currentUrl.getFile()
			);
			return newUrl.toString();
		}
		catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
}
