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
package de.catma.ui.repository.wizard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.TreeSet;

import com.vaadin.data.Property;
import com.vaadin.data.Container.ItemSetChangeEvent;
import com.vaadin.data.Container.ItemSetChangeListener;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.AbstractTextField.TextChangeEventMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.ListSelect;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import de.catma.document.source.IndexInfoSet;
import de.catma.document.source.LanguageDetector;
import de.catma.document.source.LanguageItem;
import de.catma.ui.CatmaApplication;
import de.catma.ui.dialog.wizard.DynamicWizardStep;
import de.catma.ui.dialog.wizard.WizardStepListener;

class IndexerOptionsPanel extends GridLayout implements DynamicWizardStep {
	
//    /**
//     * An item in the language list.
//     */
//    private static class LanguageItem implements Comparable<LanguageItem> {
//        private Locale locale;
//
//        /**
//         * Constructor
//         * @param locale the locale of this item
//         */
//        public LanguageItem(Locale locale) {
//            this.locale = locale;
//        }
//
//        /**
//         * @return the locale of this item
//         */
//        public Locale getLocale() {
//            return locale;
//        }
//
//        @Override
//        public String toString() {
//            return locale.getDisplayLanguage()
//                + (locale.getDisplayCountry().isEmpty()? "" : "-" + locale.getDisplayCountry());
//        }
//
//        /**
//         * Compares by string representation of the item.
//         */
//        public int compareTo(LanguageItem o) {
//            return this.toString().compareTo(o.toString());
//        }
//
//		@Override
//		public int hashCode() {
//			return locale.hashCode();
//		}
//
//		@Override
//		public boolean equals(Object obj) {
//			if (obj instanceof LanguageItem) {
//				return locale.equals(((LanguageItem)obj).locale);
//			}
//			return false;
//		}
//    }

    private static final char APOSTROPHE = '\'';
    
    private ArrayList<LanguageItem> languageItems;
	
	private boolean onAdvance;
	private WizardStepListener wizardStepListener;
	private AddSourceDocWizardResult wizardResult;
	
	private Table table;
	
	private CheckBox cbUseApostrophe;
	private CheckBox cbAdvanceOptions;
	
	private ListSelect languagesListSelect;

	private TreeSet<LanguageItem> sortedLangs;

	private ListSelect unseparableCharacterSequencesListSelect;

	private Button btLoadUcsList;

	private Button btSaveUcsList;

	private Button btAddUcs;

	private TextField tfUcs;

	private Button btRemoveUcs;
	
	private HorizontalLayout ucsAddRemoveLayout;
	
	private VerticalLayout loadSavePanel;

	public IndexerOptionsPanel(WizardStepListener wizardStepListener,
			AddSourceDocWizardResult wizardResult) {
		super(3,6);
		this.onAdvance = true;
		this.wizardStepListener = wizardStepListener;
		this.wizardResult = wizardResult;
		initComponents();
		initActions();
	}

	private void initActions() {
		
		cbUseApostrophe.setVisible(false);
		unseparableCharacterSequencesListSelect.setVisible(false);
		loadSavePanel.setVisible(false);
		ucsAddRemoveLayout.setVisible(false);

		this.languagesListSelect.addValueChangeListener(new ValueChangeListener() {
			
			public void valueChange(ValueChangeEvent event) {
				onAdvance = (languagesListSelect.getValue() != null);
				if (languagesListSelect.getValue() != null) {
					
					wizardResult.getSourceDocumentInfo().getIndexInfoSet().setLocale(
							((LanguageItem)languagesListSelect.getValue()).getLocale());
				}
				wizardStepListener.stepChanged(IndexerOptionsPanel.this);
			}
		});
		
		this.tfUcs.addTextChangeListener(new TextChangeListener() {
			public void textChange(TextChangeEvent event) {
				btAddUcs.setEnabled(
					(event.getText() != null) 
					&& (!event.getText().isEmpty()));
			}
		});
		
		this.tfUcs.addValueChangeListener(new ValueChangeListener() {
			
			public void valueChange(ValueChangeEvent event) {
				addUcs(tfUcs.getValue().toString());
			}
		});
		
		this.unseparableCharacterSequencesListSelect.addItemSetChangeListener(new ItemSetChangeListener() {
			public void containerItemSetChange(ItemSetChangeEvent event) {
				btRemoveUcs.setEnabled(
					unseparableCharacterSequencesListSelect.getContainerDataSource().size() > 0);
			}
		});
		
		btAddUcs.addClickListener(new ClickListener() {
			public void buttonClick(ClickEvent event) {
				addUcs(tfUcs.getValue().toString());
			}
		});
		
		btRemoveUcs.addClickListener(new ClickListener() {
			public void buttonClick(ClickEvent event) {
				unseparableCharacterSequencesListSelect.removeItem(
						unseparableCharacterSequencesListSelect.getValue());
				wizardResult.getSourceDocumentInfo().getIndexInfoSet().removeUnseparableCharacterSequence(
						tfUcs.getValue().toString());
				if (!wizardResult.getSourceDocumentInfo().getIndexInfoSet().getUnseparableCharacterSequences().isEmpty()) {
					unseparableCharacterSequencesListSelect.setValue(
							wizardResult.getSourceDocumentInfo().getIndexInfoSet().getUnseparableCharacterSequences().get(0));
				}
			}
		});
		
		cbUseApostrophe.addValueChangeListener(new ValueChangeListener() {
			
			public void valueChange(ValueChangeEvent event) {
				
				if (cbUseApostrophe.getValue() 
						&& (wizardResult.getSourceDocumentInfo().getIndexInfoSet().getUserDefinedSeparatingCharacters().isEmpty())) {
					wizardResult.getSourceDocumentInfo().getIndexInfoSet().addUserDefinedSeparatingCharacter(APOSTROPHE);
				}
				else {
					wizardResult.getSourceDocumentInfo().getIndexInfoSet().removeUserDefinedSeparatingCharacter(APOSTROPHE);
				}
			}
		});
		
		cbAdvanceOptions.addValueChangeListener(new ValueChangeListener() {
			
			public void valueChange(ValueChangeEvent event) {
				
				if (cbAdvanceOptions.getValue()) {
					cbUseApostrophe.setVisible(true);
					unseparableCharacterSequencesListSelect.setVisible(true);
					loadSavePanel.setVisible(true);
					ucsAddRemoveLayout.setVisible(true);
				}
				else {
					cbUseApostrophe.setVisible(false);
					unseparableCharacterSequencesListSelect.setVisible(false);
					loadSavePanel.setVisible(false);
					ucsAddRemoveLayout.setVisible(false);
				}
			}
		});
	}

