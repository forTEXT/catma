package de.catma.ui.repository.wizard;

import java.nio.charset.Charset;

import com.vaadin.data.Container;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Field;

import de.catma.document.source.FileType;
import de.catma.ui.field.FieldMapper;
import de.catma.ui.field.TableFieldGenerator;
import de.catma.ui.repository.wizard.FileTypeFieldFactory.FileTypeCharsetFieldConnectorFactory;
import de.catma.ui.repository.wizard.FileTypePanel.FileTypeCharsetValueChangeListener;

public class CharsetFieldGenerator implements TableFieldGenerator {

	private FieldMapper fieldMapper;
	private FileTypeCharsetFieldConnectorFactory fileTypeCharsetFieldConnectorFactory;
	private FileTypeCharsetValueChangeListener fileTypeCharsetValueChangeListener;

	public CharsetFieldGenerator(
			FieldMapper fieldMapper,
			FileTypeCharsetFieldConnectorFactory fileTypeCharsetFieldConnectorFactory, 
			FileTypeCharsetValueChangeListener fileTypeCharsetValueChangeListener) {
		this.fieldMapper = fieldMapper;
		this.fileTypeCharsetFieldConnectorFactory = fileTypeCharsetFieldConnectorFactory;
		this.fileTypeCharsetValueChangeListener = fileTypeCharsetValueChangeListener;
	}

	@Override
	public Field<?> createField(Container container, Object itemId, Component uiContext) {
    	ComboBox comboBox = new ComboBox(null, Charset.availableCharsets().values());
    	comboBox.setNullSelectionAllowed(false);
    	comboBox.setImmediate(true);

    	comboBox.addValueChangeListener(fileTypeCharsetValueChangeListener);

    	SourceDocumentResult sdr = (SourceDocumentResult)itemId;
    	FileType fileType = sdr.getSourceDocumentInfo().getTechInfoSet().getFileType();
    	if ((fileType == null)
    			|| (!fileType.isCharsetSupported())) {
    		comboBox.setEnabled(false);
    	}
    	
		fieldMapper.registerField(
				itemId, 
				"sourceDocumentInfo.techInfoSet.charset",  //$NON-NLS-1$
				comboBox);
	
		fieldMapper.connectFields(
			itemId, 
			"sourceDocumentInfo.techInfoSet.fileType",  //$NON-NLS-1$
			"sourceDocumentInfo.techInfoSet.charset",  //$NON-NLS-1$
			fileTypeCharsetFieldConnectorFactory);
	
		return comboBox;
	}

}
