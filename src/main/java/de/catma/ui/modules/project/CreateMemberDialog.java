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
import de.catma.repository.git.interfaces.IRemoteGitManagerRestricted;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.user.User;

public class CreateMemberDialog extends AbstractMemberDialog<RBACSubject> {

	private final String projectId;
	private final LoadingCache<Query<User,String>, List<User>> users = CacheBuilder.newBuilder()
		       .maximumSize(1000)
		       .expireAfterWrite(10, TimeUnit.MINUTES)
		       .build(
		           new CacheLoader<Query<User,String>, List<User>>() {

					@Override
					public List<User> load(Query<User,String> query) throws Exception {
						String username = query.getFilter().isPresent() ? query.getFilter().get() : "";
						return remoteGitManager.findUser(username, query.getOffset(), query.getLimit());					}
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
	
	public CreateMemberDialog(String projectId, IRemoteGitManagerRestricted remoteGitManager, SaveCancelListener<RBACSubject> saveCancelListener) {
		super("Adds a new member","Add a new team member and select his role", remoteGitManager, saveCancelListener);
		this.projectId = projectId;
		this.cb_users.setDataProvider(userDataProvider);
	}
	
	@Override
	protected RBACSubject getResult() {
		try {
			return remoteGitManager.assignOnProject(cb_users.getValue(), cb_role.getValue(), projectId);
		} catch (Exception e) {
			errorLogger.showAndLogError(e.getMessage(),e);
			return null;
		}
	}
}
