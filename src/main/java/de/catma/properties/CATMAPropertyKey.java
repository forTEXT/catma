/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009-2013  University Of Hamburg
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.catma.properties;

/**
 * The available property (settings) keys and their default values.
 * @see "/src/main/resources/catma.properties"
 *
 */
public enum CATMAPropertyKey {
	// important that this has a trailing slash because of how it's used in some places (TODO: handle both variants for all URLs)
	BASE_URL("http://localhost:8080/catma/"),

	TEMP_DIR,

	GITLAB_SERVER_URL,
	GITLAB_ADMIN_PERSONAL_ACCESS_TOKEN,

	GIT_REPOSITORY_BASE_PATH,

	// this setting is somewhat related to the "Maximum diff patch size" setting in GitLab (GitLab admin area > Settings > General > Diff limits)
	// ideally all diffs should be viewable in GitLab (especially for conflict resolution in merge requests), however a diff can be significantly
	// larger than the underlying file (a simple test showed almost double for a new annotation page file)
	// therefore this should be set lower than the GitLab setting - recommendation: about half the value of the GitLab setting, or lower
	// with the default value of 200 000 you should set the GitLab setting to the maximum permissible value (512 KB)
	// NB: setting this too low will have performance implications
	MAX_ANNOTATION_PAGE_FILE_SIZE_BYTES("200000"),

	MIN_TIME_BETWEEN_SYNCHRONIZATIONS_SECONDS("30"),
	DEV_PREVENT_PUSH("false"),
	
	MAX_KEYWORD_IN_CONTEXT_SIZE("30"),

	SQLITE_DB_BASE_PATH,

	MAIL_SMTP_HOST("localhost"),
	MAIL_SMTP_PORT("587"),
	MAIL_SMTP_AUTHENTICATION_REQUIRED("false"),
	MAIL_SMTP_USER,
	MAIL_SMTP_PASS,
	MAIL_FROM,

	GOOGLE_RECAPTCHA_SITE_KEY,
	GOOGLE_RECAPTCHA_SECRET_KEY,
	SIGNUP_TOKEN_KEY,

	GOOGLE_OAUTH_AUTHORIZATION_CODE_REQUEST_URL("https://accounts.google.com/o/oauth2/v2/auth"),
	GOOGLE_OAUTH_ACCESS_TOKEN_REQUEST_URL("https://oauth2.googleapis.com/token"),
	GOOGLE_OAUTH_CLIENT_ID,
	GOOGLE_OAUTH_CLIENT_SECRET,
	OTP_SECRET,
	OTP_DURATION,

	ABOUT_URL("https://catma.de"),
	IMPRINT_URL("https://catma.de/about/imprint/"),
	TERMS_OF_USE_URL("https://catma.de/documentation/terms-of-use/"),
	PRIVACY_POLICY_URL("https://catma.de/documentation/privacy-policy/"),
	STATUS_URL("https://catma.de/status/"),
	RESET_PASSWORD_URL("https://git.catma.de/users/password/new"),
	LOGOUT_URL("https://app.catma.de"),
	CONTEXT_DEFINITION_URL("https://www.catma.de/"),

	// important that this has a trailing slash because of how it's used in some places (TODO: handle both variants for all URLs)
	CATMA_5_API_URL("https://portal.catma.de/catma/api/"),
	EXPERT_MODE("false"),

	V6_REPO_MIGRATION_MAX_USERS("1"),
	V6_REPO_MIGRATION_USER_LIST,

	V6_REPO_MIGRATION_MAX_PROJECTS("1"),
	V6_REPO_MIGRATION_PROJECT_ID_LIST,
	V6_REPO_MIGRATION_PROJECT_ID_LIST_TO_SKIP,

	V6_REPO_MIGRATION_BACKUP_PATH,
	V6_REPO_MIGRATION_OPENCHECK_LOG_PATH,
	V6_REPO_MIGRATION_SKIP_OPENCHECK("false"),
	V6_REPO_MIGRATION_SCAN_WITH_MERGE_AND_PUSH("false"),
	V6_REPO_MIGRATION_CLEANUP_CONVERTED_V6_PROJECT("false"),
	V6_REPO_MIGRATION_SKIP_COMMENT_MIGRATION("false"),
	V6_REPO_MIGRATION_ONLY_COMMENT_MIGRATION("false"),
	V6_REPO_MIGRATION_ONLY_COMMENT_MIGRATION_UPDATED_BEFORE,
	V6_REPO_MIGRATION_REMOVE_USER_TEMP_DIRECTORY("false"),
	V6_REPO_MIGRATION_OVERWRITE_V6_PROJECT_BACKUP("false"),
	V6_REPO_MIGRATION_SCAN_MODE("ByProject"),
	V6_REPO_MIGRATION_BRANCH("v6migration"),
	V6_REPO_MIGRATION_DELETE_EMPTY_PROJECTS_IN_BYPROJECTNOHEADCOMMIT_SCAN_MODE("false")
	;

	private final String defaultValue;
	
	CATMAPropertyKey() {
		this(null);
	}

	CATMAPropertyKey(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public String getValue() {
		return CATMAProperties.INSTANCE.getProperties().getProperty(this.name(), defaultValue);
	}

	public String getValue(String defaultValueOverride) {
		return CATMAProperties.INSTANCE.getProperties().getProperty(this.name(), defaultValueOverride);
	}

	public int getIntValue() {
		return Integer.parseInt(CATMAProperties.INSTANCE.getProperties().getProperty(this.name(), defaultValue));
	}

	public int getIntValue(int defaultValueOverride) {
		return Integer.parseInt(CATMAProperties.INSTANCE.getProperties().getProperty(this.name(), String.valueOf(defaultValueOverride)));
	}

	public boolean getBooleanValue() {
		return Boolean.parseBoolean(CATMAProperties.INSTANCE.getProperties().getProperty(this.name(), defaultValue));
	}

	public boolean getBooleanValue(boolean defaultValueOverride) {
		return Boolean.parseBoolean(CATMAProperties.INSTANCE.getProperties().getProperty(this.name(), String.valueOf(defaultValueOverride)));
	}
}
