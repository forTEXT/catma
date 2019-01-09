package de.catma.ui.component.hugecard;

import com.vaadin.contextmenu.ContextMenu;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Label;

import de.catma.ui.component.IconButton;
import de.catma.ui.layout.HorizontalLayout;
import de.catma.ui.util.Styles;

/**
 *
 * renders an actions bar for the huge card
 * @author db
 */
public class HugeCardBar extends HorizontalLayout {

    private final String title;
    private final IconButton buttonMoreOptions;
    private final ContextMenu ctmMoreOptions;
    private final HorizontalLayout moreOptions;

    public HugeCardBar(String title){
        this.title = title;
        moreOptions = new HorizontalLayout();
        buttonMoreOptions = new IconButton(VaadinIcons.ELLIPSIS_DOTS_V);
        ctmMoreOptions = new ContextMenu(buttonMoreOptions, false);
        buttonMoreOptions.addClickListener((evt) ->  ctmMoreOptions.open(evt.getClientX(), evt.getClientY()));
        initComponents();
    }

    protected void initComponents() {
    	setStyleName(Styles.hugecard__bar);
        Label headerText = new Label(title);
        headerText.setWidth("100%");
        addComponent(headerText);
        moreOptions.setStyleName(Styles.hugecard__bar__moreoptions);
        buttonMoreOptions.addStyleName(Styles.hugecard__bar__moreoptions__btn);
        moreOptions.addComponent(buttonMoreOptions);
        addComponent(moreOptions);
    }

    public ContextMenu getBtnMoreOptionsContextMenu() { return this.ctmMoreOptions; }

}
