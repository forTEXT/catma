package de.catma.api.v1.filter;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
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
@Priority(Priorities.AUTHENTICATION)
public class AuthorizationRequestFilter implements ContainerRequestFilter {
	// https://eclipse-ee4j.github.io/jersey.github.io/documentation/latest/security.html#d0e13189 (18.1.1.2. Using Security Context in Container Request Filters)
	// https://eclipse-ee4j.github.io/jersey.github.io/documentation/latest/filters-and-interceptors.html#d0e10050 (Example 10.2. Container request filter)
	// https://eclipse-ee4j.github.io/jersey.github.io/documentation/latest/filters-and-interceptors.html#d0e10468 (10.7. Priorities)

	private static final Logger logger = Logger.getLogger(AuthorizationRequestFilter.class.getName());

	@Inject
	private RemoteGitManagerRestrictedProviderCache remoteGitManagerRestrictedProviderCache;
	@Inject
	private RemoteGitManagerRestrictedFactory remoteGitMangerRestrictedFactory;

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		// auth requests and openapi definitions should always pass
		if (!requestContext.getUriInfo().getPathSegments().isEmpty()) {
			String firstPathSegment = requestContext.getUriInfo().getPathSegments().getFirst().getPath();
			if (firstPathSegment.equals(AuthConstants.AUTH_SERVICE_PATH.substring(1)) || firstPathSegment.equals("openapi.json")
					|| firstPathSegment.equals("openapi.yaml")) {
				return;
			}
		}

		String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);

		if (authorizationHeader == null || !authorizationHeader.toLowerCase().startsWith(AuthConstants.AUTHENTICATION_SCHEME_BEARER_PREFIX.toLowerCase())) {
			requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
			return;
		}

		String bearerToken = authorizationHeader.substring(AuthConstants.AUTHENTICATION_SCHEME_BEARER_PREFIX.length());
		String scheme = AuthConstants.AUTHENTICATION_SCHEME_BEARER_PREFIX.trim();

		JwtValidationResult jwtValidationResult = validateJwt(bearerToken, requestContext, scheme);
		boolean isRequestAborted = false;

		switch (jwtValidationResult) {
			case INVALID -> {
				// generic abort
				requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
				isRequestAborted = true;
			}
			case INVALID_EXPIRED -> {
				// token expired - this is the only case where we let the client know the reason for the failure
				requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED.getStatusCode(), "Token expired").build());
				isRequestAborted = true;
			}
			case INVALID_NOT_A_JWT -> {
				// the token couldn't be parsed as a JWT - try to use it directly as a backend token (for convenience, we allow GitLab impersonation or
				// personal access tokens to be used directly)
				boolean success = attemptToUseTokenDirectlyAsBackendToken(bearerToken, requestContext, scheme);
				if (!success) {
					requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
					isRequestAborted = true;
				}
			}
			case VALID -> {
				// no-op
			}
		}

		if (isRequestAborted) {
			return;
		}

		// the token is valid - we perform some final checks:

		// we should have a security context with a principal - abort if not
		SecurityContext securityContext = requestContext.getSecurityContext();
		if (securityContext == null || securityContext.getUserPrincipal() == null) {
			requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
			return;
		}

		// the remoteGitManagerRestrictedProviderCache should contain a provider for the authenticated user - abort if not
		// the provider would have been added either during a previous auth request or by the INVALID_NOT_A_JWT special case (GitLab token used directly)
		try {
			RemoteGitManagerRestrictedProvider remoteGitManagerRestrictedProvider = remoteGitManagerRestrictedProviderCache.get(
					securityContext.getUserPrincipal().getName()
			);
			if (remoteGitManagerRestrictedProvider == null) { // can happen if the server is restarted
				requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED.getStatusCode(), "Please re-authenticate").build());
			}
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "Failure in AuthorizationRequestFilter", e);
			requestContext.abortWith(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
		}
	}

	private boolean attemptToUseTokenDirectlyAsBackendToken(String token, ContainerRequestContext requestContext, String scheme) {
		try {
			RemoteGitManagerRestricted remoteGitManagerRestricted = remoteGitMangerRestrictedFactory.create(token);

			remoteGitManagerRestrictedProviderCache.put(
					remoteGitManagerRestricted.getUsername(),
					new AccessTokenRemoteGitManagerRestrictedProvider(token, remoteGitMangerRestrictedFactory)
			);

			requestContext.setSecurityContext(
					new ApiSecurityContext(remoteGitManagerRestricted.getUsername(), requestContext.getSecurityContext().isSecure(), scheme)
			);

			return true;
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "The given token could not be used directly as a backend token.", e);
			return false;
		}
	}

	private void logSecurityWarning(String failureType, Exception exception, ContainerRequestContext requestContext, String token) {
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
	}

	enum JwtValidationResult {
		VALID,
		INVALID,
		INVALID_EXPIRED,
		INVALID_NOT_A_JWT
	}

	// https://connect2id.com/products/nimbus-jose-jwt/examples/jwt-with-hmac
	// https://connect2id.com/products/nimbus-jose-jwt/vulnerabilities
	// https://connect2id.com/products/nimbus-jose-jwt/examples/validating-jwt-access-tokens#claims
	// https://www.javadoc.io/doc/com.nimbusds/nimbus-jose-jwt/latest/com/nimbusds/jwt/proc/DefaultJWTClaimsVerifier.html
	private JwtValidationResult validateJwt(String token, ContainerRequestContext requestContext, String scheme) {
		try {
			SignedJWT signedJWT = SignedJWT.parse(token);

			if (!AuthConstants.PERMISSIBLE_JWS_ALGORITHMS.contains(signedJWT.getHeader().getAlgorithm())) {
				logSecurityWarning("algorithm check", null, requestContext, token);
				return JwtValidationResult.INVALID;
			}

			try {
				JWSVerifier signatureVerifier = new MACVerifier(CATMAPropertyKey.API_HMAC_SECRET.getValue());
				if (!signedJWT.verify(signatureVerifier)) {
					logSecurityWarning("signature verification", null, requestContext, token);
					return JwtValidationResult.INVALID;
				}
			}
			catch (JOSEException e) {
				logSecurityWarning("signature verification", e, requestContext, token);
				return JwtValidationResult.INVALID;
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
					return JwtValidationResult.INVALID_EXPIRED;
				}

				// is the token not yet valid?
				// if at some point we start issuing tokens that are not immediately valid, we could also check for the message "JWT before use time" here

				// some other kind of failure
				logSecurityWarning("claims verification", e, requestContext, token);
				return JwtValidationResult.INVALID;
			}

			// all checks passed
			String userIdentifier = signedJWT.getJWTClaimsSet().getSubject();
			requestContext.setSecurityContext(new ApiSecurityContext(userIdentifier, requestContext.getSecurityContext().isSecure(), scheme));
			return JwtValidationResult.VALID;
		}
		catch (ParseException e) {
			logger.log(
					Level.WARNING,
					"The given token could not be parsed as a JWT, will try using it directly as a backend token.",
					e
			);

			return JwtValidationResult.INVALID_NOT_A_JWT;
		}
	}
}
