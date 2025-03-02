package de.catma.api.pre.serialization.model_wrappers;

import java.io.IOException;
import java.util.List;

import org.apache.commons.collections4.list.UnmodifiableList;

import de.catma.document.annotation.TagReference;
import de.catma.document.source.SourceDocument;
import de.catma.tag.TagDefinition;

public class PreApiAnnotation {
    private final String id;
    private final String sourceDocumentId;
    private final List<PreApiAnnotatedPhrase> phrases;
    private final String tagId;
    private final String tagName;
    private final List<PreApiAnnotationProperty> properties;
    
    public PreApiAnnotation(String id, String sourceDocumentId, List<PreApiAnnotatedPhrase> phrases,
			String tagId, String tagName, List<PreApiAnnotationProperty> properties) {
		super();
		this.id = id;
		this.sourceDocumentId = sourceDocumentId;
		this.phrases = UnmodifiableList.unmodifiableList(phrases);
		this.tagId = tagId;
		this.tagName = tagName;
		this.properties = UnmodifiableList.unmodifiableList(properties);
	}
    
    @Deprecated
	public PreApiAnnotation(TagReference tagReference, TagDefinition tagDefinition, SourceDocument sourceDocument) throws IOException {
        id = tagReference.getTagInstanceId();
        sourceDocumentId = sourceDocument.getUuid();
        this.phrases = UnmodifiableList.unmodifiableList(List.of(new PreApiAnnotatedPhrase(tagReference.getRange().getStartPoint(), tagReference.getRange().getEndPoint(), sourceDocument.getContent(tagReference.getRange()))));
        tagId = tagDefinition.getUuid();
        tagName = tagDefinition.getName();
        properties = UnmodifiableList.unmodifiableList(tagReference.getTagInstance().getUserDefinedProperties().stream().map(
                (p) -> new PreApiAnnotationProperty(
                        tagDefinition.getUserDefinedPropertyDefinitions().stream().filter(
                                        pd -> pd.getUuid().equals(p.getPropertyDefinitionId())
                                ).findFirst().get().getName(),
                        p
                )
        ).toList());
    }

    public String getId() {
        return id;
    }

    public String getSourceDocumentId() {
        return sourceDocumentId;
    }
    
    public List<PreApiAnnotatedPhrase> getPhrases() {
		return phrases;
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
