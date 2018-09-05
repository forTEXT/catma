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
import nu.xom.Nodes;
import nu.xom.ParentNode;
import nu.xom.XPathContext;

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
	 * @param localName the local name of the child elements
	 * @return a list of child elements
	 * @see Element#getChildElements(String, String)
	 */
	public final Elements getChildElements(TeiElementName localName) {
		return getChildElements(localName.name(), TEINAMESPACE);
	}
	
	public final Nodes getChildNodes( TeiElementName teiElementName, String attributeFilter) {
		String query = 
				"//"+TeiElement.TEINAMESPACEPREFIX
				+ ":*" //wildcard
				+ "[@"+Attribute.xmlid.getPrefixedName()
				+ "='"+getID()+"']" 
				+"/"+TeiElement.TEINAMESPACEPREFIX+":"+teiElementName + "[" + attributeFilter + "]";
		
        return this.getDocument().query(query, 
            new XPathContext( TeiElement.TEINAMESPACEPREFIX, TeiElement.TEINAMESPACE ));
	}
	
	public final Nodes getChildNodes(
			TeiElementName teiElementName, AttributeFilter attributeFilter) {
		Elements childElements = getChildElements(teiElementName);
		Nodes nodes = new Nodes();
		for (int i=0; i<childElements.size();i++) {
			TeiElement element = (TeiElement)childElements.get(i);
			if (attributeFilter.matches(element)) {
				nodes.append(element);
			}
		}

		return nodes;
	}
	
	public TeiElement getFirstTeiChildElement(TeiElementName localName) {
		Elements elements = getChildElements(localName);
		if (elements.size() >0) {
			return (TeiElement)elements.get(0);
		}
		else {
			return null;
		}
	}
	
    /**
     * @param name the name of the attribute
     * @return the value
     * @see #getAttributeValue(String, String)
     */
    public String getAttributeValue(Attribute name) {
        return getAttributeValue(name.getLocalName(), "");
    }
    
    public void setAttributeValue(Attribute name, String value) {
    	nu.xom.Attribute attribute = getAttribute(name.getLocalName(), name.getNamespaceURI());
    	if (attribute == null) {
    		attribute = new nu.xom.Attribute(name.getLocalName(), name.getNamespaceURI(), value);
    		addAttribute(attribute);
    	}
    	else {
    		attribute.setValue(value);
    	}
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
     * @see TagManagerView#setActive(org.catma.tag.Tag, boolean)
     */
    public boolean isActive() {
		return active;
	}
    
    /**
     * @param active true->this element is marked as active, else false
     * @see TagManagerView#setActive(org.catma.tag.Tag, boolean)
     */
    public void setActive( boolean active ) {
		this.active = active;
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
    	return getLocalName().compareTo(o.getLocalName());
    }
    

	/**
	 * @return the xml:id of this element or <code>null</code> if the 
	 * element does not have an id.
	 */
	public String getID() {
		String id =  getAttributeValue( 
				Attribute.xmlid.getLocalName(), 
				Attribute.xmlid.getNamespaceURI());
		if (id != null) {
			return id.toUpperCase();
		}
		else {
			return null;
			
		}
	}
	
	void setID(String id) {
    	nu.xom.Attribute attribute = 
    			getAttribute(
    					Attribute.xmlid.getLocalName(), 
    					Attribute.xmlid.getNamespaceURI());
    	if (attribute == null) {
    		attribute = new nu.xom.Attribute(
    				Attribute.xmlid.getPrefixedName(), 
    				Attribute.xmlid.getNamespaceURI(), id);
    		addAttribute(attribute);
    	}
    	else {
    		attribute.setValue(id);
    	}

	}
    
}

