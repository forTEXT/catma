package de.catma.ui.analyzenew.annotation;

import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import de.catma.document.repository.Repository;
import de.catma.tag.TagDefinition;
import de.catma.ui.modules.tags.TagSelectionPanel;

public class TagSelectionStep extends VerticalLayout {
	
	private TextField selectedTagField;
	private TagSelectionPanel tagSelectionPanel;
	private TagDefinition selectedTag;
	private Repository project;

	public TagSelectionStep(Repository project) {
		this.project = project;
		initComponents();
		initActions();
	}

	private void initActions() {
		tagSelectionPanel.addTagSelectionChangedListener(tag -> {
			this.selectedTag = tag;
			selectedTagField.setReadOnly(false);
			selectedTagField.setValue(tag==null?"":project.getTagManager().getTagLibrary().getTagPath(tag.getUuid()));
			selectedTagField.setReadOnly(true);
		});
	}

	private void initComponents() {
		setSizeFull();
		setMargin(false);
		setSpacing(true);
		
		tagSelectionPanel = new TagSelectionPanel(project);
		addComponent(tagSelectionPanel);
		setExpandRatio(tagSelectionPanel, 1f);
		selectedTagField = new TextField("Selected Tag");
		selectedTagField.setReadOnly(true);
		addComponent(selectedTagField);
	}
	

}
