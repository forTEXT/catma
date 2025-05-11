package de.catma.oauth;

public class OauthException extends Exception {
    public OauthException(String message, Throwable cause) {
        super(message, cause);
    }

    public OauthException(String message) {
        super(message);
    }
}
