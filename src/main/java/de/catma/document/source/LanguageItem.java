package de.catma.document.source;

import java.util.Locale;


/**
 * An item in the language list.
 * Used for display purposes.
 */
public final class LanguageItem implements Comparable<LanguageItem> {
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
