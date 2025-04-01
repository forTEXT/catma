package de.catma.api.pre.serialization.model_wrappers;

import java.time.ZonedDateTime;
import java.util.List;

import org.apache.commons.collections4.list.UnmodifiableList;

public class PreApiAnnotation {
    private final String id;
    private final List<PreApiAnnotatedPhrase> phrases;
    private final String annotationCollectionId;
    private final String annotationCollectionName;
    private final String tagId;
    private final String tagName;
    private final String tagColor;
    private final String tagPath;
    private final String author;
    private final ZonedDateTime createdAt;
    private final List<PreApiAnnotationProperty> userProperties;
    
    public PreApiAnnotation(
            String id,
            List<PreApiAnnotatedPhrase> phrases,
            String annotationCollectionId, String annotationCollectionName,
            String tagId, String tagName, String tagColor, String tagPath,
            String author, ZonedDateTime createdAt,
            List<PreApiAnnotationProperty> userProperties
    ) {
		super();
		this.id = id;
		this.phrases = UnmodifiableList.unmodifiableList(phrases);
		this.annotationCollectionId = annotationCollectionId;
		this.annotationCollectionName = annotationCollectionName;
		this.tagId = tagId;
		this.tagName = tagName;
		this.tagColor = tagColor;
		this.tagPath = tagPath;
		this.author = author;
		this.createdAt = createdAt;
		this.userProperties = UnmodifiableList.unmodifiableList(userProperties);
	}

    public String getId() {
        return id;
    }

    public List<PreApiAnnotatedPhrase> getPhrases() {
		return phrases;
	}

    public String getAnnotationCollectionId() {
        return annotationCollectionId;
    }

    public String getAnnotationCollectionName() {
        return annotationCollectionName;
    }

    public String getTagId() {
        return tagId;
    }

    public String getTagName() {
        return tagName;
    }

    public String getTagColor() {
        return tagColor;
    }

    public String getTagPath() {
        return tagPath;
    }

    public String getAuthor() {
        return author;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public List<PreApiAnnotationProperty> getUserProperties() {
        return userProperties;
    }

}
