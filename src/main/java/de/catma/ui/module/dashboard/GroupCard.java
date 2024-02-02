package de.catma.ui.module.dashboard;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.mail.EmailException;
import org.vaadin.dialogs.ConfirmDialog;

import com.google.common.eventbus.EventBus;
import com.vaadin.contextmenu.ContextMenu;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.Page;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;
import com.vaadin.ui.renderers.HtmlRenderer;

import de.catma.project.ProjectReference;
import de.catma.project.ProjectsManager;
import de.catma.rbac.IRBACManager;
import de.catma.rbac.RBACConstraint;
import de.catma.rbac.RBACConstraintEnforcer;
import de.catma.rbac.RBACPermission;
import de.catma.rbac.RBACRole;
import de.catma.ui.component.IconButton;
import de.catma.ui.component.actiongrid.ActionGridComponent;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.dialog.SingleTextInputDialog;
import de.catma.ui.dialog.TextAreaInputDialog;
import de.catma.ui.events.GroupsChangedEvent;
import de.catma.ui.layout.FlexLayout;
import de.catma.ui.layout.HorizontalFlexLayout;
import de.catma.ui.layout.VerticalFlexLayout;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.ui.module.project.RemoveMemberDialog;
import de.catma.user.Group;
import de.catma.user.Member;
import de.catma.user.User;
import de.catma.user.signup.SignupTokenManager;

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
	private IconButton btnRemove;
	private IconButton btnEdit;
	private IconButton btnLeave;


    public GroupCard(Group group, ProjectsManager projectsManager, EventBus eventBus, IRBACManager rbacManager) {
        this.group = group;
        this.projectsManager = projectsManager;
        this.eventBus = eventBus;
        this.rbacManager = rbacManager;
        this.errorHandler = (ErrorHandler)UI.getCurrent();
        initComponents();
        initActions();
        initData();
    }
    
    private void initActions() {
        btnRemove.addClickListener(
                clickEvent -> ConfirmDialog.show(
                        UI.getCurrent(),
                        "Delete Group",
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
        
        btnEdit.addClickListener(
                clickEvent -> new SingleTextInputDialog(
						"Update Group",
						"Name",
						group.getName(),
						result -> {
							try {
								projectsManager.updateGroup(result, group);
								eventBus.post(new GroupsChangedEvent());
							} catch (IllegalArgumentException e) {
								Notification.show("Info", e.getMessage(), Type.HUMANIZED_MESSAGE);
							
							} catch (Exception e) {
                                errorHandler.showAndLogError(String.format("Failed to update group \"%s\"", group.getName()), e);
							}
						},
						new StringLengthValidator(
						        "Name must be between 2 and 50 characters long, please change the name accordingly!",
						        2, 50)
				).show()
        );

        btnLeave.addClickListener(
                clickEvent -> ConfirmDialog.show(
                        UI.getCurrent(),
                        "Leave Group",
                        "Do you want to leave the group \"" + group.getName() + "\"?",
                        "OK",
                        "Cancel",
                        confirmDialog -> {
                            try {
                                if (confirmDialog.isConfirmed()) {
                                    projectsManager.leaveGroup(group);
                                    eventBus.post(new GroupsChangedEvent());
                                }
                            }
                            catch (IOException e) {
                                errorHandler.showAndLogError(String.format("Failed to leave group \"%s\"", group.getName()), e);
                            }
                        }
                )
        );
        
        memberGridComponent.getActionGridBar().addBtnAddClickListener(clickEvent -> {
        	handleAddClickEvent();
        });
        
        ContextMenu memberGridComponentMoreOptionsContextMenu = memberGridComponent.getActionGridBar().getBtnMoreOptionsContextMenu();
		memberGridComponentMoreOptionsContextMenu.addItem("Remove Members", (selectedItem) -> handleRemoveMembers());

	}
    
	private void handleRemoveMembers() {
		Set<Member> membersToRemove = memberGrid.getSelectedItems();

		if (membersToRemove.isEmpty()) {
			Notification.show("Info", "Please select one or more members first!", Notification.Type.HUMANIZED_MESSAGE);
			return;
		}

		// remove any owner members from the selection and display an informational message
		// TODO: allow the original owner (whose namespace the project is in) to remove any other owner
		if (membersToRemove.stream().anyMatch(member -> member.getRole() == RBACRole.OWNER)) {
			membersToRemove = membersToRemove.stream().filter(
					member -> member.getRole() != RBACRole.OWNER
			).collect(Collectors.toSet());

			Notification ownerMembersSelectedNotification = new Notification(
					"Your selection includes members with the 'Owner' role, who you cannot remove.\n"
							+ "Those members have been ignored. (click to dismiss)",
					Notification.Type.WARNING_MESSAGE
			);
			ownerMembersSelectedNotification.setDelayMsec(-1);
			ownerMembersSelectedNotification.show(Page.getCurrent());
		}

		// remove the current user from the selection and display an informational message
		Optional<Member> selectedMemberCurrentUser = membersToRemove.stream().filter(
				member -> member.getUserId().equals(projectsManager.getUser().getUserId())
		).findAny();

		if (selectedMemberCurrentUser.isPresent()) {
			membersToRemove = membersToRemove.stream().filter(
					member -> member != selectedMemberCurrentUser.get()
			).collect(Collectors.toSet());

			Notification selfSelectedNotification = new Notification(
					"You cannot remove yourself from the project.\n"
							+ "Please use the 'Leave Group' button on the group card on the dashboard instead.\n"
							+ "\n"
							+ "If you are the owner of the group, please contact support to request a transfer\n"
							+ "of ownership. (click to dismiss)",
					Notification.Type.WARNING_MESSAGE
			);
			selfSelectedNotification.setDelayMsec(-1);
			selfSelectedNotification.show(Page.getCurrent());
		}

		if (!membersToRemove.isEmpty()) {
			new RemoveMemberDialog(
					membersToRemove,
					members -> {
						for (Member member : members) {
							try {
									projectsManager.unassignFromGroup(member, group);
							}
							catch (Exception e) {
								errorHandler.showAndLogError(String.format("Failed to remove %s from %s", member, group), e);
							}
							group.getMembers().remove(member);
						}
						memberGrid.getDataProvider().refreshAll();
					}).show();
		}
	}
	private void handleAddClickEvent() {

		TextAreaInputDialog dialog = new TextAreaInputDialog("Add members by email", "Comma- or newline-separated list of email addresses", new SaveCancelListener<String>() {
			
			@Override
			public void savePressed(String result) {
				handleAddListOfEmailAddresses(result);
			}
		});
		
		dialog.show();
		
	}

	private void handleAddListOfEmailAddresses(String addressList) {
		
		String[] addresses = addressList.split("[,;\n]");
		
		SignupTokenManager signupTokenManager = new SignupTokenManager();
		for (String address : addresses) {			
			try {
				signupTokenManager.sendGroupSignupEmail(address, group);
			} catch (EmailException e) {
				errorHandler.showAndLogError(String.format("Error sending group invitation link to address %s" ,  address), e);
			}
		}
		
		
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

        HorizontalLayout topPanel = new HorizontalLayout();
        topPanel.addStyleName("groupslist__card__topPanel");
        addComponent(topPanel);

        memberGrid = new Grid<>();
        memberGrid.setWidth("376px");
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
        topPanel.setExpandRatio(memberGridComponent, 1f);

        sharedProjectsGrid = new Grid<>();
        sharedProjectsGrid.setWidth("291px");
        sharedProjectsGrid.setHeightByRows(4);

        sharedProjectsGrid.addColumn(ProjectReference::getName)
                .setCaption("Title")
                .setComparator((r1, r2) -> String.CASE_INSENSITIVE_ORDER.compare(r1.getName(), r2.getName()))
                .setExpandRatio(1);

        sharedProjectsGridComponent = new ActionGridComponent<>(
                new Label("Shared Projects"),
                sharedProjectsGrid
        );
        sharedProjectsGridComponent.getActionGridBar().setMargin(new MarginInfo(false, true, false, true));
        
        sharedProjectsGridComponent.getActionGridBar().setAddBtnVisible(false);
        sharedProjectsGridComponent.getActionGridBar().setMoreOptionsBtnVisible(false);
        sharedProjectsGridComponent.setSelectionModeFixed(SelectionMode.SINGLE);

        topPanel.addComponent(sharedProjectsGridComponent);
        topPanel.setExpandRatio(sharedProjectsGridComponent, 2f);
        
        HorizontalFlexLayout titleAndActionsLayout = new HorizontalFlexLayout();
        addComponent(titleAndActionsLayout);
        titleAndActionsLayout.addStyleName("groupslist__card__title-and-actions");
        titleAndActionsLayout.setAlignItems(FlexLayout.AlignItems.BASELINE);
        titleAndActionsLayout.setWidth("100%");

        Label nameLabel = new Label(group.getName());
        nameLabel.setWidth("100%");

        titleAndActionsLayout.addComponent(nameLabel);

        btnRemove = new IconButton(VaadinIcons.TRASH);
        titleAndActionsLayout.addComponents(btnRemove);

        btnEdit = new IconButton(VaadinIcons.PENCIL);
        titleAndActionsLayout.addComponent(btnEdit);

        btnLeave = new IconButton(VaadinIcons.EXIT);
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
								&& !rbacManager.hasPermission(role, RBACPermission.GROUP_DELETE), // the owner is the only one with 'delete' permission and the owner cannot leave his group
						() -> {
							btnLeave.setVisible(false);
							btnLeave.setEnabled(false);
						}
				)
		);

    }
}
