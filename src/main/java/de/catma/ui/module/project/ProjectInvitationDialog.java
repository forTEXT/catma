package de.catma.ui.module.project;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.cache.Cache;
import javax.cache.Caching;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.jsoniter.output.JsonStream;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Label;
import com.vaadin.ui.ListSelect;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;
import com.vaadin.ui.renderers.HtmlRenderer;

import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.hazelcast.HazelCastService;
import de.catma.hazelcast.HazelcastConfiguration;
import de.catma.project.Project;
import de.catma.project.event.ChangeType;
import de.catma.project.event.CollectionChangeEvent;
import de.catma.rbac.RBACRole;
import de.catma.ui.UIMessageListener;
import de.catma.ui.events.InvitationRequestMessage;
import de.catma.ui.events.JoinedProjectMessage;
import de.catma.ui.events.ResourcesChangedEvent;
import de.catma.ui.layout.FlexLayout.JustifyContent;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.ui.layout.HorizontalFlexLayout;
import de.catma.ui.layout.VerticalFlexLayout;

/**
 * Dialog to invite other member to a project
 * @author db
 *
 */
public class ProjectInvitationDialog extends Window {

    private final Cache<Integer, String> invitationCache = 
    		Caching.getCachingProvider().getCacheManager().getCache(HazelcastConfiguration.CACHE_KEY_INVITATIONS);
    

	private final ComboBox<RBACRole> cb_role = new ComboBox<RBACRole>("role", 
			Lists.newArrayList(RBACRole.GUEST, RBACRole.REPORTER, RBACRole.ASSISTANT, RBACRole.MAINTAINER));
	private final CheckBox chbOwnCollection = new CheckBox("create own collection", false);
	private final Button btnInvite = new Button("Invite");
	private final Button btnStopInvite = new Button("Stop invitation");
	private final VerticalFlexLayout content = new VerticalFlexLayout();
	private final Label lInvitationCode = new Label("",ContentMode.HTML); 
	private final Grid<Resource> resourceGrid = new Grid<>();
	private final List<String> joinedUsers = new ArrayList<>();
	private final ListSelect<String> cbConsole = new ListSelect<>("joined users", joinedUsers);
    private final ErrorHandler errorLogger;
    private final HazelcastInstance hazelcast;
    private final ITopic<InvitationRequestMessage> invitationTopic;
    private final ITopic<JoinedProjectMessage> joinedTopic;
    private final EventBus eventBus;
    private final Set<Resource> resources;
    private final Map<String,Integer> customCollectionToUserMap = Maps.newHashMap();
    
	private ProjectInvitation projectInvitation;


	private final Project project;



	@Inject
	public ProjectInvitationDialog(
			@Assisted("project") Project project,
			@Assisted("resources") Set<Resource> resources,
			EventBus eventBus,
			HazelCastService hazelcastService){
		super("Invite to project");
		setDescription("Invite user to project");
		setModal(true);
		this.project = project;
		this.resources = resources;
	    this.errorLogger = (ErrorHandler) UI.getCurrent();
	    this.eventBus = eventBus;
	    this.hazelcast = hazelcastService.getHazelcastClient();
	    this.resourceGrid.setDataProvider(new ListDataProvider<>(resources));
	    invitationTopic = hazelcast.getTopic(HazelcastConfiguration.TOPIC_PROJECT_INVITATIONS);
	    joinedTopic = hazelcast.getTopic(HazelcastConfiguration.TOPIC_PROJECT_JOINED);
	    eventBus.register(this);
		initComponents();
	}

	private class ProjectInvitationHandler extends UIMessageListener<InvitationRequestMessage> {
		
		private Map<String, Resource> docLookup =  resources.stream().collect(Collectors.toMap(Resource::getResourceId, res -> res));
		
