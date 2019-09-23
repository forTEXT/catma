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

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;

import de.catma.document.source.FileOSType;
import de.catma.document.source.FileType;
import de.catma.document.source.IndexInfoSet;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.TechInfoSet;
import nu.xom.Elements;

/**
 * Represents the technical description within a CATMA TEI document which holds
 * information about the TEI document and possibly about a corresponding {@link SourceDocument}.
 *
 * @author Marco Petris
 * @see StandoffMarkupDocument
 * @see SourceDocument
 */
public class TechnicalDescription {
	
	private Logger logger = Logger.getLogger(TechnicalDescription.class.getName());

	/**
	 * The xml:id of the feature structure which holds the information
	 * within a Structure-{@link StandoffMarkupDocument}.
	 */
	public static final String CATMA_TECH_DESC_XML_ID = "CATMA_TECH_DESC";
	
	private static final String UNKNOWN_VALUE = "UNKNOWN";

    /**
	 * Features that can/have to be present within the target info. 
	 */
	private static enum Feature {
		/**
		 * The {@link FileType}.
		 */
		filetype, 
		/**
		 * The {@link Charset}. 
		 */
		charset, 
		/**
		 * The {@link FileOSType}.
		 */
		fileOSType, 
		/**
		 * The {@link SourceTextType}. 
		 * @deprecated just for backwards compatibility
		 */
		sourceTextType,
		/**
		 * The {@link CRC32}-checksum. 
		 */
		checksum,
        /**
         * The version of the header (version 1 is assumed if no version could be found).
         */
        version,
        /**
         * A list of unseparable character sequences.
         */
        unseparableCharacterSequences,
        /**
         * A list of user defined additional separating characters.
         */
        userDefinedSeparatingCharacters,
        /**
         * A XSLT-Stylesheet for converting a custom xml source do to CATMA readable input.
         */
        xsltDocumentName,
        ;
	}
	private TeiElement technicalDescriptionElement;

    private boolean versionChanged = false;
	
	/**
	 * Constructor.
	 * @param sourceDesc the source description element which holds the target info. 
	 * @param technicalDescriptionElement 
	 */
	public TechnicalDescription( TeiElement sourceDesc, TeiElement technicalDescriptionElement ) {
		if (technicalDescriptionElement != null) {
			this.technicalDescriptionElement = technicalDescriptionElement;
		}
		else {
			buildTechnicalDescriptionElement( sourceDesc );
		}
	}
	
	/**
	 * Excracts the target info from the given TEI SourceDesc element. 
	 * @param sourceDesc the source description element which holds the target info.
	 */
	private void buildTechnicalDescriptionElement( TeiElement sourceDesc ) {
		TeiElement abElement = new TeiElement( TeiElementName.ab ); 
		technicalDescriptionElement = new TeiElement( TeiElementName.fs );
		nu.xom.Attribute idAttribute = new nu.xom.Attribute( 
				Attribute.xmlid.getPrefixedName(), 
				Attribute.xmlid.getNamespaceURI(),
				CATMA_TECH_DESC_XML_ID ); 
		technicalDescriptionElement.addAttribute( idAttribute );
		abElement.appendChild( technicalDescriptionElement );
		sourceDesc.appendChild( abElement );
	}
	
	/**
	 * @return the file type of the target document, may return null
	 */
	public FileType getFileType() {
		String value = getFeatureValue( Feature.filetype ); 
		try {
			if( ( value != null ) && !value.equals( UNKNOWN_VALUE ) ) {
				return FileType.valueOf( value );
			}
		}
		catch( IllegalArgumentException iae ) {
			logger.log(Level.SEVERE, "error retrieving file type", iae);
		}
		
		return null;
	}
	
	public String getXsltDocumentName() {
		TeiElement featureElement = findFeature(Feature.xsltDocumentName);
		if (featureElement != null) {
			return  new StringPropertyValueFactory(featureElement).getValue();
		}
		return "";
	}
	
	/**
	 * @return the charset of the target document, never returns null
	 */
	public Charset getCharset() {
		
		String value = getFeatureValue( Feature.charset ); 
		try {
			if( ( value != null ) && !value.equals( UNKNOWN_VALUE ) ) {
				return Charset.forName( value );
			}
		}
		catch( IllegalCharsetNameException | UnsupportedCharsetException exc ) {
			logger.log(Level.SEVERE, "error retrieving charset", exc);
		}
		
		return Charset.defaultCharset();
	}
	
