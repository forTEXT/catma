package de.catma.ui.module.dashboard;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.mail.EmailException;
import org.vaadin.dialogs.ConfirmDialog;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.vaadin.contextmenu.ContextMenu;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.Page;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.ItemClick;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;
import com.vaadin.ui.renderers.HtmlRenderer;

import de.catma.project.ProjectReference;
import de.catma.project.ProjectsManager;
import de.catma.rbac.RBACManager;
import de.catma.rbac.RBACConstraint;
import de.catma.rbac.RBACConstraintEnforcer;
import de.catma.rbac.RBACPermission;
import de.catma.rbac.RBACRole;
import de.catma.ui.component.IconButton;
import de.catma.ui.component.actiongrid.ActionGridComponent;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.events.GroupsChangedEvent;
import de.catma.ui.events.MembersChangedEvent;
import de.catma.ui.events.routing.RouteToProjectEvent;
import de.catma.ui.layout.HorizontalFlexLayout;
import de.catma.ui.layout.VerticalFlexLayout;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.ui.module.project.EditMemberDialog;
import de.catma.ui.module.project.InviteMembersWithGroupDialog;
import de.catma.ui.module.project.InviteMembersWithGroupDialog.MemberData;
import de.catma.ui.module.project.ProjectParticipant;
import de.catma.ui.module.project.RemoveMemberDialog;
import de.catma.user.Group;
import de.catma.user.Member;
import de.catma.user.User;
import de.catma.user.signup.SignupTokenManager;
import de.catma.util.Pair;

public class GroupCard extends VerticalFlexLayout {

	private final Group group;
	private final ProjectsManager projectsManager;
	private final EventBus eventBus;
	
	private final ErrorHandler errorHandler;
	
	private final RBACManager rbacManager;
    private final RBACConstraintEnforcer<RBACRole> rbacEnforcer = new RBACConstraintEnforcer<>();
    
    private Grid<Member> memberGrid;
    private ActionGridComponent<Grid<Member>> memberGridComponent;

    private Grid<ProjectReference> sharedProjectsGrid;
    private ActionGridComponent<Grid<ProjectReference>> sharedProjectsGridComponent;
	private IconButton btnRemove;
	private IconButton btnEdit;
	private IconButton btnLeave;


