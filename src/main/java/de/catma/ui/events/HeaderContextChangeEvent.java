package de.catma.ui.events;

/**
 * fired when header context needs to be changed
 *
 * @author db
 */
public class HeaderContextChangeEvent  {

    private final String value;
    private final boolean dashboard;

    public HeaderContextChangeEvent(String value){
        this.value = value;
        this.dashboard = false;
    }
    
    public HeaderContextChangeEvent() {
		this.value = "";
		this.dashboard = true;
	}

	public String getValue() {
        return value;
    }
	
	public boolean isDashboard() {
		return dashboard;
	}
}
