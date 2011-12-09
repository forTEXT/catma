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


/**
 * XML attributes of elements. Besides the xml:id all attributes are defined
 * by the TEI.
 *
 * @author Marco Petris
 *
 */
public enum Attribute {
	/**
	 * the xml:id
	 */
	xmlid( null, "id", "xml", "http://www.w3.org/XML/1998/namespace", null ),
	/**
	 * 'name'-attribute of &lt;f&gt;
	 */
	f_name( TeiElementName.f, "name" ),
	/**
	 * 'value'-attribute of &lt;numeric&gt;
	 */
	numeric_value( TeiElementName.numeric, "value" ),
	numeric_max( TeiElementName.numeric, "max" ),
	/**
	 * 'target'-attribute of &lt;ptr&gt;
	 */
	ptr_target( TeiElementName.ptr, "target", new PtrValueHandler() ),
	/**
	 * 'ana'-attribute (global)
	 */
	ana( null, "ana", new AnaValueHandler() ),
	/**
	 * 'type'-attribute (typed)
	 */
	type( null, "type" ), 
	/**
	 * 'n'-attribute (global)
	 */
	n(null, "n"), 
	/**
	 * 'feats'-attribute of &lt;f&gt;
	 */
//	f_feats( TeiElementName.f, "feats", new FeatsValueHandler() ),
    /**
     * 'ident'-attribute of &lt;language&gt;
     */
    language_ident( TeiElementName.language, "ident"),
    /**
     * 'usage'-attribute of &lt;language&gt; 
     */
    language_usage( TeiElementName.language, "usage"),
    /**
     * 'org'-attribute of &lt;vColl&gt; 
     */
    vColl_org( TeiElementName.vColl, "org"),

    /**
     * 'absolute'-attribute of &lt;when&gt;
     */
    when_absolute( TeiElementName.when, "absolute", new WhenAbsoluteValueHandler()),

    u_start( TeiElementName.u, "start", new WhenRefHandler()),
    u_end( TeiElementName.u, "end", new WhenRefHandler()),

    fDecl_name( TeiElementName.fDecl, "name"),
    fDecl_optional( TeiElementName.fDecl, "optional"), 
    fsDecl_baseTypes(TeiElementName.fsDecl, "baseTypes"), 
    fsDecl_type(TeiElementName.fsDecl, "type" ),
    
    ;
	
	private TeiElementName elementScope;
	private String localName;
	private String prefix;
	private String namespaceURI;
	private AttributeValueHandler valueHandler;


    /**
	 * Constructs an Attribute with scope, name and valueHandler.
	 * @param elementScope the scope of the attribute (its xml-element)
	 * @param attributeName the name of the attribute
	 * @param valueHandler the handler to extract/create the attribute's value
	 */
	private Attribute( 
		TeiElementName elementScope, String attributeName, 
		AttributeValueHandler valueHandler ) {
		this( elementScope, attributeName, "", "", valueHandler);
	}
	

	/**
	 * Constructs an Attribute with scope and name.
	 * @param elementScope the scope of the attribute (its xml-element)
	 * @param attributeName the name of the attribute
	 */
	private Attribute( TeiElementName elementScope, String attributeName ) {
		this( elementScope, attributeName, "", "", null);
	}
	
	/**
	 * Constructs an Attribute with scope, name, valueHandler and namespace.
	 * @param elementScope the scope of the attribute (its xml-element)
	 * @param attributeName the name of the attribute
	 * @param namespaceURI the namespace of the attribute
	 */
	private Attribute( 
			TeiElementName elementScope, String attributeName, String namespaceURI ) {
		this( elementScope, attributeName, "", namespaceURI, null );
	}
	
	/**
	 * Constructs an Attribute with scope, name, prefix, namespace 
	 * and valueHandler.
	 * 
	 * @param elementScope the scope of the attribute (its xml-element)
	 * @param attributeName the name of the attribute
	 * @param prefix the prefix of the attribute (e.g. xml)
	 * @param namespaceURI the namespace of the attribute
	 * @param valueHandler the handler to extract/create the attribute's value
	 */
	private Attribute( 
		TeiElementName elementScope, String attributeName, 
		String prefix, String namespaceURI, 
		AttributeValueHandler valueHandler ) {
		
		this.elementScope = elementScope;
		this.localName = attributeName;
		this.namespaceURI = namespaceURI;
		this.valueHandler = valueHandler;
		this.prefix = prefix;
	}
	
	/**
	 * @return the local name of the attribute withou prefix
	 */
	public String getLocalName() {
		return localName;
	}

	/**
	 * @return the prefixed name of the attribute
	 */
	public String getPrefixedName() {
		return prefix.equals("")?getLocalName():prefix+":"+getLocalName();
	}
	
	/**
	 * @return the element that contains such an attribute
	 */
	public TeiElementName getElementScope() {
		return elementScope;
	}
	
	/**
	 * @return the namespace of this attribute
	 */
	public String getNamespaceURI() {
		return namespaceURI;
	}

	/**
	 * @return the handler to extract/create the attribute's value
	 */
	public AttributeValueHandler getValueHandler() {
		return valueHandler;
	}

    public static Attribute getAttribute(String prefix, String localName) {
        for(Attribute a : values()) {
            if (a.prefix.equals(prefix) && a.getLocalName().equals(localName)) {
                return a;
            }
        }

        return null;
    }

    @Override
    public String toString() {
        return "@" + localName;
    }
}
