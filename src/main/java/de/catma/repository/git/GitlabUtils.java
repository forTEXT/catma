package de.catma.repository.git;

import java.io.IOException;
import java.net.URL;

import de.catma.properties.CATMAPropertyKey;

/**
 * Gitlab utility functions
 * 
 * @author db
 *
 */
public class GitlabUtils {

	public static final char[] PWD_CHARS = (
			"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz" +
			"0123456789~`!@#$%^&*()-_=+[{]}\\|;:\'\",<.>/?"
		).toCharArray();

	
	/**
	 * rewrite the Gitlab URI when running on other port than 80
	 * @param url
	 * @return
	 */
	public static String rewriteGitLabServerUrl(String url) {
		try {
			URL currentUrl = new URL(url);
			URL gitLabServerUrl = new URL(CATMAPropertyKey.GitLabServerUrl.getValue());
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
