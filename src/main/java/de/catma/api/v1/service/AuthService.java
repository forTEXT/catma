package de.catma.api.v1.service;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;

import org.apache.http.impl.client.CloseableHttpClient;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import de.catma.api.v1.AuthConstants;
import de.catma.api.v1.backend.AccessTokenRemoteGitManagerRestrictedProvider;
import de.catma.api.v1.backend.OauthTokenRemoteGitManagerRestrictedProvider;
import de.catma.api.v1.backend.interfaces.RemoteGitManagerPrivilegedFactory;
import de.catma.api.v1.backend.interfaces.RemoteGitManagerRestrictedFactory;
import de.catma.api.v1.oauth.interfaces.HttpClientFactory;
import de.catma.api.v1.oauth.interfaces.SessionStorageHandler;
import de.catma.api.v1.cache.RemoteGitManagerRestrictedProviderCache;
import de.catma.oauth.GitLabOauthHandler;
import de.catma.oauth.GitLabOauthTokenProvider;
import de.catma.oauth.GitLabOauthTokens;
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
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.StringToClassMapItem;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path(AuthConstants.AUTH_SERVICE_PATH) // '/auth', defined as a constant because it's checked in AuthorizationRequestFilter
// swagger:
@Tag(name = "Authentication")
public class AuthService {
	private static final Logger logger = Logger.getLogger(AuthService.class.getName());

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
	private UriInfo uriInfo;

	
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.TEXT_PLAIN)
	// swagger:
	@Operation(
			description = "Authenticate with a CATMA access token, supplied either in the 'Authorization' header ('Bearer' scheme) or as the " +
					"'access_token' form parameter. The options should be viewed as mutually exclusive. Returns a JWT that should be used to authenticate " +
					"requests to any of the other, non-authentication endpoints (which only accept bearer authentication). Note that you won't be able to " +
					"test the header option in Swagger UI. If you don't have an access token, use the /auth/gitlab or /auth/google endpoints instead.",
			requestBody = @RequestBody(
					description = "Optional form parameter, required if no 'Authorization' header is supplied. Send a CATMA access token.",
					// the content schema is detected just fine automatically if we don't define the requestBody at all, but if we define it with only the
					// description, then the parameters aren't displayed
					content = @Content(
							schema = @Schema(
									type = "object",
									properties = {
											@StringToClassMapItem(key = AuthConstants.AUTH_ENDPOINT_TOKEN_FORM_PARAMETER_NAME, value = String.class)
									}
							)
					)
			),
			responses = {
					@ApiResponse(responseCode = "200", description = "A JSON Web Token"),
					@ApiResponse(responseCode = "400", description = "Missing or malformed header or parameters"),
					@ApiResponse(responseCode = "401", description = "Invalid credentials")
			}
	)
	public Response authenticate(
			@HeaderParam(HttpHeaders.AUTHORIZATION) String authorization, // bearer (token) auth scheme
			@FormParam(AuthConstants.AUTH_ENDPOINT_TOKEN_FORM_PARAMETER_NAME) String accessToken)
	{
		try {
			if (authorization != null) {
				if (authorization.toLowerCase().startsWith(AuthConstants.AUTHENTICATION_SCHEME_BEARER_PREFIX.toLowerCase())) {
					String bearerToken = authorization.substring(AuthConstants.AUTHENTICATION_SCHEME_BEARER_PREFIX.length());
					return Response.ok(authenticateWithBackendToken(bearerToken)).build();
				}
			}
			else if (accessToken != null) {
				return Response.ok(authenticateWithBackendToken(accessToken)).build();
			}

			return Response.status(Status.BAD_REQUEST).build();
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "Failed to authenticate", e);

			// check for exceptions caused by invalid credentials
			String message = ExceptionUtil.getMessageFor("org.gitlab4j.api.GitLabApiException", e);
			if (message != null && (message.equals("invalid_token") || message.equals("401 Unauthorized"))) {
				// 'invalid_token' or 401 = invalid token
				return Response.status(Status.UNAUTHORIZED).build();
			}

			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	@GET
	@Path("/gitlab")
	// swagger:
	@Operation(
			description = "Authenticate with your CATMA account. Only use this if you log in to CATMA with a username/email address and password. A browser " +
					"is required to complete the OAuth flow. Redirects to CATMA's GitLab backend and, if successfully authenticated, responds in the same " +
					"way as the /auth endpoint. Note that you won't be able to test this in Swagger UI (but you can easily visit the URL in a separate tab " +
					"to try it out).",
			responses = {
					@ApiResponse(responseCode = "307", description = "Redirect to GitLab login")
			}
	)
	public Response gitLabOauth() {
		try {
			URI authorizationUri = GitLabOauthHandler.getOauthAuthorizationRequestUri(
					// appends '/callback' to the current URL path and strips any query params (as they would cause a redirectUrl mismatch)
					uriInfo.getRequestUriBuilder().path("callback").replaceQuery("").build().toString(),
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
	@Path("/gitlab/callback")
	// swagger:
	@Hidden
	public Response gitLabOauthCallback(@QueryParam("code") String authorizationCode, @QueryParam("state") String state, @QueryParam("error") String error) {
		try {
			// state should always be present; if we don't get a code, we *should* get an error
			if (state == null || (authorizationCode == null && error == null)) {
				return Response.status(Status.BAD_REQUEST).build();
			}

			if (authorizationCode == null && error.equals("access_denied")) {
				// the user cancelled the auth process with GitLab or didn't allow the requested access
				String requestUrl = uriInfo.getRequestUri().toString();
				String gitLabAuthUrl = requestUrl.substring(0, requestUrl.lastIndexOf("/")); // removes '/callback' and any query params
				return Response.ok(
						"You seem to have cancelled the sign-in or you didn't allow the requested access. " +
								"To restart the process and try again, please visit the following URL:\n" + gitLabAuthUrl
				).build();
			}

			// strips any query params (prevents redirectUrl mismatch)
			String redirectUrl = uriInfo.getRequestUriBuilder().replaceQuery("").build().toString();

			Pair<GitLabOauthTokens, Map<String, String>> resultPair;
			try (CloseableHttpClient httpClient = httpClientFactory.create()) {
				resultPair = GitLabOauthHandler.handleCallbackAndGetTokens(
						authorizationCode,
						state,
						error,
						redirectUrl,
						httpClient,
						sessionStorageHandler::getAttribute,
						sessionStorageHandler::setAttribute
				);
			}

			return Response.ok(authenticateWithOauthTokens(resultPair.getFirst(), redirectUrl)).build();
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "Failed to process OAuth callback", e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@GET
	@Path("/google")
	// swagger:
	@Operation(
			description = "Authenticate with your Google account. Only use this if you log in to CATMA using the 'Sign in with Google' option. A browser is " +
					"required to complete the OAuth flow. Redirects to Google and, if successfully authenticated, responds in the same way as the /auth " +
					"endpoint. Note that you won't be able to test this in Swagger UI (but you can easily visit the URL in a separate tab to try it out).",
			responses = {
					@ApiResponse(responseCode = "307", description = "Redirect to Google login")
			}
	)
	public Response googleOauth() {
		try {
			URI authorizationUri = GoogleOauthHandler.getOauthAuthorizationRequestUri(
					// appends '/callback' to the current URL path and strips any query params (as they would cause a redirectUrl mismatch)
					uriInfo.getRequestUriBuilder().path("callback").replaceQuery("").build().toString(),
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
	// swagger:
	@Hidden
	public Response googleOauthCallback(@QueryParam("code") String authorizationCode, @QueryParam("state") String state, @QueryParam("error") String error) {
		try {
			// state should always be present; if we don't get a code, we *should* get an error
			if (state == null || (authorizationCode == null && error == null)) {
				return Response.status(Status.BAD_REQUEST).build();
			}

			if (authorizationCode == null && error.equals("access_denied")) {
				// the user cancelled the auth process with Google or didn't allow the requested access
				String requestUrl = uriInfo.getRequestUri().toString();
				String googleAuthUrl = requestUrl.substring(0, requestUrl.lastIndexOf("/")); // removes '/callback' and any query params
				return Response.ok(
						"You seem to have cancelled the Google sign-in or you didn't allow the requested access. " +
								"To restart the process and try again, please visit the following URL:\n" + googleAuthUrl
				).build();
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
	
	private String authenticateWithOauthTokens(GitLabOauthTokens oauthTokens, String redirectUrl) throws IOException, JOSEException {
		GitLabOauthTokenProvider oauthTokenProvider = new GitLabOauthTokenProvider(oauthTokens, redirectUrl, httpClientFactory::create);

		RemoteGitManagerRestricted remoteGitManagerRestricted = remoteGitMangerRestrictedFactory.create(oauthTokenProvider);

		remoteGitManagerRestrictedProviderCache.put(
				remoteGitManagerRestricted.getUsername(),
				new OauthTokenRemoteGitManagerRestrictedProvider(oauthTokenProvider, remoteGitMangerRestrictedFactory)
		);

		return createJWToken(remoteGitManagerRestricted.getUser());
	}
	
	private String authenticateWithThirdPartyToken(
			String oauthAuthorizationCode, String oauthState, String oauthError
	) throws OauthException, IOException, JOSEException {
			Pair<OauthIdentity, Map<String, String>> resultPair;
			try (CloseableHttpClient httpClient = httpClientFactory.create()) {
				resultPair = GoogleOauthHandler.handleCallbackAndGetIdentity(
						oauthAuthorizationCode,
						oauthState,
						oauthError,
						uriInfo.getRequestUriBuilder().replaceQuery("").build().toString(), // strips any query params (prevents redirectUrl mismatch)
						httpClient,
						sessionStorageHandler::getAttribute,
						sessionStorageHandler::setAttribute
				);
			}

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
		// also see AuthorizationRequestFilter

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
		SignedJWT signedJWT = new SignedJWT(new JWSHeader(AuthConstants.PERMISSIBLE_JWS_ALGORITHMS.getFirst()), claimsSet);
		signedJWT.sign(signer);

		return signedJWT.serialize();
		
	}
}