	/**
	 * @return the target file's OS type, may return null
	 */
	public FileOSType getFileOSType() {
		String value = getFeatureValue( Feature.fileOSType ); 
		try {
			if( ( value != null ) && !value.equals( UNKNOWN_VALUE ) ) {
				return FileOSType.valueOf( value );
			}
		}
		catch( IllegalArgumentException iae ) {
			logger.log(Level.SEVERE, "error retrieving file OS type", iae);
		}
		
		return null;
	}
	

    /**
     * @return the version of the containing {@link org.catma.document.tagset.TagsetDocument}
     */
    public TeiDocumentVersion getVersion() {
        String value = getFeatureValue( Feature.version );
        if ((value == null) || value.equals(UNKNOWN_VALUE)) {
            setValue(Feature.version, "1");
            return getVersion();
        }
        try {
            return TeiDocumentVersion.getVersion(Integer.valueOf(value));
        }
        catch(NumberFormatException nfe) {
            throw new IllegalStateException(
                "Version value " + value + " is not a valid version!", nfe);
        }
    }

    /**
     * @param version the version of the containing {@link org.catma.document.tagset.TagsetDocument}
     */
    public void setVersion(TeiDocumentVersion version) {
        if (!getVersion().equals(version)) {
            versionChanged = true;
        }
        setValue(Feature.version, version.toString());
    }

    
	/**
	 * Retrieves the given feature element.
	 * @param feature the feature to retrieve
	 * @return the element of the feature, never returns null
	 */
	private TeiElement getFeature( Feature feature ) {

		TeiElement featureElement = findFeature(feature);

        if (featureElement == null) {
            featureElement = new TeiElement( TeiElementName.f );
            nu.xom.Attribute nameAttribute =
                new nu.xom.Attribute( Attribute.f_name.getLocalName(), feature.name() );
            featureElement.addAttribute( nameAttribute );

            TeiElement valueElement = new TeiElement( TeiElementName.string );
            featureElement.appendChild( valueElement );
            valueElement.appendChild( UNKNOWN_VALUE );
            technicalDescriptionElement.appendChild( featureElement );
        }
        
		return featureElement;
	}

    /**
     * Searches the list of features for the given feature.
     * @param feature the feature to look for
     * @return the corresponding element or <code>null</code> if there is no such feature
     */
    private TeiElement findFeature( Feature feature ) {
    	//TODO: reimplement with getChildNodes...
        Elements features =
            technicalDescriptionElement.getChildElements( TeiElementName.f );
        for( int idx=0; idx<features.size(); idx++ ) {
            TeiElement featureElement = (TeiElement)features.get( idx );
            String name = featureElement.getAttributeValue( Attribute.f_name );
            if( ( name != null ) && ( name.equals( feature.name() ) ) ) {
                return featureElement;
            }
        }

        return null;
    }

	/**
	 * Retrieves the feature's value.
	 * @param feature the feature
	 * @return the feature's value.
	 */
	private String getFeatureValue( Feature feature ) {
		TeiElement featureElement = getFeature( feature );
		return new StringPropertyValueFactory(featureElement).getValue(); 
	}
	
	/**
	 * Sets the value of the feature.
	 * @param feature the relevant feature
	 * @param value the value of the feature
	 */
	private void setValue( Feature feature, String value ) {
		TeiElement featureElement = getFeature( feature );
		TeiElement stringElement =
                (TeiElement)featureElement.getChildElements(TeiElementName.string).get(0);
		stringElement.removeChildren();
		stringElement.appendChild( value );		
	}


    /**
     * Sets the given value list as a value for the given feature via {@link org.catma.tei.TeiElementName#vColl};
     * @param feature the feature to set
     * @param valueList the value
     */
    private void setValueList( Feature feature, List<String> valueList) {
        TeiElement featureElement = getFeature( feature );
        featureElement.removeChildren();
        TeiElement vCollElement = new TeiElement(TeiElementName.vColl);
        nu.xom.Attribute orgAttr =
                new nu.xom.Attribute(
                        Attribute.vColl_org.getLocalName(),
                        AttributeValue.org_list.getValueName());
        vCollElement.addAttribute(orgAttr);
        featureElement.appendChild(vCollElement);
        for (String value : valueList) {
            TeiElement valueElement = new TeiElement( TeiElementName.string );
            vCollElement.appendChild( valueElement );
            valueElement.appendChild( value );
        }
    }

