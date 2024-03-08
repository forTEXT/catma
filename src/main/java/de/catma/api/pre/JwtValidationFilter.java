package de.catma.api.pre;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.cache.Cache;
import javax.cache.Caching;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.SignedJWT;

import de.catma.hazelcast.HazelcastConfiguration;
import de.catma.properties.CATMAPropertyKey;
import de.catma.repository.git.managers.GitlabManagerRestricted;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerRestricted;

@Provider
@PreMatching
public class JwtValidationFilter implements ContainerRequestFilter {
	private final static String TOKEN_PARAM_NAME = "accesstoken";
	private final static String BEARER_SCHEME_NAME = "Bearer";
	
	private final Logger logger = Logger.getLogger(JwtValidationFilter.class.getName());
	
	private final Cache<String, RemoteGitManagerRestrictedProvider> authenticatedUsersCache = 
    		Caching.getCachingProvider().getCacheManager().getCache(
    				HazelcastConfiguration.CacheKeyName.API_AUTH.name());

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {

		
		// auth requests should pass always
		if (!requestContext.getUriInfo().getPathSegments().isEmpty() 
				&& requestContext.getUriInfo().getPathSegments().get(0).getPath().equals("auth")) {
			return;
		}
		
		String authorization = requestContext.getHeaderString("Authorization");
		if (authorization != null && authorization.toLowerCase().startsWith(BEARER_SCHEME_NAME.toLowerCase())) {
			String token = authorization.substring((BEARER_SCHEME_NAME + " ").length());
			handleJwtToken(token, requestContext, BEARER_SCHEME_NAME);
		}
		else if (requestContext.getUriInfo().getQueryParameters().keySet().contains(TOKEN_PARAM_NAME)) {
			String token = requestContext.getUriInfo().getQueryParameters().getFirst(TOKEN_PARAM_NAME);
			handleJwtToken(token, requestContext, TOKEN_PARAM_NAME);
		}
		else {
		  requestContext.abortWith(
	                Response.status(Response.Status.UNAUTHORIZED).build());
		}
	}

	private void handleJwtToken(String token, ContainerRequestContext requestContext, String scheme) {
		try {
			SignedJWT signedJWT = SignedJWT.parse(token);
	
			JWSVerifier verifier = new MACVerifier(CATMAPropertyKey.API_HMAC_SECRET.getValue());
			
			if (signedJWT.verify(verifier)) {
				if (CATMAPropertyKey.API_BASE_URL.getValue().equals(signedJWT.getJWTClaimsSet().getIssuer())) {
					String userIdentifier = signedJWT.getJWTClaimsSet().getSubject();
					if (LocalDateTime.ofInstant(
							signedJWT.getJWTClaimsSet().getExpirationTime().toInstant(), ZoneId.systemDefault()).isAfter(LocalDateTime.now())) {
						requestContext.setSecurityContext(new PreSecurityContext(userIdentifier, requestContext.getSecurityContext().isSecure(), scheme));
					}
					else {
						  requestContext.abortWith(
					                Response.status(Response.Status.UNAUTHORIZED.getStatusCode(), "token expired").build());
					}
				}
			}
			else {
				  requestContext.abortWith(
			                Response.status(Response.Status.UNAUTHORIZED.getStatusCode(), "verification failed").build());
			}
		}
		catch (ParseException e) {
			logger.log(Level.WARNING, String.format("The token could not be parsed as a JWT, will try using it as a backend token, the error was: %s", e.getMessage()));
			try {
				RemoteGitManagerRestricted remoteGitManagerRestricted = new GitlabManagerRestricted(token);
				
				authenticatedUsersCache.put(remoteGitManagerRestricted.getUsername(), new AccessTokenRemoteGitManagerRestrictedProvider(token));

				requestContext.setSecurityContext(new PreSecurityContext(remoteGitManagerRestricted.getUsername(), requestContext.getSecurityContext().isSecure(), scheme));
			}
			catch (Exception e2) {
				logger.log(Level.SEVERE, "The token could not be used as backend token", e2);
				  requestContext.abortWith(
			                Response.status(Response.Status.UNAUTHORIZED.getStatusCode(), "token invalid").build());				
			}
			
		}
		catch (JOSEException e) {
			  requestContext.abortWith(
		                Response.status(Response.Status.UNAUTHORIZED.getStatusCode(), "token invalid").build());			
		}
	}

}
