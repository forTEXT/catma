package de.catma.ui.authentication;

import java.util.HashMap;
import java.util.Map;

import com.vaadin.ui.UI;

import de.catma.ui.Parameter;
import de.catma.ui.ParameterProvider;
import de.catma.user.UserProperty;

public class AuthenticationHandler {
	
	public interface AuthenticationListener {
		public void authenticated(Map<String, String> userIdentification);
	}
	

	public void authenticate(AuthenticationListener authenticationListener) {
		String user = 
				((ParameterProvider)UI.getCurrent()).getParameter(
						Parameter.USER_IDENTIFIER);
		if (user == null) {
			user = System.getProperty("user.name"); //$NON-NLS-1$
		}
		Map<String,String> userIdentification = 
				new HashMap<String, String>(1);
		userIdentification.put(
			UserProperty.identifier.name(), user);
		userIdentification.put(
			UserProperty.provider.name(), "localhost");
		authenticationListener.authenticated(userIdentification);
		
	}
}