    /**
     * Retrieves the list of values for the given feature. It is assumed that the values
     * are stored via a {@link org.catma.tei.TeiElementName#vColl}.
     * @param feature the feature we want the value for
     * @return the value as a list (list can be empty but does not return <code>null</code>)
     */
    private List<String> getFeatureValueList(Feature feature) {
        List<String> valueList = new ArrayList<String>();

        TeiElement featureElement = findFeature( feature );

        if (featureElement != null) {
            TeiElement vCollElement =  
                    (TeiElement)featureElement.getChildElements(TeiElementName.vColl).get(0);

            Elements stringElementList =
                    vCollElement.getChildElements(TeiElementName.string);

            for (int i=0; i<stringElementList.size(); i++)  {
                TeiElement stringElement = (TeiElement)stringElementList.get(i);
                valueList.add(stringElement.getChild(0).getValue());
            }
        }

        return valueList;
    }
	
	/**
	 * Sets the values of all features using the values of the given 
	 * SourceDocumentInfo.
	 * @param sourceDocInfo contains the values to set
	 */
	public void setValuesFrom( TechInfoSet techInfoSet, IndexInfoSet indexInfoSet ) {
		
		setValue(Feature.filetype, techInfoSet.getFileType().name());
		setValue(Feature.charset, techInfoSet.getCharset().name());
		setValue(Feature.fileOSType, techInfoSet.getFileOSType().name());

		if( techInfoSet.getChecksum() != null ) {
			setValue( 
				Feature.checksum, techInfoSet.getChecksum().toString() );
		}

        setValue( Feature.version, TeiDocumentVersion.getCurrentVersion().toString() );

        if (!indexInfoSet.getUnseparableCharacterSequences().isEmpty()) {
            setValueList(
                Feature.unseparableCharacterSequences,
                indexInfoSet.getUnseparableCharacterSequences());
        }

        if (!indexInfoSet.getUserDefinedSeparatingCharacters().isEmpty()) {
            List<String> buf = new ArrayList<String>();
            for (Character c : indexInfoSet.getUserDefinedSeparatingCharacters()) {
                buf.add(c.toString());
            }
            setValueList(
                Feature.userDefinedSeparatingCharacters,
                buf);
        }
	}

    /**
     * @return the list of unseparable character sequences, does not return <code>null</code>
     */
    public List<String> getUnseparableCharacterSequenceList() {
        return Collections.unmodifiableList(
                getFeatureValueList(Feature.unseparableCharacterSequences));
    }

    /**
     * @return the list of user defined separating characters, does not return <code>null</code>
     */
    public List<Character> getUserDefinedSeparatingCharacterList() {
        List<String> buf = getFeatureValueList(Feature.userDefinedSeparatingCharacters);
        if (!buf.isEmpty()) {
            List<Character> charList = new ArrayList<Character>();
            for (String val : buf) {
                if (!val.isEmpty()) {
                    charList.add(val.charAt(0));
                }
            }
            return Collections.unmodifiableList(charList);
        }

        return Collections.emptyList();
    }
    
	
	/**
	 * @param checksum the checksum to set
	 */
	public void setChecksum( long checksum ) {
		setValue( Feature.checksum, String.valueOf( checksum ) );
        versionChanged = false;
	}
	
	/**
	 * @return the checksum of the target document, may return null
	 */
	public Long getChecksum() {
		String value = getFeatureValue( Feature.checksum );
		if( ( value != null ) 
				&& ( !value.equals( TechnicalDescription.UNKNOWN_VALUE ) ) ) {
			try {
				return Long.valueOf( value );
			}
			catch( Exception exc ) {
				logger.log(Level.SEVERE, "error retrieving checksum", exc);
				return null;
			}
		}
		return null;
	}

    /**
     * This method can be used to detect checksum mismatches due to version changes and is
     * designed for this purpose only!
     * @return true after a new version has been set and before a new checksum has been set, else false
     */
    public boolean isVersionChanged() {
        return versionChanged;
    }
}
