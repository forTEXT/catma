package de.catma.ui.dialog;

import java.util.Collection;

import com.vaadin.ui.ComboBox;
import com.vaadin.ui.ComponentContainer;

public class SingleOptionInputDialog<T> extends AbstractOkCancelDialog<T> {
	
	private String inputLabel;
	private ComboBox<T> cbItem;
	private T initialValue;
	private Collection<T> items;

	public SingleOptionInputDialog(
			String caption, 
			String inputLabel, 
			Collection<T> items,
			SaveCancelListener<T> saveCancelListener) {
		this(caption, inputLabel, items, null, saveCancelListener);
	}
	
	public SingleOptionInputDialog(
			String caption, 
			String inputLabel, 
			Collection<T> items,
			T initialValue,
			SaveCancelListener<T> saveCancelListener) {
		super(caption, saveCancelListener);
		this.items = items;
		this.inputLabel = inputLabel;
		this.initialValue = initialValue;
	}

	@Override
	protected void addContent(ComponentContainer content) {
		this.cbItem = new ComboBox<T>(inputLabel, items);
		this.cbItem.focus();
		this.cbItem.setWidth("100%");
		if (initialValue != null) {
			this.cbItem.setValue(initialValue);
		}
		content.addComponent(cbItem);
	}
	
	@Override
	public void attach() {
		super.attach();
//		((FocusHandler)UI.getCurrent()).focusDeferred(textInput);
	}

	@Override
	protected T getResult() {
		if (cbItem.getValue() == null) {
			return null;
		}
		return cbItem.getValue();
	}

}