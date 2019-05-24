package de.catma.ui.modules.project;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.cache.Cache;
import javax.cache.Caching;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import com.jsoniter.output.JsonStream;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Label;
import com.vaadin.ui.ListSelect;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

import de.catma.hazelcast.HazelCastService;
import de.catma.hazelcast.HazelcastConfiguration;
import de.catma.project.ProjectReference;
import de.catma.rbac.RBACRole;
import de.catma.repository.git.interfaces.IRemoteGitManagerRestricted;
import de.catma.ui.events.InvitationRequestMessage;
import de.catma.ui.events.JoinedProjectMessage;
import de.catma.ui.events.ResourcesChangedEvent;
import de.catma.ui.layout.FlexLayout.JustifyContent;
import de.catma.ui.layout.HorizontalLayout;
import de.catma.ui.layout.VerticalLayout;
import de.catma.ui.modules.main.ErrorHandler;

/**
 * Dialog to invite other member to a project
 * @author db
 *
 */
public class ProjectInvitationDialog extends Window {

    private final Cache<Integer, String> invitationCache = 
    		Caching.getCachingProvider().getCacheManager().getCache(HazelcastConfiguration.CACHE_KEY_INVITATIONS);
    

	private final ProjectReference projectRef;
	private final ComboBox<RBACRole> cb_role = new ComboBox<RBACRole>("role", 
			Lists.newArrayList(RBACRole.values()));
	private final Button btnInvite = new Button("Invite");
	private final Button btnStopInvite = new Button("Stop invitation");
	private final VerticalLayout content = new VerticalLayout();
	private final Label lInvitationCode = new Label("",ContentMode.HTML); 
	private final List<String> joinedUsers = new ArrayList<>();
	private final ListSelect<String> cbConsole = new ListSelect<>("joined users", joinedUsers);
	private final IRemoteGitManagerRestricted gitmanagerRestricted;
    private final ErrorHandler errorLogger;
    private final HazelcastInstance hazelcast;
    private final ITopic<InvitationRequestMessage> invitationTopic;
    private final ITopic<JoinedProjectMessage> joinedTopic;
    private final EventBus eventBus;
    
	private ProjectInvitation projectInvitation;



	@Inject
	public ProjectInvitationDialog(
			@Assisted("projectref") ProjectReference projectRef,
			IRemoteGitManagerRestricted gitmanagerRestricted,
			EventBus eventBus,
			HazelCastService hazelcastService){
		super("Invite to project");
		setDescription("Invite user to project");
		setModal(true);
		this.projectRef = projectRef;
		this.gitmanagerRestricted = gitmanagerRestricted;
	    this.errorLogger = (ErrorHandler) UI.getCurrent();
	    this.eventBus = eventBus;
	    this.hazelcast = hazelcastService.getHazelcastClient();
	    invitationTopic = hazelcast.getTopic(HazelcastConfiguration.TOPIC_PROJECT_INVITATIONS);
	    joinedTopic = hazelcast.getTopic(HazelcastConfiguration.TOPIC_PROJECT_JOINED);
		initComponents();
	}

	private class ProjectInvitationHandler implements MessageListener<InvitationRequestMessage> {

		@Override
		public void onMessage(Message<InvitationRequestMessage> message) {
			if(message.getMessageObject().getCode() == projectInvitation.getKey()){
				try {
					gitmanagerRestricted.assignOnProject(() -> message.getMessageObject().getUserid(), 
							RBACRole.forValue(projectInvitation.getDefaultRole()), projectInvitation.getProjectId());
					joinedTopic.publish(new JoinedProjectMessage(projectInvitation));
					joinedUsers.add(message.getMessageObject().getUsername());
					cbConsole.setItems(joinedUsers);
					cbConsole.markAsDirty();
					cbConsole.setVisible(true);
					ProjectInvitationDialog.this.getUI().push();
				} catch (IOException e) {
					errorLogger.showAndLogError("Can't assign UserId " + message.getMessageObject().getUsername() + "to this project", e);
				}
			}			
		}
	}
	
	private void initComponents() {
		content.addStyleName("spacing");
		content.addStyleName("margin");

		lInvitationCode.setCaption("Your Invitation code");
		lInvitationCode.setVisible(false);
		content.addComponent(lInvitationCode);

		cb_role.setWidth("100%");
		cb_role.setItemCaptionGenerator(RBACRole::getRolename);
		cb_role.setEmptySelectionAllowed(false);
		
		content.addComponent(cb_role);
		
		cbConsole.setWidth("100%");
		cbConsole.setCaption("Users");
		cbConsole.setReadOnly(true);
		cbConsole.setVisible(false);

		content.addComponent(cbConsole);
		
		HorizontalLayout buttonPanel = new HorizontalLayout();
		buttonPanel.addStyleName("spacing-left-right");
		buttonPanel.setJustifyContent(JustifyContent.FLEX_END);
		
		buttonPanel.addComponent(btnInvite);
		buttonPanel.addComponent(btnStopInvite);
		
		btnInvite.addClickListener(this::handleInvitePressed);
		
		btnStopInvite.addClickListener(evt -> close());
		
		content.addComponent(buttonPanel);
		setContent(content);
	}
	
	private void handleInvitePressed(ClickEvent clickEvent){
		setDescription("Invitation running... keep this Dialog open");
		projectInvitation = new ProjectInvitation(projectRef.getProjectId(), cb_role.getValue().value, projectRef.getName(), projectRef.getDescription());
		invitationCache.put(projectInvitation.getKey(), JsonStream.serialize(projectInvitation));
		lInvitationCode.setValue("<h1>" + projectInvitation.getKey() + "</h2>");
		lInvitationCode.setVisible(true);
		btnInvite.setEnabled(false);
	    String regid = invitationTopic.addMessageListener(new ProjectInvitationHandler());      
		addCloseListener((evt) -> invitationTopic.removeMessageListener(regid));
	}
	
	public void show(){
		UI.getCurrent().addWindow(this);
	}
	
	@Override
	public void close() {
		super.close();
		eventBus.post(new ResourcesChangedEvent<>(this));
	}
}
