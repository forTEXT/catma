package de.catma.v10ui.modules.main;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.router.RouterLink;
import de.catma.v10ui.util.Version;


public class CatmaHeader extends Composite<Header>  {

    private final Div contextInformation = new Div();

    @Override
    protected Header initContent() {
        Header header = new Header();
        header.add(new H3(new RouterLink("Catma " + Version.LATEST,MainView.class)));
        header.add(contextInformation);

        return header;
    }

    public void setContextInformation(String value) {
        contextInformation.removeAll();
        contextInformation.add(new H3(value));
    }


}
