package de.catma.ui.module.project;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.cache.Cache;
import javax.cache.Caching;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.gson.Gson;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.topic.ITopic;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.ListSelect;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import de.catma.hazelcast.HazelCastService;
import de.catma.hazelcast.HazelcastConfiguration;
import de.catma.project.Project;
import de.catma.rbac.RBACRole;
import de.catma.ui.events.InvitationRequestMessage;
import de.catma.ui.events.JoinedProjectMessage;
import de.catma.ui.events.MembersChangedEvent;

/**
 * Dialog to invite other member to a project
 * @author db
 *
 */
public class ProjectInvitationDialog extends Window {
	
	private Label lInvitationCode; 
	private ComboBox<RBACRole> roleBox;
	private CheckBox cbOwnCollection;
	private ListSelect<String> joinedUsersConsole;
	private Button btnInvite;
	private Button btnStopInvite;
	private ProgressBar progressIndicator;
	
    private final Cache<Integer, String> invitationCache = 
    		Caching.getCachingProvider().getCacheManager().getCache(
    				HazelcastConfiguration.CacheKeyName.PROJECT_INVITATION.name());
    
	private final List<String> joinedUsers = new ArrayList<>();
	
    private final HazelcastInstance hazelcast;
    private final ITopic<InvitationRequestMessage> invitationTopic;
    private final ITopic<JoinedProjectMessage> joinedTopic;
    private final EventBus eventBus;
    
    private final Project project;

    private final List<DocumentResource> documentsForCollectionCreation;
    
	private ProjectInvitation projectInvitation;

	public ProjectInvitationDialog(
			Project project,
			List<DocumentResource> documentsForCollectionCreation,
			EventBus eventBus,
			HazelCastService hazelcastService) {
		
		super("Invite Others to the Project");
		
		this.project = project;
		this.documentsForCollectionCreation = documentsForCollectionCreation;
	    this.eventBus = eventBus;
	    this.hazelcast = hazelcastService.getHazelcastClient();
	    
	    this.invitationTopic = hazelcast.getTopic(HazelcastConfiguration.TopicName.PROJECT_INVITATION.name());
	    this.joinedTopic = hazelcast.getTopic(HazelcastConfiguration.TopicName.PROJECT_JOINED.name());
	    
		initComponents();
		initActions();
	}

	private void initActions() {
		btnInvite.addClickListener(this::handleInvitePressed);
		btnStopInvite.addClickListener(evt -> close());
	}

	private void setInvitationSettingsEnabled(boolean enabled) {
		cbOwnCollection.setEnabled(enabled);
		roleBox.setEnabled(enabled);
	}
	
	private void initComponents() {
		setModal(true);
		setWidth("30%");
		setHeight("50%");
		
		VerticalLayout content = new VerticalLayout();
		content.setSizeFull();
		
		lInvitationCode = new Label("",ContentMode.HTML); 
		lInvitationCode.setVisible(false);
		
		content.addComponent(lInvitationCode);

		cbOwnCollection = new CheckBox("Create one collection per document and joined user", false);
		cbOwnCollection.setValue(false);
		cbOwnCollection.setVisible(false); //TODO: value of this CB as no effect on the joining side so far 
		
		content.addComponent(cbOwnCollection);
		cbOwnCollection.setEnabled(!documentsForCollectionCreation.isEmpty());
		
		roleBox = new ComboBox<RBACRole>("Role", 
				Lists.newArrayList(RBACRole.ASSISTANT, RBACRole.MAINTAINER));
		roleBox.setWidth("100%");
		roleBox.setItemCaptionGenerator(RBACRole::getRoleName);
		roleBox.setEmptySelectionAllowed(false);
		content.addComponent(roleBox);
		
		joinedUsersConsole = new ListSelect<>("Joined Users", joinedUsers);
		joinedUsersConsole.setWidth("100%");
		joinedUsersConsole.setCaption("Users");
		joinedUsersConsole.setReadOnly(true);
		joinedUsersConsole.setVisible(false);

		content.addComponent(joinedUsersConsole);
		
		HorizontalLayout buttonPanel = new HorizontalLayout();
		buttonPanel.setWidth("100%");
		buttonPanel.setMargin(new MarginInfo(true, false));
		
		btnInvite = new Button("Invite");
		btnStopInvite = new Button("Stop invitation");
		
		progressIndicator = new ProgressBar();
		progressIndicator.setIndeterminate(false);
		progressIndicator.setVisible(false);
		
		buttonPanel.addComponent(progressIndicator);
		buttonPanel.setExpandRatio(progressIndicator, 0.5f);
		
		buttonPanel.addComponent(btnInvite);
		buttonPanel.addComponent(btnStopInvite);
		buttonPanel.setComponentAlignment(btnInvite, Alignment.BOTTOM_RIGHT);
		buttonPanel.setComponentAlignment(btnStopInvite, Alignment.BOTTOM_RIGHT);
		buttonPanel.setExpandRatio(btnInvite, 0.5f);
		
		content.addComponent(buttonPanel);
		content.setComponentAlignment(buttonPanel, Alignment.BOTTOM_RIGHT);
		
		setContent(content);
	}
	
	private void handleInvitePressed(ClickEvent clickEvent) {
		if (roleBox.getValue() == null) {
			Notification.show(
				"Info", 
				"Please select a project role for the joining users!",
				Type.HUMANIZED_MESSAGE);
			return;
		}
		
		setInvitationSettingsEnabled(false);
		progressIndicator.setIndeterminate(true);
		progressIndicator.setVisible(true);
		progressIndicator.setCaption("Invitation running, keep this dialog window open...");
		
		projectInvitation = new ProjectInvitation(
				project.getId(),
				roleBox.getValue().getAccessLevel(), 
				project.getName(), 
				project.getDescription(),
				cbOwnCollection.getValue());
		
		invitationCache.put(projectInvitation.getKey(), new Gson().toJson(projectInvitation));
		
		lInvitationCode.setValue(
			"Your invitation code: <b style=\"font-size: xx-large;\">" + projectInvitation.getKey() + "</b>");

		lInvitationCode.setVisible(true);
		btnInvite.setEnabled(false);
		
	    UUID regid = invitationTopic.addMessageListener(
	    	new ProjectInvitationHandler(
	    		UI.getCurrent(),
	    		projectInvitation,
	    		project,
	    		joinedTopic,
	    		joinedUsers,
	    		joinedUsersConsole
	    ));      
	    
		addCloseListener((evt) -> invitationTopic.removeMessageListener(regid));
	}
	
	public void show(){
		UI.getCurrent().addWindow(this);
	}


	@Override
	public void close() {
		super.close();
		eventBus.post(new MembersChangedEvent());
	}
}
