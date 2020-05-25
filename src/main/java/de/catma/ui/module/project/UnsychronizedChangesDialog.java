package de.catma.ui.module.project;

import java.util.List;
import java.util.regex.Pattern;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import de.catma.project.CommitInfo;
import de.catma.ui.util.Cleaner;

public class UnsychronizedChangesDialog extends Window {
	
	public UnsychronizedChangesDialog(List<CommitInfo> unsynchronizedChanges) {
		super("Unsynchronized changes");
		initComponents(unsynchronizedChanges);
	}

	private void initComponents(List<CommitInfo> unsynchronizedChanges) {
		setHeight("400px");
		setWidth("600px");
		VerticalLayout content = new VerticalLayout();
		
		setContent(content);
		
		for (CommitInfo ci : unsynchronizedChanges) {
			Label l = new Label(
					ci.getCommitId() 
					+ "<br/><b>"+Cleaner.clean(ci.getCommitMsg().replaceAll(Pattern.quote("with")+"\\s+" + Pattern.quote("ID") + "\\s+\\S+\\s?", ""))+"</b>");
			l.setContentMode(ContentMode.HTML);
			content.addComponent(l);
		}
		
	}

	public void show(int x, int y) {
		setPosition(x-500, y);
		UI.getCurrent().addWindow(this);
	}
}
