package de.catma.api.pre;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.cache.Cache;
import javax.cache.Caching;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import de.catma.hazelcast.HazelcastConfiguration;
import de.catma.properties.CATMAPropertyKey;
import de.catma.repository.git.managers.GitlabManagerPrivileged;
import de.catma.repository.git.managers.GitlabManagerRestricted;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerPrivileged;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerRestricted;
import de.catma.user.User;

@Path("/auth")
public class PreAuthService {

	private final Logger logger = Logger.getLogger(PreAuthService.class.getName());
	private final byte[] secret = CATMAPropertyKey.API_HMAC_SECRET.getValue().getBytes(StandardCharsets.UTF_8);
	
	private final Cache<String, RemoteGitManagerRestrictedProvider> authenticatedUsersCache = 
    		Caching.getCachingProvider().getCacheManager().getCache(
    				HazelcastConfiguration.CacheKeyName.API_AUTH.name());
	
	@Produces(MediaType.APPLICATION_JSON)
	@GET
	public Response authenticate(
			@HeaderParam("Authorization") String authorization,
			@QueryParam("accesstoken") String accessToken, 
			@QueryParam("username") String username, @QueryParam("password") String password) {

		try {
			if (authorization != null) {
				if (authorization.toLowerCase().startsWith("bearer")) {
					String token = new String(Base64.getDecoder().decode(authorization.substring(7).getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
					return Response.ok(authenticateWithBackendToken(token)).build();
				}
				else if (authorization.toLowerCase().startsWith("basic")) {
					String[] usernamePassword = 
							new String(
									Base64.getDecoder().decode(authorization.substring(6).getBytes(StandardCharsets.UTF_8)), 
									StandardCharsets.UTF_8)
							.split(":");
					if (usernamePassword.length == 2) {
						return Response.ok(authenticateWithUsernamePassword(usernamePassword[0].trim(), usernamePassword[1].trim())).build();
					}
				}
			}
			else if (accessToken != null) {
				return Response.ok(authenticateWithBackendToken(accessToken)).build();				
			}
			else if (username != null && password != null) {
				return Response.ok(authenticateWithUsernamePassword(username, password)).build();				
			}
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, 
					String.format(
						"Failed to authenticate, got authorization '%s', accessToken '%s', username '%s' %s", 
						authorization, accessToken, username, (password==null||password.isEmpty())?"":"and a password"), e);
		}
		
		return Response.status(Status.FORBIDDEN.getStatusCode(), "authentication failed").build();
	}
	
	
	private String authenticateWithBackendToken(String backendToken) throws IOException, JOSEException {
		RemoteGitManagerRestricted remoteGitManagerRestricted = new GitlabManagerRestricted(backendToken);
		
		authenticatedUsersCache.put(remoteGitManagerRestricted.getUsername(), new AccessTokenRemoteGitManagerRestrictedProvider(backendToken));
		
		return createJWToken(remoteGitManagerRestricted.getUser());
	}
	
	private String authenticateWithUsernamePassword(String username, String password) throws IOException, JOSEException {
		RemoteGitManagerRestricted remoteGitManagerRestricted = new GitlabManagerRestricted(username, password);

		authenticatedUsersCache.put(remoteGitManagerRestricted.getUsername(), new CredentialsRemoteGitManagerRestrictedProvider(username, password));
		
		return createJWToken(remoteGitManagerRestricted.getUser());
	}
	
	private String authenticateWithThirdPartyToken() {
		RemoteGitManagerPrivileged gitlabManagerPrivileged = new GitlabManagerPrivileged();
		//TODO: handle oauth
//		Pair<GitUser, String> userAndToken = gitlabManagerPrivileged.acquireImpersonationToken(identifier, provider, email, name);
//		remoteGitServerManager = remoteGitManagerFactory.createFromImpersonationToken(userAndToken.getSecond());
		return null;
	}
	
	private String createJWToken(User user) throws JOSEException {
		// HMAC signer
		JWSSigner signer = new MACSigner(secret);

		// JWT claims set
		JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
		    .subject(user.getIdentifier())
		    .issuer(CATMAPropertyKey.API_BASE_URL.getValue())
		    .expirationTime(
		    		java.util.Date.from(LocalDateTime.now().plus(1, ChronoUnit.HOURS).atZone(ZoneId.systemDefault())
		    	    	      .toInstant()))
		    .build();

		// sign
		SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
		signedJWT.sign(signer);

		return signedJWT.serialize();
		
	}
}
