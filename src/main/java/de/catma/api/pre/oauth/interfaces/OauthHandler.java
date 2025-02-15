package de.catma.api.pre.oauth.interfaces;

import java.io.IOException;
import java.net.URI;

import de.catma.api.pre.oauth.OauthIdentity;

public interface OauthHandler {
	public URI getAuthorizationUri(String oauthToken) throws Exception;

	public OauthIdentity getIdentity(String oauthAuthorizationCode, String oauthToken) throws IOException;
}
