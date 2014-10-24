package de.catma.ui.repository.sharing;

import de.catma.document.AccessMode;

public class SharingOptions {
	private String userIdentification;
	private AccessMode accessMode;
	
	public SharingOptions() {
		this.userIdentification = "";
		this.accessMode = AccessMode.READ;
	}

	public String getUserIdentification() {
		return userIdentification;
	}
	
	public void setUserIdentification(String userIdentification) {
		this.userIdentification = userIdentification;
	}

	public AccessMode getAccessMode() {
		return accessMode;
	}

	public void setAccessMode(AccessMode accessMode) {
		this.accessMode = accessMode;
	}
	
	
	
}
