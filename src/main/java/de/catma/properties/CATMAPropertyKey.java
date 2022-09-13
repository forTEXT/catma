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
	BaseURL("http://localhost:8080/catma/"),

	TempDir,

	GitLabServerUrl,
	GitLabAdminPersonalAccessToken,

	GitBasedRepositoryBasePath,

	MaxPageFileSizeBytes("200000"),

	MinTimeBetweenSynchronizationsInSeconds("30"),
	devPreventPush("false"),

	SqliteDbBasePath,

	MailHost("localhost"),
	MailPort("587"),
	MailAuthenticationNeeded("false"),
	MailUser,
	MailPass,
	MailFrom,

	Google_recaptchaSiteKey,
	Google_recaptchaSecretKey,
	signup_tokenKey,

	Google_oauthAuthorizationCodeRequestURL("https://accounts.google.com/o/oauth2/v2/auth"),
	Google_oauthAccessTokenRequestURL("https://oauth2.googleapis.com/token"),
	Google_oauthClientId,
	Google_oauthClientSecret,
	otpSecret,
	otpDuration,

	AboutURL("https://catma.de"),
	ImprintURL("https://catma.de/about/imprint/"),
	TermsOfUseURL("https://catma.de/documentation/terms-of-use/"),
	PrivacyPolicyURL("https://catma.de/documentation/privacy-policy/"),
	StatusURL("https://catma.de/status/"),
	ResetPasswordURL("https://git.catma.de/users/password/new"),
	LogoutURL("https://app.catma.de"),
	ContextDefinitionURL("https://www.catma.de/"),

	// important that this has a trailing slash because of how it's used in some places (TODO: handle both variants for all URLs)
	CATMA5API("https://portal.catma.de/catma/api/"),
	EXPERT("false"),

	Repo6MigrationMaxUsers("1"),
	Repo6MigrationUserList,

	Repo6MigrationMaxProjects("1"),
	Repo6MigrationProjectIdList,

	Repo6MigrationBackupPath,
	Repo6MigrationScanWithMergeAndPush("false"),
	Repo6MigrationCleanupConvertedC6Project("false"),
	Repo6MigrationRemoveUserTempDirectory("false"),
	Repo6MigrationOverwriteC6ProjectBackup("false"),
	Repo6MigrationScanMode("ByProject"),
	Repo6MigrationBranch("c6migration"),
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
