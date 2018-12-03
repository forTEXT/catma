package de.catma.v10ui.modules.main;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.router.RouterLink;
import de.catma.v10ui.util.Version;

/**
 * Displays the header and context information
 *
 * @author db
 */
public class CatmaHeader extends Composite<Header>  {

    @Inject
    public CatmaHeader(EventBus eventBus){
        eventBus.register(this);
    }

    private final Div contextInformation = new Div();

    @Override
    protected Header initContent() {
        Header header = new Header();
        header.add(new H2(new RouterLink("Catma " + Version.LATEST, MainView.class)));
        contextInformation.addClassName("header__context");
        header.add(contextInformation);

        return header;
    }

    @Subscribe
    public void headerChangeEvent(HeaderContextChangeEvent headerContextChangeEvent){
        contextInformation.removeAll();
        contextInformation.add(headerContextChangeEvent.getValue());
    }

}
