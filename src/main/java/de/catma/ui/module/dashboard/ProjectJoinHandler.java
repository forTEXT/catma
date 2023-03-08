package de.catma.ui.module.dashboard;

import com.google.common.eventbus.EventBus;
import com.hazelcast.core.Message;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;
import de.catma.ui.UIMessageListener;
import de.catma.ui.events.JoinedProjectMessage;
import de.catma.ui.events.ProjectsChangedEvent;
import de.catma.ui.module.project.ProjectInvitation;

public class ProjectJoinHandler extends UIMessageListener<JoinedProjectMessage> {
	public interface CloseHandler {
		void close();
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
	public void uiOnMessage(Message<JoinedProjectMessage> joinedProjectMessage) {
		if (joinedProjectMessage.getMessageObject().getInvitation().getKey() == invitation.getKey()) {
			eventBus.post(new ProjectsChangedEvent());

			Notification.show(
					"Joined successfully",
					String.format("Successfully joined project \"%s\"", invitation.getName()),
					Type.HUMANIZED_MESSAGE
			);
			getUi().push();

			closeHandler.close();
		}
	}
}
