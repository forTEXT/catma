package de.catma.ui.events;

import java.io.Serializable;

public class InvitationRequestMessage implements Serializable {

	private static final long serialVersionUID = 6959708701334863307L;
	
	private final int code;
	private final String name;

	private final long userId;

	public InvitationRequestMessage(long userId, String name, int code){
		this.code = code;
		this.name = name;
		this.userId = userId;
	}

	public int getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	public long getUserId() {
		return userId;
	}
}
