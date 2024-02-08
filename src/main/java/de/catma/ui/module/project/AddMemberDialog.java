package de.catma.ui.module.project;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.Query;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.VerticalLayout;

import de.catma.rbac.RBACRole;
import de.catma.rbac.RBACSubject;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.rbac.RBACAssignmentFunction;
import de.catma.user.User;

public class AddMemberDialog extends AbstractMemberDialog<RBACSubject> {
	private final RBACAssignmentFunction rbacAssignmentFunction;
	private final QueryFunction<User> userQueryFunction;

	// TODO: the way the user search is implemented here causes the userQueryFunction to be called way more often than needed, fix
	private final LoadingCache<Query<User,String>, List<User>> users = CacheBuilder.newBuilder()
			.maximumSize(1000)
			.expireAfterWrite(10, TimeUnit.MINUTES)
			.build(
					new CacheLoader<Query<User, String>, List<User>>() {
						@Override
						public List<User> load(Query<User, String> query) throws Exception {
							return userQueryFunction.apply(query);
						}
					}
			);

	private final DataProvider<User, String> userDataProvider =
			DataProvider.fromFilteringCallbacks(
					(query) -> {
						try {
							return users.get(query).stream();
						}
						catch (Exception e) {
							errorLogger.showAndLogError("Failed to fetch users", e);
							return new ArrayList<User>().stream();
						}
					},
					query -> {
						try {
							return users.get(query).size();
						}
						catch (Exception e) {
							errorLogger.showAndLogError("Failed to fetch users", e);
							return 0;
						}
					}
	);

	public AddMemberDialog(
			RBACAssignmentFunction rbacAssignmentFunction,
			QueryFunction<User> userQueryFunction,
			SaveCancelListener<RBACSubject> saveCancelListener
	) {
		super("Add Member", "Choose a new project member and specify their role", saveCancelListener);

		this.rbacAssignmentFunction = rbacAssignmentFunction;
		this.userQueryFunction = userQueryFunction;

		this.cbUsers.setDataProvider(userDataProvider);
		this.cbRole.setValue(RBACRole.MAINTAINER);
	}

	@Override
	protected void addContent(ComponentContainer content) {
		content.addComponent(descriptionLabel);
		content.addComponent(cbUsers);
		
		cbUsers.focus();

		Label lblMemberComboboxDescription = new Label(
				"Start typing above to search by username or public name,"
				+ " then select a member from the result list"
		);
		lblMemberComboboxDescription.setWidth("100%");
		lblMemberComboboxDescription.setStyleName("add-member-dialog-member-combobox-description");
		content.addComponent(lblMemberComboboxDescription);

		content.addComponent(cbRole);
		
		if (content instanceof AbstractOrderedLayout) {
			((AbstractOrderedLayout)content).setExpandRatio(cbRole, 1.0f);
		}

	}


	@Override
	protected void layoutContent(VerticalLayout layout) {
		super.layoutContent(layout);
		// prevent layoutContent in AbstractOkCancelDialog from running
		// we want the layout to have an undefined height, just like the dialog/window
	}

	@Override
	protected void handleOkPressed() {
		if (cbUsers.getValue() == null) {
			Notification.show("Info", "Please select a user!", Type.HUMANIZED_MESSAGE);
			return;
		}

		if (cbRole.getValue() == null) {
			Notification.show("Info", "Please select a role!", Type.HUMANIZED_MESSAGE);
			return;
		}

		super.handleOkPressed();
	}

	@Override
	protected RBACSubject getResult() {
		try {
			return rbacAssignmentFunction.assign(cbUsers.getValue(), cbRole.getValue());
		}
		catch (Exception e) {
			errorLogger.showAndLogError(null, e);
			return null;
		}
	}
}
