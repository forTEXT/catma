
package de.catma.ui.dialog;

import com.vaadin.data.Validator;
import com.vaadin.ui.TextArea;


public class TextAreaInputDialog extends AbstractTextInputDialog<TextArea> {

	public TextAreaInputDialog(String caption, String inputLabel, SaveCancelListener<String> saveCancelListener) {
		super(caption, inputLabel, saveCancelListener);
	}

	public TextAreaInputDialog(String caption, String inputLabel, String initialValue,
			SaveCancelListener<String> saveCancelListener, Validator<String>... validators) {
		super(caption, inputLabel, initialValue, saveCancelListener, validators);
	}

	public TextAreaInputDialog(String caption, String inputLabel, String initialValue,
			SaveCancelListener<String> saveCancelListener) {
		super(caption, inputLabel, initialValue, saveCancelListener);
	}

	@Override
	protected TextArea createTextField(String inputLabel) {
		TextArea textArea = new TextArea(inputLabel);
		textArea.setHeightFull();
		return textArea;
	}
	
	

}
