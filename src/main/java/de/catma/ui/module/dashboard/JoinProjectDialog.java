package de.catma.ui.module.dashboard;

import javax.cache.Cache;
import javax.cache.Caching;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.jsoniter.JsonIterator;
import com.vaadin.data.HasValue.ValueChangeEvent;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import de.catma.hazelcast.HazelCastService;
import de.catma.hazelcast.HazelcastConfiguration;
import de.catma.rbac.RBACRole;
import de.catma.ui.UIMessageListener;
import de.catma.ui.events.InvitationRequestMessage;
import de.catma.ui.events.JoinedProjectMessage;
import de.catma.ui.events.ResourcesChangedEvent;
import de.catma.ui.module.project.ProjectInvitation;
import de.catma.user.User;
import de.catma.util.DammAlgorithm;

/**
 * Dialog that creates a Project
 * 
 * @author db
 *
 */
public class JoinProjectDialog extends Window {

	private TextField tfCode;
	private TextField tfName;
	private ComboBox<RBACRole> cbRole;
	private TextArea taDescription;
	
	
	private Button btnJoin;
	private Button btnCancel;
    
	private final Cache<Integer, String> invitationCache = 
    		Caching.getCachingProvider().getCacheManager().getCache(
    				HazelcastConfiguration.CacheKeyName.PROJECT_INVITATION.name());
    
    private final User currentUser;
    
    private final EventBus eventBus;

    private final ITopic<InvitationRequestMessage> invitationTopic;
    private final ITopic<JoinedProjectMessage> joinedTopic;

	private ProjectInvitation invitation;
	
	@Inject
	public JoinProjectDialog(User currentUser, EventBus eventBus, HazelCastService hazelcastService) {
		super("Join project");
		this.currentUser = currentUser;
		this.eventBus = eventBus;
	    this.invitationTopic = 
	    		hazelcastService.getHazelcastClient().getTopic(
	    				HazelcastConfiguration.TopicName.PROJECT_INVITATION.name());
	    this.joinedTopic = 
	    		hazelcastService.getHazelcastClient().getTopic(
	    				HazelcastConfiguration.TopicName.PROJECT_JOINED.name());
		initComponents();
		initActions();
	}
	
	private void initActions() {
		tfCode.addValueChangeListener(this::onCodeEntered);
		btnJoin.addClickListener(this::handleJoinPressed);
		btnCancel.addClickListener(evt -> close());
	}

	private class ProjectJoinHandler extends UIMessageListener<JoinedProjectMessage>{
		
		public ProjectJoinHandler(UI ui) {
			super(ui);
		}

		@Override
		public void uiOnMessage(Message<JoinedProjectMessage> message) {
			if(message.getMessageObject().getInvitation().getKey() == JoinProjectDialog.this.invitation.getKey()) {	
				JoinProjectDialog.this.eventBus.post(new ResourcesChangedEvent());
				Notification.show("Joined successfully", "Sucessfully joined Project " + 
						JoinProjectDialog.this.invitation.getName() , Type.HUMANIZED_MESSAGE);
				JoinProjectDialog.this.getUI().push();
				JoinProjectDialog.this.close();
			}
		}
		
	}
	
	private void initComponents() {	
		setWidth("30%");
		setHeight("90%");
		center();
		
		VerticalLayout content = new VerticalLayout();
		content.setSizeFull();
		
		Label lDescription = new Label("Please enter your invitation code to find and join a Project");
		content.addComponent(lDescription);

		tfCode = new TextField("Code");
		tfCode.setWidth("100%");
		tfCode.setDescription("Enter your invitation code here");
		content.addComponent(tfCode);
		content.setExpandRatio(tfCode, 0.3f);
		
		tfName = new TextField("Name");
		tfName.setWidth("100%");
		tfName.setReadOnly(true);
		tfName.setVisible(false);
		content.addComponent(tfName);
		
		cbRole = new ComboBox<RBACRole>("Role", 
				Lists.newArrayList(RBACRole.values()));
		cbRole.setWidth("100%");
		cbRole.setItemCaptionGenerator(RBACRole::getRoleName);
		cbRole.setEmptySelectionAllowed(false);
		cbRole.setReadOnly(true);
		cbRole.setVisible(false);
		content.addComponent(cbRole);
		
		taDescription = new TextArea("Description");
		taDescription.setWidth("100%");
		taDescription.setHeight("100%");
		taDescription.setVisible(false);
		
		content.addComponent(taDescription);
		content.setExpandRatio(taDescription, 1f);
		
		HorizontalLayout buttonPanel = new HorizontalLayout();
		buttonPanel.setWidth("100%");
		
		btnJoin = new Button("Join");
	    btnCancel = new Button("Cancel");
	    
		btnJoin.setEnabled(false);

		buttonPanel.addComponent(btnJoin);
		buttonPanel.addComponent(btnCancel);
		buttonPanel.setExpandRatio(btnJoin, 1f);
		buttonPanel.setComponentAlignment(btnJoin, Alignment.BOTTOM_RIGHT);
		buttonPanel.setComponentAlignment(btnCancel, Alignment.BOTTOM_RIGHT);

		content.addComponent(buttonPanel);
		setContent(content);

	}

	private void handleJoinPressed(ClickEvent event) {
		if(invitation != null) {
		    String regid = joinedTopic.addMessageListener(new ProjectJoinHandler(UI.getCurrent()));      
			addCloseListener((evt) -> joinedTopic.removeMessageListener(regid));
			invitationTopic.publish(
				new InvitationRequestMessage(
						currentUser.getUserId(), 
						currentUser.getName(), 
						invitation.getKey()));
		}
	}
	
	private void onCodeEntered(ValueChangeEvent<String> changeEvent){
		try { 
			Integer code = Integer.parseInt(changeEvent.getValue());
			if(DammAlgorithm.validate(code)){
				String marshalledInvitation = invitationCache.get(code);
				if(marshalledInvitation != null) {
					invitation = JsonIterator.deserialize(marshalledInvitation, ProjectInvitation.class);
					
					tfCode.setReadOnly(true);
					tfName.setValue(invitation.getName());
					tfName.setVisible(true);
					taDescription.setValue(invitation.getDescription() == null? "": invitation.getDescription());
					taDescription.setVisible(true);
					cbRole.setValue(RBACRole.forValue(invitation.getDefaultRole()));
					cbRole.setVisible(true);
					btnJoin.setEnabled(true);
				}
			}
		} catch (NumberFormatException ne){
			//NOOP
		} 
	}
	
	public void show(){
		UI.getCurrent().addWindow(this);
	}
}
