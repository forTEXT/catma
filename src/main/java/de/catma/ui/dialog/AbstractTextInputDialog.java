
package de.catma.ui.dialog;

import java.util.stream.Collectors;

import com.google.common.base.Joiner;
import com.vaadin.data.Binder;
import com.vaadin.data.ValidationException;
import com.vaadin.data.Validator;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.AbstractTextField;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;


public abstract class AbstractTextInputDialog<T extends AbstractTextField> extends AbstractOkCancelDialog<String> {
	private static class ValueContainer {
		String value;
		public void setValue(String value) {
			this.value = value;
		}
		public String getValue() {
			return value;
		}
	}
	
	private final String inputLabel;
	private final T textInput;
	private final String initialValue;
	private final Binder<ValueContainer> inputBinder = new Binder<>();

	public AbstractTextInputDialog(
			String caption, 
			String inputLabel, 
			SaveCancelListener<String> saveCancelListener) {
		this(caption, inputLabel, null, saveCancelListener);
	}

	public AbstractTextInputDialog(
			String caption, 
			String inputLabel, 
			String initialValue,
			SaveCancelListener<String> saveCancelListener) {
		this(caption, inputLabel, initialValue, saveCancelListener, (Validator<String>[])null);
	}
	
	@SafeVarargs
	public AbstractTextInputDialog(
			String caption, 
			String inputLabel, 
			String initialValue,
			SaveCancelListener<String> saveCancelListener, 
			Validator<String>... validators) {
		super(caption, saveCancelListener);
		this.inputLabel = inputLabel;
		this.initialValue = initialValue;
		this.textInput = createTextField(inputLabel);
		
		var bindingBuilder = inputBinder.forField(textInput);
		if (validators != null) {
			for (Validator<String> validator : validators) {
				bindingBuilder = bindingBuilder.withValidator(validator);
			}
 		}

		bindingBuilder.bind(ValueContainer::getValue, ValueContainer::setValue);
	}

	protected abstract T createTextField(String inputLabel);

	@Override
	protected void addContent(ComponentContainer content) {
		this.textInput.focus();
		this.textInput.setWidth("100%");
		if (initialValue != null) {
			this.textInput.setValue(initialValue);
		}
		content.addComponent(textInput);
		if (content instanceof AbstractOrderedLayout) {
			((AbstractOrderedLayout)content).setExpandRatio(textInput, 1.0f);
		}
	}
	
	@Override
	public void attach() {
		super.attach();
//		((FocusHandler)UI.getCurrent()).focusDeferred(textInput);
	}
	
	@Override
	protected void handleOkPressed() {
		var valueContainer = new ValueContainer();
		try {
			inputBinder.writeBean(valueContainer);
			super.handleOkPressed();
		} catch (ValidationException e) {
			Notification.show(
					Joiner
					.on("\n")
					.join(
							e.getValidationErrors().stream()
							.map(msg -> msg.getErrorMessage())
							.collect(Collectors.toList())),Type.ERROR_MESSAGE);
		}	}

	@Override
	protected String getResult() {
		if (textInput.getValue() == null) {
			return null;
		}
		return textInput.getValue().toString();
	}
	
}
