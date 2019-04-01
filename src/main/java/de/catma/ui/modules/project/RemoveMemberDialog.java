package de.catma.ui.modules.project;

import java.util.Set;

import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.ListSelect;

import de.catma.repository.git.interfaces.IRemoteGitManagerRestricted;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.user.Member;
import de.catma.user.User;

public class RemoveMemberDialog extends AbstractMemberDialog<Void> {

	private final Set<Member> members;
	private final String projectId;
	
	private ListSelect<Member> ls_members;
	
	public RemoveMemberDialog(String projectId, 
			Set<Member> members,
			IRemoteGitManagerRestricted remoteGitManager, SaveCancelListener<Void> saveCancelListener) {
		super("Remove member","Removes the selected memeber from current project",remoteGitManager, saveCancelListener);
		this.members = members;
		this.projectId = projectId;
	}
	
	@Override
	protected void addContent(ComponentContainer content) {
		ls_members = new ListSelect<>("Members", members);
		ls_members.setReadOnly(true);
		ls_members.setWidth("100%");
		ls_members.setItemCaptionGenerator(User::getIdentifier);
		content.addComponent(ls_members);		
	}
	
	@Override
	protected Void getResult() {
		try {
			for(Member member : members){
				remoteGitManager.unassignFromProject(member,  projectId);
			}
			return null;
		} catch (Exception e) {
			errorLogger.showAndLogError(e.getMessage(), e);
			return null;
		}
	}
}
