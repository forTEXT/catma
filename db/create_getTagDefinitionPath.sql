DELIMITER $$
CREATE FUNCTION getTagDefinitionPath(curTagDefinitionID INT) RETURNS VARCHAR(2048)
READS SQL DATA
BEGIN

  DECLARE currentParent INTEGER;
  DECLARE currentPath VARCHAR(2048)  DEFAULT '';
  
  SELECT parentID, name 
  INTO currentParent, currentPath
  FROM CatmaRepository.tagdefinition 
  WHERE tagDefinitionID = curTagDefinitionID;
  
  WHILE currentParent is not null DO
    SELECT parentId, concat(name, '/', currentPath) 
    INTO currentParent, currentPath
    FROM CatmaRepository.tagdefinition 
    where tagDefinitionID = currentParent;
    
  END WHILE;
  
  RETURN concat('/', currentPath);
END;

