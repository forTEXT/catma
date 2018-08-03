/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009  University Of Hamburg
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

package de.catma.serialization.tei;

import java.util.Locale;
import java.util.regex.Pattern;

import nu.xom.Elements;
import de.catma.document.source.ContentInfoSet;
import de.catma.document.source.IndexInfoSet;
import de.catma.document.source.TechInfoSet;

/**
 * The representation of a TEI header.<br><br>
 * 
 * see <a href="http://www.tei-c.org/release/doc/tei-p5-doc/en/html/index.html">
 * TEI P5 Reference</a>
 *
 * @author Marco Petris
 *
 */
public class TeiHeader {
		
	private TeiElement teiHeaderElement;
	private TeiElement title;
	private TeiElement author;
	private TeiElement publisher;
	private TeiElement sourceDesc;
    private TeiElement language;
    private TechnicalDescription technicalDescription;
	
	/**
	 * Constructor with basic TEI header elements.
	 * @param teiHeaderElement the xml element of the header itself
	 * @param title the title of the TEI document
	 * @param author the author of the TEI document
	 * @param publisher the publisher of the TEI document
	 * @param sourceDesc the source description of the TEI document
     * @param language the language of content of the  TEI document
	 * @param technicalDescriptionElement the technical description (can be <code>null</code>)
	 */
	public TeiHeader(
			TeiElement teiHeaderElement,
			TeiElement title, TeiElement author, 
			TeiElement publisher, TeiElement sourceDesc,
            TeiElement language, TeiElement technicalDescriptionElement) {
		this.teiHeaderElement = teiHeaderElement;
		this.title = title;
		this.author = author;
		this.publisher = publisher;
		this.sourceDesc = sourceDesc;
        this.language = language;
		this.technicalDescription = 
				new TechnicalDescription( sourceDesc, technicalDescriptionElement );
	}

	/**
	 * @return a string representation of the title i. e. the value string or 
	 * for complex values a xml representation
	 */
	public String getSimpleTitle() {
		return getSimpleValue(title);
	}

	/**
	 * @return a string representation of the author i. e. the value string or 
	 * for complex values a xml representation
	 */
	public String getSimpleAuthor() {
		return getSimpleValue(author);
	}

	/**
	 * @return a string representation of the publisher i. e. the value string or 
	 * for complex values a xml representation
	 */
	public String getSimplePublisher() {
		return getSimpleValue(publisher);
	}

    /**
     * @return the locale representing the language of the source document or <code>null</code> if
     * there is no language set or the language code is not a valid ISO 639 language code.
     */
    public Locale getLanguage() {
        Locale locale = null;
        
        if (language != null) {
            String ident = language.getAttributeValue(Attribute.language_ident);

            // try to build a locale with ident
            if (ident != null) {
                String parts[] = ident.split(Pattern.quote("-"));

                // we try to find the language
                String langCode = null;
                String langPart = parts[0].toLowerCase();

                // validate language
                for( String validLangCode : Locale.getISOLanguages() ) {
                    if (validLangCode.equals(langPart)) {
                        langCode = validLangCode;
                        break;
                    }
                }

                if (langCode != null) {
                    // ok we have a valid language

                    // try to find the country
                    String countryCode = null;
                    for (int i=1; i<parts.length; i++) {
                        String countryPart = parts[i].toUpperCase();
                        // validate country
                        for (String validCountryCode : Locale.getISOCountries()) {
                            if (validCountryCode.equals(countryPart)) {
                                countryCode = validCountryCode;
                                break;
                            }
                        }
                    }

                    if (countryCode != null) {
                        // ok, locale with language and country
                        locale = new Locale(langCode, countryCode);
                    }
                    else {
                        // no country available
                        locale = new Locale(langCode);
                    }
                }
            }
        }

        return locale;
    }

	/**
	 * @return a string representation of the source desc i. e. the value string 
	 * (only one &lt;p&gt; or &lt;ab&gt; element)
	 * or for more complex values a xml representation
	 */
	public String getSimpleSourceDesc() {
		// FIXME: too naive:
		Elements rsList = sourceDesc.getChildElements(TeiElementName.p);
		if( rsList.size() > 0 ) {
			return getSimpleValue( (TeiElement)rsList.get(0) );
		}
		
		rsList = sourceDesc.getChildElements(TeiElementName.ab);
		if( rsList.size() > 0 ) {
			return getSimpleValue( (TeiElement)rsList.get(0) );
		}
		
		return getSimpleValue(sourceDesc);
	}
	
