package de.catma.user.signup;

/**
 * Signup token bean class. It can be marshalled to json and back. This is useful for verification purpose.
 * @author db
 *
 */
public record AccountSignupToken(String requestDate, String email, String token) {
}
