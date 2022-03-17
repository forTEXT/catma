package de.catma.ui.module.project;

import java.util.Collection;
import java.util.stream.Collectors;

import com.vaadin.ui.ComboBox;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;

import de.catma.tag.TagsetMetadata;
import de.catma.ui.dialog.AbstractOkCancelDialog;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.user.Member;

public class EditTagsetDialog extends AbstractOkCancelDialog<TagsetMetadata> {
	
	private TextField nameField;
	private TextArea descriptionField;
	private TagsetMetadata tagsetMetadata;
	private ComboBox<Member> responsableBox;

	public EditTagsetDialog(
			TagsetMetadata tagsetMetadata,
			Collection<Member> projectMembers,
			SaveCancelListener<TagsetMetadata> saveCancelListener) {
		super("Edit Tagset metadata", saveCancelListener);
		this.tagsetMetadata = tagsetMetadata;
		initComponents(tagsetMetadata, projectMembers);
	}
	
	private void initComponents(TagsetMetadata tagsetMetadata, Collection<Member> projectMembers) {
		nameField = new TextField("Name");
		if (tagsetMetadata.getName() != null) {
			nameField.setValue(tagsetMetadata.getName());
		}

		descriptionField = new TextArea("Description");
		if (tagsetMetadata.getDescription() != null) {
			descriptionField.setValue(tagsetMetadata.getDescription());
		}

		Member currentMember = null; 
		if (tagsetMetadata.getResponsableUser() != null) {
			currentMember = 
				projectMembers.stream()
				.filter(member -> member.getIdentifier().equals(tagsetMetadata.getResponsableUser()))
				.findFirst()
				.orElse(null);
		}
		if (projectMembers != null) {
			responsableBox = new ComboBox<Member>(
					"Responsable member", projectMembers.stream().sorted().collect(Collectors.toList()));
			responsableBox.setEmptySelectionAllowed(true);
			
			if (currentMember != null) {
				responsableBox.setValue(currentMember);
			}
		}
	}

	@Override
	protected void addContent(ComponentContainer content) {
		content.addComponent(nameField);
		nameField.setWidth("75%");
		content.addComponent(descriptionField);
		descriptionField.setWidth("75%");
		if (responsableBox != null) {
			content.addComponent(responsableBox);
			responsableBox.setWidth("75%");
		}
	}

	@Override
	protected TagsetMetadata getResult() {
		Member currentMember = null;
		if (responsableBox != null) {
			currentMember = responsableBox.getValue();
		}
		this.tagsetMetadata.setName(trimNonNull(nameField.getValue()));
		this.tagsetMetadata.setDescription(trimNonNull(descriptionField.getValue()));
		this.tagsetMetadata.setResponsableUser(
			currentMember==null?null:currentMember.getIdentifier());
		return this.tagsetMetadata;
	}

	private String trimNonNull(String value) {
		if (value != null) {
			return value.trim();
		}
		
		return value;
	}
}
