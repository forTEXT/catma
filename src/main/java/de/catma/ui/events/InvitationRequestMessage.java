package de.catma.ui.events;

import java.io.Serializable;

public class InvitationRequestMessage implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6959708701334863307L;
	
	private final int code;
	private final String name;
	private final int userid;

	public InvitationRequestMessage(int userid, String name, int code){
		this.code = code;
		this.name = name;
		this.userid = userid;
	}

	public int getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	public int getUserid() {
		return userid;
	}

	
}
