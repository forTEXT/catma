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
package de.catma.document.repository;

import java.util.Properties;

/**
 * All possible keys for a {@link Repository}'s property. The keys are indexed so
 * more than one repository can be configured in the same properties file. 
 * The indices starting with 1. A key named 'Repositoy' in this enumeration would appear 
 * as 'Repository1' for the first configured repository in the properties file.
 * A RepositoryPropetyKey provides several convenience methods for value testing and value access.
 * 
 * @author marco.petris@web.de
 *
 */
public enum RepositoryPropertyKey {
	/**
	 * The name of the repository.
	 */
	Repository,
	/**
	 * The full class name of the implementation of the {@link de.catma.serialization.SerializationHandlerFactory}.
	 */
	SerializationHandlerFactory,
	/**
	 * The full path to the repository's folder.
	 */
	RepositoryFolderPath,
	/**
	 * The full class name of the implementation of the {@link de.catma.document.repository.RepositoryFactory}.
	 */
	RepositoryFactory, 
	/**
	 * boolean flag 'true'->repository access requires authentication
	 */
	RepositoryAuthenticationRequired,
	/**
	 * a URL that gives access to the repository data (e.g. a JDBC URL).
	 */
	RepositoryUrl,
	/**
	 * a repository system user 
	 */
	RepositoryUser,
	/**
	 * the password for the repository system user
	 */
	RepositoryPass,
	/**
	 * the class name of the implementation of the {@link de.catma.indexer.IndexerFactory} 
	 * for {@link de.catma.indexer.IndexedRepository IndexedRepositories}.
	 */
	IndexerFactory, 
	/**
	 * a URL that gives access to the index data (e.g. a JDBC URL).
	 */
	IndexerUrl, 
	/**
	 * a temporal directory, that can be used e.g. for uploads
	 */
	TempDir,
	LoginType,
	InitType,
	GraphDbPath, 
	SourceDocumentIndexMaintainer, 
	SourceDocumentIndexMaintainerMaxObjects,
	HeurecleaFolder,
	AnnotationGeneratorURL,
	BaseURL("http://www.digitalhumanities.it/catma/"),
	otpSecret,
	otpDuration,
	signup_tokenKey,
	version,
	Google_oauthAuthorizationCodeRequestURL,
	Google_oauthAccessTokenRequestURL,
	Google_oauthClientId,
	Google_oauthClientSecret,
	Google_recaptchaSiteKey,
	Google_recaptchaSecretKey,
	CATMA_oauthAuthorizationCodeRequestURL,
	CATMA_oauthAccessTokenRequestURL,
	CATMA_oauthClientId,
	CATMA_oauthClientSecret, 
	commitAfterNodeCount,
	commitAfterRelationCount,
	DBIndexMaintenanceJobIntervalInSeconds, 
	DBIndexMaintainerMaxObjects, 
	IndexMaintainerEnabled, 
	GuestAccessCountExpirationInDays, 
	GuestAccessCountConcurrencyLevel, 
	GuestAccessCountMax, 
	SpamProtectionAnswer, 
	SpamProtectionQuestion,
	GitBasedRepositoryBasePath, 
	GitLabAdminPersonalAccessToken, 
	GitLabServerUrl, 
	GraphDbUri, 
	GraphDbUser, 
	GraphDbPass, 
	GraphDbGitMountBasePath,
	MailHost,
	MailPort,
	MailAuthenticationNeeded,
	MailUser,
	MailPass,
	MailFrom,
	ShowAnalyzer5,
	;

	private final String defaultValue;
	
	private RepositoryPropertyKey() {
		this(null);
	}

	private RepositoryPropertyKey(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	/**
	 * @param properties the key/value store
	 * @param index the index of this property
	 * @param defaultValue a default value if the given properties do not contain
	 * this key 
	 * @return if the key exist either true or false depending on the value present
	 * or if the key does not exist the default value
	 */
	public boolean isTrue(Properties properties, int index, boolean defaultValue) {
		if (properties.containsKey(this.name()+index)) {
			return isTrue(properties, index);
		}
		else {
			return defaultValue;
		}
	}
	
	/**
	 * @param properties the key/value store
	 * @param index the index of this property
	 * @return <code>true</code> if the key is present and its value is true, else <code>false</code>
	 */
	public boolean isTrue(Properties properties, int index) {
		return Boolean.parseBoolean(properties.getProperty(this.name()+index));
	}
	
	/**
	 * @param properties the key/value store
	 * @param index the index of this property
	 * @return the value or <code>null</code> if the key is not present
	 */
	public String getProperty(Properties properties, int index) {
		return properties.getProperty(this.name()+index);
	}
	
	/**
	 * @param properties the key/value store
	 * @param index the index of this property
	 * @return <code>true</code> if the key is present, else <code>false</code>
	 */
	public boolean exists(Properties properties, int index) {
		return properties.containsKey(this.name()+index);
	}
	
	public String getIndexedValue(int index) {
		return RepositoryProperties.INSTANCE.getProperties().getProperty(this.name()+index);
	}
	
	public String getIndexedValue(int index, String defaultValue) {
		return RepositoryProperties.INSTANCE.getProperties().getProperty(this.name()+index, defaultValue);
	}
	
	public int getIndexedValue(int index, int defaultValue) {
		return Integer.valueOf(RepositoryProperties.INSTANCE.getProperties().getProperty(this.name()+index, String.valueOf(defaultValue)));
	}
	
	public String getValue() {
		return RepositoryProperties.INSTANCE.getProperties().getProperty(this.name());
	}
	public String getValue(String defaultValue) {
		return RepositoryProperties.INSTANCE.getProperties().getProperty(this.name(), defaultValue);
	}
	
	public long getValue(long defaultValue) {
		return Long.valueOf(RepositoryProperties.INSTANCE.getProperties().getProperty(this.name(), String.valueOf(defaultValue)));
	}

	public int getValue(int defaultValue) {
		return Integer.valueOf(RepositoryProperties.INSTANCE.getProperties().getProperty(this.name(), String.valueOf(defaultValue)));
	}
	
	public boolean isTrueIndexed(int index, boolean defaultValue) {
		return isTrue(RepositoryProperties.INSTANCE.getProperties(), index, defaultValue);
	}

	public boolean getValue(boolean defaultValue) {
		return Boolean.valueOf(RepositoryProperties.INSTANCE.getProperties().getProperty(this.name(), String.valueOf(defaultValue)));
	}
}
