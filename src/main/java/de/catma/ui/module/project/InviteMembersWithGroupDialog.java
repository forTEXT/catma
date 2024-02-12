package de.catma.ui.module.project;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.vaadin.data.ValidationResult;
import com.vaadin.data.Validator;
import com.vaadin.data.ValueContext;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.DateField;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;

import de.catma.rbac.RBACRole;
import de.catma.ui.dialog.AbstractOkCancelDialog;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.user.Group;

public class InviteMembersWithGroupDialog extends AbstractOkCancelDialog<de.catma.ui.module.project.InviteMembersWithGroupDialog.MemberData> {
	
	public static record MemberData(String groupName, RBACRole projectRole, List<String> emailAdresses, Group group, LocalDate expiresAt) {}; 
	protected static class Mode {
		private final Set<Component> modeComponents;
		private final Set<Component> nonModeComponents;
		private final Function<AbstractOrderedLayout, Runnable> configureLayoutAction;
		private final Supplier<MemberData> memberDataSupplier;
		private Validator<MemberData> validator;
		public Mode(
				Set<Component> modeComponents, 
				Set<Component> nonModeComponents, 
				Function<AbstractOrderedLayout, Runnable> enableLayoutAction,
				Supplier<MemberData> memberDataSupplier,
				Validator<MemberData> validator) {
			super();
			this.modeComponents = modeComponents;
			this.nonModeComponents = nonModeComponents;
			this.configureLayoutAction = enableLayoutAction;
			this.memberDataSupplier = memberDataSupplier;
			this.validator = validator;
		}
		
		public void activate(ComponentContainer container) {
			modeComponents.forEach(c -> c.setVisible(true));
			nonModeComponents.forEach(c -> c.setVisible(false));
			if (container instanceof AbstractOrderedLayout) {
				configureLayoutAction.apply((AbstractOrderedLayout) container).run();
			}
		}
		
		public MemberData getMemberData() {
			return memberDataSupplier.get();
		}
		
		public ValidationResult validate() {
			return validator.apply(getMemberData(), null);
		}
	}

	private HorizontalLayout groupSelectionPanel;
	private ComboBox<Group> cbGroup;
	private Button btSwitchGroupSelectionToCreateGroup;
	
	private TextField groupName;
	private TextArea emailAddressListInput;

	private Button btSwitchMemberInvitationtoCreateGroup;
	
	private ComboBox<RBACRole> cbRole;
	private DateField expiresAtInput;
	
	private Mode addGroupMode;
	private Mode createGroupMode;
	private Mode inviteProjectMembersMode;
	private Mode inviteGroupMembersMode;
	
	private Mode currentMode;
	

	protected InviteMembersWithGroupDialog(SaveCancelListener<MemberData> saveCancelListener) {
		this(saveCancelListener, () -> Collections.<Group>emptyList());
	}
	
	public InviteMembersWithGroupDialog(SaveCancelListener<MemberData> saveCancelListener,
			Supplier<List<Group>> groupsSupplier) {
		super(
				"", // will be set by Mode, see createComponents below
				saveCancelListener);
		createComponents(groupsSupplier);
	}

	public static InviteMembersWithGroupDialog buildAddGroupDialog(SaveCancelListener<MemberData> saveCancelListener, Supplier<List<Group>> groupsSupplier) {
		InviteMembersWithGroupDialog dialog = new InviteMembersWithGroupDialog(saveCancelListener, groupsSupplier);
		dialog.currentMode = dialog.addGroupMode;
		return dialog;
	}
	
	public static InviteMembersWithGroupDialog buildCreateGroupDialog(SaveCancelListener<MemberData> saveCancelListener) {
		InviteMembersWithGroupDialog dialog = new InviteMembersWithGroupDialog(saveCancelListener);
		dialog.currentMode = dialog.createGroupMode;
		return dialog;
	}
	
