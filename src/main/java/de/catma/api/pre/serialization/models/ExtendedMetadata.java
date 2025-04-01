package de.catma.api.pre.serialization.models;

import java.util.Map;

import de.catma.api.pre.serialization.model_wrappers.PreApiAnnotationCollection;
import de.catma.api.pre.serialization.model_wrappers.PreApiSourceDocument;
import de.catma.api.pre.serialization.model_wrappers.PreApiTagDefinition;
import de.catma.api.pre.serialization.model_wrappers.PreApiTagsetDefinition;

public record ExtendedMetadata(
	Map<String, PreApiSourceDocument> documents,
	Map<String, PreApiAnnotationCollection> annotationCollections,
	Map<String, PreApiTagsetDefinition> tagsets,
	Map<String, PreApiTagDefinition> tags) {
}
