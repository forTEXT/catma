package de.catma.ui.repository.wizard;

import java.io.IOException;
import java.util.Locale;
import java.util.TreeSet;

import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.ListSelect;

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

	public IndexerOptionsPanel(WizardStepListener wizardStepListener,
			WizardResult wizardResult) {
		super(2,4);
		this.onAdvance = true;
		this.wizardStepListener = wizardStepListener;
		this.wizardResult = wizardResult;
		this.sourceDocumentInfo = wizardResult.getSourceDocumentInfo();
		initComponents();
	}

	private void initComponents() {
		setMargin(true, true, false, true);
		setSpacing(true);
		setWidth("100%");
		
		Label infoLabel = new Label();
		
		infoLabel.setContentMode(Label.CONTENT_XHTML);
		infoLabel.setValue(
				"<p>This section allows you to finetune the creation " +
				"of the word list of your Source Document.</p>" +
				"<p>If you are unsure what to do, just select the language " +
				"and leave everything else unmodified.</p>");
		addComponent(infoLabel, 0, 0, 1, 0);
		
		cbUseApostrophe = new CheckBox("always use the apostrophe as a word separator");
		
		addComponent(cbUseApostrophe, 0, 1, 1, 1);
		
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
        
        addComponent(languagesListSelect, 0, 2);
        
        unseparableCharacterSequencesListSelect = 
        		new ListSelect("Unseparable character sequences:");
        unseparableCharacterSequencesListSelect.setNullSelectionAllowed(false);
        unseparableCharacterSequencesListSelect.setSizeFull();
        
        addComponent(unseparableCharacterSequencesListSelect, 1, 2);
        
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
			
			/*
			 * hier gehts weiter:
			 * 
			 * listen to list selection bei languages
			 * add/remove bei ucs
			 * store values in indexinfoset
			 * 
			 * 
			 */
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
