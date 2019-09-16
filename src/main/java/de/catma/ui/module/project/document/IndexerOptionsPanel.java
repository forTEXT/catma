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
package de.catma.ui.module.project.document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.v7.data.Container.ItemSetChangeEvent;
import com.vaadin.v7.data.Container.ItemSetChangeListener;
import com.vaadin.v7.data.Property;
import com.vaadin.v7.data.Property.ValueChangeEvent;
import com.vaadin.v7.data.Property.ValueChangeListener;
import com.vaadin.v7.data.util.BeanItemContainer;
import com.vaadin.v7.event.FieldEvents.TextChangeEvent;
import com.vaadin.v7.event.FieldEvents.TextChangeListener;
import com.vaadin.v7.shared.ui.label.ContentMode;
import com.vaadin.v7.ui.AbstractTextField.TextChangeEventMode;
import com.vaadin.v7.ui.CheckBox;
import com.vaadin.v7.ui.ComboBox;
import com.vaadin.v7.ui.HorizontalLayout;
import com.vaadin.v7.ui.Label;
import com.vaadin.v7.ui.ListSelect;
import com.vaadin.v7.ui.Table;
import com.vaadin.v7.ui.TextField;
import com.vaadin.v7.ui.VerticalLayout;
import com.vaadin.v7.ui.themes.Reindeer;

import de.catma.document.source.IndexInfoSet;
import de.catma.document.source.LanguageDetector;
import de.catma.document.source.LanguageItem;
import de.catma.ui.CatmaApplication;
import de.catma.ui.legacy.wizard.DynamicWizardStep;
import de.catma.ui.legacy.wizard.WizardStepListener;

class IndexerOptionsPanel extends Panel implements DynamicWizardStep {
	
    private static final char APOSTROPHE = '\'';
    
    private ArrayList<LanguageItem> languageItems;
	
	private boolean onAdvance;
	private WizardStepListener wizardStepListener;
	private AddSourceDocWizardResult wizardResult;
	
	private Table table;
	
	private CheckBox cbUseApostrophe;
	private CheckBox cbAdvanceOptions;
	
	private ListSelect unseparableCharacterSequencesListSelect;

	private Button btLoadUcsList;

	private Button btSaveUcsList;

	private Button btAddUcs;

	private TextField tfUcs;

	private Button btRemoveUcs;
	
	private HorizontalLayout ucsAddRemoveLayout;
	
	private VerticalLayout loadSavePanel;

	private ComboBox cbApplyToAllLanguageBox;

	private Button btApplyLangToAll;