	private void initComponents() {
		setSpacing(true);
		setMargin(true);
		
		setSizeFull();
		
		Locale[] availableLocales = Locale.getAvailableLocales();
		languageItems = new ArrayList<LanguageItem>();
		for (Locale locale : availableLocales) {
			languageItems.add(new LanguageItem(locale));
		}
		
		BeanItemContainer<SourceDocumentResult> container = new BeanItemContainer<SourceDocumentResult>(SourceDocumentResult.class);
		container.addNestedContainerProperty("sourceDocumentInfo.techInfoSet.fileName");
		container.addNestedContainerProperty("sourceDocumentInfo.indexInfoSet.language");
		
		table = new Table("Documents", container);
		
		//TODO: investigate whether using a FieldFactory would make things easier..
		table.addGeneratedColumn("sourceDocumentInfo.indexInfoSet.language", 
				new ComboBoxColumnGenerator(languageItems, makeComboBoxListenerGenerator())
		);
		
		table.setVisibleColumns(new Object[]{
				"sourceDocumentInfo.techInfoSet.fileName",
				"sourceDocumentInfo.indexInfoSet.language"
		});
		table.setColumnHeaders(new String[]{"File Name", "Language"});
		
		table.setSelectable(true);
		table.setNullSelectionAllowed(false);
		table.setImmediate(true);
		
		addComponent(table);
		
		
	
		Label infoLabel = new Label();
			
		infoLabel.setContentMode(ContentMode.HTML);
		infoLabel.setValue(
				"<p>This section allows you to finetune the creation " +
				"of the word list of your Source Document.</p>" +
				"<p>If you are unsure what to do, just select the language " +
				"and leave everything else unmodified.</p>" +
				"or you can click below for the Expert Features:"
				);
		
		addComponent(infoLabel, 0, 1, 0, 1);
		
		cbAdvanceOptions = new CheckBox("Advanced Options");
		
		addComponent(cbAdvanceOptions, 0, 2, 2, 2);
		
		cbUseApostrophe = new CheckBox("always use the apostrophe as a word separator");
		
		addComponent(cbUseApostrophe, 0, 5);
					
        Locale[] all = Locale.getAvailableLocales();

        sortedLangs = new TreeSet<LanguageItem>();
        for (Locale locale : all) {
            sortedLangs.add(new LanguageItem(locale));
        }

        languagesListSelect = 
        		new ListSelect(
        				"Please select the predominant language of the Source Document:", 
        				sortedLangs);
        languagesListSelect.setNullSelectionAllowed(false);
        languagesListSelect.setSizeFull();
        languagesListSelect.setImmediate(true);
        
        addComponent(languagesListSelect, 0, 3, 0, 4);
               
        unseparableCharacterSequencesListSelect = 
        		new ListSelect("Unseparable character sequences:");
        unseparableCharacterSequencesListSelect.setNullSelectionAllowed(false);
        unseparableCharacterSequencesListSelect.setSizeFull();
        unseparableCharacterSequencesListSelect.setImmediate(true);
        
        addComponent(unseparableCharacterSequencesListSelect, 1, 3, 1, 4);
        
        ucsAddRemoveLayout = new HorizontalLayout();
        Panel ucsAddRemovePanel = new Panel(ucsAddRemoveLayout);
        ucsAddRemovePanel.setStyleName("no-border");
        ucsAddRemoveLayout.setSpacing(true);
        ucsAddRemoveLayout.setSizeFull();
        
        btAddUcs = new Button("Add entry");
        btAddUcs.setEnabled(false);
        ucsAddRemoveLayout.addComponent(btAddUcs);
        tfUcs = new TextField();
        tfUcs.setInputPrompt("Add things like 'e.g.' as you see fit.");
        tfUcs.setImmediate(true);
        tfUcs.setTextChangeEventMode(TextChangeEventMode.EAGER);
        tfUcs.setWidth("100%");
        
        ucsAddRemoveLayout.addComponent(tfUcs);
        ucsAddRemoveLayout.setExpandRatio(tfUcs, 2);
        btRemoveUcs = new Button("Remove entry");
        btRemoveUcs.setEnabled(false);
        ucsAddRemoveLayout.addComponent(btRemoveUcs);
        
        addComponent(ucsAddRemovePanel, 1, 5);

        loadSavePanel = new VerticalLayout();
        loadSavePanel.setSpacing(true);
        loadSavePanel.setWidth("80px");
        
        btLoadUcsList = new Button("Load list");
        loadSavePanel.addComponent(btLoadUcsList);
        btSaveUcsList = new Button("Save list");
        loadSavePanel.addComponent(btSaveUcsList);

        addComponent(loadSavePanel, 2, 3);
        
        setColumnExpandRatio(0, 2);
        setColumnExpandRatio(1, 2);

        setRowExpandRatio(3, 2);
        setRowExpandRatio(4, 2);
	}
	
