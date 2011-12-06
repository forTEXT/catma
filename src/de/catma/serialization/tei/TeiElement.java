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


import java.util.SortedSet;
import java.util.TreeSet;

import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.ParentNode;

/**
 * Represents a TEI xml-element.<br>
 * <a href="http://www.tei-c.org/release/doc/tei-p5-doc/en/html/index.html">
 * TEI P5 Reference</a>
 *
 * @see TeiNodeFactory
 *
 * @author Marco Petris
 *
 */
public class TeiElement extends Element implements Comparable<TeiElement> {
	
	/**
	 * The TEI Namespace <code>http://www.tei-c.org/ns/1.0</code>
	 */
	public static final String TEINAMESPACE = "http://www.tei-c.org/ns/1.0";
	/**
	 * TEI Namespace prefix <code>tei</code>
	 */
	public static final String TEINAMESPACEPREFIX = "tei";
	private boolean active;
	
	/**
	 * @see Element#Element(Element)
	 */
	public TeiElement(TeiElement element) {
		super(element);
	}

	/**
	 * @see Element#Element(String, String)
	 */
	public TeiElement(String name, String uri) {
		super(name, uri);
	}

	/**
	 * @see Element#Element(String)
	 */
	public TeiElement(String name) {
		super(name);
	}

	/**
	 * Uses the {@link #TEINAMESPACE}.
	 * @param name the name of the element
	 * @see Element#Element(String, String)
	 */
	public TeiElement(TeiElementName name) {
		super(name.name(), TEINAMESPACE);
	}
	
	/**
	 * @return true if this element has at least one child element, else false
	 */
	public boolean hasChildElements() {
		return (getChildElements().size() > 0);
	}
	
	/* (non-Javadoc)
	 * @see nu.xom.Element#shallowCopy()
	 */
	@Override
	protected Element shallowCopy() {
		return new TeiElement(getQualifiedName(), getNamespaceURI());
	}
	
	/**
	 * Uses the {@link #TEINAMESPACE}.
	 * @param localName the lcoal name of the child elements
	 * @return a list of child elements
	 * @see Element#getChildElements(String, String)
	 */
	public final Elements getChildElements(TeiElementName localName) {
		return getChildElements(localName.name(), TEINAMESPACE);
	}
	
    /**
     * @param name the name of the attribute
     * @return the value
     * @see #getAttributeValue(String, String)
     */
    public String getAttributeValue(Attribute name) {
        return getAttributeValue(name.getLocalName(), "");
    }
    
    /**
     * @return the {@link TeiElement} which is the parent of this element or 
     * <code>null</code> if there is no parent TeiElement. 
     */
    public TeiElement getTeiElementParent() {
		ParentNode parent = getParent();
		if( ( parent != null ) && ( parent instanceof TeiElement ) ) {
			return (TeiElement)parent;
		}
		return null;
    }
    
    /**
     * @param nameToTest the name to test
     * @return true if the local name of this element equals the given element name.
     */
    public boolean is( TeiElementName nameToTest ) {
    	return getLocalName().equalsIgnoreCase( nameToTest.name() );
    }

    /**
     * @param nameToTest the name to test
     * @return true if the local name of the parent element equals the given element name.
     */
    public boolean parentIs( TeiElementName nameToTest ) {
        return (getTeiElementParent() != null) && getTeiElementParent().is(nameToTest);
    }

    /**
     * @return a string representation of this element, including attributes 
     * and their values
     */
    public String getStringRepresentation() {
    	StringBuilder sb = new StringBuilder();
    	sb.append(  this.toString() );
    	SortedSet<String> attributes = new TreeSet<String>();
    	
    	for( int idx=0; idx<this.getAttributeCount(); idx++ ) {
    		attributes.add( this.getAttribute( idx ).getQualifiedName() );
    	}
		sb.append( "[" );
    	for( String attributeName : attributes ) {
    		sb.append( " " );
    		sb.append( attributeName );
    		sb.append( "=" );
            String value = this.getAttributeValue( attributeName );

            if (attributeName.contains(":")) {
                String attributeNameParts[] = attributeName.split(":");
                Attribute a =
                        Attribute.getAttribute(
                                attributeNameParts[0].trim(), attributeNameParts[1].trim());
                if (a!=null) {
                    nu.xom.Attribute attribute = getAttribute(a.getLocalName(), a.getNamespaceURI());
                    if (attribute != null) {
                        value = attribute.getValue();
                    }
                }
            }
            sb.append( value );
    	}
		sb.append( " ]" );
    	
    	return sb.toString();
    }
    
    /**
     * @return true if the {@link Attribute#type} has the 
     * {@link AttributeValue#type_catma}, else false
     */
    public boolean isCatmaElement() {
		String typeValue = 
			this.getAttributeValue( Attribute.type );
		
		if( typeValue != null ) { 
			return AttributeValue.type_catma.getValueName().equalsIgnoreCase(    
				 typeValue );
		}
		
		return false;
    }
    
    /**
     * @return true, if this element is active
     * @see TagManager#setActive(org.catma.tag.Tag, boolean)
     */
    public boolean isActive() {
		return active;
	}
    
    /**
     * @param active true->this element is marked as active, else false
     * @see TagManager#setActive(org.catma.tag.Tag, boolean)
     */
    public void setActive( boolean active ) {
		this.active = active;
	}
    
    /**
     * <b>This method is under construction</b>
     */
    public String getNumberedLabel() {
    	// FIXME: provisorial hack
    	if( getLocalName().equals( TeiElementName.p.name() ) ) {
    		return "Paragraph " + getAttributeValue( Attribute.n ); 
    	}
    	else if( getLocalName().equals( TeiElementName.div.name() ) ) {
    		return "Act " + getAttributeValue( Attribute.n );
    	}
    	return getLocalName()
                + ( (getAttributeValue( Attribute.n ) == null)
                ?  ""
                : " " + getAttributeValue( Attribute.n ));
    }
    
    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo( TeiElement o ) {
    	if( getLocalName().equals( o.getLocalName() ) ) {
    		String thisNValue = getAttributeValue( Attribute.n );
    		String oNValue =  o.getAttributeValue( Attribute.n );
    		if(  thisNValue == null ) {
    			return (oNValue==null)? 0 : -1;
    		}
    		else if( oNValue == null ) {
    			return (thisNValue==null)? 0 : 1;
    		}
    		else {
    			return Integer.valueOf( thisNValue ).compareTo( 
    					Integer.valueOf( oNValue ) );
    		}
    	}
    	return this.getNumberedLabel().compareTo( o.getNumberedLabel() );
    }
    

	/**
	 * @return the xml:id of this element or <code>null</code> if the 
	 * element does not have an id.
	 */
	public String getID() {
		return getAttributeValue( 
				Attribute.xmlid.getLocalName(), 
				Attribute.xmlid.getNamespaceURI() );
	}
    
}

