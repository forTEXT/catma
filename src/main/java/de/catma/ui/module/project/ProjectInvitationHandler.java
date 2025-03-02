package de.catma.ui.module.project;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hazelcast.topic.ITopic;
import com.hazelcast.topic.Message;
import com.vaadin.ui.ListSelect;
import com.vaadin.ui.UI;

import de.catma.project.Project;
import de.catma.rbac.RBACRole;
import de.catma.ui.UIMessageListener;
import de.catma.ui.events.InvitationRequestMessage;
import de.catma.ui.events.JoinedProjectMessage;
import de.catma.ui.module.main.ErrorHandler;

public class ProjectInvitationHandler extends UIMessageListener<InvitationRequestMessage> {
	
	private final Set<Long> assignedUsers = new HashSet<>();
	private final ProjectInvitation projectInvitation;
	private final Project project;
	private final ITopic<JoinedProjectMessage> joinedTopic;
	private final List<String> joinedUsers;
	private final ListSelect<String> joinedUsersConsole;
	
	public ProjectInvitationHandler(
			UI ui, 
			ProjectInvitation projectInvitation, 
			Project project,
			ITopic<JoinedProjectMessage> joinedTopic, 
			List<String> joinedUsers, 
			ListSelect<String> joinedUsersConsole) {
		super(ui);
		this.projectInvitation = projectInvitation;
		this.project = project;
		this.joinedTopic = joinedTopic;
		this.joinedUsers = joinedUsers;
		this.joinedUsersConsole = joinedUsersConsole;
	}

	@Override
	public void uiOnMessage(Message<InvitationRequestMessage> message) {
		if (message.getMessageObject().getCode() == projectInvitation.getKey()) {
			try {
				Long userId = Long.valueOf(message.getMessageObject().getUserId());
				
				if (!assignedUsers.contains(userId)) {
					
					assignedUsers.add(userId);
					
					project.assignRoleToSubject(
							() -> userId, 
							RBACRole.forValue(projectInvitation.getDefaultRole()),
							projectInvitation.getExpiresAtDate()==null?null:LocalDate.parse(projectInvitation.getExpiresAtDate(), DateTimeFormatter.ISO_LOCAL_DATE));

					joinedTopic.publish(new JoinedProjectMessage(projectInvitation));
					
					joinedUsers.add(message.getMessageObject().getName());
					joinedUsersConsole.getDataProvider().refreshAll();
					joinedUsersConsole.setVisible(true);
					
					this.getUi().push();
				}					
			} catch (IOException e) {
				((ErrorHandler) getUi()).showAndLogError(
						"Can't assign user "
								+ message.getMessageObject().getName() 
								+ " to this project", e);
			}
		}
	}
}
