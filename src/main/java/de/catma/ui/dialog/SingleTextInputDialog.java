package de.catma.ui.dialog;

import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.TextField;

public class SingleTextInputDialog extends AbstractOkCancelDialog<String> {
	
	private String inputLabel;
	private TextField textInput;

	public SingleTextInputDialog(
			String caption, 
			String inputLabel, 
			SaveCancelListener<String> saveCancelListener) {
		super(caption, saveCancelListener);
		this.inputLabel = inputLabel;
	}

	@Override
	protected void addContent(ComponentContainer content) {
		this.textInput = new TextField(inputLabel);
		this.textInput.focus();
		content.addComponent(textInput);
	}
	
	@Override
	public void attach() {
		super.attach();
//		((FocusHandler)UI.getCurrent()).focusDeferred(textInput);
	}

	@Override
	protected String getResult() {
		if (textInput.getValue() == null) {
			return null;
		}
		return textInput.getValue().toString();
	}

}
