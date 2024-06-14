package de.catma.document.source.contenthandler;

import nu.xom.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * The content handler that handles XML files.
 *
 * @see de.catma.document.source.TechInfoSet
 */
public class XML2ContentHandler extends AbstractSourceContentHandler {
	private final boolean simpleXml;

	protected List<String> inlineElements;

	public XML2ContentHandler(boolean simpleXml) {
		this.simpleXml = simpleXml;

		this.inlineElements = new ArrayList<>();
	}

	private void load(InputStream inputStream) throws IOException {
		try {
			Builder builder = new Builder();
			Document document = builder.build(inputStream);

			StringBuilder contentBuilder = new StringBuilder();
			processTextNodes(contentBuilder, document.getRootElement(), null);

			setContent(contentBuilder.toString());
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}

	@Override
	public void load() throws IOException {
		try (InputStream inputStream = getSourceDocumentInfo().getTechInfoSet().getURI().toURL().openStream()) {
			load(inputStream);
		}
	}

	public interface AdditionalElementProcessingCallback {
		void process(final Element element, final int elementRangeStart, final int elementRangeEnd);
	}

	/**
	 * Recursively appends text elements to the given StringBuilder, starting from the given element.
	 *
	 * @param contentBuilder the {@link StringBuilder} to be populated
	 * @param element the {@link Element} from which to start processing
	 * @param additionalElementProcessingCallback an optional callback function to perform additional processing for each element
	 */
	public void processTextNodes(StringBuilder contentBuilder, Element element, AdditionalElementProcessingCallback additionalElementProcessingCallback) {
		int elementRangeStart = contentBuilder.length();

		for (int i=0; i<element.getChildCount(); i++) {
			Node currentChildNode = element.getChild(i);

			if (currentChildNode instanceof Text) {
				addTextContent(contentBuilder, currentChildNode.getValue());
			}
			else if (currentChildNode instanceof Element) {
				processTextNodes(contentBuilder, (Element) currentChildNode, additionalElementProcessingCallback);
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

	public void addTextContent(StringBuilder contentBuilder, String content) {
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
	public boolean hasIntrinsicAnnotationCollection() {
		return true;
	}
}
