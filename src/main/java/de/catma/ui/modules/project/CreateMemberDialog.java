package de.catma.ui.modules.project;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.Query;

import de.catma.rbac.RBACSubject;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.rbac.RBACAssignmentFunction;
import de.catma.user.User;

public  class CreateMemberDialog extends AbstractMemberDialog<RBACSubject> {

	private final RBACAssignmentFunction assignment;
	
	private final QueryFunction<User> getUserQuery;

	private final LoadingCache<Query<User,String>, List<User>> users = CacheBuilder.newBuilder()
		       .maximumSize(1000)
		       .expireAfterWrite(10, TimeUnit.MINUTES)
		       .build(
		           new CacheLoader<Query<User,String>, List<User>>() {

					@Override
					public List<User> load(Query<User,String> query) throws Exception {
//						String username = query.getFilter().isPresent() ? query.getFilter().get() : "";
						return getUserQuery.apply(query);
//						return remoteGitManager.findUser(query.getFilter().isPresent() ? query.getFilter().get() : "", query.getOffset(), query.getLimit());	
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
                        errorLogger.showAndLogError("Can't get projects from ProjectManager",e);
                        return 0;
                    }
                }
    );


	
	public CreateMemberDialog(
			RBACAssignmentFunction assignment, 
			QueryFunction<User> getUserQuery, 
			SaveCancelListener<RBACSubject> saveCancelListener) {
		super("Adds a new member","Add a new team member and select his role", saveCancelListener);
		this.assignment = assignment;
		this.getUserQuery = getUserQuery;
		this.cb_users.setDataProvider(userDataProvider);
	}
	
	@Override
	protected RBACSubject getResult() {
		try {
			return assignment.assign(cb_users.getValue(), cb_role.getValue());
		} catch (Exception e) {
			errorLogger.showAndLogError(e.getMessage(),e);
			return null;
		}
	}
	
}
