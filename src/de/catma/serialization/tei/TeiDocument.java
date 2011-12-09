package de.catma.serialization.tei;

import java.io.IOException;

import nu.xom.Document;
import nu.xom.Node;
import nu.xom.Nodes;
import nu.xom.Serializer;
import nu.xom.XPathContext;

class TeiDocument {
	
	private Document document;
	private XPathContext xpathcontext; 
	private TeiHeader teiHeader;
	
	public TeiDocument(Document document) {
		super();
		this.document = document;
		xpathcontext = new XPathContext( TeiElement.TEINAMESPACEPREFIX, TeiElement.TEINAMESPACE );
	}
	
	
	
	/**
	 * @param xpath the xpath expression that leads to the wanted element
	 * @return a tei header element that matches the given xpath 
	 */
	public TeiElement getMandatoryElement( String xpath ) {
		Nodes resultList = document.query( xpath, xpathcontext );
        return ((TeiElement)resultList.get(0));
	}
	
	/**
	 * Constructs a xpath expression with the given elements and returns a
	 * list of matching nodes.
	 * @param xPathElements the elements for the xpath expression
	 * @return the list of matching nodes
	 */
	public Nodes getElements( TeiElementName... xPathElements ) {
		
		StringBuilder xPathBuilder = new StringBuilder();
		String conc = "//";
		for( TeiElementName teiElementName : xPathElements ) {
			xPathBuilder.append( conc );
			conc = "/";
			xPathBuilder.append( TeiElement.TEINAMESPACEPREFIX );
			xPathBuilder.append( ":" );
			xPathBuilder.append( teiElementName );
		}
		
		return getElements( xPathBuilder.toString() );	
	}
	
	/**
	 * Constructs a xpath expression with the given element and returns a
	 * list of matching nodes.
	 * @param teiElementName  the element for the xpath expression
	 * @return  the list of matching nodes
	 */
	public Nodes getElements( TeiElementName teiElementName ) {
		return getElements( 
			"//"+TeiElement.TEINAMESPACEPREFIX+":"+teiElementName );
	}

    /**
     * Constructs a xpath expression with the given element and attribute/attributeValue and returns a
     * list of matching nodes.
     * @param teiElementName  the element for the xpath expression
     * @param attributeValue the attribute/value combination to filter on
     * @return  the list of matching nodes
     */
    public Nodes getElements( TeiElementName teiElementName, AttributeValue attributeValue) {
        return getElements(
            "//"+TeiElement.TEINAMESPACEPREFIX+":"+teiElementName + "[" + attributeValue + "]");
    }


	/**
	 * @param xpath the xpath expression that represents to the wanted elements
	 * @return the list of matching nodes
	 */
	Nodes getElements( String xpath ) {
		return document.query( xpath, xpathcontext );
	}
	
	/**
	 * Creates an xpath expression with the given xml:id and returns the
	 * target element.
	 * @param id the xml:id (of an {@link TeiElement}!) for the xpath expression
	 * @return the element that matched the xpath expression or <code>null</code>
	 * if there is no such element
	 */
	public TeiElement getElementByID( String id ) {
		Nodes result = getElements( 
			"//"+TeiElement.TEINAMESPACEPREFIX
			+ ":*" //wildcard
			+ "[@"+Attribute.xmlid.getPrefixedName()
			+ "='"+id+"']" );
		
		if( result.size() > 0 ) {
			return (TeiElement)result.get( 0 );
		}
		return null;
	}
	
    /**
     * @return a tei header language element
     */
    private TeiElement getLanguageElement() {
        Nodes resultList = document.query(
                "//"+TeiElement.TEINAMESPACEPREFIX+":"+TeiElementName.language, xpathcontext );

        TeiElement languageElement = null;

        if (resultList.size() == 1) {
            return (TeiElement)resultList.get(0);
        }
        else if (resultList.size() == 0) {
            return null;
        }
        else {

            int curMaxUsage = Integer.MIN_VALUE;
            for (int i=0; i<resultList.size(); i++) {
                Node node = resultList.get(i);

                if (node instanceof TeiElement) {
                    String usage =
                        ((TeiElement)node).getAttributeValue(Attribute.language_usage);
                    if (usage == null) {
                        if (languageElement == null) {
                            languageElement = (TeiElement)node;
                        }
                        break;
                    }
                    else {
                        Integer usageValue = null;
                        try {
                            usageValue = Integer.parseInt(usage);
                        }
                        catch (NumberFormatException nfe) {
                            if (languageElement == null) {
                                languageElement = (TeiElement)node;
                            }
                            break;
                        }

                        if ((usageValue != null) && ( curMaxUsage < usageValue )) {
                            curMaxUsage = usageValue;
                            languageElement = (TeiElement)node;
                        }
                    }
                }
            }
        }

        return languageElement;
    }
    
	/**
	 * Creates a header representation from the underlying {@link Document}.
	 * @return the new header representation
	 */
	void loadHeader() {
		this.teiHeader =  new TeiHeader( 
			getMandatoryElement( 
				"/"+TeiElement.TEINAMESPACEPREFIX+":"+TeiElementName.TEI+
				"/"+TeiElement.TEINAMESPACEPREFIX+":"+TeiElementName.teiHeader ),
			getMandatoryElement( "//"+TeiElement.TEINAMESPACEPREFIX+":"+TeiElementName.title ), 
			getMandatoryElement( "//"+TeiElement.TEINAMESPACEPREFIX+":"+TeiElementName.author ), 
			getMandatoryElement( "//"+TeiElement.TEINAMESPACEPREFIX+":"+TeiElementName.publisher ), 
			getMandatoryElement( "//"+TeiElement.TEINAMESPACEPREFIX+":"+TeiElementName.sourceDesc ),
            getLanguageElement(),
            getElementByID(TechnicalDescription.CATMA_TECH_DESC_XML_ID));
	}	
	
	Nodes getTagLibraryElements() {
		return getElements( 
				"//" + TeiElement.TEINAMESPACEPREFIX+":"+TeiElementName.encodingDesc
				+"/"+TeiElement.TEINAMESPACEPREFIX+":"+TeiElementName.fsdDecl );
	}
	
	TeiHeader getTeiHeader() {
		return teiHeader;
	}
	
	/**
	 * Prints the underlying document to {@link System#out}.
	 */
	public void printXmlDocument() {
		Serializer serializer = new Serializer( System.out );
		serializer.setIndent( 4 );

		try {
			serializer.write( document );
		} catch( IOException e ) {
			e.printStackTrace();
		}
	}
}
