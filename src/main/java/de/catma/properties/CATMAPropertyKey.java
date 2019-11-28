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

import java.util.Properties;

/**
 * All possible keys for a CATMA property. 
 * 
 * @author marco.petris@web.de
 *
 */
public enum CATMAPropertyKey {

	TempDir,
	LoginType,
	InitType,
	BaseURL("http://localhost:8080/catma/"),
	otpSecret,
	otpDuration,
	signup_tokenKey,
	version,
	LogoutURL("https://www.catma.de"),
	AboutURL("https://www.catma.de"),
	TermsOfUseURL("http://catma.de/documentation/terms-of-use/"),
	PrivacyStatementURL("http://catma.de/documentation/privacy-policy/"),
	ImprintURL("http://www.catma.de/about/imprint"),
	
	Google_oauthAuthorizationCodeRequestURL,
	Google_oauthAccessTokenRequestURL,
	Google_oauthClientId,
	Google_oauthClientSecret,
	Google_recaptchaSiteKey,
	Google_recaptchaSecretKey,
	
	GitBasedRepositoryBasePath, 
	GitLabAdminPersonalAccessToken, 
	GitLabServerUrl, 
	
	GraphDbGitMountBasePath,
	
	MailHost,
	MailPort,
	MailAuthenticationNeeded,
	MailUser,
	MailPass,
	MailFrom, 
	
	SpamProtectionAnswer, 
	SpamProtectionQuestion,
	;

	private final String defaultValue;
	
	private CATMAPropertyKey() {
		this(null);
	}

	private CATMAPropertyKey(String defaultValue) {
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
	
	public boolean isTrue(Properties properties, boolean defaultValue) {
		if (properties.containsKey(this.name())) {
			return isTrue(properties);
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

	public boolean isTrue(Properties properties) {
		return Boolean.parseBoolean(properties.getProperty(this.name()));
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
		return CATMAProperties.INSTANCE.getProperties().getProperty(this.name()+index);
	}
	
	public String getIndexedValue(int index, String defaultValue) {
		return CATMAProperties.INSTANCE.getProperties().getProperty(this.name()+index, defaultValue);
	}
	
	public int getIndexedValue(int index, int defaultValue) {
		return Integer.valueOf(CATMAProperties.INSTANCE.getProperties().getProperty(this.name()+index, String.valueOf(defaultValue)));
	}
	
	public String getValue() {
		return CATMAProperties.INSTANCE.getProperties().getProperty(this.name());
	}
	public String getValue(String defaultValue) {
		return CATMAProperties.INSTANCE.getProperties().getProperty(this.name(), defaultValue);
	}
	
	public long getValue(long defaultValue) {
		return Long.valueOf(CATMAProperties.INSTANCE.getProperties().getProperty(this.name(), String.valueOf(defaultValue)));
	}

	public int getValue(int defaultValue) {
		return Integer.valueOf(CATMAProperties.INSTANCE.getProperties().getProperty(this.name(), String.valueOf(defaultValue)));
	}

	public boolean getValue(boolean defaultValue) {
		return Boolean.valueOf(CATMAProperties.INSTANCE.getProperties().getProperty(this.name(), String.valueOf(defaultValue)));
	}
}