	private ValueChangeListenerGenerator makeComboBoxListenerGenerator(){
		return new ValueChangeListenerGenerator() {
			public ValueChangeListener generateValueChangeListener(Table source, final Object itemId, Object columnId) {
				return new Property.ValueChangeListener() {
					public void valueChange(ValueChangeEvent event) {
						SourceDocumentResult sdr = (SourceDocumentResult) itemId;
						
						//TODO: useful things
					}
				};
			}
		};
	}

	public Component getContent() {
		return this;
	}

	public boolean onAdvance() {
		return onAdvance;
	}

	public boolean onBack() {
		return true;
	}
	
	@Override
	public String getCaption() {
		return "Wordlist options";
	}

	public void stepActivated(boolean forward) {
		if (!forward) {
			return;
		}
		
		Collection<SourceDocumentResult> sourceDocumentResults = wizardResult.GetSourceDocumentResults();
				
		try {
			LanguageDetector languageDetector = new LanguageDetector();
			
			for (SourceDocumentResult sdr : sourceDocumentResults) {
				IndexInfoSet newIndexInfoSet = new IndexInfoSet();				
				
				Locale locale = languageDetector.getLocale(
					languageDetector.detect(sdr.getSourceDocument().getContent())
				);
				
				LanguageItem detectedLanguage = new LanguageItem(locale);
				if (!languageItems.contains(detectedLanguage)) {
					// Because the LanguageDetector can return a locale that is not present in the languageItems collection
					// we explicitly add it here if it's missing
					// See the comments in https://github.com/catmadevel/catma/commit/cd3e86b61596ce618338b0ab0295f240cbbd6f7f for more details
					languageItems.add(detectedLanguage);
				}

				newIndexInfoSet.setLanguage(detectedLanguage);
				
				sdr.getSourceDocumentInfo().setIndexInfoSet(newIndexInfoSet);
			}
			
			Collections.sort(languageItems); // in case items were added above
			
			BeanItemContainer<SourceDocumentResult> container = (BeanItemContainer<SourceDocumentResult>)table.getContainerDataSource();
			container.addAll(sourceDocumentResults);
			
			if(sourceDocumentResults.size() > 0){
				table.select(sourceDocumentResults.toArray()[0]);
			}
		} catch (IOException e) {
			((CatmaApplication)UI.getCurrent()).showAndLogError(
				"Error during language detection!", e);
		}
	}
	
	private void addUcs(String ucs) {
		if ((ucs != null) && !ucs.isEmpty()) {
			unseparableCharacterSequencesListSelect.addItem(ucs);
			unseparableCharacterSequencesListSelect.setValue(ucs);
			wizardResult.getSourceDocumentInfo().getIndexInfoSet().addUnseparableCharacterSequence(ucs);
			tfUcs.setValue("");
			btAddUcs.setEnabled(false);
		}
	}

	public boolean onFinish() {
		return false;
	}
	
	public boolean onFinishOnly() {
		return false;
	}
	
	public void stepDeactivated(boolean forward){ /* noop */}
	public void stepAdded() {/* noop */}

}
