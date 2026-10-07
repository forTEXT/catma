package de.catma.servlet;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.commons.lang3.StringUtils;

import de.catma.api.v1.AuthConstants;
import de.catma.properties.CATMAPropertyKey;
import de.catma.repository.git.managers.GitlabManagerPrivileged;

/**
 * Checks at startup that the GitLab backend is configured such that CATMA can do what it needs to: as an administrator, through the admin personal access
 * token, and on behalf of its users, through the OAuth application that they sign in through.
 * <p>
 * The admin token's <code>sudo</code> scope became a requirement when creating an account stopped using an impersonation token to disable the new user's
 * notifications. Without this check an instance upgraded without replacing its token would look healthy and fail only for the first person to try to sign up -
 * after their account had been created but before it was usable. Similarly, a missing or misconfigured OAuth application would only show up as a failure to
 * sign in, with the reason visible to nobody but the operator.
 * <p>
 * A definite misconfiguration aborts deployment. Being unable to check at all - typically because the GitLab server is unreachable while both are starting -
 * only logs: CATMA can't do anything useful without its backend either way, but it recovers on its own once GitLab answers, whereas refusing to deploy would
 * need someone to intervene.
 */
public class GitLabCapabilitiesCheckServlet extends HttpServlet {
	private interface Check {
		List<String> run() throws Exception;
	}

	@Override
	public void init() throws ServletException {
		super.init();

		// both checks need this, so it's checked once here rather than reported by each
		if (StringUtils.isBlank(CATMAPropertyKey.GITLAB_SERVER_URL.getValue())) {
			throw new ServletException(String.format("The %s property is not set", CATMAPropertyKey.GITLAB_SERVER_URL.name()));
		}

		List<String> failures = new ArrayList<>();

		runCheck("admin personal access token", GitlabManagerPrivileged::checkAdminTokenCapabilities, failures);
		runCheck(
				"OAuth application",
				() -> GitlabManagerPrivileged.checkOauthApplication(
						List.of(CATMAPropertyKey.BASE_URL.getValue(), AuthConstants.getGitLabOauthRedirectUrl())
				),
				failures
		);

		if (!failures.isEmpty()) {
			throw new ServletException(String.join(". ", failures));
		}
	}

	private void runCheck(String subject, Check check, List<String> failures) {
		log(String.format("Checking GitLab %s...", subject));

		List<String> problems;
		try {
			problems = check.run();
		}
		catch (Exception e) {
			log(String.format("Couldn't check the GitLab %s, continuing startup anyway", subject), e);
			return;
		}

		if (!problems.isEmpty()) {
			failures.add(String.format("The GitLab %s is not usable: %s", subject, String.join("; ", problems)));
			return;
		}

		log(String.format("GitLab %s OK", subject));
	}
}
