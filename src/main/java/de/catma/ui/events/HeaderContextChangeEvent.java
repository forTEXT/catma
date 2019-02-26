package de.catma.ui.events;

import com.vaadin.ui.Component;

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
