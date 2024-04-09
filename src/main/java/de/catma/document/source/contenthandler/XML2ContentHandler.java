package de.catma.document.source.contenthandler;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Text;

public class XML2ContentHandler extends AbstractSourceContentHandler {
	private boolean simpleXml;

	protected List<String> inlineElements;

	public XML2ContentHandler(boolean simpleXml) {
		this.simpleXml = simpleXml;

		this.inlineElements = new ArrayList<>();
	}

	/* (non-Javadoc)
	 * @see de.catma.document.source.contenthandler.SourceContentHandler#load(java.io.InputStream)
	 */
	public void load(InputStream is) throws IOException {
		try {
	        Builder builder = new Builder();
	        
	        Document document = builder.build(is);
	        StringBuilder contentBuilder = new StringBuilder();
	        processTextNodes(contentBuilder, document.getRootElement(), null);
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

	public interface AdditionalElementProcessingCallback {
		void process(final Element element, final int elementRangeStart, final int elementRangeEnd);
	}

    /**
     * Appends text elements to the given builder otherwise descents deeper into the
     * document tree.
     * @param contentBuilder the builder is filled with text elements
     * @param element the current element to process
     */
    public void processTextNodes(
			StringBuilder contentBuilder, Element element, AdditionalElementProcessingCallback additionalElementProcessingCallback) {
    	int elementRangeStart = contentBuilder.length();

		for( int idx=0; idx<element.getChildCount(); idx++) {
            Node curChild = element.getChild(idx);
            if (curChild instanceof Text) {
            	addTextContent(contentBuilder, element, curChild.getValue());
            }
            else if (curChild instanceof Element) { //descent
                processTextNodes(contentBuilder, (Element)curChild, additionalElementProcessingCallback);
            
            }
        }
		
		if (!simpleXml && element.getChildCount() != 0) {
			addBreak(contentBuilder, element);
		}

		int elementRangeEnd = contentBuilder.length();

		if (additionalElementProcessingCallback != null) {
			additionalElementProcessingCallback.process(element, elementRangeStart, elementRangeEnd);
		}
    }

	public void addTextContent(StringBuilder contentBuilder, Element element,
			String content) {
    	if (simpleXml || !content.trim().isEmpty()) {
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
