package de.catma.ui.analyzer.querybuilder;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;

import de.catma.queryengine.querybuilder.WildcardBuilder;

public class WordPanel extends GridLayout{
	
	public static interface WordChangeListener {
		public void wordChanged();
	}

	private static class PositionItem {
		private int position;
		private String displayString;
		private PositionItem(int pos, String displayString) {
			this.position = pos;
			this.displayString = displayString;
		}
		
		@Override
		public String toString() {
			return displayString;
		}
		
		public int getPosition() {
			return position;
		}
	}
	private TextField startsWithField;
	private TextField containsField;
	private TextField endsWithField;
	private TextField exactField;
	private ComboBox positionBox;
	private Button btRemove;
	private WildcardBuilder wildcardBuilder;
	private boolean withPositionBox;

	public WordPanel() {
		this(false, null);
	}
	
	public WordPanel(
			boolean withPositionBox, List<WordPanel> wordPanelList) {
		super(2,withPositionBox?6:4);
		this.wildcardBuilder = new WildcardBuilder();
		this.withPositionBox = withPositionBox;
		initComponents();
		initActions(wordPanelList);
	}

	private void initActions(
			final List<WordPanel> wordPanelList) {
		if (withPositionBox) {
			btRemove.addListener(new ClickListener() {
				
				public void buttonClick(ClickEvent event) {
					((ComponentContainer)WordPanel.this.getParent()).removeComponent(
							WordPanel.this);
					wordPanelList.remove(WordPanel.this);
				}
			});
		}
		exactField.addListener(new ValueChangeListener() {
			
			public void valueChange(ValueChangeEvent event) {
				String value = (String)event.getProperty().getValue();
				if ((value != null) && (!value.isEmpty())){
					setWildcardInputFieldsEnabled(false);
				}
				else {
					setWildcardInputFieldsEnabled(true);
				}
			}
		});
	}

	private void setWildcardInputFieldsEnabled(boolean enabled) {
		startsWithField.setEnabled(enabled);
		containsField.setEnabled(enabled);
		endsWithField.setEnabled(enabled);		
	}

	private void initComponents() {
		setSpacing(true);
		setMargin(true);
		setSizeFull();
		
		String next = " first ";
		if (withPositionBox) {
			next = " next ";
		}
		Label startsWithLabel = new Label("The"+next+"word starts with");
		addComponent(startsWithLabel, 0, 0);
		startsWithField = new TextField();
		startsWithField.setImmediate(true);
		addComponent(startsWithField, 1, 0);
		
		Label containsLabel = new Label("The"+next+"word contains");
		addComponent(containsLabel, 0, 1);
		containsField = new TextField();
		containsField.setImmediate(true);
		addComponent(containsField, 1, 1);
		
		Label endsWithLabel = new Label("The"+next+"word ends with");
		addComponent(endsWithLabel, 0, 2);
		endsWithField = new TextField();
		endsWithField.setImmediate(true);
		addComponent(endsWithField, 1, 2);
		
		Label exactLabel = new Label("The"+next+"word is exactly");
		addComponent(exactLabel, 0, 3);
		exactField = new TextField();
		exactField.setImmediate(true);
		addComponent(exactField, 1, 3);
		
		if (withPositionBox) {
			List<PositionItem> options = new ArrayList<PositionItem>();
			for (int i=1; i<=10; i++) {
				options.add(
					new PositionItem(
						i, i+" word"+(i==1?"":"s")+" after the previous word"));
			}
			Label positionLabel = new Label("The position of this word is");
			addComponent(positionLabel, 0, 4);
			
			positionBox =
					new ComboBox("", options);
			positionBox.setImmediate(true);
			addComponent(positionBox, 1, 4);
			positionBox.setNullSelectionAllowed(false);
			positionBox.setNewItemsAllowed(false);
			positionBox.setValue(options.get(0));
			
			btRemove = new Button("Remove");
			addComponent(btRemove, 0, 5, 1, 5);
			setComponentAlignment(btRemove, Alignment.MIDDLE_CENTER);
		}
	}
	
	public String getWildcardWord() {
		String exactValue = (String)exactField.getValue();
		if ((exactValue != null) && !exactValue.isEmpty()) {
			return exactValue;
		}
		return wildcardBuilder.getWildcardFor(
			(String)startsWithField.getValue(), 
			(String)containsField.getValue(), 
			(String)endsWithField.getValue(), 
			(withPositionBox?((PositionItem)positionBox.getValue()).getPosition():1));
	}
}
