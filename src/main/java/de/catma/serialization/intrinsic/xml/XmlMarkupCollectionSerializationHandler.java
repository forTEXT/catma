package de.catma.serialization.intrinsic.xml;

import de.catma.document.Range;
import de.catma.document.annotation.AnnotationCollection;
import de.catma.document.annotation.TagReference;
import de.catma.document.source.ContentInfoSet;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.contenthandler.XML2ContentHandler;
import de.catma.serialization.AnnotationCollectionSerializationHandler;
import de.catma.tag.*;
import de.catma.util.ColorConverter;
import de.catma.util.IDGenerator;
import nu.xom.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class XmlMarkupCollectionSerializationHandler implements AnnotationCollectionSerializationHandler {
	public final static String DEFAULT_COLLECTION_TITLE = "Intrinsic Markup";

	private final TagManager tagManager;
	private final XML2ContentHandler xmlContentHandler;
	private final String author;

	private final IDGenerator idGenerator;

	public XmlMarkupCollectionSerializationHandler(TagManager tagManager, XML2ContentHandler xmlContentHandler, String author) {
		this.tagManager = tagManager;
		this.xmlContentHandler = xmlContentHandler;
		this.author = author;

		this.idGenerator = new IDGenerator();
	}

	@Override
	public void serialize(AnnotationCollection userMarkupCollection, SourceDocument sourceDocument, OutputStream outputStream) throws IOException {
		throw new UnsupportedOperationException("Serialization of XML intrinsic annotation collections to their original format is not supported yet");
	}

	@Override
	public AnnotationCollection deserialize(SourceDocument sourceDocument, String id, InputStream inputStream) throws IOException {
		try {
			Builder builder = new Builder();
			Document document = builder.build(inputStream);

			Map<String, String> namespacePrefixesToTagsetIds = new HashMap<>();
			for (int i=0; i<document.getRootElement().getNamespaceDeclarationCount(); i++) {
				String prefix = document.getRootElement().getNamespacePrefix(i);
				String namespaceUri = document.getRootElement().getNamespaceURI(prefix);

				if (namespaceUri != null && !namespaceUri.isEmpty()) {
					String tagsetId = idGenerator.generateTagsetId(namespaceUri);

					if (tagManager.getTagLibrary().getTagsetDefinition(tagsetId) == null) {
						TagsetDefinition tagsetDefinition = new TagsetDefinition(tagsetId, namespaceUri);
						tagsetDefinition.setResponsibleUser(author);
						tagManager.addTagsetDefinition(tagsetDefinition);
					}

					namespacePrefixesToTagsetIds.put(prefix, tagsetId);
				}
			}

			String defaultIntrinsicXmlTagsetId = KnownTagsetDefinitionName.DEFAULT_INTRINSIC_XML.asTagsetId();
			if (tagManager.getTagLibrary().getTagsetDefinition(defaultIntrinsicXmlTagsetId) == null) {
				TagsetDefinition tagsetDefinition = new TagsetDefinition(defaultIntrinsicXmlTagsetId, null);
				tagManager.addTagsetDefinition(tagsetDefinition);
			}

			AnnotationCollection annotationCollection = new AnnotationCollection(
					id,
					new ContentInfoSet(
							"",
							String.format("%s Intrinsic Markup", sourceDocument),
							"",
							DEFAULT_COLLECTION_TITLE
					),
					tagManager.getTagLibrary(),
					sourceDocument.getUuid(),
					null,
					null
			);

			StringBuilder contentBuilder = new StringBuilder();
			Stack<String> elementStack = new Stack<>();
			scanElements(
					contentBuilder,
					elementStack,
					document.getRootElement(),
					tagManager,
					namespacePrefixesToTagsetIds,
					annotationCollection,
					sourceDocument.getUuid(),
					sourceDocument.getLength()
			);

			return annotationCollection;
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}

	private void scanElements(
			StringBuilder contentBuilder,
			Stack<String> elementStack,
			Element element,
			TagManager tagManager,
			Map<String, String> namespacePrefixesToTagsetIds,
			AnnotationCollection annotationCollection,
			String sourceDocumentId,
			int sourceDocumentLength
	) {
		xmlContentHandler.processTextNodes(
				contentBuilder,
				element,
				(currentElement, elementRangeStart, elementRangeEnd) -> {
					StringBuilder parentPathBuilder = new StringBuilder();
					Node parentNode = currentElement.getParent();
					while (parentNode != null) {
						if (parentNode instanceof Element) {
							parentPathBuilder.insert(0, "/" + ((Element) parentNode).getLocalName());
						}
						parentNode = parentNode.getParent();
					}
					String parentPath = parentPathBuilder.toString();

					String tagName = currentElement.getLocalName();

					String elementPath = String.format("%s/%s", parentPath, tagName);

					String tagsetId = namespacePrefixesToTagsetIds.get(currentElement.getNamespacePrefix());
					if (tagsetId == null) {
						tagsetId = KnownTagsetDefinitionName.DEFAULT_INTRINSIC_XML.asTagsetId();
					}

					TagsetDefinition tagsetDefinition = tagManager.getTagLibrary().getTagsetDefinition(tagsetId);
					TagDefinition tagDefinition = tagsetDefinition.getTagDefinitionsByName(tagName).findFirst().orElse(null);

					String pathPropertyDefinitionId;

					if (tagDefinition != null) {
						pathPropertyDefinitionId = tagDefinition.getPropertyDefinition("path").getUuid();
					}
					else {
						tagDefinition = new TagDefinition(
								idGenerator.generate(),
								tagName,
								null, // no parent, hierarchy is stored in 'path' annotation property
								tagsetId
						);
						tagDefinition.addSystemPropertyDefinition(
								new PropertyDefinition(
										idGenerator.generate(PropertyDefinition.SystemPropertyName.catma_displaycolor.name()),
										PropertyDefinition.SystemPropertyName.catma_displaycolor.name(),
										Collections.singletonList(ColorConverter.toRGBIntAsString(ColorConverter.randomHex()))
								)
						);
						tagDefinition.addSystemPropertyDefinition(
								new PropertyDefinition(
										idGenerator.generate(PropertyDefinition.SystemPropertyName.catma_markupauthor.name()),
										PropertyDefinition.SystemPropertyName.catma_markupauthor.name(),
										Collections.singletonList(author)
								)
						);

						pathPropertyDefinitionId = idGenerator.generate();

						PropertyDefinition pathPropertyDefinition = new PropertyDefinition(
								pathPropertyDefinitionId,
								"path",
								Collections.emptyList()
						);
						tagDefinition.addUserDefinedPropertyDefinition(pathPropertyDefinition);

						tagManager.addTagDefinition(tagsetDefinition, tagDefinition);
					}

					Range range = new Range(elementRangeStart, elementRangeEnd);

					if (range.isSinglePoint()) {
						int newStart = range.getStartPoint();
						if (newStart > 0) {
							newStart = newStart - 1;
						}

						int newEnd = range.getEndPoint();
						if (newEnd < sourceDocumentLength - 1) {
							newEnd = newEnd + 1;
						}

						range = new Range(newStart, newEnd);
					}

					TagInstance tagInstance = new TagInstance(
							idGenerator.generate(),
							tagDefinition.getUuid(),
							author,
							ZonedDateTime.now().format(DateTimeFormatter.ofPattern(Version.DATETIMEPATTERN)),
							tagDefinition.getUserDefinedPropertyDefinitions(),
							tagDefinition.getTagsetDefinitionUuid()
					);

					for (int i=0; i<currentElement.getAttributeCount(); i++) {
						PropertyDefinition propertyDefinition = tagDefinition.getPropertyDefinition(currentElement.getAttribute(i).getQualifiedName());

						if (propertyDefinition == null) {
							propertyDefinition = new PropertyDefinition(
									idGenerator.generate(),
									currentElement.getAttribute(i).getQualifiedName(),
									Collections.singleton(currentElement.getAttribute(i).getValue())
							);
							tagManager.addUserDefinedPropertyDefinition(tagDefinition, propertyDefinition);
						}
						else if (!propertyDefinition.getPossibleValueList().contains(currentElement.getAttribute(i).getValue())) {
							List<String> newValueList = new ArrayList<>(propertyDefinition.getPossibleValueList());
							newValueList.add(currentElement.getAttribute(i).getValue());
							propertyDefinition.setPossibleValueList(newValueList);
						}

						Property property = new Property(
								propertyDefinition.getUuid(),
								Collections.singleton(currentElement.getAttribute(i).getValue())
						);
						tagInstance.addUserDefinedProperty(property);
					}

					Property pathProperty = new Property(pathPropertyDefinitionId, Collections.singletonList(elementPath));
					tagInstance.addUserDefinedProperty(pathProperty);

					TagReference tagReference = new TagReference(
							annotationCollection.getId(),
							tagInstance,
							sourceDocumentId,
							range
					);
					annotationCollection.addTagReference(tagReference);
		});
	}
}
