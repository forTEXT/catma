package de.catma.api.pre.filter;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashSet;
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
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.JWTClaimsSetVerifier;

import de.catma.api.pre.PreSecurityContext;
import de.catma.api.pre.backend.AccessTokenRemoteGitManagerRestrictedProvider;
import de.catma.api.pre.backend.interfaces.RemoteGitManagerRestrictedFactory;
import de.catma.api.pre.cache.RemoteGitManagerRestrictedProviderCache;
import de.catma.api.pre.service.PreAuthService;
import de.catma.properties.CATMAPropertyKey;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerRestricted;

@Provider
@PreMatching
public class JwtValidationFilter implements ContainerRequestFilter {
	// https://connect2id.com/products/nimbus-jose-jwt/examples/jwt-with-hmac
	// https://connect2id.com/products/nimbus-jose-jwt/vulnerabilities
	// https://connect2id.com/products/nimbus-jose-jwt/examples/validating-jwt-access-tokens#claims
	// https://www.javadoc.io/doc/com.nimbusds/nimbus-jose-jwt/latest/com/nimbusds/jwt/proc/DefaultJWTClaimsVerifier.html

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

	private void logSecurityWarningAndAbort(String failureType, Exception exception, ContainerRequestContext requestContext, String token) {
		logger.log(
				Level.WARNING,
				String.format(
						"JWT %s failed for a token received from %s. The token was: %s",
						failureType,
						requestContext.getHeaderString("X-Real-IP"), // assumes we are behind some kind of reverse proxy
						token
				),
				exception
		);
		requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
	}

	private void handleJwtToken(String token, ContainerRequestContext requestContext, String scheme) {
		try {
			SignedJWT signedJWT = SignedJWT.parse(token);

			if (!PreAuthService.PERMISSIBLE_JWS_ALGORITHMS.contains(signedJWT.getHeader().getAlgorithm())) {
				logSecurityWarningAndAbort("algorithm check", null, requestContext, token);
				return;
			}

			try {
				JWSVerifier signatureVerifier = new MACVerifier(CATMAPropertyKey.API_HMAC_SECRET.getValue());
				if (!signedJWT.verify(signatureVerifier)) {
					logSecurityWarningAndAbort("signature verification", null, requestContext, token);
					return;
				}
			}
			catch (JOSEException e) {
				logSecurityWarningAndAbort("signature verification", e, requestContext, token);
				return;
			}

			JWTClaimsSetVerifier<?> claimsVerifier = new DefaultJWTClaimsVerifier<>(
					new JWTClaimsSet.Builder()
							.issuer(requestContext.getUriInfo().getBaseUri().toString()) // must be present and match exactly - did this server issue the token?
							.build(),
					new HashSet<>(Arrays.asList("exp", "nbf")) // expiration & not-before; must be present and will be checked automatically
			);

			try {
				claimsVerifier.verify(signedJWT.getJWTClaimsSet(), null);
			}
			catch (BadJWTException e) {
				// is the token expired?
				if (e.getMessage().toLowerCase().contains("expired")) { // "Expired JWT" was the actual message text seen at the time of writing this
					// yes - this is the only case where we let the client know the reason for the failure
					requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED.getStatusCode(), "Token expired").build());
					return;
				}

				// is the token not yet valid?
				// if at some point we start issuing tokens that are not immediately valid, we could also check for the message "JWT before use time" here

				// some other kind of failure
				logSecurityWarningAndAbort("claims verification", e, requestContext, token);
				return;
			}

			// all checks passed
			String userIdentifier = signedJWT.getJWTClaimsSet().getSubject();
			requestContext.setSecurityContext(new PreSecurityContext(userIdentifier, requestContext.getSecurityContext().isSecure(), scheme));
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
	}

}
