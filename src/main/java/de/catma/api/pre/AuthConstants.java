package de.catma.api.pre;

import com.google.common.collect.ImmutableList;
import com.nimbusds.jose.JWSAlgorithm;

public class AuthConstants {
    public static final String AUTH_SERVICE_PATH = "/auth";

    public static final ImmutableList<JWSAlgorithm> PERMISSIBLE_JWS_ALGORITHMS = ImmutableList.of(
            JWSAlgorithm.HS256 // the default, iteration order is guaranteed with ImmutableList
    );

    public static final String AUTHORIZATION_HEADER_NAME = "Authorization";
    public static final String AUTHENTICATION_SCHEME_BEARER_PREFIX = "Bearer ";
    public static final String AUTHENTICATION_SCHEME_BASIC_PREFIX = "Basic ";

    // query parameters - if Authorization header is not used
    // initial auth only, GitLab impersonation or personal access token, swapped for a JWT
    public static final String AUTH_ENDPOINT_TOKEN_PARAMETER_NAME = "accessToken";
    // all non-auth requests, our own JWT in the normal flow, but can also be a GitLab token (see JwtValidationFilter)
    public static final String API_TOKEN_PARAMETER_NAME = "apiToken";
}
