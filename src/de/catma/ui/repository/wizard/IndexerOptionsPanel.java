package de.catma.ui.repository.wizard;

import java.io.IOException;
import java.util.Locale;
import java.util.TreeSet;

import com.vaadin.data.Container.ItemSetChangeEvent;
import com.vaadin.data.Container.ItemSetChangeListener;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.ui.Button;
import com.vaadin.ui.AbstractTextField.TextChangeEventMode;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.themes.Reindeer;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.ListSelect;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import de.catma.core.document.source.IndexInfoSet;
import de.catma.core.document.source.LanguageDetector;
import de.catma.core.document.source.SourceDocumentInfo;

public class IndexerOptionsPanel extends GridLayout implements DynamicWizardStep {
	
    /**
     * An item in the language list.
     */
    private static class LanguageItem implements Comparable<LanguageItem> {
        private Locale locale;

        /**
         * Constructor
         * @param locale the locale of this item
         */
        public LanguageItem(Locale locale) {
            this.locale = locale;
        }

        /**
         * @return the locale of this item
         */
        public Locale getLocale() {
            return locale;
        }

        @Override
        public String toString() {
            return locale.getDisplayLanguage()
                + (locale.getDisplayCountry().isEmpty()? "" : "-" + locale.getDisplayCountry());
        }

        /**
         * Compares by string representation of the item.
         */
        public int compareTo(LanguageItem o) {
            return this.toString().compareTo(o.toString());
        }

