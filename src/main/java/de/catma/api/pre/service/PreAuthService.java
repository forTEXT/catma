package de.catma.api.pre.service;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.http.client.ClientProtocolException;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import de.catma.api.pre.backend.AccessTokenRemoteGitManagerRestrictedProvider;
import de.catma.api.pre.backend.CredentialsRemoteGitManagerRestrictedProvider;
import de.catma.api.pre.backend.interfaces.RemoteGitManagerPrivilegedFactory;
import de.catma.api.pre.backend.interfaces.RemoteGitManagerRestrictedFactory;
import de.catma.api.pre.cache.RemoteGitManagerRestrictedProviderCache;
import de.catma.api.pre.oauth.OauthIdentity;
import de.catma.api.pre.oauth.interfaces.OauthHandler;
import de.catma.properties.CATMAPropertyKey;
import de.catma.repository.git.GitUser;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerPrivileged;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerRestricted;
import de.catma.user.User;
import de.catma.util.Pair;

@Path("/auth")
public class PreAuthService {
	
	private final Logger logger = Logger.getLogger(PreAuthService.class.getName());
	private final byte[] secret = CATMAPropertyKey.API_HMAC_SECRET.getValue().getBytes(StandardCharsets.UTF_8);
	
	@Inject
	private RemoteGitManagerRestrictedProviderCache remoteGitManagerRestrictedProviderCache;
	@Inject
	private RemoteGitManagerRestrictedFactory remoteGitMangerRestrictedFactory;
	@Inject
	private RemoteGitManagerPrivilegedFactory remoteGitManagerPrivilegedFactory;
	
	@Inject
	private OauthHandler oauthHandler;
	
	@Context
	private HttpServletRequest servletRequest;    

	
	@Produces(MediaType.APPLICATION_JSON)
	@GET
	public Response authenticate(
			@HeaderParam("Authorization") String authorization, // user/password or accesstoken in authorization-header
			@QueryParam("accesstoken") String accessToken, 
			@QueryParam("username") String username, @QueryParam("password") String password,
			// oauth parameters when redirecting after third party authentication 
			@QueryParam("code") String oauthAuthorizationCode, @QueryParam("state") String oauthState, @QueryParam("error") String oauthError) {

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
			else if (oauthError != null) {
				logger.log(Level.SEVERE, String.format("Got an oauthError: %s", oauthError));	
			}
			else if (oauthAuthorizationCode != null) {
				return Response.ok(authenticateWithThirdPartyToken(oauthAuthorizationCode, oauthState)).build();
			}

		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "Failed to authenticate", e);
		}
		
		return Response.status(Status.FORBIDDEN.getStatusCode(), "authentication failed").build();
	}
	
	@GET
	@Path("/google")
	public Response googleOauth() {
		try {
	        String oauthToken = new BigInteger(130, new SecureRandom()).toString(32);
	        
	        URI authorizationUri = oauthHandler.getAuthorizationUri(oauthToken);
	        logger.info(String.format("Redirecting to third party oauth provider %s", authorizationUri));
	        
	        return Response.temporaryRedirect(authorizationUri).build();
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "Failed to create oauth redirection", e);
		}
		
		return Response.status(Status.FORBIDDEN.getStatusCode(), "authentication failed").build();
	}
	
	
	private String authenticateWithBackendToken(String backendToken) throws IOException, JOSEException {
		RemoteGitManagerRestricted remoteGitManagerRestricted = remoteGitMangerRestrictedFactory.create(backendToken);
		
		remoteGitManagerRestrictedProviderCache.put(remoteGitManagerRestricted.getUsername(), new AccessTokenRemoteGitManagerRestrictedProvider(backendToken, remoteGitMangerRestrictedFactory));
		
		return createJWToken(remoteGitManagerRestricted.getUser());
	}
	
	private String authenticateWithUsernamePassword(String username, String password) throws IOException, JOSEException {
		RemoteGitManagerRestricted remoteGitManagerRestricted = remoteGitMangerRestrictedFactory.create(username, password);

		remoteGitManagerRestrictedProviderCache.put(remoteGitManagerRestricted.getUsername(), new CredentialsRemoteGitManagerRestrictedProvider(username, password, remoteGitMangerRestrictedFactory));
		
		return createJWToken(remoteGitManagerRestricted.getUser());
	}
	
	private String authenticateWithThirdPartyToken(String oauthAuthorizationCode, String otpTimestamp) throws ClientProtocolException, IOException, JOSEException {
		OauthIdentity oauthIdentity = oauthHandler.getIdentity(oauthAuthorizationCode, otpTimestamp);
		
		RemoteGitManagerPrivileged gitlabManagerPrivileged = remoteGitManagerPrivilegedFactory.create();
		Pair<GitUser, String> userAndToken = gitlabManagerPrivileged.acquireImpersonationToken(oauthIdentity.identifier(), oauthIdentity.provider(), oauthIdentity.email(), oauthIdentity.name());
		return authenticateWithBackendToken(userAndToken.getSecond());
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