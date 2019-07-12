package de.catma.ui.events;

/**
 * fired when header context needs to be changed
 *
 * @author db
 */
public class HeaderContextChangeEvent  {

    private final String value;

    public HeaderContextChangeEvent(String value){
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
