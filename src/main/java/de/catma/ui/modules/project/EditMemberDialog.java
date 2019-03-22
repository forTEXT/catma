package de.catma.ui.modules.project;

import com.vaadin.ui.ComponentContainer;

import de.catma.rbac.RBACRole;
import de.catma.rbac.RBACSubject;
import de.catma.repository.git.interfaces.IRemoteGitManagerRestricted;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.user.Member;

public class EditMemberDialog extends AbstractMemberDialog {

	private final Member member;
	private final String projectId;
	
	private RBACRole accesslevel;
	
	public EditMemberDialog(String projectId, 
			Member member,
			IRemoteGitManagerRestricted remoteGitManager, SaveCancelListener<RBACSubject> saveCancelListener) {
		super("Updates a member","update the role",remoteGitManager, saveCancelListener);
		this.member = member;
		this.projectId = projectId;
		this.accesslevel = member.getRole();
	}
	
	@Override
	protected void addContent(ComponentContainer content) {
		super.addContent(content);
		cb_users.setValue(member);
		cb_users.setReadOnly(true);
		cb_role.setValue(accesslevel);
	}

	@Override
	protected RBACSubject getResult() {
		try {
			return remoteGitManager.assignOnProject(member, cb_role.getValue(), projectId);
		} catch (Exception e) {
			errorLogger.showAndLogError(e.getMessage(),e);
			return null;
		}
	}
}