    public GroupCard(Group group, ProjectsManager projectsManager, EventBus eventBus, RBACManager rbacManager) {
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
                                    eventBus.post(new GroupsChangedEvent());
                                }
                            }
                            catch (IOException e) {
                                errorHandler.showAndLogError(String.format("Failed to delete group \"%s\"", group.getName()), e);
                            }
                        }
                )
        );
        
        btnEdit.addClickListener(
                clickEvent -> new EditGroupDialog(
                        projectsManager,
                        group,
                        result -> eventBus.post(new GroupsChangedEvent())
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
        
        rbacEnforcer.register(
    		RBACConstraint.ifAuthorized(
				role -> rbacManager.hasPermission(role, RBACPermission.GROUP_MEMBERS_EDIT), 
				() -> {
					memberGridComponentMoreOptionsContextMenu.addItem("Edit Members", (selectedItem) -> handleEditMembers());
					memberGridComponentMoreOptionsContextMenu.addItem("Remove Members", (selectedItem) -> handleRemoveMembers());
				}));
        rbacEnforcer.register(
    		RBACConstraint.ifNotAuthorized(
				role -> rbacManager.hasPermission(role, RBACPermission.GROUP_MEMBERS_EDIT), 
				() -> {
					memberGridComponent.getActionGridBar().setAddBtnVisible(false);
					memberGridComponent.getActionGridBar().setMoreOptionsBtnVisible(false);
			        memberGridComponent.getActionGridBar().setMargin(new MarginInfo(false, true, false, true));
				}));
        
        sharedProjectsGrid.addItemClickListener(itemClickEvent -> handleProjectItemClick(itemClickEvent));
	}
    
	private void handleProjectItemClick(ItemClick<ProjectReference> itemClickEvent) {
		if (!itemClickEvent.getMouseEventDetails().isDoubleClick()) {
			return;

		}
		ProjectReference projectReference = itemClickEvent.getItem();
		
		eventBus.post(new RouteToProjectEvent(projectReference));
	}

	private void handleEditMembers() {
		Set<Member> membersToEdit = memberGrid.getSelectedItems();

		if (membersToEdit.isEmpty()) {
			Notification.show("Info", "Please select one or more members first!", Notification.Type.HUMANIZED_MESSAGE);
			return;
		}
	
		// remove the current user from the selection and display an informational message
		Optional<Member> selectedMemberCurrentUser = membersToEdit.stream().filter(
				member -> member.getUserId().equals(projectsManager.getUser().getUserId())
		).findAny();

		if (selectedMemberCurrentUser.isPresent()) {
			membersToEdit = membersToEdit.stream().filter(
					member -> member != selectedMemberCurrentUser.get()
			).collect(Collectors.toSet());

			Notification selfSelectedNotification = new Notification(
					"You cannot change your own role.\n"
							+ "Please have another owner of the group change your role instead.\n"
							+ "\n"
							+ "If you are the only owner of the group, assign ownership to another member of the group. \n"
							+ "(click to dismiss)",
					Notification.Type.WARNING_MESSAGE
			);
			selfSelectedNotification.setDelayMsec(-1);
			selfSelectedNotification.show(Page.getCurrent());
			
			
		}
	
		if (!membersToEdit.isEmpty()) {
			final Set<Member> participants = membersToEdit;
			new EditMemberDialog(
					membersToEdit.stream().map(GroupMemberParticipant::new).collect(Collectors.toSet()),
					Lists.newArrayList(RBACRole.ASSISTANT, RBACRole.MAINTAINER, RBACRole.OWNER),
					new SaveCancelListener<Pair<RBACRole, LocalDate>>() {
						public void savePressed(Pair<RBACRole, LocalDate> roleAndExpiresAt) {
							try {
								for (Member participant : participants) {
									projectsManager.updateAssignmentOnGroup(participant.getUserId(), group.getId(), roleAndExpiresAt.getFirst(), roleAndExpiresAt.getSecond());
								}
							}
							catch (Exception e) {
								errorHandler.showAndLogError("Error changing role!", e);
							}
							
							eventBus.post(new MembersChangedEvent());
						};
					}
			).show();
		}

	}
	
	
	private void handleRemoveMembers() {
		Set<Member> membersToRemove = memberGrid.getSelectedItems();

		if (membersToRemove.isEmpty()) {
			Notification.show("Info", "Please select one or more members first!", Notification.Type.HUMANIZED_MESSAGE);
			return;
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
					"You cannot remove yourself from the group.\n"
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
					"group",
					membersToRemove.stream().map(m -> new GroupMemberParticipant(m)).collect(Collectors.toSet()),
					members -> {
						for (ProjectParticipant member : members) {
							try {
									projectsManager.unassignFromGroup(((GroupMemberParticipant)member).getMember(), group);
							}
							catch (Exception e) {
								errorHandler.showAndLogError(String.format("Failed to remove %s from %s", member, group), e);
							}
							group.getMembers().remove(((GroupMemberParticipant)member).getMember());
						}
						memberGrid.getDataProvider().refreshAll();
					}).show();
		}
	}
	private void handleAddClickEvent() {
		
		InviteMembersWithGroupDialog dialog = InviteMembersWithGroupDialog.buildInviteGroupMembersDialog(new SaveCancelListener<InviteMembersWithGroupDialog.MemberData>() {
			
			@Override
			public void savePressed(MemberData result) {
				handleAddListOfEmailAddresses(result.emailAdresses(), result.expiresAt());
			}
		});

		dialog.show();
		
	}

	private void handleAddListOfEmailAddresses(List<String> addresses, LocalDate expiresAt) {
				
		SignupTokenManager signupTokenManager = new SignupTokenManager();
		for (String address : addresses) {			
			try {
				signupTokenManager.sendGroupSignupEmail(address, group, expiresAt);
			} catch (EmailException e) {
				errorHandler.showAndLogError(String.format("Error sending group invitation link to address %s" ,  address), e);
			}
		}
		
		
	}
	
	private void initData() {
		try {
			RBACRole groupRole = rbacManager.getRoleOnGroup(projectsManager.getUser(), group);
			rbacEnforcer.enforceConstraints(groupRole); // normally done in reload();
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

        HorizontalFlexLayout hflTitle = new HorizontalFlexLayout();
        hflTitle.addStyleName("groupslist__card__title");

        Label lblName = new Label(group.getName());
        lblName.setWidth("100%");
        hflTitle.addComponent(lblName);

        addComponent(hflTitle);

        HorizontalLayout hlContent = new HorizontalLayout();
        hlContent.addStyleName("groupslist__card__content");

        memberGrid = new Grid<>();
        memberGrid.setHeightByRows(4);
        memberGrid.addColumn((user) -> VaadinIcons.USER.getHtml(), new HtmlRenderer());
        memberGrid.addColumn(User::getName)
                .setCaption("Name")
                .setComparator((r1, r2) -> String.CASE_INSENSITIVE_ORDER.compare(r1.getName(), r2.getName()))
                .setDescriptionGenerator(User::preciseName).setExpandRatio(1);
        memberGrid.addColumn(Member::getRole);
        memberGridComponent = new ActionGridComponent<>(
                new Label("Members"),
                memberGrid
        );
        
        memberGridComponent.addStyleName("groupslist__card__grid");
        
        hlContent.addComponent(memberGridComponent);

        sharedProjectsGrid = new Grid<>();
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

        hlContent.addComponent(sharedProjectsGridComponent);

        addComponent(hlContent);

        HorizontalFlexLayout hflDescriptionAndActions = new HorizontalFlexLayout();
        hflDescriptionAndActions.addStyleName("groupslist__card__description-and-actions");
        hflDescriptionAndActions.setAlignItems(AlignItems.CENTER);
        hflDescriptionAndActions.setWidth("100%");

        Label lblDescription = new Label(group.getDescription());
        lblDescription.setWidth("100%");

        hflDescriptionAndActions.addComponent(lblDescription);

        btnRemove = new IconButton(VaadinIcons.TRASH);
        hflDescriptionAndActions.addComponents(btnRemove);

        btnEdit = new IconButton(VaadinIcons.PENCIL);
        hflDescriptionAndActions.addComponent(btnEdit);

        btnLeave = new IconButton(VaadinIcons.EXIT);
        hflDescriptionAndActions.addComponent(btnLeave);

        addComponent(hflDescriptionAndActions);

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
