package de.catma.ui.component.hugecard;

import com.vaadin.contextmenu.ContextMenu;
import com.vaadin.ui.VerticalLayout;

/**
 * Renders one huge card with action bar in the main view section
 *
 * @author db
 */
public class HugeCard extends VerticalLayout {

    private final HugeCardBar hugeCardBar;

    public HugeCard(String title) {
        this.hugeCardBar = new HugeCardBar(title);
        initCardComponents();
    }

   
    private void initCardComponents() {
    	setSizeFull();
    	setMargin(false);
        addStyleNames("hugecard");
        addComponent(hugeCardBar);
    }

    public ContextMenu getMoreOptionsContextMenu() { return hugeCardBar.getMoreOptionsContextMenu(); }
    protected HugeCardBar getHugeCardBar() {
		return hugeCardBar;
	}

}
