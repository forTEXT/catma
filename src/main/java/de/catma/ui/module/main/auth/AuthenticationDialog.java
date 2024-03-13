package de.catma.ui.module.main.auth;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.logging.Logger;

import org.jboss.aerogear.security.otp.Totp;
import org.jboss.aerogear.security.otp.api.Clock;

import com.vaadin.server.VaadinSession;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

import de.catma.properties.CATMAPropertyKey;
import de.catma.ui.CatmaApplication;
import de.catma.ui.Parameter;
import de.catma.ui.ParameterProvider;

public abstract class AuthenticationDialog extends Window {
    public AuthenticationDialog(String caption) {
        super(caption);
    }

    /**
     * @see CatmaApplication#handleOauth(com.vaadin.server.VaadinRequest)
     */
    protected String getGoogleOauthAuthorisationRequestUrl() throws UnsupportedEncodingException {
    	String requestUrl = CATMAPropertyKey.BASE_URL.getValue(); // redirect_uri does not support dynamic arguments, 
    															  // the uri has to match exactly the one configured in the console
    	
        // https://developers.google.com/identity/protocols/oauth2/openid-connect#authenticatingtheuser
    	
    	// the created oauth token gets added to the session and a timed-one-time-password (Totp) is created out of it
    	// by clicking the authorization-request-url created in this method the user is taken to the 
    	// authentication page of the third party oauth provider
    	// the oauth provider will redirect back to the given 'redirect_uri' with the authentication result
    	// the redirected call will then be verified using the oauthToken from the session and the Totp timestamp
    	// from the state in CatmaApplication::handleRequestOauth
        String oauthToken = new BigInteger(130, new SecureRandom()).toString(32);
        VaadinSession.getCurrent().setAttribute("OAUTHTOKEN", oauthToken);

        Totp totp = new Totp(
                CATMAPropertyKey.OTP_SECRET.getValue() + oauthToken,
                new Clock(CATMAPropertyKey.OTP_DURATION.getIntValue())
        );
        
        String state = totp.now();

        // if we got an action and a token for further processing after authentication
        // we add it to the state (do not add params to the redirect_url!, see above)
        ParameterProvider parameterProvider = (ParameterProvider) UI.getCurrent();
        String token = parameterProvider.getParameter(Parameter.TOKEN);
        String action = parameterProvider.getParameter(Parameter.ACTION);
        if (token != null && !token.isEmpty() && action != null && !action.isEmpty()) {
        	state += ":" + action + ":" + token;
        }
        
        Logger.getLogger(getClass().getName()).info("OAUTH RedirectURL is " + requestUrl + " state is"+state);
        
        return String.format(
                // note %% escape, otherwise an exception is thrown
                "%s?client_id=%s&response_type=code&scope=openid%%20email&redirect_uri=%s&state=%s",
                CATMAPropertyKey.GOOGLE_OAUTH_AUTHORIZATION_CODE_REQUEST_URL.getValue(),
                CATMAPropertyKey.GOOGLE_OAUTH_CLIENT_ID.getValue(),
                URLEncoder.encode(requestUrl, "UTF-8"),
                new String(Base64.getEncoder().encode(state.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8)
        );
    }
}
