package de.catma.ui.modules.project;

import javax.cache.Cache;
import javax.cache.Caching;

import com.google.common.collect.Lists;
import com.jsoniter.output.JsonStream;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

import de.catma.config.HazelcastConfiguration;
import de.catma.project.ProjectReference;
import de.catma.rbac.RBACRole;
import de.catma.ui.layout.FlexLayout.JustifyContent;
import de.catma.ui.layout.HorizontalLayout;
import de.catma.ui.layout.VerticalLayout;

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
	private ProjectInvitation projectInvitation;;

	public ProjectInvitationDialog(ProjectReference projectRef){
		super("Invite to project");
		setDescription("Invite user to project");
		setModal(true);
		this.projectRef = projectRef;
		initComponents();
	}

	private void initComponents() {
		content.addStyleName("spacing");
		content.addStyleName("margin");

		lInvitationCode.setCaption("Your Invitation code");
		lInvitationCode.setVisible(false);
		content.addComponent(lInvitationCode);

		cb_role.setWidth("100%");
		cb_role.setItemCaptionGenerator(RBACRole::name);
		cb_role.setEmptySelectionAllowed(false);
		
		content.addComponent(cb_role);
		
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
		projectInvitation = new ProjectInvitation(projectRef.getProjectId(), cb_role.getValue().value, projectRef.getName(), projectRef.getDescription());
		invitationCache.put(projectInvitation.getKey(), JsonStream.serialize(projectInvitation));
		lInvitationCode.setValue("<h1>" + projectInvitation.getKey() + "</h2>");
		lInvitationCode.setVisible(true);
		btnInvite.setEnabled(false);
		addCloseListener((evt) -> invitationCache.remove(projectInvitation.getKey()));
	}
	
	public void show(){
		UI.getCurrent().addWindow(this);
	}
	
}