		@Override
		public int hashCode() {
			return locale.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof LanguageItem) {
				return locale.equals(((LanguageItem)obj).locale);
			}
			return false;
		}
    }

    private static final char APOSTROPHE = '\'';
	
	private boolean onAdvance;
	private WizardStepListener wizardStepListener;
	private WizardResult wizardResult;
	private CheckBox cbUseApostrophe;
	private SourceDocumentInfo sourceDocumentInfo;

	private ListSelect languagesListSelect;

	private TreeSet<LanguageItem> sortedLangs;

	private ListSelect unseparableCharacterSequencesListSelect;

	private Button btLoadUcsList;

	private Button btSaveUcsList;

	private Button btAddUcs;

	private TextField tfUcs;

	private Button btRemvoeUcs;

	public IndexerOptionsPanel(WizardStepListener wizardStepListener,
			WizardResult wizardResult) {
		super(3,5);
		this.onAdvance = true;
		this.wizardStepListener = wizardStepListener;
		this.wizardResult = wizardResult;
		this.sourceDocumentInfo = wizardResult.getSourceDocumentInfo();
		initComponents();
		initActions();
	}

	private void initActions() {
		this.languagesListSelect.addListener(new ValueChangeListener() {
			
			public void valueChange(ValueChangeEvent event) {
				onAdvance = (languagesListSelect.getValue() != null);
				if (languagesListSelect.getValue() != null) {
					
					sourceDocumentInfo.getContentInfoSet().setLocale(
							((LanguageItem)languagesListSelect.getValue()).getLocale());
				}
				wizardStepListener.stepChanged(IndexerOptionsPanel.this);
			}
		});
		
		this.tfUcs.addListener(new TextChangeListener() {
			public void textChange(TextChangeEvent event) {
				btAddUcs.setEnabled(
					(event.getText() != null) 
					&& (!event.getText().isEmpty()));
			}
		});
		
		this.tfUcs.addListener(new ValueChangeListener() {
			
			public void valueChange(ValueChangeEvent event) {
				addUcs(tfUcs.getValue().toString());
			}
		});
		
		this.unseparableCharacterSequencesListSelect.addListener(new ItemSetChangeListener() {
			public void containerItemSetChange(ItemSetChangeEvent event) {
				btRemvoeUcs.setEnabled(
					unseparableCharacterSequencesListSelect.getContainerDataSource().size() > 0);
			}
		});
		
		btAddUcs.addListener(new ClickListener() {
			public void buttonClick(ClickEvent event) {
				addUcs(tfUcs.getValue().toString());
			}
		});
		
		btRemvoeUcs.addListener(new ClickListener() {
			public void buttonClick(ClickEvent event) {
				unseparableCharacterSequencesListSelect.removeItem(
						unseparableCharacterSequencesListSelect.getValue());
				sourceDocumentInfo.getIndexInfoSet().removeUnseparableCharacterSequence(
						tfUcs.getValue().toString());
				if (sourceDocumentInfo.getIndexInfoSet().getUnseparableCharacterSequences().size() > 0) {
					unseparableCharacterSequencesListSelect.setValue(
						sourceDocumentInfo.getIndexInfoSet().getUnseparableCharacterSequences().get(0));
				}
			}
		});
		
		cbUseApostrophe.addListener(new ValueChangeListener() {
			
			public void valueChange(ValueChangeEvent event) {
				
				if (cbUseApostrophe.booleanValue() 
						&& (sourceDocumentInfo.getIndexInfoSet().getUserDefinedSeparatingCharacters().isEmpty())) {
					sourceDocumentInfo.getIndexInfoSet().addUserDefinedSeparatingCharacter(APOSTROPHE);
				}
				else {
					sourceDocumentInfo.getIndexInfoSet().removeUserDefinedSeparatingCharacter(APOSTROPHE);
				}
			}
		});
	}

	private void initComponents() {
		setSpacing(true);
		setSizeFull();
		
		Label infoLabel = new Label();
		
		infoLabel.setContentMode(Label.CONTENT_XHTML);
		infoLabel.setValue(
				"<p>This section allows you to finetune the creation " +
				"of the word list of your Source Document.</p>" +
				"<p>If you are unsure what to do, just select the language " +
				"and leave everything else unmodified.</p>");
		addComponent(infoLabel, 0, 0, 2, 0);
		
		cbUseApostrophe = new CheckBox("always use the apostrophe as a word separator");
		
		addComponent(cbUseApostrophe, 0, 1, 2, 1);
		
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
        
        addComponent(languagesListSelect, 0, 2, 0, 3);
        
        unseparableCharacterSequencesListSelect = 
        		new ListSelect("Unseparable character sequences:");
        unseparableCharacterSequencesListSelect.setNullSelectionAllowed(false);
        unseparableCharacterSequencesListSelect.setSizeFull();
        unseparableCharacterSequencesListSelect.setImmediate(true);
        
        addComponent(unseparableCharacterSequencesListSelect, 1, 2, 1, 3);
        
        HorizontalLayout ucsAddRemoveLayout = new HorizontalLayout();
        Panel ucsAddRemovePanel = new Panel(ucsAddRemoveLayout);
        ucsAddRemovePanel.setStyleName(Reindeer.PANEL_LIGHT);
        ucsAddRemoveLayout.setSpacing(true);
        ucsAddRemoveLayout.setSizeFull();
        
        btAddUcs = new Button("Add entry");
        btAddUcs.setEnabled(false);
        ucsAddRemovePanel.addComponent(btAddUcs);
        tfUcs = new TextField();
        tfUcs.setInputPrompt("Add things like 'e.g.' as you see fit.");
        tfUcs.setImmediate(true);
        tfUcs.setTextChangeEventMode(TextChangeEventMode.EAGER);
        tfUcs.setWidth("100%");
        
        ucsAddRemovePanel.addComponent(tfUcs);
        ucsAddRemoveLayout.setExpandRatio(tfUcs, 2);
        btRemvoeUcs = new Button("Remove entry");
        btRemvoeUcs.setEnabled(false);
        ucsAddRemovePanel.addComponent(btRemvoeUcs);
        
        addComponent(ucsAddRemovePanel, 1, 4);

        VerticalLayout loadSavePanel = new VerticalLayout();
        loadSavePanel.setSpacing(true);
        loadSavePanel.setWidth("80px");
        
        btLoadUcsList = new Button("Load list");
        loadSavePanel.addComponent(btLoadUcsList);
        btSaveUcsList = new Button("Save list");
        loadSavePanel.addComponent(btSaveUcsList);

        addComponent(loadSavePanel, 2, 2);
        
        setColumnExpandRatio(0, 2);
        setColumnExpandRatio(1, 2);

        setRowExpandRatio(2, 2);
        setRowExpandRatio(3, 2);
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

	public void stepActivated() {
		try {
			LanguageDetector ld = new LanguageDetector();
			Locale locale = ld.getLocale(ld.detect(wizardResult.getSourceDocument().getContent()));
			sourceDocumentInfo.getContentInfoSet().setLocale(locale);
			
			LanguageItem current = new LanguageItem(locale);
			for (LanguageItem li : this.sortedLangs) {
				if(li.getLocale().getLanguage().equals(
                        current.getLocale().getLanguage())) {
					this.languagesListSelect.setValue(li);
					break;
				}
			}
			
			sourceDocumentInfo.setIndexInfoSet(new IndexInfoSet());
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	private void addUcs(String ucs) {
		if ((ucs != null) && !ucs.isEmpty()) {
			unseparableCharacterSequencesListSelect.addItem(ucs);
			unseparableCharacterSequencesListSelect.setValue(ucs);
			sourceDocumentInfo.getIndexInfoSet().addUnseparableCharacterSequence(ucs);
			tfUcs.setValue("");
			btAddUcs.setEnabled(false);
		}
	}

	public boolean onFinish() {
		return true;
	}
	
	public boolean onFinishOnly() {
		return false;
	}
	
	public void stepDeactivated(){ /*not needed*/}

}
