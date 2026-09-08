package de.catma.servlet;

import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import de.catma.repository.git.managers.GitlabManagerPrivileged;

/**
 * Checks at startup that the GitLab backend is configured such that CATMA can do what it needs to as an administrator.
 * <p>
 * The admin token's <code>sudo</code> scope became a requirement when creating an account stopped using an impersonation token to disable the new user's
 * notifications. Without this check an instance upgraded without replacing its token would look healthy and fail only for the first person to try to sign up -
 * after their account had been created but before it was usable.
 * <p>
 * A definite misconfiguration aborts deployment. Being unable to check at all - typically because the GitLab server is unreachable while both are starting -
 * only logs: CATMA can't do anything useful without its backend either way, but it recovers on its own once GitLab answers, whereas refusing to deploy would
 * need someone to intervene.
 */
public class GitLabCapabilitiesCheckServlet extends HttpServlet {
	@Override
	public void init() throws ServletException {
		super.init();

		log("Checking GitLab admin personal access token capabilities...");

		List<String> problems;
		try {
			problems = GitlabManagerPrivileged.checkAdminTokenCapabilities();
		}
		catch (Exception e) {
			log("Couldn't check the GitLab admin personal access token, continuing startup anyway", e);
			return;
		}

		if (!problems.isEmpty()) {
			throw new ServletException(
					"The GitLab admin personal access token is not usable: " + String.join("; ", problems)
			);
		}

		log("GitLab admin personal access token capabilities OK");
	}
}
