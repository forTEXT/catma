package de.catma.ui.modules.project;

import de.catma.rbac.RBACSubject;
import de.catma.repository.git.interfaces.IRemoteGitManagerRestricted;
import de.catma.ui.dialog.SaveCancelListener;

public class CreateMemberDialog extends AbstractMemberDialog {

	private final String projectId;

	public CreateMemberDialog(String projectId, IRemoteGitManagerRestricted remoteGitManager, SaveCancelListener<RBACSubject> saveCancelListener) {
		super("Adds a new member","Add a new team member and select his role", remoteGitManager, saveCancelListener);
		this.projectId = projectId;
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
