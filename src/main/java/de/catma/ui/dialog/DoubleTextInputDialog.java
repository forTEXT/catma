
package de.catma.ui.dialog;

import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.TextField;

import de.catma.util.Pair;

public class DoubleTextInputDialog extends AbstractOkCancelDialog<Pair<String,String>> {
	
	private String inputLabel1;
	private String inputLabel2; 
	private TextField textInput1;
	private TextField textInput2;
	private String initialValue1;
	private String initialValue2;

	public DoubleTextInputDialog(
			String caption, 
			String inputLabel1, 
			String inputLabel2,
			SaveCancelListener<Pair<String,String>> saveCancelListener) {
		this(caption, inputLabel1, inputLabel2, null, null, saveCancelListener);
	}
	
	public DoubleTextInputDialog(
			String caption, 
			String inputLabel1,
			String inputLabel2,
			String initialValue1,
			String initialValue2,
			SaveCancelListener<Pair<String,String>> saveCancelListener) {
		super(caption, saveCancelListener);
		this.inputLabel1 = inputLabel1;
		this.inputLabel2 = inputLabel2;
		this.initialValue1 = initialValue1;
		this.initialValue2 = initialValue2;
	}

	@Override
	protected void addContent(ComponentContainer content) {
		this.textInput1 = new TextField(inputLabel1);
		this.textInput1.focus();
		this.textInput1.setWidth("100%");
		if (initialValue1 != null) {
			this.textInput1.setValue(initialValue1);
		}
		content.addComponent(textInput1);
		
		this.textInput2 = new TextField(inputLabel2);
		this.textInput2.setWidth("100%");
		if (initialValue2 != null) {
			this.textInput2.setValue(initialValue2);
		}
		content.addComponent(textInput2);		
	}
	

	@Override
	protected Pair<String,String> getResult() {
		String value1 = null;
		if (textInput1.getValue() != null) {
			value1 = textInput1.getValue().toString(); 
		}
		String value2 = null;
		if (textInput2.getValue() != null) {
			value2 = textInput2.getValue().toString();
		}
		return new Pair<>(value1, value2);
	}

}
