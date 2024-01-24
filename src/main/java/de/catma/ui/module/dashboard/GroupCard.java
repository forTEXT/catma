package de.catma.ui.module.dashboard;

import com.google.common.eventbus.EventBus;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.renderers.HtmlRenderer;
import de.catma.project.ProjectReference;
import de.catma.project.ProjectsManager;
import de.catma.rbac.IRBACManager;
import de.catma.rbac.RBACConstraint;
import de.catma.rbac.RBACConstraintEnforcer;
import de.catma.rbac.RBACPermission;
import de.catma.rbac.RBACRole;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerRestricted;
import de.catma.ui.component.IconButton;
import de.catma.ui.component.actiongrid.ActionGridComponent;
import de.catma.ui.events.GroupsChangedEvent;
import de.catma.ui.events.ProjectsChangedEvent;
import de.catma.ui.layout.FlexLayout;
import de.catma.ui.layout.HorizontalFlexLayout;
import de.catma.ui.layout.VerticalFlexLayout;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.user.Group;
import de.catma.user.Member;
import de.catma.user.User;
import org.vaadin.dialogs.ConfirmDialog;

import java.io.IOException;

public class GroupCard extends VerticalFlexLayout {
	
	private final Group group;
	private final ProjectsManager projectsManager;
	private final EventBus eventBus;
	
	private final ErrorHandler errorHandler;
	
	private final IRBACManager rbacManager;
    private final RBACConstraintEnforcer<RBACRole> rbacEnforcer = new RBACConstraintEnforcer<>();
    
    private Grid<Member> memberGrid;
    private ActionGridComponent<Grid<Member>> memberGridComponent;

    private Grid<ProjectReference> sharedProjectsGrid;
    private ActionGridComponent<Grid<ProjectReference>> sharedProjectsGridComponent;


    public GroupCard(Group group, ProjectsManager projectsManager, EventBus eventBus, IRBACManager rbacManager) {
        this.group = group;
        this.projectsManager = projectsManager;
        this.eventBus = eventBus;
        this.rbacManager = rbacManager;
        this.errorHandler = (ErrorHandler)UI.getCurrent();
        initComponents();
        initData();
    }
    private void initData() {
		try {
			RBACRole projectRole = rbacManager.getRoleOnGroup(projectsManager.getUser(), group);
			rbacEnforcer.enforceConstraints(projectRole); // normally done in reload();
		} catch (IOException e) {
            errorHandler.showAndLogError(String.format("Can't fetch permissions for group \"%s\"", group.getName()), e);
		}

        ListDataProvider<Member> memberDataProvider = new ListDataProvider<>(group.getMembers());
        memberGrid.setDataProvider(memberDataProvider);

        ListDataProvider<ProjectReference> sharedProjectsDataProvider = new ListDataProvider<>(group.getSharedProjects());
        sharedProjectsGrid.setDataProvider(sharedProjectsDataProvider);

    }

