package de.catma.api.v1.serialization.models;

import java.util.Map;

import de.catma.api.v1.serialization.model_wrappers.ProjectExportAnnotationCollection;
import de.catma.api.v1.serialization.model_wrappers.ProjectExportSourceDocument;
import de.catma.api.v1.serialization.model_wrappers.ProjectExportTagDefinition;
import de.catma.api.v1.serialization.model_wrappers.ProjectExportTagsetDefinition;

public record ProjectExportExtendedMetadata(
	Map<String, ProjectExportSourceDocument> documents,
	Map<String, ProjectExportAnnotationCollection> annotationCollections,
	Map<String, ProjectExportTagsetDefinition> tagsets,
	Map<String, ProjectExportTagDefinition> tags) {
}
