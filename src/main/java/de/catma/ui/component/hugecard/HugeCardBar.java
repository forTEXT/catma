package de.catma.ui.component.hugecard;

import com.vaadin.contextmenu.ContextMenu;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;

import de.catma.ui.component.IconButton;

/**
 *
 * renders an actions bar for the huge card
 * @author db
 */
public class HugeCardBar extends HorizontalLayout {

    private final String title;
    private final IconButton btMoreOptions;
    private final ContextMenu moreOptionsContextMenu;

    public HugeCardBar(String title){
        this.title = title;
        btMoreOptions = new IconButton(VaadinIcons.ELLIPSIS_DOTS_V);
        moreOptionsContextMenu = new ContextMenu(btMoreOptions, false);
        btMoreOptions.addClickListener((evt) ->  moreOptionsContextMenu.open(evt.getClientX(), evt.getClientY()));
        initComponents();
    }

    protected void initComponents() {
    	setWidth("100%");
    	addStyleName("hugecard-bar");
    	
        Label headerText = new Label(title);
        addComponent(headerText);
        addComponent(btMoreOptions);
        setComponentAlignment(btMoreOptions, Alignment.MIDDLE_RIGHT);
    }

    public ContextMenu getMoreOptionsContextMenu() { return this.moreOptionsContextMenu; }

}
