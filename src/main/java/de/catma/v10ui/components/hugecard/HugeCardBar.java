package de.catma.v10ui.components.hugecard;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.shared.Registration;
import de.catma.v10ui.components.IconButton;
import de.catma.v10ui.components.contextmenu.ContextMenu;
import de.catma.v10ui.components.html.I;
import de.catma.v10ui.util.Styles;

/**
 *
 * renders an actions bar for the huge card
 * @author db
 */
public class HugeCardBar extends Composite<Div> implements ClickNotifier<NativeButton> {

    private final String title;
    private IconButton buttonMoreOptions;
    private Div moreOptions;

    public HugeCardBar(String title){
        this.title = title;
    }

    @Override
    protected Div initContent() {
        Div content = new Div();
        content.addClassName(Styles.hugecard__bar);

        Span headerText = new Span(title);
        content.add(headerText);
        moreOptions = new Div();
        moreOptions.setClassName(Styles.hugecard__bar__moreoptions);
        buttonMoreOptions = new IconButton( VaadinIcon.ELLIPSIS_DOTS_V.create());
        buttonMoreOptions.setClassName(Styles.hugecard__bar__moreoptions__btn);
        moreOptions.add(buttonMoreOptions);
        content.add(moreOptions);

        return content;
    }

    @Override
    public Registration addClickListener(ComponentEventListener<ClickEvent<NativeButton>> listener) {
        return buttonMoreOptions.addClickListener(listener);
    }
}
