package de.catma.ui.module.project;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.vaadin.ui.ListSelect;
import com.vaadin.ui.UI;

import de.catma.project.Project;
import de.catma.rbac.RBACRole;
import de.catma.ui.UIMessageListener;
import de.catma.ui.events.InvitationRequestMessage;
import de.catma.ui.events.JoinedProjectMessage;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.util.ColorConverter;

public class ProjectInvitationHandler extends UIMessageListener<InvitationRequestMessage> {
	
	private final Map<String, DocumentResource> documentResourceByUuid;
	private final Map<Integer, String> userId2Color = new HashMap<>();
	private final Set<String> colors = ColorConverter.getColorNames();
	private final Set<Integer> assignedUsers = new HashSet<>();
	private final ProjectInvitation projectInvitation;
	private final Project project;
	private final ITopic<JoinedProjectMessage> joinedTopic;
	private final List<String> joinedUsers;
	private final ListSelect<String> joinedUsersConsole;
	
	public ProjectInvitationHandler(
			UI ui, 
			Map<String, DocumentResource> documentResourceByUuid, 
			ProjectInvitation projectInvitation, 
			Project project,
			ITopic<JoinedProjectMessage> joinedTopic, 
			List<String> joinedUsers, 
			ListSelect<String> joinedUsersConsole) {
		super(ui);
		this.documentResourceByUuid = documentResourceByUuid;
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
				Integer userId = Integer.valueOf(message.getMessageObject().getUserId());
				
				if (!assignedUsers.contains(userId)) {
					
					assignedUsers.add(userId);
					
					project.assignOnProject(
							() -> userId, 
							RBACRole.forValue(projectInvitation.getDefaultRole()));
					
					if (projectInvitation.isCreateOwnCollection()) {
						String color = userId2Color.get(userId);
						
						if (color == null && !colors.isEmpty()) {
							color = colors.iterator().next();
							colors.remove(color);
							userId2Color.put(userId, color);
						}
						
						for (String documentId : projectInvitation.getDocumentIds()) {
							if (projectInvitation.getDefaultRole() < RBACRole.REPORTER.getAccessLevel()) {
								// minimum role 
								project.assignOnResource(
										() -> userId, 
										RBACRole.REPORTER, documentId);
							}							
							
							DocumentResource docResource = 
									(DocumentResource) documentResourceByUuid.get(documentId);
							if (docResource != null) {
								String collectionName = 
									color 
									+ " " 
									+ message.getMessageObject().getName() 
									+ " "
									+ docResource.getName();
								
								// collection creation with minimum role assignment
								project.createUserMarkupCollectionWithAssignment(
									collectionName, 
									docResource.getDocument(), 
									projectInvitation.getDefaultRole() < RBACRole.ASSISTANT.getAccessLevel()?
											userId:null,
									RBACRole.ASSISTANT);
							}
							
						}	
					}
					
					joinedTopic.publish(new JoinedProjectMessage(projectInvitation));
					
					joinedUsers.add(message.getMessageObject().getName());
					joinedUsersConsole.getDataProvider().refreshAll();
					joinedUsersConsole.setVisible(true);
					
					this.getUi().push();
				}					
			} catch (IOException e) {
				((ErrorHandler) getUi()).showAndLogError(
						"Can't assign User " 
								+ message.getMessageObject().getName() 
								+ " to this Project", e);
			}
		}
	}
}
