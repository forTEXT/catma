package de.catma.v10ui.modules.main;

import com.vaadin.flow.component.Component;

/**
 * fired when header context needs to be changed
 *
 * @author db
 */
public class HeaderContextChangeEvent  {

    private final Component value;

    public HeaderContextChangeEvent(Component value){
        this.value = value;
    }

    public Component getValue() {
        return value;
    }
}
