package de.catma.ui.events;

/**
 * fired when header context needs to be changed
 *
 * @author db
 */
public class HeaderContextChangeEvent  {

    private final String value;
    private final boolean dashboard;
    private final boolean readonly;

    public HeaderContextChangeEvent(String value) {
    	this(value, false);
    }
    
    public HeaderContextChangeEvent(String value, boolean readonly) {
        this.value = value;
        this.dashboard = false;
        this.readonly = readonly;
    }
    
    public HeaderContextChangeEvent() {
		this.value = "";
		this.dashboard = true;
		this.readonly = false;
	}

	public String getValue() {
        return value;
    }
	
	public boolean isDashboard() {
		return dashboard;
	}
	
	public boolean isReadonly() {
		return readonly;
	}
}
