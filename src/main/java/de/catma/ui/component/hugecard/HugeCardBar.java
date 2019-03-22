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
    private final IconButton btMoreOptions;
    private final ContextMenu moreOptionsContextMenu;
    private final HorizontalLayout moreOptionsPanel;

    public HugeCardBar(String title){
        this.title = title;
        moreOptionsPanel = new HorizontalLayout();
        btMoreOptions = new IconButton(VaadinIcons.ELLIPSIS_DOTS_V);
        moreOptionsContextMenu = new ContextMenu(btMoreOptions, false);
        btMoreOptions.addClickListener((evt) ->  moreOptionsContextMenu.open(evt.getClientX(), evt.getClientY()));
        initComponents();
    }

    protected void initComponents() {
    	setStyleName(Styles.hugecard__bar);
        Label headerText = new Label(title);
        headerText.setWidth("100%");
        addComponent(headerText);
        moreOptionsPanel.setStyleName(Styles.hugecard__bar__moreoptions);
        btMoreOptions.addStyleName(Styles.hugecard__bar__moreoptions__btn);
        moreOptionsPanel.addComponent(btMoreOptions);
        addComponent(moreOptionsPanel);
    }

    public ContextMenu getMoreOptionsContextMenu() { return this.moreOptionsContextMenu; }

}
