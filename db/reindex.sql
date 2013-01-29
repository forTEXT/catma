delimiter $$

DROP PROCEDURE IF EXISTS reindex$$

CREATE PROCEDURE reindex()
BEGIN

	DELETE FROM tagreference;
	
	INSERT INTO tagreference(documentID, userMarkupCollectionID, tagDefinitionPath, tagDefinitionID, tagInstanceID, tagDefinitionVersion, characterStart, characterEnd)
	SELECT s.localUri, CAST(u.userMarkupCollectionID as CHAR), getTagDefinitionPath(td.tagDefinitionID), td.uuid, ti.uuid, td.version, tr.characterStart, tr.characterEnd
	FROM CatmaRepository.tagreference tr
	JOIN CatmaRepository.taginstance ti ON tr.tagInstanceID = ti.tagInstanceID
	JOIN CatmaRepository.tagdefinition td ON ti.tagDefinitionID = td.tagDefinitionID
	JOIN CatmaRepository.usermarkupcollection u ON tr.userMarkupCollectionID = u.userMarkupCollectionID
	JOIN CatmaRepository.sourcedocument s ON u.sourceDocumentID = s.sourceDocumentID;
	
	DELETE FROM property;
	
	INSERT INTO property(tagInstanceID, propertyDefinitionID, value)
	SELECT ti.uuid, pd.uuid, v.value
	FROM CatmaRepository.propertyvalue v
	JOIN CatmaRepository.property p ON v.propertyID = p.propertyId
	JOIN CatmaRepository.propertydefinition pd ON p.propertyDefinitionID = pd.propertyDefinitionID
	JOIN CatmaRepository.taginstance ti ON ti.tagInstanceID = p.tagInstanceID;

END$$

delimiter ;
