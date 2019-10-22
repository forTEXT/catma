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

import com.google.common.collect.Lists;
import com.vaadin.data.HasValue.ValueChangeEvent;
import com.vaadin.data.HasValue.ValueChangeListener;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import de.catma.document.source.IndexInfoSet;
import de.catma.document.source.LanguageDetector;
import de.catma.document.source.LanguageItem;
import de.catma.ui.CatmaApplication;
import de.catma.ui.dialog.HelpWindow;
import de.catma.ui.legacy.wizard.DynamicWizardStep;
import de.catma.ui.legacy.wizard.WizardStepListener;

class IndexerOptionsPanel extends VerticalLayout implements DynamicWizardStep {
	
    private static final char APOSTROPHE = '\'';
    
    private ArrayList<LanguageItem> languageItems;
	
	private boolean onAdvance;
	private WizardStepListener wizardStepListener;
	private AddSourceDocWizardResult wizardResult;
	
	private Grid<SourceDocumentResult> table;
	
	private CheckBox cbUseApostrophe;
	
	private ComboBox<LanguageItem> cbApplyToAllLanguageBox;

	private Button btApplyLangToAll;

	private ListDataProvider<SourceDocumentResult> sourceDocumentResultDataProvider;

	public IndexerOptionsPanel(WizardStepListener wizardStepListener,
			AddSourceDocWizardResult wizardResult) {
		this.onAdvance = true;
		this.wizardStepListener = wizardStepListener;
		this.wizardResult = wizardResult;
		initComponents();
		initActions();
	}

	private void initActions() {
		
		cbUseApostrophe.setVisible(true);

		cbUseApostrophe.addValueChangeListener(new ValueChangeListener<Boolean>() {
			
			public void valueChange(ValueChangeEvent<Boolean> event) {
				sourceDocumentResultDataProvider.getItems().forEach(sdr -> {
					if (event.getValue()) {
						sdr.getSourceDocumentInfo().getIndexInfoSet().setUserDefinedSeparatingCharacters(
								Lists.newArrayList(APOSTROPHE));
					}
					else {
						sdr.getSourceDocumentInfo().getIndexInfoSet().setUserDefinedSeparatingCharacters(
								Lists.newArrayList());						
					}
				});
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
					sourceDocumentResultDataProvider.getItems().forEach(sdr -> {
						sdr.getSourceDocumentInfo().getIndexInfoSet().setLanguage(
								(LanguageItem) cbApplyToAllLanguageBox.getValue());
					});
					sourceDocumentResultDataProvider.refreshAll();
					updateOnAdvance();
				}
				
			}
		});
	}

	private void initComponents() {
		setSpacing(false);
		setMargin(false);
		setSizeFull();
		
		HelpWindow helpWindow = new HelpWindow(
			"Wordlist options", 
			"<p>This step allows you to fine tune indexing and language settings.</p><p>Setting the correct language is especially important for handling right-to-left languages like Arabic.</p><p>To change the language detected by CATMA double click on a Document row.</p>" );
		
		Button btHelp = helpWindow.createHelpWindowButton();
		addComponent(btHelp);
		setComponentAlignment(btHelp, Alignment.TOP_RIGHT);
		
		VerticalLayout content = new VerticalLayout();
		content.setSizeFull();
		content.setSpacing(true);
		content.setMargin(new MarginInfo(false, true, true, true));
		addComponent(content);
		setExpandRatio(content, 1f);
		
		Locale[] availableLocales = Locale.getAvailableLocales();
		languageItems = new ArrayList<LanguageItem>();
		for (Locale locale : availableLocales) {
			languageItems.add(new LanguageItem(locale));
		}
		sourceDocumentResultDataProvider = new ListDataProvider<SourceDocumentResult>(new ArrayList<>());
		table = new Grid<>("Documents", sourceDocumentResultDataProvider);
		table.setSizeFull();
		table.addColumn(
				sourceDocumentResult -> sourceDocumentResult.getSourceDocumentInfo().getTechInfoSet().getFileName())
			.setCaption("Filename");
		
		table.addColumn(
				sourceDocumentResult -> 
					new LanguageItem(sourceDocumentResult.getSourceDocumentInfo().getIndexInfoSet().getLocale()))
			.setCaption("Language")
			.setEditorComponent(
				new ComboBox<LanguageItem>(null, languageItems),
				(sourceDocumentResult, languageItem) -> {
					sourceDocumentResult.getSourceDocumentInfo().getIndexInfoSet().setLanguage(languageItem);
					updateOnAdvance();
					
				})
			.setEditable(true);
		
		table.getEditor().setEnabled(true);
		
		content.addComponent(table);
		content.setExpandRatio(table, 1f);
		
		HorizontalLayout commonLanguagePanel = new HorizontalLayout();
		commonLanguagePanel.setSpacing(true);
		
		cbApplyToAllLanguageBox = new ComboBox<LanguageItem>("Common Language", languageItems);
		commonLanguagePanel.addComponent(cbApplyToAllLanguageBox);
		
		btApplyLangToAll = new Button("Set for all documents");
		commonLanguagePanel.addComponent(btApplyLangToAll);
		commonLanguagePanel.setComponentAlignment(btApplyLangToAll, Alignment.BOTTOM_CENTER);
		content.addComponent(commonLanguagePanel);
		
		cbUseApostrophe = new CheckBox("always use the apostrophe as a word separator");
		
		content.addComponent(cbUseApostrophe);
	}
	
	private void updateOnAdvance() {
		onAdvance = true;
		for(SourceDocumentResult result : wizardResult.getSourceDocumentResults()){
			if (result.getSourceDocumentInfo().getIndexInfoSet().getLocale() == null){
				onAdvance = false;
				break;
			}
		}
		
		wizardStepListener.stepChanged(IndexerOptionsPanel.this);	
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
		
		sourceDocumentResultDataProvider.getItems().clear();
		
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
			
			
			sourceDocumentResultDataProvider.getItems().addAll(sourceDocumentResults);
			sourceDocumentResultDataProvider.refreshAll();
			
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
	
	public boolean onFinish() {
		return false;
	}
	
	public boolean onFinishOnly() {
		return false;
	}
	
	public void stepDeactivated(boolean forward){ /* noop */}
	public void stepAdded() {/* noop */}

}
