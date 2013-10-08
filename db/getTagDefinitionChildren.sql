delimiter $$
DROP PROCEDURE `getTagDefinitionChildren`$$

CREATE DEFINER=`root`@`localhost` PROCEDURE `getTagDefinitionChildren`(startTagDefinitionID INT)
BEGIN

	DECLARE currentParentID INT;

	CREATE TEMPORARY TABLE getTagDefChildren_children (
		tagDefinitionID INT,
		checked BOOLEAN
	);
	
	SET currentParentID = startTagDefinitionID;

	WHILE currentParentID IS NOT NULL DO
		INSERT INTO getTagDefChildren_children (tagDefinitionID, checked)
		SELECT tagDefinitionID, false 
		FROM tagdefinition WHERE parentID = currentParentID;
	
		UPDATE getTagDefChildren_children
		SET checked = true
		WHERE tagDefinitionID = currentParentID;

		SET currentParentID = null;

		SELECT tagDefinitionID INTO currentParentID
		FROM getTagDefChildren_children
		WHERE checked = false
		LIMIT 0, 1;
	END WHILE;
	
	SELECT tagDefinitionID
	FROM getTagDefChildren_children;

	DROP TABLE getTagDefChildren_children;

END$$

