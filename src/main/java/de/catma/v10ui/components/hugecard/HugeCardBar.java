package de.catma.v10ui.components.hugecard;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.shared.Registration;
import de.catma.v10ui.components.IconButton;
import de.catma.v10ui.util.Styles;

/**
 *
 * renders an actions bar for the huge card
 * @author db
 */
public class HugeCardBar extends Composite<Div> {

    private final String title;
    private final IconButton buttonMoreOptions;
    private final ContextMenu ctmMoreOptions;
    private final Div moreOptions;

    public HugeCardBar(String title){
        this.title = title;
        moreOptions = new Div();
        buttonMoreOptions = new IconButton( VaadinIcon.ELLIPSIS_DOTS_V.create());
        ctmMoreOptions = new ContextMenu(buttonMoreOptions);
    }

    @Override
    protected Div initContent() {
        Div content = new Div();
        content.addClassName(Styles.hugecard__bar);
        Span headerText = new Span(title);
        headerText.setWidth("100%");
        content.add(headerText);
        moreOptions.setClassName(Styles.hugecard__bar__moreoptions);
        buttonMoreOptions.setClassName(Styles.hugecard__bar__moreoptions__btn);
        moreOptions.add(buttonMoreOptions);
        content.add(moreOptions);

        return content;
    }

    public ContextMenu getBtnMoreOptionsContextMenu() { return this.ctmMoreOptions; }

}
