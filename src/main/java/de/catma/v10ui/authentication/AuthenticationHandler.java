package de.catma.v10ui.authentication;

import de.catma.user.UserProperty;

import java.util.HashMap;
import java.util.Map;

//import de.catma.ui.Parameter;
//import de.catma.ui.ParameterProvider;

public class AuthenticationHandler {

    public interface AuthenticationListener {
        public void authenticated(Map<String, String> userIdentification);
    }


    public void authenticate(AuthenticationListener authenticationListener) {

        String user = System.getProperty("user.name"); //$NON-NLS-1$

        Map<String,String> userIdentification =
                new HashMap<String, String>();
        userIdentification.put(
                UserProperty.identifier.name(), user);
        userIdentification.put(
                UserProperty.provider.name(), "catma");

        userIdentification.put(
                UserProperty.email.name(), user + "@catma.de"); //TODO: debugging purposes only
        userIdentification.put(
                UserProperty.name.name(), user);

        authenticationListener.authenticated(userIdentification);

    }
}