    private void initComponents() {
        addStyleName("groupslist__card");

        HorizontalFlexLayout topPanel = new HorizontalFlexLayout();
        topPanel.addStyleName("groupslist__card__topPanel");
        addComponent(topPanel);

        memberGrid = new Grid<>();
//        memberGrid.setHeaderVisible(false);
        memberGrid.setWidth("402px");
        memberGrid.setHeightByRows(4);
        memberGrid.addColumn((user) -> VaadinIcons.USER.getHtml(), new HtmlRenderer());
        memberGrid.addColumn(User::getName)
//                .setWidth(300)
                .setCaption("Name")
                .setComparator((r1, r2) -> String.CASE_INSENSITIVE_ORDER.compare(r1.getName(), r2.getName()))
                .setDescriptionGenerator(User::preciseName).setExpandRatio(1);
//        memberGrid.addColumn(Member::getRole).setExpandRatio(1);

        memberGridComponent = new ActionGridComponent<>(
                new Label("Members"),
                memberGrid
        );

        topPanel.addComponent(memberGridComponent);


        sharedProjectsGrid = new Grid<>();
        sharedProjectsGrid.setWidth("422px");
        sharedProjectsGrid.setHeightByRows(4);

        sharedProjectsGrid.addColumn(ProjectReference::getName)
//                .setWidth(420)
                .setCaption("Title")
                .setComparator((r1, r2) -> String.CASE_INSENSITIVE_ORDER.compare(r1.getName(), r2.getName()))
                .setExpandRatio(1);

        sharedProjectsGridComponent = new ActionGridComponent<>(
                new Label("Shared Projects"),
                sharedProjectsGrid
        );

        topPanel.addComponent(sharedProjectsGridComponent);

        HorizontalFlexLayout titleAndActionsLayout = new HorizontalFlexLayout();
        addComponent(titleAndActionsLayout);
        titleAndActionsLayout.addStyleName("groupslist__card__title-and-actions");
        titleAndActionsLayout.setAlignItems(FlexLayout.AlignItems.BASELINE);
        titleAndActionsLayout.setWidth("100%");

        Label nameLabel = new Label(group.getName());
        nameLabel.setWidth("100%");

        titleAndActionsLayout.addComponent(nameLabel);

        IconButton btnRemove = new IconButton(VaadinIcons.TRASH);
        btnRemove.addClickListener(
                clickEvent -> ConfirmDialog.show(
                        UI.getCurrent(),
                        "Delete Project",
                        String.format("Do you want to delete the group \"%s\" and remove it from all shared projects?", group.getName()),
                        "OK",
                        "Cancel",
                        confirmDialog -> {
                            try {
                                if (confirmDialog.isConfirmed()) {
                                    projectsManager.deleteGroup(group);
                                    eventBus.post(new GroupsChangedEvent(group.getId()));
                                }
                            }
                            catch (IOException e) {
                                errorHandler.showAndLogError(String.format("Failed to delete group \"%s\"", group.getName()), e);
                            }
                        }
                )
        );
        titleAndActionsLayout.addComponents(btnRemove);

        IconButton btnEdit = new IconButton(VaadinIcons.PENCIL);
//        btnEdit.addClickListener(
//                clickEvent -> new EditProjectDialog(
//                        projectReference,
//                        projectManager,
//                        result -> {
//                            try {
//                                projectManager.updateProjectMetadata(result);
//                                nameLabel.setValue(result.getName());
//                                descriptionLabel.setValue(result.getDescription());
//                            }
//                            catch (IOException e) {
//                                errorLogger.showAndLogError(String.format("Failed to update project \"%s\"", projectReference.getName()), e);
//                            }
//                        }
//                ).show()
//        );
        titleAndActionsLayout.addComponent(btnEdit);

        IconButton btnLeave = new IconButton(VaadinIcons.EXIT);
//        btnLeave.addClickListener(
//                clickEvent -> ConfirmDialog.show(
//                        UI.getCurrent(),
//                        "Leave Project",
//                        "Do you want to leave the project \"" + projectReference.getName() + "\"?",
//                        "OK",
//                        "Cancel",
//                        confirmDialog -> {
//                            try {
//                                if (confirmDialog.isConfirmed()) {
//                                    projectManager.leaveProject(projectReference);
//                                    eventBus.post(new ProjectsChangedEvent());
//                                }
//                            }
//                            catch (IOException e) {
//                                errorLogger.showAndLogError(String.format("Failed to leave project \"%s\"", projectReference.getName()), e);
//                            }
//                        }
//                )
//        );
        titleAndActionsLayout.addComponent(btnLeave);
        

		rbacEnforcer.register(
				RBACConstraint.ifNotAuthorized(
						role -> rbacManager.hasPermission(role, RBACPermission.GROUP_EDIT),
						() -> {
							btnEdit.setVisible(false);
							btnEdit.setEnabled(false);
						}
				)
		);

		rbacEnforcer.register(
				RBACConstraint.ifNotAuthorized(
						role -> rbacManager.hasPermission(role, RBACPermission.GROUP_DELETE),
						() -> {
							btnRemove.setVisible(false);
							btnRemove.setEnabled(false);
						}
				)
		);

		rbacEnforcer.register(
				RBACConstraint.ifNotAuthorized(
						role -> rbacManager.hasPermission(role, RBACPermission.GROUP_LEAVE)
								&& !rbacManager.hasPermission(role, RBACPermission.GROUP_DELETE), // the owner is the only one with 'delete' permission and the owner should cannot leave his group
						() -> {
							btnLeave.setVisible(false);
							btnLeave.setEnabled(false);
						}
				)
		);

    }
}
