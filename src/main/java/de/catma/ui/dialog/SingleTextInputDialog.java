
package de.catma.ui.dialog;

import com.vaadin.data.Validator;
import com.vaadin.ui.TextField;


public class SingleTextInputDialog extends AbstractTextInputDialog<TextField> {

	public SingleTextInputDialog(String caption, String inputLabel, SaveCancelListener<String> saveCancelListener) {
		super(caption, inputLabel, saveCancelListener);
	}

	public SingleTextInputDialog(String caption, String inputLabel, String initialValue,
			SaveCancelListener<String> saveCancelListener, Validator<String>... validators) {
		super(caption, inputLabel, initialValue, saveCancelListener, validators);
	}

	public SingleTextInputDialog(String caption, String inputLabel, String initialValue,
			SaveCancelListener<String> saveCancelListener) {
		super(caption, inputLabel, initialValue, saveCancelListener);
	}

	@Override
	protected TextField createTextField(String inputLabel) {
		return new TextField(inputLabel);
	}
	
	

}
