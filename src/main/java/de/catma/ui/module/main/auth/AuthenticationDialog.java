package de.catma.ui.module.main.auth;

import com.vaadin.server.VaadinSession;
import com.vaadin.ui.Window;
import de.catma.properties.CATMAPropertyKey;
import org.jboss.aerogear.security.otp.Totp;
import org.jboss.aerogear.security.otp.api.Clock;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.security.SecureRandom;

public abstract class AuthenticationDialog extends Window {
    public AuthenticationDialog(String caption) {
        super(caption);
    }

    protected String getGoogleOauthAuthorisationRequestUrl() throws UnsupportedEncodingException {
        // https://developers.google.com/identity/protocols/oauth2/openid-connect#authenticatingtheuser
        // TODO: document how this interacts with handleRequestOauth in CatmaApplication
        String token = new BigInteger(130, new SecureRandom()).toString(32);
        VaadinSession.getCurrent().setAttribute("OAUTHTOKEN", token);

        Totp totp = new Totp(
                CATMAPropertyKey.OTP_SECRET.getValue() + token,
                new Clock(CATMAPropertyKey.OTP_DURATION.getIntValue())
        );

        return String.format(
                // note %% escape, otherwise an exception is thrown
                "%s?client_id=%s&response_type=code&scope=openid%%20email&redirect_uri=%s&state=%s",
                CATMAPropertyKey.GOOGLE_OAUTH_AUTHORIZATION_CODE_REQUEST_URL.getValue(),
                CATMAPropertyKey.GOOGLE_OAUTH_CLIENT_ID.getValue(),
                URLEncoder.encode(CATMAPropertyKey.BASE_URL.getValue(), "UTF-8"),
                totp.now()
        );
    }
}
