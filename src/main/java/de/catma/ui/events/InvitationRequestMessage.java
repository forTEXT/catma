package de.catma.ui.events;

import java.io.Serializable;

public class InvitationRequestMessage implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6959708701334863307L;
	
	private final int code;
	private final String username;
	private final int userid;

	public InvitationRequestMessage(int userid, String username, int code){
		this.code = code;
		this.username = username;
		this.userid = userid;
	}

	public int getCode() {
		return code;
	}

	public String getUsername() {
		return username;
	}

	public int getUserid() {
		return userid;
	}

	
}