	public IndexerOptionsPanel(WizardStepListener wizardStepListener,
			AddSourceDocWizardResult wizardResult) {
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
		
		table.addValueChangeListener(new ValueChangeListener() {
			public void valueChange(ValueChangeEvent event) {
				if(table.getValue() == null){
					return;
				}
				
				SourceDocumentResult sdr = (SourceDocumentResult)table.getValue();
				
				BeanItemContainer<String> container = 
						new BeanItemContainer<String>(String.class, 
							sdr.getSourceDocumentInfo().getIndexInfoSet().getUnseparableCharacterSequences());
				
				unseparableCharacterSequencesListSelect.setContainerDataSource(container);
				
				IndexInfoSet indexInfoSet = sdr.getSourceDocumentInfo().getIndexInfoSet();
				boolean apostropheSeperator = indexInfoSet.getUserDefinedSeparatingCharacters().contains(APOSTROPHE);				
				cbUseApostrophe.setValue(apostropheSeperator);
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
				btRemoveUcs.setEnabled(unseparableCharacterSequencesListSelect.getContainerDataSource().size() > 0);
			}
		});
		
		btAddUcs.addClickListener(new ClickListener() {
			public void buttonClick(ClickEvent event) {
				addUcs(tfUcs.getValue().toString());
			}
		});
		
		btRemoveUcs.addClickListener(new ClickListener() {
			public void buttonClick(ClickEvent event) {
				Object ucs = unseparableCharacterSequencesListSelect.getValue();
				unseparableCharacterSequencesListSelect.removeItem(ucs);
				IndexInfoSet indexInfoSet = ((SourceDocumentResult)table.getValue()).getSourceDocumentInfo().getIndexInfoSet();
				indexInfoSet.removeUnseparableCharacterSequence(ucs.toString());
				if (!indexInfoSet.getUnseparableCharacterSequences().isEmpty()) {
					unseparableCharacterSequencesListSelect.setValue(indexInfoSet.getUnseparableCharacterSequences().get(0));
				}
			}
		});
		
		cbUseApostrophe.addValueChangeListener(new ValueChangeListener() {
			
			public void valueChange(ValueChangeEvent event) {
				
				IndexInfoSet indexInfoSet = ((SourceDocumentResult)table.getValue()).getSourceDocumentInfo().getIndexInfoSet();
				if (cbUseApostrophe.getValue() && (!indexInfoSet.getUserDefinedSeparatingCharacters().contains(APOSTROPHE))) {
					indexInfoSet.addUserDefinedSeparatingCharacter(APOSTROPHE);
				}
				else if(!cbUseApostrophe.getValue()){
					indexInfoSet.removeUserDefinedSeparatingCharacter(APOSTROPHE);
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
		
		btApplyLangToAll.addClickListener(new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				if (cbApplyToAllLanguageBox.getValue() == null) {
					Notification.show(
						"Info", 
						"Please select a common language first!", 
						Type.TRAY_NOTIFICATION);
				}
				else {
					for (Object itemId : table.getItemIds()) {
						SourceDocumentResult sdr = (SourceDocumentResult)itemId;
						sdr.getSourceDocumentInfo().getIndexInfoSet().setLanguage(
								(LanguageItem) cbApplyToAllLanguageBox.getValue());
						
					}
					table.refreshRowCache();
				}
				
			}
		});
	}

	private void initComponents() {
		addStyleName(Reindeer.PANEL_LIGHT);
		GridLayout content = new GridLayout(2, 2);
		
		content.setSpacing(true);
		content.setMargin(true);
		
		content.setSizeFull();
		
		Locale[] availableLocales = Locale.getAvailableLocales();
		languageItems = new ArrayList<LanguageItem>();
		for (Locale locale : availableLocales) {
			languageItems.add(new LanguageItem(locale));
		}
		
		BeanItemContainer<SourceDocumentResult> container = new BeanItemContainer<SourceDocumentResult>(SourceDocumentResult.class);
		container.addNestedContainerProperty("sourceDocumentInfo.techInfoSet.fileName"); //$NON-NLS-1$
		container.addNestedContainerProperty("sourceDocumentInfo.indexInfoSet.language"); //$NON-NLS-1$
		
		table = new Table("Documents", container); 
		
		//TODO: investigate whether using a FieldFactory would make things easier..
		table.addGeneratedColumn("sourceDocumentInfo.indexInfoSet.language",  //$NON-NLS-1$
				new ComboBoxColumnGenerator(languageItems, makeComboBoxListenerGenerator())
		);
		
		table.setVisibleColumns(new Object[]{
				"sourceDocumentInfo.techInfoSet.fileName", //$NON-NLS-1$
				"sourceDocumentInfo.indexInfoSet.language" //$NON-NLS-1$
		});
		table.setColumnHeaders(new String[]{"File Name", "Language"});
		
		table.setSelectable(true);
		table.setNullSelectionAllowed(false);
		table.setImmediate(true);
		
		content.addComponent(table, 0, 0);
		
		HorizontalLayout commonLanguagePanel = new HorizontalLayout();
		commonLanguagePanel.setSpacing(true);
		
		cbApplyToAllLanguageBox = new ComboBox("Common Language", languageItems);
		commonLanguagePanel.addComponent(cbApplyToAllLanguageBox);
		
		btApplyLangToAll = new Button("Set for all documents");
		commonLanguagePanel.addComponent(btApplyLangToAll);
		commonLanguagePanel.setComponentAlignment(btApplyLangToAll, Alignment.BOTTOM_CENTER);
		content.addComponent(commonLanguagePanel, 0, 1);
		
		
		VerticalLayout expertLayout = new VerticalLayout();
		expertLayout.setStyleName("expert-features"); //$NON-NLS-1$
	
		Label infoLabel = new Label();
			
		infoLabel.setContentMode(ContentMode.HTML);
		infoLabel.setValue(
			"<p>This section allows you to finetune the creation of the word list of the selected Source Document.</p><p>If you are unsure what to do, just select the language Click below for Expert Features:</p>");
		
		expertLayout.addComponent(infoLabel);
		
		cbAdvanceOptions = new CheckBox("Advanced Options");
		
		expertLayout.addComponent(cbAdvanceOptions);
		
		cbUseApostrophe = new CheckBox("always use the apostrophe as a word separator");
		
		expertLayout.addComponent(cbUseApostrophe);
		
        unseparableCharacterSequencesListSelect = new ListSelect("Unseparable character sequences:"); 
        unseparableCharacterSequencesListSelect.setNullSelectionAllowed(false);
        unseparableCharacterSequencesListSelect.setSizeFull();
        unseparableCharacterSequencesListSelect.setImmediate(true);
        
        expertLayout.addComponent(unseparableCharacterSequencesListSelect);
        
        ucsAddRemoveLayout = new HorizontalLayout();
        
        Panel ucsAddRemovePanel = new Panel(ucsAddRemoveLayout);
        ucsAddRemovePanel.setStyleName("no-border"); //$NON-NLS-1$
        ucsAddRemoveLayout.setSpacing(true);
        ucsAddRemoveLayout.setSizeFull();
        
        btAddUcs = new Button("Add entry");
        btAddUcs.setEnabled(false);
        ucsAddRemoveLayout.addComponent(btAddUcs);
        
        tfUcs = new TextField();
        tfUcs.setInputPrompt("Add things like 'e.g.' as you see fit.");
        tfUcs.setImmediate(true);
        tfUcs.setTextChangeEventMode(TextChangeEventMode.EAGER);
        tfUcs.setWidth("100%");         //$NON-NLS-1$
        ucsAddRemoveLayout.addComponent(tfUcs);        
        ucsAddRemoveLayout.setExpandRatio(tfUcs, 2);
        
        btRemoveUcs = new Button("Remove entry");
        btRemoveUcs.setEnabled(false);
        ucsAddRemoveLayout.addComponent(btRemoveUcs);
        
        expertLayout.addComponent(ucsAddRemovePanel);

        loadSavePanel = new VerticalLayout();
        loadSavePanel.setSpacing(true);
        loadSavePanel.setWidth("80px"); //$NON-NLS-1$
        
        btLoadUcsList = new Button("Load list");
        btLoadUcsList.setVisible(false); // no handler
        loadSavePanel.addComponent(btLoadUcsList);
        
        btSaveUcsList = new Button("Save list");
        btSaveUcsList.setVisible(false); // no handler
        loadSavePanel.addComponent(btSaveUcsList);

        expertLayout.addComponent(loadSavePanel);
        
        content.addComponent(expertLayout, 1, 0);
        
        content.setColumnExpandRatio(1, 1);
        
        setContent(content);
	}
	
	private ValueChangeListenerGenerator makeComboBoxListenerGenerator(){
		return new ValueChangeListenerGenerator() {
			public ValueChangeListener generateValueChangeListener(Table source, final Object itemId, Object columnId) {
				return new Property.ValueChangeListener() {
					public void valueChange(ValueChangeEvent event) {
						SourceDocumentResult sdr = (SourceDocumentResult) itemId;
						
						onAdvance = true;
						for(SourceDocumentResult result : wizardResult.getSourceDocumentResults()){
							if (result.getSourceDocumentInfo().getIndexInfoSet().getLocale() == null){
								onAdvance = false;
								break;
							}
						}
						
						wizardStepListener.stepChanged(IndexerOptionsPanel.this);
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
		
		table.removeAllItems();
		
		Collection<SourceDocumentResult> sourceDocumentResults = wizardResult.getSourceDocumentResults();
				
		try {
			LanguageDetector languageDetector = new LanguageDetector();
			
			for (SourceDocumentResult sdr : sourceDocumentResults) {
				IndexInfoSet newIndexInfoSet = new IndexInfoSet();				

				try {
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
				}
				finally {
					sdr.getSourceDocument().unload();
				}
				
				sdr.getSourceDocumentInfo().setIndexInfoSet(newIndexInfoSet);
			}
			
			Collections.sort(languageItems); // in case items were added above
			
			BeanItemContainer<SourceDocumentResult> container = (BeanItemContainer<SourceDocumentResult>)table.getContainerDataSource();
			container.addAll(sourceDocumentResults);
			
			if(sourceDocumentResults.size() > 0){
				table.select(sourceDocumentResults.toArray()[0]);
			}
			
			onAdvance = true;
			for(SourceDocumentResult result : sourceDocumentResults){
				if (result.getSourceDocumentInfo().getIndexInfoSet().getLocale() == null){
					onAdvance = false;
					break;
				}
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
			IndexInfoSet indexInfoSet = ((SourceDocumentResult)table.getValue()).getSourceDocumentInfo().getIndexInfoSet();
			indexInfoSet.addUnseparableCharacterSequence(ucs);
			tfUcs.setValue(""); //$NON-NLS-1$
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
