package de.catma.ui.module.project;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.Query;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;

import de.catma.rbac.RBACRole;
import de.catma.rbac.RBACSubject;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.rbac.RBACAssignmentFunction;
import de.catma.user.User;

public  class AddMemberDialog extends AbstractMemberDialog<RBACSubject> {

	private final RBACAssignmentFunction assignment;
	
	private final QueryFunction<User> getUserQuery;

	private final LoadingCache<Query<User,String>, List<User>> users = CacheBuilder.newBuilder()
		       .maximumSize(1000)
		       .expireAfterWrite(10, TimeUnit.MINUTES)
		       .build(
		           new CacheLoader<Query<User,String>, List<User>>() {

					@Override
					public List<User> load(Query<User,String> query) throws Exception {
						return getUserQuery.apply(query);
					}
		           });
	
	private final DataProvider<User, String> userDataProvider =
        DataProvider.fromFilteringCallbacks(
                (query) -> {
                    try {
                    	return users.get(query).stream();
                    } catch (Exception e) {
                        errorLogger.showAndLogError("Can't get users from remote git manager",e);
                    	return new ArrayList<User>().stream();
                    }
                },
                query -> {
                    try {
                        	return users.get(query).size();
                    } catch (Exception e) {
                        errorLogger.showAndLogError("Can't get users from remote git manager",e);
                        return 0;
                    }
                }
    );


	
	public AddMemberDialog(
			RBACAssignmentFunction assignment, 
			QueryFunction<User> getUserQuery, 
			SaveCancelListener<RBACSubject> saveCancelListener) {
		super("Add a new Member","Select a new team Member and his/her role", saveCancelListener);
		this.assignment = assignment;
		this.getUserQuery = getUserQuery;
		this.cbUsers.setDataProvider(userDataProvider);
		this.cbRole.setValue(RBACRole.MAINTAINER);
	}
	
	@Override
	protected void handleOkPressed() {
		
		if (cbUsers.getValue() == null) {
			Notification.show("Info", "Please select a valid member first!", Type.HUMANIZED_MESSAGE);
			return;
		}
		
		if (cbRole.getValue() == null) {
			Notification.show("Info", "Please select a role first!", Type.HUMANIZED_MESSAGE);
			return;
		}
		
		super.handleOkPressed();
	}
	
	@Override
	protected RBACSubject getResult() {
		try {
			return assignment.assign(cbUsers.getValue(), cbRole.getValue());
		} catch (Exception e) {
			errorLogger.showAndLogError(e.getMessage(),e);
			return null;
		}
	}
	
}
