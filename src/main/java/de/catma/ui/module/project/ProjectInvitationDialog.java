package de.catma.ui.module.project;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.cache.Cache;
import javax.cache.Caching;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.jsoniter.output.JsonStream;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.ListSelect;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.renderers.HtmlRenderer;

import de.catma.hazelcast.HazelCastService;
import de.catma.hazelcast.HazelcastConfiguration;
import de.catma.project.Project;
import de.catma.rbac.RBACRole;
import de.catma.ui.UIMessageListener;
import de.catma.ui.events.InvitationRequestMessage;
import de.catma.ui.events.JoinedProjectMessage;
import de.catma.ui.events.MembersChangedEvent;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.util.ColorConverter;

/**
 * Dialog to invite other member to a project
 * @author db
 *
 */
public class ProjectInvitationDialog extends Window {
	
	private Label lInvitationCode; 
	private ComboBox<RBACRole> roleBox;
	private CheckBox cbOwnCollection;
	private Grid<DocumentResource> documentGrid;
	private ListSelect<String> joinedUsersConsole;
	private Button btnInvite;
	private Button btnStopInvite;
	private ProgressBar progressIndicator;
	
    private final Cache<Integer, String> invitationCache = 
    		Caching.getCachingProvider().getCacheManager().getCache(
    				HazelcastConfiguration.CacheKeyName.PROJECT_INVITATION.name());
    
	private final List<String> joinedUsers = new ArrayList<>();
	
    private final ErrorHandler errorLogger;
    private final HazelcastInstance hazelcast;
    private final ITopic<InvitationRequestMessage> invitationTopic;
    private final ITopic<JoinedProjectMessage> joinedTopic;
    private final EventBus eventBus;
    
    private final Project project;

    private final List<DocumentResource> documentsForCollectionCreation;
    
	private ProjectInvitation projectInvitation;

	@Inject
	public ProjectInvitationDialog(
			@Assisted("project") Project project,
			@Assisted("resources") List<DocumentResource> documentsForCollectionCreation,
			EventBus eventBus,
			HazelCastService hazelcastService) {
		
		super("Invite others to the Project");
		
		this.project = project;
		this.documentsForCollectionCreation = documentsForCollectionCreation;
	    this.errorLogger = (ErrorHandler) UI.getCurrent();
	    this.eventBus = eventBus;
	    this.hazelcast = hazelcastService.getHazelcastClient();
	    
	    this.invitationTopic = hazelcast.getTopic(HazelcastConfiguration.TopicName.PROJECT_INVITATION.name());
	    this.joinedTopic = hazelcast.getTopic(HazelcastConfiguration.TopicName.PROJECT_JOINED.name());
	    
		initComponents();
		initActions();
	}

	private void initActions() {
		cbOwnCollection.addValueChangeListener(event -> documentGrid.setEnabled(event.getValue()));
		btnInvite.addClickListener(this::handleInvitePressed);
		btnStopInvite.addClickListener(evt -> close());
	}

	private class ProjectInvitationHandler extends UIMessageListener<InvitationRequestMessage> {
		
		private final Map<String, DocumentResource> documentResourceByUuid =  
			documentsForCollectionCreation.stream().collect(
					Collectors.toMap(Resource::getResourceId, res -> res));
		
		private final Map<Integer, String> userId2Color = new HashMap<Integer, String>();
		private final Set<String> colors = ColorConverter.getColorNames();
		
		public ProjectInvitationHandler(UI ui) {
			super(ui);
		}

		@Override
		public void uiOnMessage(Message<InvitationRequestMessage> message) {
			if (message.getMessageObject().getCode() == projectInvitation.getKey()) {
				try {
					project.assignOnProject(
							() -> message.getMessageObject().getUserId(), 
							RBACRole.forValue(projectInvitation.getDefaultRole()));

					if (projectInvitation.isCreateOwnCollection()) {
						String color = userId2Color.get(message.getMessageObject().getUserId());
						
						if (color == null && !colors.isEmpty()) {
							color = colors.iterator().next();
							colors.remove(color);
							userId2Color.put(message.getMessageObject().getUserId(), color);
						}
						
						for (String documentId : projectInvitation.getDocumentIds()) {
							if (projectInvitation.getDefaultRole() < RBACRole.REPORTER.getAccessLevel()) {
								// minimum role 
								project.assignOnResource(
										() -> message.getMessageObject().getUserId(), 
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
											message.getMessageObject().getUserId():null,
									RBACRole.ASSISTANT);
							}
							
						}	
					}
					
					joinedTopic.publish(new JoinedProjectMessage(projectInvitation));
					
					joinedUsers.add(message.getMessageObject().getName());
					joinedUsersConsole.getDataProvider().refreshAll();
					joinedUsersConsole.setVisible(true);
					
					ProjectInvitationDialog.this.getUI().push();
					
				} catch (IOException e) {
					errorLogger.showAndLogError(
							"Can't assign User " 
									+ message.getMessageObject().getName() 
									+ " to this Project", e);
				}
			}
		}
	}
	
