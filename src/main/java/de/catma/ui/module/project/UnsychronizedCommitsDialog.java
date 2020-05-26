package de.catma.ui.module.project;

import java.util.List;
import java.util.regex.Pattern;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import de.catma.project.CommitInfo;
import de.catma.ui.util.Cleaner;

public class UnsychronizedCommitsDialog extends Window {
	
	public interface SyncClient {
		public void synchronizeProject();
	}
	
	private Button btSynchNow;

	public UnsychronizedCommitsDialog(List<CommitInfo> unsynchronizedChanges, SyncClient syncClient) {
		super("Unsynchronized commits");
		initComponents(unsynchronizedChanges);
		initActions(syncClient);
	}

	private void initActions(SyncClient syncClient) {
		btSynchNow.addClickListener(clickEvent -> {
			UnsychronizedCommitsDialog.this.close();
			syncClient.synchronizeProject();
		});
	}

	private void initComponents(List<CommitInfo> unsynchronizedChanges) {
		setHeight("400px");
		setWidth("600px");
		setModal(true);
		center();
		VerticalLayout content = new VerticalLayout();
		
		setContent(content);
		
		if (unsynchronizedChanges.isEmpty()) {
			content.addComponent(new Label("All your commits are synchronized!"));
		}
		else {
			content.addComponent(
				new Label(
					String.format(
							"You have %1$d unsynchronized commit%2$s:", 
							unsynchronizedChanges.size(),
							unsynchronizedChanges.size()==1?"":"s")));
		}
		
		for (CommitInfo ci : unsynchronizedChanges) {
			Label l = new Label(
					ci.getCommitId() 
					+ "<br/><b>"
					+ Cleaner.clean(
						ci.getCommitMsg().replaceAll(
								Pattern.quote("with")
								+"\\s+" 
								+ Pattern.quote("ID") 
								+ "\\s+\\S+\\s?", ""))
					+"</b>");
			l.setContentMode(ContentMode.HTML);
			content.addComponent(l);
		}
		
		btSynchNow = new Button("Synchronize with the team now");
		btSynchNow.addStyleName("primary-button"); //$NON-NLS-1$
		
		content.addComponent(btSynchNow);
		content.setExpandRatio(btSynchNow, 1.0f);
		content.setComponentAlignment(btSynchNow, Alignment.BOTTOM_RIGHT);
	}

	public void show() {
		UI.getCurrent().addWindow(this);
	}
}