		@Override
		public void uiBlockingOnMessage(Message<InvitationRequestMessage> message) {
			UI.getCurrent().access( () -> {
				if(message.getMessageObject().getCode() == projectInvitation.getKey()){
					try {
						if(projectInvitation.isCreateOwnCollection()) {
							
							for(String resId : projectInvitation.getResources() ) {
								project.assignOnResource(() -> message.getMessageObject().getUserid(), RBACRole.REPORTER, resId);
								DocumentResource docResource = (DocumentResource) docLookup.get(resId);
								if(docResource != null){
									String collectionName = message.getMessageObject().getName() + "s collection";
									customCollectionToUserMap.put(collectionName, message.getMessageObject().getUserid());
									project.createUserMarkupCollection(collectionName, docResource.getDocument());
								}
							}	
						}
						
						project.assignOnProject(() -> message.getMessageObject().getUserid(), 
								RBACRole.forValue(projectInvitation.getDefaultRole()));
						joinedTopic.publish(new JoinedProjectMessage(projectInvitation));
						joinedUsers.add(message.getMessageObject().getName());
						cbConsole.setItems(joinedUsers);
						cbConsole.markAsDirty();
						cbConsole.setVisible(true);
						ProjectInvitationDialog.this.getUI().push();
					} catch (IOException e) {
						errorLogger.showAndLogError("Can't assign UserId " + message.getMessageObject().getName() + "to this project", e);
					}
				}
			}
			);
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
		
		chbOwnCollection.addValueChangeListener(event -> {
			if(event.getValue()){
				resourceGrid.setEnabled(true);
			}
			else {	
				resourceGrid.setEnabled(false);
			}
		});
		content.addComponent(chbOwnCollection);
		
        resourceGrid.addStyleName("project-view-document-grid");
        resourceGrid.setHeaderVisible(false);
        resourceGrid.setRowHeight(45);
        resourceGrid.setDescription("Document for default collection");
        
		resourceGrid
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
      
        resourceGrid
        	.addColumn(resource -> buildNameFunction.apply(resource), new HtmlRenderer())  	
        	.setCaption("Name")
        	.setWidthUndefined();
        resourceGrid.setEnabled(false);
        
        content.addComponent(resourceGrid);

		content.addComponent(cb_role);
		
		cbConsole.setWidth("100%");
		cbConsole.setCaption("Users");
		cbConsole.setReadOnly(true);
		cbConsole.setVisible(false);

		content.addComponent(cbConsole);
		
		HorizontalFlexLayout buttonPanel = new HorizontalFlexLayout();
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
		if(chbOwnCollection.getValue() && resourceGrid.getSelectedItems().isEmpty() ){
			Notification.show("No document selected", "Please select documents where own collections should be created", Notification.Type.WARNING_MESSAGE);
			return;
		}
		setDescription("Invitation running... keep this Dialog open");
		projectInvitation = new ProjectInvitation(
				project.getProjectId(), 
				cb_role.getValue(). 
				value, 
				project.getName(), 
				project.getDescription(),
				chbOwnCollection.getValue(),
				resourceGrid.getSelectedItems().stream().map(Resource::getResourceId).collect(Collectors.toSet()));
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
	
	@Subscribe
	public void handleCollectionChanged(CollectionChangeEvent collectionChangeEvent) {
		if (collectionChangeEvent.getChangeType().equals(ChangeType.CREATED)) {
    		AnnotationCollectionReference collectionReference = 
    				collectionChangeEvent.getCollectionReference();
    		
    		Integer userId = customCollectionToUserMap.get(collectionReference.getName());
    		
    		if(userId == null) {
    			errorLogger.showAndLogError("UserId empty for own collection",
    					new NullPointerException("UserId null for collection " + collectionReference.getName() ));
    			return;
    		}

    		try {
				project.assignOnResource(() -> userId, 
						RBACRole.ASSISTANT, collectionReference.getId());
			} catch (IOException e) {
    			errorLogger.showAndLogError("failed to assign permission on collection", e);
			}
    		
			Notification.show(
				"Info", 
				String.format("permission on own collection %1$s has been set", collectionReference.toString()),  
				Type.TRAY_NOTIFICATION);			
		}

	}

	@Override
	public void close() {
		super.close();
	    eventBus.unregister(this);
		eventBus.post(new ResourcesChangedEvent<>(this));
	}
}
