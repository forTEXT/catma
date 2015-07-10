DELIMITER $$

DROP PROCEDURE IF EXISTS reconstructMissingPropertiesFromIndex$$

CREATE PROCEDURE reconstructMissingPropertiesFromIndex() 
BEGIN
	DECLARE done INT DEFAULT FALSE;
	DECLARE _tagInstanceUUID BINARY(16);
	DECLARE _propDefUUID BINARY(16);
	DECLARE _tagInstanceID INT;
	DECLARE _propDefID INT;
	DECLARE _val VARCHAR(300);
	DECLARE _propertyID INT;
	
	DECLARE cur1 CURSOR FOR
	SELECT ip.tagInstanceID, ip.propertyDefinitionID, ip.value FROM CatmaIndex.property  ip
	WHERE LEFT(ip.name, 6) != 'catma_' 
	AND NOT EXISTS (
		SELECT 1 FROM CatmaRepository.property rp
		JOIN CatmaRepository.propertydefinition rpd
			ON rpd.propertyDefinitionID = rp.propertyDefinitionID
		JOIN CatmaRepository.taginstance rti
			ON rti.tagInstanceID = rp.tagInstanceID
		WHERE rpd.uuid = ip.propertyDefinitionID
		AND rti.uuid = ip.tagInstanceID)
	AND EXISTS (
		SELECT 1 FROM CatmaRepository.taginstance rti2
				WHERE rti2.uuid = ip.tagInstanceID);

	DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

	OPEN cur1;

	read_loop: LOOP
		FETCH cur1 INTO _tagInstanceUUID, _propDefUUID, _val;
		SET _propertyID = NULL;
		IF done THEN
			LEAVE read_loop;
		END IF;
		
		SELECT t.tagInstanceID INTO _tagInstanceID 
		FROM CatmaRepository.taginstance t
		WHERE t.uuid = _tagInstanceUUID;
		
		SELECT pd.propertyDefinitionID INTO _propDefID
		FROM CatmaRepository.propertydefinition pd
		WHERE pd.uuid = _propDefUUID;
		
		INSERT INTO property(tagInstanceID, propertyDefinitionID)
		VALUES(_tagInstanceID, _propDefID);
		
		SELECT LAST_INSERT_ID() INTO _propertyID;
		INSERT INTO propertyvalue(propertyID, value)
		VALUES(_propertyID, _val);
	END LOOP;

	CLOSE cur1;
END$$


