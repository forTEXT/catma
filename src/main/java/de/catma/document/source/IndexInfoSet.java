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
package de.catma.document.source;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Metadata concerning indexing of a {@link SourceDocument}.
 * 
 * @author marco.petris@web.de
 *
 */
public class IndexInfoSet {

    private List<String> unseparableCharacterSequences;
    private List<Character> userDefinedSeparatingCharacters;
    private Locale locale;
    
    /**
     * @param unseparableCharacterSequences a list of character sequences that should be treated as single tokens
     * although a tokenizer would probably detect them as individual tokens. 
     * @param userDefinedSeparatingCharacters a list of characters that seperate tokens (in addition to the characters
     * that usually separate tokens like whitespace characters)
     * @param locale the main locale of the text to be indexed
     */
    public IndexInfoSet(List<String> unseparableCharacterSequences,
			List<Character> userDefinedSeparatingCharacters,
			Locale locale) {
		super();
		this.unseparableCharacterSequences = unseparableCharacterSequences;
		this.userDefinedSeparatingCharacters = userDefinedSeparatingCharacters;
		this.locale = locale;
	}

	/**
	 * Constructor with default locale, no special unseparable character sequences 
	 * and no user defined separating characters.
	 */
	public IndexInfoSet() {
		this.unseparableCharacterSequences = new ArrayList<String>();
		this.userDefinedSeparatingCharacters = new ArrayList<Character>();
	}
	
    /**
     * @return the locale specified or {@link Locale#getDefault()}.
     */
    public Locale getLocale() {
        return (locale==null) ? Locale.getDefault() : locale;
    }
    
	/**
	 * @param locale the main locale of the text to be indexed 
	 */
	public void setLocale(Locale locale) {
		this.locale = locale;
	}
	
	public LanguageItem getLanguage() {
		return new LanguageItem(locale);
	}
	
	public void setLanguage(LanguageItem language) {
		this.locale = language.getLocale();
	}

	/**
     * @return a (possibly empty) list of unseparable character sequences,
     * does not return <oode>null</oode>
     */
    public List<String> getUnseparableCharacterSequences() {
        return (unseparableCharacterSequences==null) ?
                Collections.<String>emptyList() : unseparableCharacterSequences;
    }

    public void setUnseparableCharacterSequences(List<String> unseparableCharacterSequences) {
        this.unseparableCharacterSequences = unseparableCharacterSequences;
    }
    
    /**
     * @return a (possibly empty) list of user defined speparating character sequences,
     * does not return <oode>null</oode>
     */
    public List<Character> getUserDefinedSeparatingCharacters() {
        return (userDefinedSeparatingCharacters ==null) ?
                Collections.<Character>emptyList() : userDefinedSeparatingCharacters;
    }

    public void setUserDefinedSeparatingCharacters(List<Character> userDefinedSeparatingCharacters) {
        this.userDefinedSeparatingCharacters = userDefinedSeparatingCharacters;
    }
    
    /**
     * @param character a user defined separating character (null is not allowed)
     */
    public void addUserDefinedSeparatingCharacter(Character character) {
    	if (userDefinedSeparatingCharacters == null) {
    		userDefinedSeparatingCharacters = new ArrayList<Character>();
    	}
    	userDefinedSeparatingCharacters.add(character);
    }
    
    /**
     * @param ucs null is not allowed
     */
    public void addUnseparableCharacterSequence(String ucs) {
    	if (unseparableCharacterSequences == null) {
    		unseparableCharacterSequences = new ArrayList<String>();
    	}
    	unseparableCharacterSequences.add(ucs);
    }

    /**
     * @param character null is not allowed
     */
    public void removeUserDefinedSeparatingCharacter(Character character) {
    	if (userDefinedSeparatingCharacters != null) {
    		userDefinedSeparatingCharacters.remove(character);
    	}
    }
    
    /**
     * @param ucs null is not allowed
     */
    public void removeUnseparableCharacterSequence(String ucs) {
    	if (unseparableCharacterSequences != null) {
    		unseparableCharacterSequences.remove(ucs);
    	}
    }
    //TODO: we should use Character.getDirectonality to support mixed content
    public boolean isRightToLeftWriting() {
		String lang = getLocale().getLanguage().toLowerCase();
		if (lang.equals(new Locale("iw").getLanguage().toLowerCase())) {
			return true;
		}
		else if (lang.equals(new Locale("he").getLanguage().toLowerCase())) {
			return true;
		}
		else if (lang.equals(new Locale("ar").getLanguage().toLowerCase())) {
			return true;
		}
		else if (lang.equals(new Locale("ara").getLanguage().toLowerCase())) {
			return true;
		}
		return false;
    }
}
