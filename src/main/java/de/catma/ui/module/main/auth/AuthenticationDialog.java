package de.catma.ui.module.main.auth;

import com.vaadin.server.VaadinSession;
import com.vaadin.ui.Button;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;
import de.catma.oauth.GoogleOauthHandler;
import de.catma.properties.CATMAPropertyKey;
import de.catma.ui.Parameter;
import de.catma.ui.ParameterProvider;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public abstract class AuthenticationDialog extends Window {
    public AuthenticationDialog(String caption) {
        super(caption);
    }

    protected void googleLinkClickListener(Button.ClickEvent event) {
        Map<String, String> optionalStateParams = null;

        // there could be action and token parameters (ours - used for signups and invitations, although only the latter are relevant here)
        // if so then we pass them along so that we can retrieve them after the user has authenticated
        ParameterProvider parameterProvider = (ParameterProvider) UI.getCurrent();
        String action = parameterProvider.getParameter(Parameter.ACTION);
        String token = parameterProvider.getParameter(Parameter.TOKEN);
        if (!StringUtils.isBlank(token) && !StringUtils.isBlank(action)) {
            optionalStateParams = new HashMap<>();
            optionalStateParams.put(Parameter.ACTION.getKey(), action);
            optionalStateParams.put(Parameter.TOKEN.getKey(), token);
        }

        URI googleOauthAuthorizationRequestUri = GoogleOauthHandler.getOauthAuthorizationRequestUri(
                CATMAPropertyKey.BASE_URL.getValue(),
                VaadinSession.getCurrent()::setAttribute,
                optionalStateParams
        );

        UI.getCurrent().getPage().setLocation(googleOauthAuthorizationRequestUri);
        close();
    }
}
