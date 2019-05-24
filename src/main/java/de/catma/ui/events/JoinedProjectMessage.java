package de.catma.ui.events;

import java.io.Serializable;

import de.catma.ui.modules.project.ProjectInvitation;

public class JoinedProjectMessage implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4432309957370280636L;
	private final ProjectInvitation invitation;

	public JoinedProjectMessage(ProjectInvitation invitation) {
		this.invitation = invitation;
	}

	public ProjectInvitation getInvitation() {
		return invitation;
	}

}
