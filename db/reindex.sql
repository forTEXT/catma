DELETE FROM tagreference;

INSERT INTO tagreference(documentID, userMarkupCollectionID, tagDefinitionPath, tagDefinitionID, tagInstanceID, tagDefinitionVersion, characterStart, characterEnd)
SELECT s.localUri, CAST(u.userMarkupCollectionID as CHAR), getTagDefinitionPath(td.tagDefinitionID), td.uuid, ti.uuid, td.version, tr.characterStart, tr.characterEnd
FROM catmarepository.tagreference tr
JOIN catmarepository.taginstance ti ON tr.tagInstanceID = ti.tagInstanceID
JOIN catmarepository.tagdefinition td ON ti.tagDefinitionID = td.tagDefinitionID
JOIN catmarepository.usermarkupcollection u ON tr.userMarkupCollectionID = u.userMarkupCollectionID
JOIN catmarepository.sourcedocument s ON u.sourceDocumentID = s.sourceDocumentID;

DELETE FROM property;

INSERT INTO property(tagInstanceID, propertyDefinitionID, value)
SELECT ti.uuid, pd.uuid, v.value
FROM catmarepository.propertyvalue v
JOIN catmarepository.property p ON v.propertyID = p.propertyId
JOIN catmarepository.propertydefinition pd ON p.propertyDefinitionID = pd.propertyDefinitionID
JOIN catmarepository.taginstance ti ON ti.tagInstanceID = p.tagInstanceID;