	public static InviteMembersWithGroupDialog buildInviteProjectMembersDialog(SaveCancelListener<MemberData> saveCancelListener) {
		InviteMembersWithGroupDialog dialog = new InviteMembersWithGroupDialog(saveCancelListener);
		dialog.currentMode = dialog.inviteProjectMembersMode;
		return dialog;
	}

	public static InviteMembersWithGroupDialog buildInviteGroupMembersDialog(SaveCancelListener<MemberData> saveCancelListener) {
		InviteMembersWithGroupDialog dialog = new InviteMembersWithGroupDialog(saveCancelListener);
		dialog.currentMode = dialog.inviteGroupMembersMode;
		return dialog;
	}


	private void createComponents(Supplier<List<Group>> groupsSupplier) {
		groupSelectionPanel = new HorizontalLayout();
		cbGroup = new ComboBox<Group>("Group", groupsSupplier.get());
		cbGroup.setWidth("100%");
		cbGroup.setPageLength(20);
		cbGroup.setItemCaptionGenerator(Group::getName);
		cbGroup.setRequiredIndicatorVisible(true);
		btSwitchGroupSelectionToCreateGroup = new Button("Create a new Group");
		
		groupName = new TextField("Group name");
		groupName.setWidth("100%");
		groupName.setRequiredIndicatorVisible(true);
		emailAddressListInput = new TextArea("Type in the email addresses of the new members as a comma- or newline separated list");
		emailAddressListInput.setSizeFull();
		
		btSwitchMemberInvitationtoCreateGroup = new Button("Create a new Group");
		btSwitchMemberInvitationtoCreateGroup.setDescription("Consider creating a new Group for the project members given above");
		
		cbRole = new ComboBox<RBACRole>("Role", 
				Lists.newArrayList(RBACRole.ASSISTANT, RBACRole.MAINTAINER));

		cbRole.setWidth("100%");
		cbRole.setItemCaptionGenerator(RBACRole::getRoleName);
		cbRole.setEmptySelectionAllowed(false);
		cbRole.setValue(RBACRole.MAINTAINER);
		
		expiresAtInput = new DateField("Membership expires at (optional)");
		expiresAtInput.setDateFormat("yyyy/MM/dd");
		expiresAtInput.setPlaceholder("yyyy/mm/dd");
		expiresAtInput.setWidth("100%");
		
		addGroupMode = new Mode(
				Set.of(cbGroup, btSwitchGroupSelectionToCreateGroup, groupSelectionPanel, expiresAtInput), 
				Set.of(groupName, emailAddressListInput, btSwitchMemberInvitationtoCreateGroup), 
				(content) -> {
					return () -> {
						setCaption("Add user group");
						content.setExpandRatio(expiresAtInput, 1.0f);
						cbGroup.focus();
					};
				},
				() -> new MemberData(
						null, 
						cbRole.getValue(), 
						null, 
						cbGroup.getValue(),
						expiresAtInput.getValue()), 
				new Validator<MemberData>() {

					@Override
					public ValidationResult apply(MemberData memberData, ValueContext context) {
						if (memberData.group == null) {
							return ValidationResult.error("You have to select a group! You can also create a new group.");
						}
						return ValidationResult.ok();
					}
					
				}
		);
		
		createGroupMode = new Mode(
				Set.of(groupName, emailAddressListInput, expiresAtInput), 
				Set.of(cbGroup, btSwitchGroupSelectionToCreateGroup, btSwitchMemberInvitationtoCreateGroup, groupSelectionPanel), 
				(content) -> {
					return () -> {
						setCaption("Add user group");
						content.setExpandRatio(emailAddressListInput, 1.0f);
						content.setExpandRatio(expiresAtInput, 0.0f);
						groupName.focus();
					};
				},
				() -> new MemberData(
						groupName.getValue(), 
						cbRole.getValue(), 
						Arrays.stream(emailAddressListInput.getValue().split("[,;\n]")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList()), 
						null,
						expiresAtInput.getValue()),
				new Validator<MemberData>() {
					@Override
					public ValidationResult apply(MemberData memberData, ValueContext context) {
						if (memberData.groupName() == null || memberData.groupName().trim().isEmpty()) {
							return ValidationResult.error("You have to provide a name for the new group!");
						}

						return ValidationResult.ok();
					}
				}
		);
		
		inviteProjectMembersMode = new Mode(
				Set.of(emailAddressListInput, btSwitchMemberInvitationtoCreateGroup, expiresAtInput), 
				Set.of(cbGroup, groupName, btSwitchGroupSelectionToCreateGroup, groupSelectionPanel), 
				(content) -> {
					return () -> {
						setCaption("Add members");
						content.setExpandRatio(emailAddressListInput, 1.0f);
						content.setExpandRatio(expiresAtInput, 0.0f);
						emailAddressListInput.focus();
					};
				},
				() -> new MemberData(
						null, 
						cbRole.getValue(), 
						Arrays.stream(emailAddressListInput.getValue().split("[,;\n]")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList()), 
						null,
						expiresAtInput.getValue()),
				new Validator<MemberData>() {
					@Override
					public ValidationResult apply(MemberData memberData, ValueContext context) {
						if (memberData.emailAdresses.isEmpty()) {
							return ValidationResult.error("You have provide at least one valid email address to invite someone to the Group!");
						}
						return ValidationResult.ok();
					}
				}
		);
		
		inviteGroupMembersMode = new Mode(
				Set.of(emailAddressListInput, expiresAtInput), 
				Set.of(cbGroup, groupName, btSwitchGroupSelectionToCreateGroup, btSwitchMemberInvitationtoCreateGroup, groupSelectionPanel, cbRole), 
				(content) -> {
					return () -> {
						setCaption("Add members");
						content.setExpandRatio(emailAddressListInput, 1.0f);
						content.setExpandRatio(expiresAtInput, 0.0f);
						emailAddressListInput.focus();
					};
				},
				() -> new MemberData(
						null, 
						null, 
						Arrays.stream(emailAddressListInput.getValue().split("[,;\n]")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList()), 
						null,
						expiresAtInput.getValue()),
				new Validator<MemberData>() {
					@Override
					public ValidationResult apply(MemberData memberData, ValueContext context) {
						if (memberData.emailAdresses.isEmpty()) {
							return ValidationResult.error("You have provide at least one valid email address to invite someone to the Project!");
						}
						return ValidationResult.ok();
					}
				}
		);
		
	}

