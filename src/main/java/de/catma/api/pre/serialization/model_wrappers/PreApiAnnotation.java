package de.catma.api.pre.serialization.model_wrappers;

import de.catma.document.annotation.TagReference;
import de.catma.document.source.SourceDocument;
import de.catma.tag.TagDefinition;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class PreApiAnnotation {
    private final String id;
    private final String sourceDocumentId;
    private final int startOffset;
    private final int endOffset;
    private final String phrase;
    private final String tagId;
    private final String tagName;
    private final List<PreApiAnnotationProperty> properties;
    
    public PreApiAnnotation(String id, String sourceDocumentId, int startOffset, int endOffset, String phrase,
			String tagId, String tagName, List<PreApiAnnotationProperty> properties) {
		super();
		this.id = id;
		this.sourceDocumentId = sourceDocumentId;
		this.startOffset = startOffset;
		this.endOffset = endOffset;
		this.phrase = phrase;
		this.tagId = tagId;
		this.tagName = tagName;
		this.properties = properties;
	}

	public PreApiAnnotation(TagReference tagReference, TagDefinition tagDefinition, SourceDocument sourceDocument) throws IOException {
        id = tagReference.getTagInstanceId();
        sourceDocumentId = sourceDocument.getUuid();
        startOffset = tagReference.getRange().getStartPoint();
        endOffset = tagReference.getRange().getEndPoint();
        phrase = sourceDocument.getContent(tagReference.getRange());
        tagId = tagDefinition.getUuid();
        tagName = tagDefinition.getName();
        properties = tagReference.getTagInstance().getUserDefinedProperties().stream().map(
                (p) -> new PreApiAnnotationProperty(
                        tagDefinition.getUserDefinedPropertyDefinitions().stream().filter(
                                        pd -> pd.getUuid().equals(p.getPropertyDefinitionId())
                                ).findFirst().get().getName(),
                        p
                )
        ).collect(Collectors.toList());
    }

    public String getId() {
        return id;
    }

    public String getSourceDocumentId() {
        return sourceDocumentId;
    }

    public int getStartOffset() {
        return startOffset;
    }

    public int getEndOffset() {
        return endOffset;
    }

    public String getPhrase() {
        return phrase;
    }

    public String getTagId() {
        return tagId;
    }

    public String getTagName() {
        return tagName;
    }

    public List<PreApiAnnotationProperty> getProperties() {
        return properties;
    }

}
