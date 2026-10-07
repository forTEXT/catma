package org.gitlab4j.api;

import org.gitlab4j.api.models.Application;

/**
 * Extends {@link Application} with the <code>confidential</code> field, which upstream's model omits (as of 5.8.1).
 * <p>
 * See {@link ExtendedApplicationsApi#getExtendedApplications}, which is what populates it.
 */
public class ExtendedApplication extends Application {

	private Boolean confidential;

	public ExtendedApplication() {
		super();
	}

	public void setConfidential(Boolean confidential) {
		this.confidential = confidential;
	}

	public Boolean getConfidential() {
		return confidential;
	}

}
