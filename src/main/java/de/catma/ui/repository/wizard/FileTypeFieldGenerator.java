package de.catma.ui.repository.wizard;

import com.vaadin.v7.data.Container;
import com.vaadin.v7.data.Property;
import com.vaadin.v7.data.Property.ValueChangeEvent;
import com.vaadin.v7.data.Property.ValueChangeListener;
import com.vaadin.v7.data.util.ObjectProperty;
import com.vaadin.v7.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.v7.ui.Field;

import de.catma.document.source.FileType;
import de.catma.ui.field.FieldMapper;
import de.catma.ui.field.TableFieldGenerator;
import de.catma.ui.repository.wizard.FileTypeFieldFactory.FileTypeCharsetFieldConnectorFactory;
import de.catma.ui.repository.wizard.FileTypePanel.FileTypeCharsetValueChangeListener;

public class FileTypeFieldGenerator implements TableFieldGenerator {

	private FieldMapper fieldMapper;
	private FileTypeCharsetFieldConnectorFactory fileTypeCharsetFieldConnectorFactory;
	private FileTypeCharsetValueChangeListener fileTypeCharsetValueChangeListener;
	private ValueChangeListener fileTypeValueChangeListener;

	public FileTypeFieldGenerator(
			FieldMapper fieldMapper, 
			FileTypeCharsetFieldConnectorFactory fileTypeCharsetFieldConnectorFactory, 
			FileTypeCharsetValueChangeListener fileTypeCharsetValueChangeListener,
			ValueChangeListener fileTypeValueChangeListener) {
		
		this.fieldMapper = fieldMapper;
		this.fileTypeCharsetFieldConnectorFactory = fileTypeCharsetFieldConnectorFactory;
		this.fileTypeCharsetValueChangeListener = fileTypeCharsetValueChangeListener;
		this.fileTypeValueChangeListener = fileTypeValueChangeListener; 
	}

	@Override
	public Field<?> createField(Container container, Object itemId, Component uiContext) {
		
    	ComboBox comboBox = new ComboBox(null, FileType.getActiveFileTypes());
    	comboBox.setNullSelectionAllowed(false);
    	comboBox.setImmediate(true);
    	
    	comboBox.addValueChangeListener(fileTypeCharsetValueChangeListener);
    	comboBox.addValueChangeListener(new ValueChangeListener() {
			
			@Override
			public void valueChange(ValueChangeEvent event) {
				fileTypeValueChangeListener.valueChange(new ValueChangeEvent() {
					
					@Override
					public Property getProperty() {
						return new ObjectProperty<Object>(itemId);
					}
				});
			}
		});
    	
    	
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
