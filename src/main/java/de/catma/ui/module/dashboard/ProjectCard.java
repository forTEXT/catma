package de.catma.ui.module.dashboard;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Set;

import com.vaadin.ui.*;
import org.vaadin.dialogs.ConfirmDialog;

import com.google.common.eventbus.EventBus;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Notification.Type;

import de.catma.project.ProjectReference;
import de.catma.project.ProjectsManager;
import de.catma.rbac.RBACConstraint;
import de.catma.rbac.RBACConstraintEnforcer;
import de.catma.rbac.RBACPermission;
import de.catma.rbac.RBACRole;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerRestricted;
import de.catma.ui.component.IconButton;
import de.catma.ui.events.ProjectsChangedEvent;
import de.catma.ui.events.routing.RouteToProjectEvent;
import de.catma.ui.layout.HorizontalFlexLayout;
import de.catma.ui.layout.VerticalFlexLayout;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.user.Member;
import de.catma.user.SharedGroupMember;

/**
 * Displays a single project reference as a card
 *
 * @author db
 */
public class ProjectCard extends VerticalFlexLayout  {
	
	public interface ClickAction {
		public void projectCardClicked(ProjectReference projectReference);
	}

    private ProjectReference projectReference;

    private final ErrorHandler errorLogger;
    private final ProjectsManager projectManager;

	private final EventBus eventBus;

	private final RemoteGitManagerRestricted rbacManager;
	
	private final RBACConstraintEnforcer<RBACRole> rbacEnforcer = new RBACConstraintEnforcer<>();

	private final ClickAction clickAction;

	public ProjectCard(ProjectReference projectReference, ProjectsManager projectManager, EventBus eventBus, RemoteGitManagerRestricted rbacManager) {
		this(projectReference, projectManager, eventBus, rbacManager, ref -> eventBus.post(new RouteToProjectEvent(ref)));
	}

    private ProjectCard(
    		ProjectReference projectReference, ProjectsManager projectManager, 
    		EventBus eventBus, RemoteGitManagerRestricted rbacManager, 
    		ClickAction clickAction){
        this.projectReference = Objects.requireNonNull(projectReference) ;
        this.projectManager = projectManager;
        this.eventBus = eventBus;
        this.rbacManager = rbacManager;
        this.errorLogger = (ErrorHandler) UI.getCurrent();
        this.clickAction = clickAction;
        initComponents();
        initData();
    }



    private void initData() {
		try {
			RBACRole projectRole = rbacManager.getRoleOnProject(projectManager.getUser(), projectReference);
			rbacEnforcer.enforceConstraints(projectRole); // normally done in reload();
		} catch (IOException e) {
            errorLogger.showAndLogError(String.format("Can't fetch permissions for project \"%s\"", projectReference.getName()), e);
		}
	}

	protected void initComponents() {
		addStyleName("projectlist__card");

		HorizontalFlexLayout hlTitle = new HorizontalFlexLayout();
		hlTitle.addStyleName("projectlist__card__title");
		hlTitle.addLayoutClickListener(layoutClickEvent -> handleOpenProjectRequest());

		Label lblName = new Label(projectReference.getName());
		lblName.setWidth("100%");
		hlTitle.addComponent(lblName);

		addComponent(hlTitle);

		VerticalLayout vlContent = new VerticalLayout();
		vlContent.addStyleName("projectlist__card__content");
		vlContent.addLayoutClickListener(layoutClickEvent -> handleOpenProjectRequest());

		Label lblDescription = new Label(projectReference.getDescription());
		lblDescription.setWidth("100%");
		vlContent.addComponent(lblDescription);

		addComponent(vlContent);

		HorizontalFlexLayout hflMetadataAndActions = new HorizontalFlexLayout();
		hflMetadataAndActions.addStyleName("projectlist__card__metadata-and-actions");
		hflMetadataAndActions.setAlignItems(AlignItems.CENTER);
		hflMetadataAndActions.setWidth("100%");

		VerticalLayout vlMetadata = new VerticalLayout();
		vlMetadata.setWidth("100%");
		vlMetadata.setMargin(false);
		vlMetadata.setSpacing(false);
		vlMetadata.addStyleName("projectlist__card__dates");
		vlMetadata.addLayoutClickListener(layoutClickEvent -> handleOpenProjectRequest());

		Label lblCreatedDate = new Label(
				String.format(
						"<span>Created on:</span> %s",
						projectReference.getCreatedAt() == null ? "n/a" : projectReference.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE)
				),
				ContentMode.HTML
		);
		vlMetadata.addComponent(lblCreatedDate);

