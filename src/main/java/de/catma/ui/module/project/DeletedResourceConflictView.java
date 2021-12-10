package de.catma.ui.module.project;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import de.catma.project.conflict.DeletedResourceConflict;
import de.catma.project.conflict.Resolution;

public class DeletedResourceConflictView extends VerticalLayout {
	
	private Button btMine;
	private Button btTheirs;
	private ResolutionListener resolutionListener;
	private DeletedResourceConflict deletedResourceConflict;
	private TextField resourceNameField;
	private TextArea myCommitInfoField;
	private TextArea theirCommitInfoField;


	public DeletedResourceConflictView(DeletedResourceConflict deletedResourceConflict,
			ResolutionListener resolutionListener) {
		this.deletedResourceConflict = deletedResourceConflict;
		this.resolutionListener = resolutionListener;
		initComponents();
		initActions();
		initData();
	}

	private void initData() {
		this.resourceNameField.setValue(deletedResourceConflict.getContentInfoSet().getTitle());

		switch (deletedResourceConflict.getResourceType()) {
			case ANNOTATION_COLLECTION:
				this.resourceNameField.setCaption("Annotation Collection");
				break;
			case TAGSET:
				this.resourceNameField.setCaption("Tagset");
				break;
			case SOURCE_DOCUMENT:
				this.resourceNameField.setCaption("Document");
				break;
			default:
				this.resourceNameField.setCaption("Resource");
		}

		if (deletedResourceConflict.isDeletedByThem()) {
			btMine.setCaption("Keep mine");
			btTheirs.setCaption("Accept their deletion");
		}
		else {
			btMine.setCaption("Enforce my deletion");
			btTheirs.setCaption("Keep theirs");			
		}

		myCommitInfoField.setValue(
				"'"
				+ deletedResourceConflict.getOurLastCommitMsg()
				+ "' \n\nCommit ID "
				+ deletedResourceConflict.getOurCommitName());

		theirCommitInfoField.setValue(
				"'"
				+ deletedResourceConflict.getTheirLastCommitMsg() 
				+ "'\nby Team member " 
				+ deletedResourceConflict.getTheirCommiterName()
				+ " \n\nCommit ID "
				+ deletedResourceConflict.getTheirCommitName());
	}

	private void initComponents() {
		setSizeFull();
		addComponent(new Label("A resource has been deleted and modified concurrently: "));
		
		this.resourceNameField = new TextField();
		this.resourceNameField.setReadOnly(true);
		this.resourceNameField.setWidth("100%");
		this.resourceNameField.addStyleName("deleted-resource-conflict-view-resource-field");
		addComponent(resourceNameField);		
		
		HorizontalLayout commitInfoPanel = new HorizontalLayout();
		commitInfoPanel.setSizeFull();
		this.myCommitInfoField = new TextArea("My last change commit message");
		this.myCommitInfoField.setSizeFull();
		this.myCommitInfoField.setReadOnly(true);
		commitInfoPanel.addComponent(myCommitInfoField);
		
		this.theirCommitInfoField = new TextArea("Their last change commit message");
		this.theirCommitInfoField.setSizeFull();
		this.theirCommitInfoField.setReadOnly(true);
		commitInfoPanel.addComponent(theirCommitInfoField);
		
		addComponent(commitInfoPanel);
		setExpandRatio(commitInfoPanel, 1.0f);
		
		HorizontalLayout buttonPanel = new HorizontalLayout();
		buttonPanel.setWidth("100%");
		
		btMine = new Button("Take mine");
		buttonPanel.addComponent(btMine);
		buttonPanel.setComponentAlignment(btMine, Alignment.BOTTOM_CENTER);
		btTheirs = new Button("Take theirs");
		buttonPanel.addComponent(btTheirs);
		buttonPanel.setComponentAlignment(btTheirs, Alignment.BOTTOM_CENTER);
		
		addComponent(buttonPanel);
	}
	
	private void initActions() {
		btMine.addClickListener(event -> handleResolved(Resolution.MINE));
		btTheirs.addClickListener(event -> handleResolved(Resolution.THEIRS));
	}

	private void handleResolved(Resolution resolution) {
		deletedResourceConflict.setResolution(resolution);
		this.resolutionListener.resolved();
	}
}
