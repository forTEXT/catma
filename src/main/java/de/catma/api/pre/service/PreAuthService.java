package de.catma.api.pre.service;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;

import com.google.common.collect.ImmutableList;
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
import de.catma.api.pre.oauth.interfaces.HttpClientFactory;
import de.catma.api.pre.oauth.interfaces.SessionStorageHandler;
import de.catma.api.pre.cache.RemoteGitManagerRestrictedProviderCache;
import de.catma.oauth.GoogleOauthHandler;
import de.catma.oauth.OauthException;
import de.catma.oauth.OauthIdentity;
import de.catma.properties.CATMAPropertyKey;
import de.catma.repository.git.GitUser;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerPrivileged;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerRestricted;
import de.catma.user.User;
import de.catma.util.ExceptionUtil;
import de.catma.util.Pair;

@Path("/auth")
public class PreAuthService {
	public static final ImmutableList<JWSAlgorithm> PERMISSIBLE_JWS_ALGORITHMS = ImmutableList.of(
			JWSAlgorithm.HS256 // the default, iteration order is guaranteed with ImmutableList
	);

	private static final Logger logger = Logger.getLogger(PreAuthService.class.getName());

	private final byte[] secret = CATMAPropertyKey.API_HMAC_SECRET.getValue().getBytes(StandardCharsets.UTF_8);
	
	@Inject
	private RemoteGitManagerRestrictedProviderCache remoteGitManagerRestrictedProviderCache;
	@Inject
	private RemoteGitManagerRestrictedFactory remoteGitMangerRestrictedFactory;
	@Inject
	private RemoteGitManagerPrivilegedFactory remoteGitManagerPrivilegedFactory;
	
	@Inject
	private HttpClientFactory httpClientFactory;
	@Inject
	private SessionStorageHandler sessionStorageHandler;

	@Context
	private HttpServletRequest servletRequest;
	@Context
	private UriInfo uriInfo;

	
	@Produces(MediaType.TEXT_PLAIN)
	@GET
	public Response authenticate(
			@HeaderParam("Authorization") String authorization, // user/password or accesstoken in authorization-header
			@QueryParam("accesstoken") String accessToken, 
			@QueryParam("username") String username, @QueryParam("password") String password) {

		try {
			if (authorization != null) {
				if (authorization.toLowerCase().startsWith("bearer")) {
					String token = authorization.substring(7);
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

			return Response.status(Status.BAD_REQUEST).build();
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "Failed to authenticate", e);

			// check for exceptions caused by invalid credentials
			String message = ExceptionUtil.getMessageFor("org.gitlab4j.api.GitLabApiException", e);
			if (message != null && (message.equals("invalid_grant") || message.equals("invalid_token") || message.equals("401 Unauthorized"))) {
				// 'invalid_grant' = invalid username / password, 'invalid_token' or 401 = invalid token
				return Response.status(Status.UNAUTHORIZED).build();
			}

			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	@GET
	@Path("/google")
	public Response googleOauth() {
		try {
			URI authorizationUri = GoogleOauthHandler.getOauthAuthorizationRequestUri(
					UriBuilder.fromUri(servletRequest.getRequestURL().toString()).path("callback").build().toString(),
					sessionStorageHandler::setAttribute,
					null
			);
	        
	        return Response.temporaryRedirect(authorizationUri).build();
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "Failed to perform OAuth redirection", e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@Produces(MediaType.TEXT_PLAIN)
	@GET
	@Path("/google/callback")
	public Response googleOauthCallback(@QueryParam("code") String authorizationCode, @QueryParam("state") String state, @QueryParam("error") String error) {
		try {
			if (authorizationCode == null || state == null) {
				return Response.status(Status.BAD_REQUEST).build();
			}

			return Response.ok(authenticateWithThirdPartyToken(authorizationCode, state, error)).build();
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "Failed to process OAuth callback", e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
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
	
	private String authenticateWithThirdPartyToken(
			String oauthAuthorizationCode, String oauthState, String oauthError
	) throws OauthException, IOException, JOSEException {
			Pair<OauthIdentity, Map<String, String>> resultPair = GoogleOauthHandler.handleCallbackAndGetIdentity(
					oauthAuthorizationCode,
					oauthState,
					oauthError,
					servletRequest.getRequestURL().toString(),
					httpClientFactory.create(),
					sessionStorageHandler::getAttribute,
					sessionStorageHandler::setAttribute
			);

			OauthIdentity oauthIdentity = resultPair.getFirst();
			Map<String, String> additionalStateParams = resultPair.getSecond(); // should be null, see googleOauth function

			RemoteGitManagerPrivileged gitlabManagerPrivileged = remoteGitManagerPrivilegedFactory.create();
			Pair<GitUser, String> userAndToken = gitlabManagerPrivileged.acquireImpersonationToken(
					oauthIdentity.identifier(),
					oauthIdentity.provider(),
					oauthIdentity.email(),
					oauthIdentity.name()
			);
			return authenticateWithBackendToken(userAndToken.getSecond());
	}
	
	private String createJWToken(User user) throws JOSEException {
		// https://connect2id.com/products/nimbus-jose-jwt/examples/jwt-with-hmac
		// also see JwtValidationFilter

		// HMAC signer
		JWSSigner signer = new MACSigner(secret);

		// JWT claims set
		JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
		    .subject(user.getIdentifier())
		    .issuer(uriInfo.getBaseUri().toString())
		    .notBeforeTime(new Date())
		    .expirationTime(
		    		Date.from(LocalDateTime.now().plus(1, ChronoUnit.HOURS).atZone(ZoneId.systemDefault())
		    	    	      .toInstant()))
		    .build();

		// sign
		SignedJWT signedJWT = new SignedJWT(new JWSHeader(PERMISSIBLE_JWS_ALGORITHMS.getFirst()), claimsSet);
		signedJWT.sign(signer);

		return signedJWT.serialize();
		
	}
}