	/**
	 * @param element the element which holds the value
	 * @return the simple value of the given element
	 */
	private String getSimpleValue( TeiElement element ) {
		return (!element.hasChildElements() ? element.getValue() : element.toXML());
	}
	
	/**
	 * @return a string representation of the TEI-header
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append( "Header[Title:'" );
		builder.append( getSimpleTitle() );
		builder.append( "' Author:'" );
		builder.append( getSimpleAuthor() );
		builder.append( "' Publisher:'" );
		builder.append( getSimplePublisher() );
		builder.append( "' SourceDesc:'" );
		builder.append( getSimpleSourceDesc() );
		builder.append( "']" );
		
		return builder.toString();
	}
	
	/**
	 * @param value the value to test
	 * @return the value if it is not <code>null</code> and not an empty string
	 * else the string "empty".
	 */
	private String getNonEmptyValue( String value ) {
		if( ( value == null ) || value.isEmpty() ) {
			return "empty";
		}
		
		return value;
	}


	/**
	 * Sets the standard fields of the header with the values from the given
	 * Standoff Markup Document parameters.
	 * @param parameters the paramters that contain the values for the header
	 */
	public void setStandardFieldValues( 
			ContentInfoSet contentInfoSet) {
		title.removeChildren();
		title.appendChild( getNonEmptyValue( contentInfoSet.getTitle() ) );
		author.removeChildren();
		author.appendChild( getNonEmptyValue( contentInfoSet.getAuthor() ) );
		publisher.removeChildren();
		publisher.appendChild( getNonEmptyValue( contentInfoSet.getPublisher() ) );
		Elements elements = sourceDesc.getChildElements( TeiElementName.p );
		TeiElement pElement = null;
		if( elements.size() == 0 ) {
			pElement = new TeiElement( TeiElementName.p );
			sourceDesc.appendChild( pElement );
		}
		else {
			pElement = (TeiElement)elements.get(0);
			pElement.removeChildren();
		}
		pElement.appendChild( getNonEmptyValue( contentInfoSet.getDescription() ) );
	}

    /**
     * @param locale the predominant language of the
     * targeted {@link de.catma.document.source.SourceDocument}
     */
    private void setLanguage( Locale locale ) {
        String identValue = locale.getLanguage();
        if (!locale.getCountry().isEmpty()) {
            identValue += "-" + locale.getCountry();
        }

        if (language == null) {
            language = new TeiElement(TeiElementName.language);
            nu.xom.Attribute identAttr = new nu.xom.Attribute(
                    Attribute.language_ident.getLocalName(), identValue);

            language.addAttribute(identAttr);
            language.appendChild(locale.getDisplayLanguage());
            
            TeiElement langUsage = new TeiElement(TeiElementName.langUsage);
            langUsage.appendChild(language);

            TeiElement profileDesc = new TeiElement(TeiElementName.profileDesc);
            profileDesc.appendChild(langUsage);
            teiHeaderElement.appendChild(profileDesc);
        }
        else {
            nu.xom.Attribute identAttr =
                    language.getAttribute(Attribute.language_ident.getLocalName());
            identAttr.setValue(identValue);
            language.removeChildren();
            language.appendChild(locale.getDisplayLanguage());
        }
    }
	
	/**
	 * Sets the standard fields of the header with the values from the given
	 * Source Document Info and the fields of the header's {@link TechnicalDescription}.
	 */
	public void setValues( ContentInfoSet contentInfoSet, TechInfoSet techInfoSet, IndexInfoSet indexInfoSet ) {
		setStandardFieldValues( contentInfoSet );
        setLanguage(indexInfoSet.getLocale());
		technicalDescription.setValuesFrom( techInfoSet, indexInfoSet );
	}
	
	/**
	 * @return the technical description within this header
	 */
	public TechnicalDescription getTechnicalDescription() {
		return technicalDescription;
	}

	public void setValues(ContentInfoSet contentInfoSet) {
		setStandardFieldValues(contentInfoSet);
	}
	
	TeiElement getEncodingDescElement() {
		TeiElement encodingDescElement = 
			teiHeaderElement.getFirstTeiChildElement(TeiElementName.encodingDesc);
		if (encodingDescElement == null) {
			encodingDescElement = new TeiElement(TeiElementName.encodingDesc);
			teiHeaderElement.appendChild(encodingDescElement);
		}
		return encodingDescElement;
	}
}
