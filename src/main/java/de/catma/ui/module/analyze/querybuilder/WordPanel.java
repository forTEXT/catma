/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009-2013  University Of Hamburg
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.catma.ui.module.analyze.querybuilder;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import com.vaadin.data.HasValue.ValueChangeEvent;
import com.vaadin.data.HasValue.ValueChangeListener;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextField;

import de.catma.queryengine.querybuilder.WildcardBuilder;
import de.catma.ui.component.IconButton;

public class WordPanel extends FormLayout {
	
	public static interface WordConfigurationChangedListener {
		public void wordConfigurationChanged(WordPanel wordPanel);
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
	private ComboBox<PositionItem> positionBox;
	private Button btRemove;
	private WildcardBuilder wildcardBuilder;
	private boolean withPositionBox;

	public WordPanel(WordConfigurationChangedListener wordConfigurationChangedListener) {
		this(false, wordConfigurationChangedListener);
	}
	
	public WordPanel(
			boolean withPositionBox, 
			WordConfigurationChangedListener wordConfigurationChangedListener) {
		this.wildcardBuilder = new WildcardBuilder();
		this.withPositionBox = withPositionBox;
		initComponents();
		initActions(wordConfigurationChangedListener);
	}

	private void initActions(
			WordConfigurationChangedListener wordConfigurationChangedListener) {
		if (withPositionBox) {
			btRemove.addClickListener(new ClickListener() {
				
				public void buttonClick(ClickEvent event) {
					((ComponentContainer)WordPanel.this.getParent()).removeComponent(
							WordPanel.this);
					wordConfigurationChangedListener.wordConfigurationChanged(WordPanel.this);
				}
			});
		}
		exactField.addValueChangeListener(new ValueChangeListener<String>() {
			
			public void valueChange(ValueChangeEvent<String> event) {
				String value = event.getValue();
				if ((value != null) && (!value.isEmpty())){
					setWildcardInputFieldsEnabled(false);
				}
				else {
					setWildcardInputFieldsEnabled(true);
				}
			}
		});
		startsWithField.addValueChangeListener(event -> wordConfigurationChangedListener.wordConfigurationChanged(this));
		containsField.addValueChangeListener(event -> wordConfigurationChangedListener.wordConfigurationChanged(this));
		endsWithField.addValueChangeListener(event -> wordConfigurationChangedListener.wordConfigurationChanged(this));
		exactField.addValueChangeListener(event -> wordConfigurationChangedListener.wordConfigurationChanged(this));
		if (positionBox != null) {
			positionBox.addValueChangeListener(event -> wordConfigurationChangedListener.wordConfigurationChanged(this));
		}
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
		
		String next = "first ";  
		if (withPositionBox) {
			next = "next "; 
		}

		startsWithField = new TextField();
		startsWithField.setCaption(MessageFormat.format("The {0} word starts with", next)); 
		addComponent(startsWithField);
		
		containsField = new TextField();
		containsField.setCaption(MessageFormat.format("The {0} word contains", next)); 
		addComponent(containsField);
		
		endsWithField = new TextField();
		endsWithField.setCaption(MessageFormat.format("The {0} word ends with", next)); 
		addComponent(endsWithField);
		
		exactField = new TextField();
		exactField.setCaption(MessageFormat.format("The {0} word is exactly", next)); 
		addComponent(exactField);
		
		if (withPositionBox) {
			List<PositionItem> options = new ArrayList<PositionItem>();
			for (int i=1; i<=10; i++) {
				options.add(
					new PositionItem(
						i, MessageFormat.format("{0,number,integer} {1} after the previous word",    
							i, 
							((i==1)?"word":"words")))); 
			}
			
			positionBox =
					new ComboBox<PositionItem>("The position of this word is", options); 
			
			addComponent(positionBox);
			positionBox.setEmptySelectionAllowed(false);
			positionBox.setValue(options.get(0));
			
			btRemove = new IconButton(VaadinIcons.ERASER); 
			addComponent(btRemove);
			setComponentAlignment(btRemove, Alignment.MIDDLE_CENTER);
		}
	}
	
	public String getWildcardWord() {
		String exactValue = (String)exactField.getValue();
		if ((exactValue != null) && !exactValue.isEmpty()) {
			if (withPositionBox) {
				return wildcardBuilder.getWildcardFor(exactValue, ((PositionItem)positionBox.getValue()).getPosition());
			}
			return exactValue;
		}
		return wildcardBuilder.getWildcardFor(
			(String)startsWithField.getValue(), 
			(String)containsField.getValue(), 
			(String)endsWithField.getValue(), 
			(withPositionBox?((PositionItem)positionBox.getValue()).getPosition():1));
	}
}
