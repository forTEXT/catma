package de.catma.ui.repository.wizard;

import com.vaadin.data.Container;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Field;

import de.catma.document.source.FileType;
import de.catma.ui.field.FieldMapper;
import de.catma.ui.field.TableFieldGenerator;
import de.catma.ui.repository.wizard.FileTypeFieldFactory.FileTypeCharsetFieldConnectorFactory;
import de.catma.ui.repository.wizard.FileTypePanel.FileTypeCharsetValueChangeListener;

public class FileTypeFieldGenerator implements TableFieldGenerator {

	private FieldMapper fieldMapper;
	private FileTypeCharsetFieldConnectorFactory fileTypeCharsetFieldConnectorFactory;
	private FileTypeCharsetValueChangeListener fileTypeCharsetValueChangeListener;

	public FileTypeFieldGenerator(
			FieldMapper fieldMapper, 
			FileTypeCharsetFieldConnectorFactory fileTypeCharsetFieldConnectorFactory, 
			FileTypeCharsetValueChangeListener fileTypeCharsetValueChangeListener) {
		this.fieldMapper = fieldMapper;
		this.fileTypeCharsetFieldConnectorFactory = fileTypeCharsetFieldConnectorFactory;
		this.fileTypeCharsetValueChangeListener = fileTypeCharsetValueChangeListener;
	}

	@Override
	public Field<?> createField(Container container, Object itemId, Component uiContext) {
		
    	ComboBox comboBox = new ComboBox(null, FileType.getActiveFileTypes());
    	comboBox.setNullSelectionAllowed(false);
    	comboBox.setImmediate(true);
    	
    	comboBox.addValueChangeListener(fileTypeCharsetValueChangeListener);

		fieldMapper.registerField(
				itemId, 
				"sourceDocumentInfo.techInfoSet.fileType", 
				comboBox);
	
		fieldMapper.connectFields(
			itemId, 
			"sourceDocumentInfo.techInfoSet.fileType", 
			"sourceDocumentInfo.techInfoSet.charset", 
			fileTypeCharsetFieldConnectorFactory);
	
		return comboBox;
	}

}
