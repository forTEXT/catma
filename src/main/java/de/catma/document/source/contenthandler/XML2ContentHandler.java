package de.catma.document.source.contenthandler;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Text;

public class XML2ContentHandler extends AbstractSourceContentHandler {
	protected List<String> inlineElements = new ArrayList<String>();

	public XML2ContentHandler() {
		inlineElements = new ArrayList<String>();
	}
	
	
	/* (non-Javadoc)
	 * @see de.catma.document.source.contenthandler.SourceContentHandler#load(java.io.InputStream)
	 */
	public void load(InputStream is) throws IOException {
		try {
	        Builder builder = new Builder();
	        
	        Document document = builder.build(is);
	        StringBuilder contentBuilder = new StringBuilder();
	        processTextNodes(
	        		contentBuilder, 
	        		document.getRootElement());
	        setContent(contentBuilder.toString());	
		} catch (Exception e) {
			throw new IOException(e);
		}
	}
    
    /* (non-Javadoc)
     * @see de.catma.document.source.contenthandler.SourceContentHandler#load()
     */
    public void load() throws IOException {
    	
        try {
        	InputStream is = getSourceDocumentInfo().getTechInfoSet().getURI().toURL().openStream();
        	try {
        		load(is);
        	}
        	finally {
        		is.close();
        	}
        }
        catch (Exception e) {
        	throw new IOException(e);
        }
        

    }

    /**
     * Appends text elements to the given builder otherwise descents deeper into the
     * document tree.
     * @param contentBuilder the builder is filled with text elements
     * @param element the current element to process
     * @throws URISyntaxException 
     */
    protected void processTextNodes(
    		StringBuilder contentBuilder, Element element) throws URISyntaxException {
    	
		for( int idx=0; idx<element.getChildCount(); idx++) {
            Node curChild = element.getChild(idx);
            if (curChild instanceof Text) {
            	addTextContent(contentBuilder, element, curChild.getValue());
            }
            else if (curChild instanceof Element) { //descent
                processTextNodes(
                	contentBuilder, 
                	(Element)curChild);
            
            }
        }
		
		if (element.getChildCount() == 0) { //empty elements
			addEmptyElement(contentBuilder, element);
		}
		else {
			addBreak(contentBuilder, element);
		}
    }
    
    @Deprecated
    public void addEmptyElement(StringBuilder contentBuilder, Element element) {
		//contentBuilder.append(" ");
	}


	public void addTextContent(StringBuilder contentBuilder, Element element,
			String content) {
    	if (!content.trim().isEmpty()) {
    		contentBuilder.append(content);
    	}
	}
	
	public void addBreak(StringBuilder contentBuilder, Element element) {
		if (!inlineElements.contains(element.getLocalName())) {
			contentBuilder.append("\n");
		}
	}


    @Override
    public boolean hasIntrinsicMarkupCollection() {
    	return true;
    }

}
