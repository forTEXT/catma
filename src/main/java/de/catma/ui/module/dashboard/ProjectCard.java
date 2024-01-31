package de.catma.ui.module.dashboard;

import java.io.IOException;
import java.util.Objects;

import org.vaadin.dialogs.ConfirmDialog;

import com.google.common.eventbus.EventBus;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;

import de.catma.project.ProjectsManager;
import de.catma.project.ProjectReference;
import de.catma.rbac.IRBACManager;
import de.catma.rbac.RBACConstraint;
import de.catma.rbac.RBACConstraintEnforcer;
import de.catma.rbac.RBACPermission;
import de.catma.rbac.RBACRole;
import de.catma.ui.component.IconButton;
import de.catma.ui.events.ProjectsChangedEvent;
import de.catma.ui.events.routing.RouteToProjectEvent;
import de.catma.ui.layout.FlexLayout;
import de.catma.ui.layout.HorizontalFlexLayout;
import de.catma.ui.layout.VerticalFlexLayout;
import de.catma.ui.module.main.ErrorHandler;

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

	private final IRBACManager rbacManager;
	
	private final RBACConstraintEnforcer<RBACRole> rbacEnforcer = new RBACConstraintEnforcer<>();

	private Label descriptionLabel;
	private Label nameLabel;

	private final ClickAction clickAction;

	public ProjectCard(ProjectReference projectReference, ProjectsManager projectManager, EventBus eventBus, IRBACManager rbacManager) {
		this(projectReference, projectManager, eventBus, rbacManager, ref -> eventBus.post(new RouteToProjectEvent(ref)));
	}

	public ProjectCard(ProjectReference projectReference, ProjectsManager projectManager, 
    		EventBus eventBus, ClickAction clickAction) {
		this(projectReference, projectManager, eventBus, new NoopRBACManager(), 
				ref -> clickAction.projectCardClicked(ref));
	}

    private ProjectCard(
    		ProjectReference projectReference, ProjectsManager projectManager, 
    		EventBus eventBus, IRBACManager rbacManager, 
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

		CssLayout previewLayout = new CssLayout();
		previewLayout.addStyleName("projectlist__card__preview");
		previewLayout.addLayoutClickListener(layoutClickEvent -> handleOpenProjectRequest());

		descriptionLabel = new Label(projectReference.getDescription());
		descriptionLabel.setWidth("100%");
		previewLayout.addComponents(descriptionLabel);

		addComponent(previewLayout);

		HorizontalFlexLayout titleAndActionsLayout = new HorizontalFlexLayout();
		titleAndActionsLayout.addStyleName("projectlist__card__title-and-actions");
		titleAndActionsLayout.setAlignItems(FlexLayout.AlignItems.BASELINE);
		titleAndActionsLayout.setWidth("100%");

		nameLabel = new Label(projectReference.getName());
		nameLabel.setWidth("100%");

		titleAndActionsLayout.addComponent(nameLabel);

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
		titleAndActionsLayout.addComponents(btnRemove);

		IconButton btnEdit = new IconButton(VaadinIcons.PENCIL);
		btnEdit.addClickListener(
				clickEvent -> new EditProjectDialog(
						projectReference,
						projectManager,
						result -> {
							try {
								projectManager.updateProjectMetadata(result);
								nameLabel.setValue(result.getName());
								descriptionLabel.setValue(result.getDescription());
							}
							catch (IOException e) {
								errorLogger.showAndLogError(String.format("Failed to update project \"%s\"", projectReference.getName()), e);
							}
						}
				).show()
		);
		titleAndActionsLayout.addComponent(btnEdit);

		IconButton btnLeave = new IconButton(VaadinIcons.EXIT);
		btnLeave.addClickListener(
				clickEvent -> ConfirmDialog.show(
						UI.getCurrent(),
						"Leave Project",
						"Do you want to leave the project \"" + projectReference.getName() + "\"?",
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
								errorLogger.showAndLogError(String.format("Failed to leave project \"%s\"", projectReference.getName()), e);
							}
						}
				)
		);
		titleAndActionsLayout.addComponent(btnLeave);

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
								&& !rbacManager.hasPermission(role, RBACPermission.PROJECT_DELETE), // TODO: why do we care about the delete permission here?
						() -> {
							btnLeave.setVisible(false);
							btnLeave.setEnabled(false);
						}
				)
		);

		addComponents(titleAndActionsLayout);
	}

    private void handleOpenProjectRequest() {
    	clickAction.projectCardClicked(this.projectReference);
    }



	public String toString() {
    	return projectReference.getProjectId() + " " + projectReference.getName() + " "+ projectReference.getDescription();
    }
}
