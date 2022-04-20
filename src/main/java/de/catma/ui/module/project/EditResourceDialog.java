package de.catma.ui.module.project;

import java.util.Collection;
import java.util.stream.Collectors;

import com.vaadin.ui.ComboBox;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.TextField;

import de.catma.document.source.ContentInfoSet;
import de.catma.ui.dialog.AbstractOkCancelDialog;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.user.Member;
import de.catma.util.Pair;

public class EditResourceDialog extends AbstractOkCancelDialog<Pair<String, ContentInfoSet>> {

	private TextField titleField;
	private TextField authorField;
	private TextField descriptionField;
	private TextField publisherField;
	private ComboBox<Member> responsibleBox;
	private ContentInfoSet contentInfoSet;

	public EditResourceDialog(
			String responsibleUser, ContentInfoSet contentInfoSet,
			Collection<Member> projectMembers,
			SaveCancelListener<Pair<String, ContentInfoSet>> saveCancelListener) {
		super("Edit Collection metadata", saveCancelListener);
		this.contentInfoSet = contentInfoSet;
		initComponents(responsibleUser, contentInfoSet, projectMembers);
	}
	
	public EditResourceDialog(ContentInfoSet contentInfoSet,
			SaveCancelListener<Pair<String, ContentInfoSet>> saveCancelListener) {
		super("Edit Document metadata", saveCancelListener);
		this.contentInfoSet = contentInfoSet;
		initComponents(null, contentInfoSet, null);
	}
	

	private void initComponents(String responsibleUser, ContentInfoSet contentInfoSet, Collection<Member> projectMembers) {
		titleField = new TextField("Title");
		if (contentInfoSet.getTitle() != null) {
			titleField.setValue(contentInfoSet.getTitle());
		}
		authorField = new TextField("Author");
		if (contentInfoSet.getAuthor() != null) {
			authorField.setValue(contentInfoSet.getAuthor());
		}
		descriptionField = new TextField("Description");
		if (contentInfoSet.getDescription() != null) {
			descriptionField.setValue(contentInfoSet.getDescription());
		}
		publisherField = new TextField("Publisher");
		if (contentInfoSet.getPublisher() != null) {
			publisherField.setValue(contentInfoSet.getPublisher());
		}
		
		Member currentMember = null; 
		if (responsibleUser != null) {
			currentMember = 
				projectMembers.stream()
				.filter(member -> member.getIdentifier().equals(responsibleUser))
				.findFirst()
				.orElse(null);
		}
		if (projectMembers != null) {
			responsibleBox = new ComboBox<Member>(
					"Responsible member", projectMembers.stream().sorted().collect(Collectors.toList()));
			responsibleBox.setEmptySelectionAllowed(true);
			
			if (currentMember != null) {
				responsibleBox.setValue(currentMember);
			}
		}
	}

	@Override
	protected void addContent(ComponentContainer content) {
		content.addComponent(titleField);
		titleField.setWidth("75%");
		content.addComponent(authorField);
		authorField.setWidth("75%");
		content.addComponent(descriptionField);
		descriptionField.setWidth("75%");
		content.addComponent(publisherField);
		publisherField.setWidth("75%");
		if (responsibleBox != null) {
			content.addComponent(responsibleBox);
			responsibleBox.setWidth("75%");
		}
	}

	@Override
	protected Pair<String, ContentInfoSet> getResult() {
		Member currentMember = null;
		if (responsibleBox != null) {
			currentMember = responsibleBox.getValue();
		}
		this.contentInfoSet.setAuthor(trimNonNull(authorField.getValue()));
		this.contentInfoSet.setTitle(trimNonNull(titleField.getValue()));
		this.contentInfoSet.setDescription(trimNonNull(descriptionField.getValue()));
		this.contentInfoSet.setPublisher(trimNonNull(publisherField.getValue()));
		return new Pair<String, ContentInfoSet>(
			currentMember==null?null:currentMember.getIdentifier(),
			this.contentInfoSet);
	}

	private String trimNonNull(String value) {
		if (value != null) {
			return value.trim();
		}
		
		return value;
	}
}
