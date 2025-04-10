package de.catma.api.pre.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.SignedJWT;

import de.catma.api.pre.PreSecurityContext;
import de.catma.api.pre.backend.AccessTokenRemoteGitManagerRestrictedProvider;
import de.catma.api.pre.backend.interfaces.RemoteGitManagerRestrictedFactory;
import de.catma.api.pre.cache.RemoteGitManagerRestrictedProviderCache;
import de.catma.properties.CATMAPropertyKey;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerRestricted;

@Provider
@PreMatching
public class JwtValidationFilter implements ContainerRequestFilter {
	private final static String TOKEN_PARAM_NAME = "accesstoken";
	private final static String BEARER_SCHEME_NAME = "Bearer";
	
	private final Logger logger = Logger.getLogger(JwtValidationFilter.class.getName());
	
	@Inject
	private RemoteGitManagerRestrictedProviderCache remoteGitManagerRestrictedProviderCache;
	@Inject
	private RemoteGitManagerRestrictedFactory remoteGitMangerRestrictedFactory;

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
					                Response.status(Response.Status.UNAUTHORIZED.getStatusCode(), "Token expired").build());
					}
				}
			}
			else {
				  requestContext.abortWith(
			                Response.status(Response.Status.UNAUTHORIZED.getStatusCode(), "Token verification failed").build());
			}
		}
		catch (ParseException e) {
			logger.log(Level.WARNING, String.format("The token could not be parsed as a JWT, will try using it as a backend token. The error was: %s", e.getMessage()));
			try {
				RemoteGitManagerRestricted remoteGitManagerRestricted = remoteGitMangerRestrictedFactory.create(token);
				
				remoteGitManagerRestrictedProviderCache.put(remoteGitManagerRestricted.getUsername(), new AccessTokenRemoteGitManagerRestrictedProvider(token, remoteGitMangerRestrictedFactory));

				requestContext.setSecurityContext(new PreSecurityContext(remoteGitManagerRestricted.getUsername(), requestContext.getSecurityContext().isSecure(), scheme));
			}
			catch (Exception e2) {
				logger.log(Level.SEVERE, "The token could not be used as backend token", e2);
				  requestContext.abortWith(
			                Response.status(Response.Status.UNAUTHORIZED.getStatusCode(), "Token invalid").build());
			}
			
		}
		catch (JOSEException e) {
			  requestContext.abortWith(
		                Response.status(Response.Status.UNAUTHORIZED.getStatusCode(), "Token invalid").build());
		}
	}

}