		Label lblLastActivityDate = new Label(
				String.format(
						"<span>Last activity on:</span> %s",
						projectReference.getLastActivityAt() == null ? "n/a" : projectReference.getLastActivityAt().format(DateTimeFormatter.ISO_LOCAL_DATE)
				),
				ContentMode.HTML
		);
		vlMetadata.addComponent(lblLastActivityDate);

		hflMetadataAndActions.addComponent(vlMetadata);

		IconButton btnRemove = new IconButton(VaadinIcons.TRASH);
		btnRemove.addClickListener(
				clickEvent -> ConfirmDialog.show(
						UI.getCurrent(),
						"Delete Project",
						String.format("Do you want to delete the whole project \"%s\"?", projectReference.getName()),
						"OK",
						"Cancel",
						confirmDialog -> {
							try {
								if (confirmDialog.isConfirmed()) {
									projectManager.deleteProject(projectReference);
									eventBus.post(new ProjectsChangedEvent(projectReference.getProjectId()));
								}
							}
							catch (IOException e) {
								errorLogger.showAndLogError(String.format("Failed to delete project \"%s\"", projectReference.getName()), e);
							}
						}
				)
		);
		hflMetadataAndActions.addComponents(btnRemove);

		IconButton btnEdit = new IconButton(VaadinIcons.PENCIL);
		btnEdit.addClickListener(
				clickEvent -> new EditProjectDialog(
						projectReference,
						projectManager,
						result -> {
							try {
								projectManager.updateProjectMetadata(result);
								lblName.setValue(result.getName());
								lblDescription.setValue(result.getDescription());
							}
							catch (IOException e) {
								errorLogger.showAndLogError(String.format("Failed to update project \"%s\"", projectReference.getName()), e);
							}
						}
				).show()
		);
		hflMetadataAndActions.addComponent(btnEdit);

		IconButton btnLeave = new IconButton(VaadinIcons.EXIT);
		btnLeave.addClickListener(
				clickEvent -> {
					
					try {
						Set<Member> members = rbacManager.getProjectMembers(projectReference);
						Member self = members.stream().filter(m -> m.getUserId().equals(rbacManager.getUser().getUserId())).findAny().orElse(null);
						if (self != null) {
							if (self instanceof SharedGroupMember) {
								Notification.show(
										"Info", 
										String.format(
												"You are participating in this project because you are part of the user group '%s'.\n"
												+ "You cannot leave the project directly, you can only leave the user group.\n"
												+ "Leaving the user group will disconnect you from all projects shared with this group.", 
												((SharedGroupMember)self).getSharedGroup().name()), 
										Type.HUMANIZED_MESSAGE);
								return;
							}
						}
					} catch (IOException e) {
						errorLogger.showAndLogError(String.format("Could not load members for project '%s'", projectReference.getName()), e);
					}
					
					ConfirmDialog.show(
				
						UI.getCurrent(),
						"Leave Project",
						String.format("Do you want to leave the project '%s'?", projectReference.getName()),
						"OK",
						"Cancel",
						confirmDialog -> {
							try {
								if (confirmDialog.isConfirmed()) {
									projectManager.leaveProject(projectReference);
									eventBus.post(new ProjectsChangedEvent());
								}
							}
							catch (IOException e) {
								errorLogger.showAndLogError(String.format("Failed to leave project '%s'", projectReference.getName()), e);
							}
						}
					);
				}
		);
		hflMetadataAndActions.addComponent(btnLeave);

		addComponents(hflMetadataAndActions);

		rbacEnforcer.register(
				RBACConstraint.ifNotAuthorized(
						role -> rbacManager.hasPermission(role, RBACPermission.PROJECT_EDIT),
						() -> {
							btnEdit.setVisible(false);
							btnEdit.setEnabled(false);
						}
				)
		);

		rbacEnforcer.register(
				RBACConstraint.ifNotAuthorized(
						role -> rbacManager.hasPermission(role, RBACPermission.PROJECT_DELETE),
						() -> {
							btnRemove.setVisible(false);
							btnRemove.setEnabled(false);
						}
				)
		);

		rbacEnforcer.register(
				RBACConstraint.ifNotAuthorized(
						role -> rbacManager.hasPermission(role, RBACPermission.PROJECT_LEAVE)
								&& !rbacManager.hasPermission(role, RBACPermission.PROJECT_DELETE), // only owners can delete projects and owners should not be able to leave
						() -> {
							btnLeave.setVisible(false);
							btnLeave.setEnabled(false);
						}
				)
		);
	}

    private void handleOpenProjectRequest() {
    	clickAction.projectCardClicked(this.projectReference);
    }



	public String toString() {
    	return projectReference.getProjectId() + " " + projectReference.getName() + " "+ projectReference.getDescription();
    }
}
