package de.catma.api.v1.filter;

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
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.JWTClaimsSetVerifier;

import de.catma.api.v1.AuthConstants;
import de.catma.api.v1.ApiSecurityContext;
import de.catma.api.v1.backend.AccessTokenRemoteGitManagerRestrictedProvider;
import de.catma.api.v1.backend.interfaces.RemoteGitManagerRestrictedFactory;
import de.catma.api.v1.backend.interfaces.RemoteGitManagerRestrictedProvider;
import de.catma.api.v1.cache.RemoteGitManagerRestrictedProviderCache;
import de.catma.properties.CATMAPropertyKey;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerRestricted;

@Provider
@PreMatching
public class JwtValidationFilter implements ContainerRequestFilter {
	// https://connect2id.com/products/nimbus-jose-jwt/examples/jwt-with-hmac
	// https://connect2id.com/products/nimbus-jose-jwt/vulnerabilities
	// https://connect2id.com/products/nimbus-jose-jwt/examples/validating-jwt-access-tokens#claims
	// https://www.javadoc.io/doc/com.nimbusds/nimbus-jose-jwt/latest/com/nimbusds/jwt/proc/DefaultJWTClaimsVerifier.html

	private final Logger logger = Logger.getLogger(JwtValidationFilter.class.getName());
	
	@Inject
	private RemoteGitManagerRestrictedProviderCache remoteGitManagerRestrictedProviderCache;
	@Inject
	private RemoteGitManagerRestrictedFactory remoteGitMangerRestrictedFactory;

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		// auth requests should always pass
		if (!requestContext.getUriInfo().getPathSegments().isEmpty()
				&& requestContext.getUriInfo().getPathSegments().getFirst().getPath().equals(AuthConstants.AUTH_SERVICE_PATH.substring(1))) {
			return;
		}

		String authorization = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);

		if (authorization == null || !authorization.toLowerCase().startsWith(AuthConstants.AUTHENTICATION_SCHEME_BEARER_PREFIX.toLowerCase())) {
			requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
			return;
		}

		String bearerToken = authorization.substring(AuthConstants.AUTHENTICATION_SCHEME_BEARER_PREFIX.length());
		boolean isTokenValid = handleJwtToken(bearerToken, requestContext, AuthConstants.AUTHENTICATION_SCHEME_BEARER_PREFIX.trim());

		if (!isTokenValid) {
			return; // the token is invalid and handleJwtToken already aborted the request, we do nothing further
		}

		// the token is valid - we perform some final checks:

		// we should have a security context with a principal - abort if not
		SecurityContext securityContext = requestContext.getSecurityContext();
		if (securityContext == null || securityContext.getUserPrincipal() == null) {
			requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
			return;
		}

		// the remoteGitManagerRestrictedProviderCache should contain a provider for the authenticated user - abort if not
		// the provider would have been added either during a previous auth request or by the special case in handleJwtToken (GitLab token used directly)
		try {
			RemoteGitManagerRestrictedProvider remoteGitManagerRestrictedProvider = remoteGitManagerRestrictedProviderCache.get(
					securityContext.getUserPrincipal().getName()
			);
			if (remoteGitManagerRestrictedProvider == null) { // can happen if the server is restarted
				requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED.getStatusCode(), "Please re-authenticate").build());
			}
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "Failure in JwtValidationFilter", e);
			requestContext.abortWith(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
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

	private boolean handleJwtToken(String token, ContainerRequestContext requestContext, String scheme) {
		try {
			SignedJWT signedJWT = SignedJWT.parse(token);

			if (!AuthConstants.PERMISSIBLE_JWS_ALGORITHMS.contains(signedJWT.getHeader().getAlgorithm())) {
				logSecurityWarningAndAbort("algorithm check", null, requestContext, token);
				return false;
			}

			try {
				JWSVerifier signatureVerifier = new MACVerifier(CATMAPropertyKey.API_HMAC_SECRET.getValue());
				if (!signedJWT.verify(signatureVerifier)) {
					logSecurityWarningAndAbort("signature verification", null, requestContext, token);
					return false;
				}
			}
			catch (JOSEException e) {
				logSecurityWarningAndAbort("signature verification", e, requestContext, token);
				return false;
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
					return false;
				}

				// is the token not yet valid?
				// if at some point we start issuing tokens that are not immediately valid, we could also check for the message "JWT before use time" here

				// some other kind of failure
				logSecurityWarningAndAbort("claims verification", e, requestContext, token);
				return false;
			}

			// all checks passed
			String userIdentifier = signedJWT.getJWTClaimsSet().getSubject();
			requestContext.setSecurityContext(new ApiSecurityContext(userIdentifier, requestContext.getSecurityContext().isSecure(), scheme));
			return true;
		}
		catch (ParseException e) {
			// for convenience, we allow GitLab impersonation or personal access tokens to be used directly
			logger.log(
					Level.WARNING,
					"The given token could not be parsed as a JWT, will try using it directly as a backend token.",
					e
			);

			try {
				RemoteGitManagerRestricted remoteGitManagerRestricted = remoteGitMangerRestrictedFactory.create(token);

				// TODO: test what happens if the cache contains a provider with a token that was valid but that has subsequently expired
				remoteGitManagerRestrictedProviderCache.put(
						remoteGitManagerRestricted.getUsername(),
						new AccessTokenRemoteGitManagerRestrictedProvider(token, remoteGitMangerRestrictedFactory)
				);

				requestContext.setSecurityContext(
						new ApiSecurityContext(remoteGitManagerRestricted.getUsername(), requestContext.getSecurityContext().isSecure(), scheme)
				);
				return true;
			}
			catch (Exception e2) {
				logger.log(Level.SEVERE, "The given token could not be used directly as a backend token.", e2);
				requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
				return false;
			}
		}
	}
}
