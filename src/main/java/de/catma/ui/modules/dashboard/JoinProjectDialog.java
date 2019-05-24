package de.catma.ui.modules.dashboard;

import javax.cache.Cache;
import javax.cache.Caching;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import com.jsoniter.JsonIterator;
import com.vaadin.data.HasValue.ValueChangeEvent;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

import de.catma.DammAlgorithm;
import de.catma.hazelcast.HazelCastService;
import de.catma.hazelcast.HazelcastConfiguration;
import de.catma.rbac.RBACRole;
import de.catma.ui.events.InvitationRequestMessage;
import de.catma.ui.events.JoinedProjectMessage;
import de.catma.ui.events.ResourcesChangedEvent;
import de.catma.ui.layout.FlexLayout.JustifyContent;
import de.catma.ui.layout.HorizontalLayout;
import de.catma.ui.layout.VerticalLayout;
import de.catma.ui.modules.project.ProjectInvitation;
import de.catma.user.User;

/**
 * Dialog that creates a Project
 * 
 * @author db
 *
 */
public class JoinProjectDialog extends Window {

	private final TextField tfCode = new TextField("Code");
	private final TextField tfName = new TextField("Name");
	private final TextArea taDescription = new TextArea("Decription");
	private final ComboBox<RBACRole> cbRole = new ComboBox<RBACRole>("role", 
			Lists.newArrayList(RBACRole.values()));
    private final Cache<Integer, String> invitationCache = 
    		Caching.getCachingProvider().getCacheManager().getCache(HazelcastConfiguration.CACHE_KEY_INVITATIONS);
    private final VerticalLayout content = new VerticalLayout();
    private final User currentUser;
    
    private final Button btnJoin = new Button("Join");
    private final Button btnCancel = new Button("Cancel");
    private final EventBus eventBus;

    private final ITopic<InvitationRequestMessage> invitationTopic;
    private final ITopic<JoinedProjectMessage> joinedTopic;

	private ProjectInvitation invitation;
	
	@Inject
	public JoinProjectDialog(User currentUser, EventBus eventBus, HazelCastService hazelcastService) {
		super("Join project");
		this.currentUser = currentUser;
		this.eventBus = eventBus;
	    invitationTopic = hazelcastService.getHazelcastClient().getTopic(HazelcastConfiguration.TOPIC_PROJECT_INVITATIONS);
	    joinedTopic = hazelcastService.getHazelcastClient().getTopic(HazelcastConfiguration.TOPIC_PROJECT_JOINED);
		initComponents();
	}
	
	private class ProjectJoinHandler implements MessageListener<JoinedProjectMessage>{
		
		@Override
		public void onMessage(Message<JoinedProjectMessage> message) {
			if(message.getMessageObject().getInvitation().getKey() == JoinProjectDialog.this.invitation.getKey()) {	
				JoinProjectDialog.this.eventBus.post(new ResourcesChangedEvent<Component>(null));
				JoinProjectDialog.this.getUI().access(() -> 
				Notification.show("Joined successfully", "sucessfully join project " + JoinProjectDialog.this.invitation.getName() , Type.HUMANIZED_MESSAGE)
						);
				JoinProjectDialog.this.getUI().push();
				JoinProjectDialog.this.close();
			}
		}
		
	}
	
	private void initComponents() {		
		content.addStyleName("spacing");
		content.addStyleName("margin");

		Label lDescription = new Label("Please enter your invitation code to find and join a project");
		content.addComponent(lDescription);
		
		tfCode.setWidth("100%");
		tfCode.setCaption("Invitation code");
		tfCode.setDescription("Enter your invitation code here");
		tfCode.addValueChangeListener(this::onCodeEntered);
		content.addComponent(tfCode);
		
		tfName.setWidth("100%");
		tfName.setCaption("");
		tfName.setReadOnly(true);
		tfName.setVisible(false);
		content.addComponent(tfName);
		
		cbRole.setWidth("100%");
		cbRole.setItemCaptionGenerator(RBACRole::getRolename);
		cbRole.setEmptySelectionAllowed(false);
		cbRole.setReadOnly(true);
		cbRole.setVisible(false);
		content.addComponent(cbRole);
		
		taDescription.setWidth("100%");
		taDescription.setHeight("100%");
		taDescription.setVisible(false);
		
		content.addComponent(taDescription);
		
		HorizontalLayout buttonPanel = new HorizontalLayout();
		buttonPanel.addStyleName("spacing-left-right");
		buttonPanel.setJustifyContent(JustifyContent.FLEX_END);
		
		btnJoin.addClickListener(this::handleJoinPressed);
		btnJoin.setEnabled(false);
		
		btnCancel.addClickListener(evt -> close());

		buttonPanel.addComponent(btnJoin);
		buttonPanel.addComponent(btnCancel);
		
		content.addComponent(buttonPanel);
		setContent(content);

	}

	private void handleJoinPressed(ClickEvent event) {
		if(invitation != null) {
		    String regid = joinedTopic.addMessageListener(new ProjectJoinHandler());      
			addCloseListener((evt) -> joinedTopic.removeMessageListener(regid));
			invitationTopic.publish(new InvitationRequestMessage(currentUser.getUserId(), currentUser.getIdentifier(), invitation.getKey()));
		}
	}
	
	private void onCodeEntered(ValueChangeEvent<String> changeEvent){
		try { 
			Integer code = Integer.parseInt(changeEvent.getValue());
			if(DammAlgorithm.validate(code)){
				String marshalledInvitation = invitationCache.get(code);
				if(marshalledInvitation != null){
					invitation = JsonIterator.deserialize(marshalledInvitation, ProjectInvitation.class);
					
					tfCode.setReadOnly(true);
					tfName.setValue(invitation.getName());
					tfName.setVisible(true);
					taDescription.setValue(invitation.getDescription() == null ? "nonexistent description": invitation.getDescription() );
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
	
	@Override
	public void close() {
		super.close();
	}

}