	private void initComponents() {
		setModal(true);
		setWidth("60%");
		setHeight("90%");
		
		VerticalLayout content = new VerticalLayout();
		content.setSizeFull();
		
		lInvitationCode = new Label("",ContentMode.HTML); 
		lInvitationCode.setVisible(false);
		
		content.addComponent(lInvitationCode);

		cbOwnCollection = new CheckBox("Create one collection per Document and joined User", false);
		cbOwnCollection.setValue(false);
		
		content.addComponent(cbOwnCollection);
		cbOwnCollection.setEnabled(!documentsForCollectionCreation.isEmpty());
		
		documentGrid = new Grid<>();
		documentGrid.setSizeFull();
		documentGrid.setRowHeight(45);
        documentGrid.setCaption("Documents");
	    documentGrid.setDataProvider(new ListDataProvider<>(documentsForCollectionCreation));
	    documentGrid.setSelectionMode(SelectionMode.MULTI);
	    
		documentGrid
			.addColumn(resource -> resource.getIcon(), new HtmlRenderer())
			.setWidth(100);
        
		Function<Resource,String> buildNameFunction = (resource) -> {
			StringBuilder sb = new StringBuilder()
			  .append("<div class='documentsgrid__doc'> ")
		      .append("<div class='documentsgrid__doc__title'> ")
		      .append(resource.getName())
		      .append("</div>");
			if(resource.hasDetail()){
		        sb
		        .append("<span class='documentsgrid__doc__author'> ")
		        .append(resource.getDetail())
		        .append("</span>");
			}
			sb.append("</div>");
				        
		    return sb.toString();
		};
      
        documentGrid
        	.addColumn(resource -> buildNameFunction.apply(resource), new HtmlRenderer())  	
        	.setCaption("Name");
//        	.setExpandRatio(1);
        
        documentGrid.setEnabled(false);
        
        content.addComponent(documentGrid);
        content.setExpandRatio(documentGrid, 1f);
        

		roleBox = new ComboBox<RBACRole>("Role", 
				Lists.newArrayList(RBACRole.GUEST, RBACRole.REPORTER, RBACRole.ASSISTANT, RBACRole.MAINTAINER));
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
				"Please select a Project role for the joining users!", 
				Type.HUMANIZED_MESSAGE);
			return;
		}
		
		if(cbOwnCollection.getValue() && documentGrid.getSelectedItems().isEmpty() ){
			Notification.show(
					"Info", 
					"Please select at least one Document to create a Collection for each joining user!", 
					Notification.Type.HUMANIZED_MESSAGE);
			return;
		}
		
		progressIndicator.setIndeterminate(true);
		progressIndicator.setVisible(true);
		progressIndicator.setCaption("Invitation running, keep this dialog window open...");
		
		projectInvitation = new ProjectInvitation(
				project.getProjectId(), 
				roleBox.getValue().getAccessLevel(), 
				project.getName(), 
				project.getDescription(),
				cbOwnCollection.getValue(),
				documentGrid.getSelectedItems().stream().map(Resource::getResourceId).collect(Collectors.toSet()));
		
		invitationCache.put(projectInvitation.getKey(), JsonStream.serialize(projectInvitation));
		
		lInvitationCode.setValue(
			"Your invitation code: <b style=\"font-size: xx-large;\">" + projectInvitation.getKey() + "</b>");

		lInvitationCode.setVisible(true);
		btnInvite.setEnabled(false);
		
	    String regid = invitationTopic.addMessageListener(new ProjectInvitationHandler(UI.getCurrent()));      
	    
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
