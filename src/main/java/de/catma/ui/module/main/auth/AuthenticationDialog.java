package de.catma.ui.module.main.auth;

import com.vaadin.server.VaadinSession;
import com.vaadin.ui.Button;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;
import de.catma.oauth.GitLabOauthHandler;
import de.catma.oauth.GoogleOauthHandler;
import de.catma.oauth.OauthConstants;
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

    protected void gitLabLinkClickListener(Button.ClickEvent event) {
        redirectToOauthProvider(OauthConstants.OauthProvider.GITLAB, true);
    }

    protected void googleLinkClickListener(Button.ClickEvent event) {
        redirectToOauthProvider(OauthConstants.OauthProvider.GOOGLE, true);
    }

    /**
     * Redirects the client to the authorization endpoint of the given OAuth provider.
     * <p>
     * Both providers redirect back to <code>BASE_URL</code>, where {@link de.catma.ui.CatmaApplication#init} picks the callback up and dispatches it based on
     * the provider that was recorded in the session by the corresponding handler.
     *
     * @param provider the OAuth provider to redirect to
     * @param forwardRequestTokenParams whether to carry our own action and token request parameters through the flow, so that the context can be recovered
     *                                  when the user returns. Callers that have already consumed those parameters must pass <code>false</code>, otherwise
     *                                  they are handled a second time when the user returns (see {@link de.catma.ui.RequestTokenHandler}).
     */
    protected void redirectToOauthProvider(OauthConstants.OauthProvider provider, boolean forwardRequestTokenParams) {
        Map<String, String> optionalStateParams = null;

        // there could be action and token parameters (ours - used for signups and invitations, although only the latter are relevant here)
        // if so then we pass them along so that we can retrieve them after the user has authenticated
        if (forwardRequestTokenParams) {
            ParameterProvider parameterProvider = (ParameterProvider) UI.getCurrent();
            String action = parameterProvider.getParameter(Parameter.ACTION);
            String token = parameterProvider.getParameter(Parameter.TOKEN);
            if (!StringUtils.isBlank(token) && !StringUtils.isBlank(action)) {
                optionalStateParams = new HashMap<>();
                optionalStateParams.put(Parameter.ACTION.getKey(), action);
                optionalStateParams.put(Parameter.TOKEN.getKey(), token);
            }
        }

        URI oauthAuthorizationRequestUri = provider == OauthConstants.OauthProvider.GITLAB
                ? GitLabOauthHandler.getOauthAuthorizationRequestUri(
                        CATMAPropertyKey.BASE_URL.getValue(),
                        VaadinSession.getCurrent()::setAttribute,
                        optionalStateParams
                )
                : GoogleOauthHandler.getOauthAuthorizationRequestUri(
                        CATMAPropertyKey.BASE_URL.getValue(),
                        VaadinSession.getCurrent()::setAttribute,
                        optionalStateParams
                );

        UI.getCurrent().getPage().setLocation(oauthAuthorizationRequestUri);
        close();
    }
}
