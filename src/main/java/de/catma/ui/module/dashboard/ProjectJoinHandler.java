package de.catma.ui.module.dashboard;

import com.google.common.eventbus.EventBus;
import com.hazelcast.core.Message;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;

import de.catma.ui.UIMessageListener;
import de.catma.ui.events.JoinedProjectMessage;
import de.catma.ui.events.ProjectChangedEvent;
import de.catma.ui.module.project.ProjectInvitation;

public class ProjectJoinHandler extends UIMessageListener<JoinedProjectMessage> {
	
	public static interface CloseHandler {
		public void close();
	}
	
	private final CloseHandler closeHandler;
	private final ProjectInvitation invitation;
	private final EventBus eventBus;
	
	

	public ProjectJoinHandler(UI ui, CloseHandler closeHandler, ProjectInvitation invitation, EventBus eventBus) {
		super(ui);
		this.closeHandler = closeHandler;
		this.invitation = invitation;
		this.eventBus = eventBus;
	}

	@Override
	public void uiOnMessage(Message<JoinedProjectMessage> message) {
		if(message.getMessageObject().getInvitation().getKey() == this.invitation.getKey()) {	
			this.eventBus.post(new ProjectChangedEvent());
			Notification.show("Joined successfully", "Sucessfully joined Project " + 
					this.invitation.getName() , Type.HUMANIZED_MESSAGE);
			this.getUi().push();
			closeHandler.close();
		}	
	}

}
