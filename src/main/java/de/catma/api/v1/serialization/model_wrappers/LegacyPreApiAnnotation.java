package de.catma.api.v1.serialization.model_wrappers;

import java.util.List;

@Deprecated
public class LegacyPreApiAnnotation {
    private final String id;
    private final String sourceDocumentId;
    private final int startOffset;
    private final int endOffset;
    private final String phrase;
    private final String tagId;
    private final String tagName;
    private final List<ProjectExportAnnotationProperty> properties;

    public LegacyPreApiAnnotation(String id, String sourceDocumentId, int startOffset, int endOffset, String phrase,
                                  String tagId, String tagName, List<ProjectExportAnnotationProperty> properties) {
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

    public List<ProjectExportAnnotationProperty> getProperties() {
        return properties;
    }
}
