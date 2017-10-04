package de.catma.repository.git;

import java.net.URI;
import java.net.URISyntaxException;

public class GitLabAuthenticationHelper {
	public static String buildAuthenticatedRepositoryUrl(
		String repositoryUrl, String gitLabUserImpersonationToken) throws URISyntaxException {
		// http://www.codeaffine.com/2014/12/09/jgit-authentication/
		URI repoUri = new URI(repositoryUrl);
		String authorityComponent = String.format(
			"gitlab-ci-token:%s", gitLabUserImpersonationToken
		);
		URI authenticatedUri = new URI(
			repoUri.getScheme(), authorityComponent, repoUri.getHost(),
			repoUri.getPort(), repoUri.getPath(), repoUri.getQuery(),
			repoUri.getFragment()
		);
		return authenticatedUri.toString();
	}
}
