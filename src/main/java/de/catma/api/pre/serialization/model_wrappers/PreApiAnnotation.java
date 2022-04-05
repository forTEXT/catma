package de.catma.api.pre.serialization.model_wrappers;

import de.catma.document.annotation.TagReference;
import de.catma.document.source.SourceDocument;
import de.catma.tag.TagDefinition;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class PreApiAnnotation {
    private String id;
    private String sourceDocumentId;
    private int startOffset;
    private int endOffset;
    private String phrase;
    private String tagId;
    private String tagName;
    private List<PreApiAnnotationProperty> properties;

    public PreApiAnnotation() {
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

    public void setId(String id) {
        this.id = id;
    }

    public String getSourceDocumentId() {
        return sourceDocumentId;
    }

    public void setSourceDocumentId(String sourceDocumentId) {
        this.sourceDocumentId = sourceDocumentId;
    }

    public int getStartOffset() {
        return startOffset;
    }

    public void setStartOffset(int startOffset) {
        this.startOffset = startOffset;
    }

    public int getEndOffset() {
        return endOffset;
    }

    public void setEndOffset(int endOffset) {
        this.endOffset = endOffset;
    }

    public String getPhrase() {
        return phrase;
    }

    public void setPhrase(String phrase) {
        this.phrase = phrase;
    }

    public String getTagId() {
        return tagId;
    }

    public void setTagId(String tagId) {
        this.tagId = tagId;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public List<PreApiAnnotationProperty> getProperties() {
        return properties;
    }

    public void setProperties(List<PreApiAnnotationProperty> properties) {
        this.properties = properties;
    }
}