	@Override
	protected void addContent(ComponentContainer content) {
		
		groupSelectionPanel.setWidth("100%");
		groupSelectionPanel.addComponent(cbGroup);
		groupSelectionPanel.addComponent(btSwitchGroupSelectionToCreateGroup);
		groupSelectionPanel.setComponentAlignment(btSwitchGroupSelectionToCreateGroup, Alignment.BOTTOM_RIGHT);;
		groupSelectionPanel.setExpandRatio(cbGroup, 1.0f);
		content.addComponent(groupSelectionPanel);
		content.addComponent(groupName);
		content.addComponent(emailAddressListInput);
		content.addComponent(btSwitchMemberInvitationtoCreateGroup);
		content.addComponent(cbRole);
		content.addComponent(expiresAtInput);
		
		btSwitchGroupSelectionToCreateGroup.addClickListener(event -> {
			createGroupMode.activate(content);
			currentMode = createGroupMode;
		});
		
		btSwitchMemberInvitationtoCreateGroup.addClickListener(event -> {
			createGroupMode.activate(content);
			currentMode = createGroupMode;
		});
		
		currentMode.activate(content);
	}
	

	@Override
	protected void handleOkPressed() {
		ValidationResult vr = currentMode.validate();
		if (vr.isError()) {
			Notification.show("Info", vr.getErrorMessage(), Type.HUMANIZED_MESSAGE);
		}
		else {
			super.handleOkPressed();
		}
	}

	@Override
	protected MemberData getResult() {
		return currentMode.getMemberData();
	}

	
	
}
