package de.catma.ui.component.hugecard;

import com.vaadin.contextmenu.ContextMenu;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
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
        setExpandRatio(headerText, 1.0f);
        addComponent(btMoreOptions);
        setComponentAlignment(btMoreOptions, Alignment.MIDDLE_RIGHT);
    }

    public ContextMenu getMoreOptionsContextMenu() { return this.moreOptionsContextMenu; }
    public void setMoreOptionsButtonVisible(boolean visible) {
    	this.btMoreOptions.setVisible(visible);
    }

    public void addComponentBeforeMoreOptions(Component comp) {
    	addComponent(comp, getComponentIndex(btMoreOptions));
    	setComponentAlignment(comp, Alignment.MIDDLE_RIGHT);
    }
}
