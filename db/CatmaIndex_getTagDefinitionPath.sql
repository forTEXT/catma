/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009-2013  University Of Hamburg
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

DELIMITER $$
DROP PROCEDURE IF EXISTS `getTagDefinitionPath`$$

/**
 * USE catmaindex
 *  
 * Gives the TagDefinition path for the given tagDefinitionID
 * Used by de.catma.repository.db.maintenance.DBIndexMaintainer
 * 
 * author: marco.petris@web.de
 */

CREATE FUNCTION getTagDefinitionPath(curTagDefinitionID INT) RETURNS VARCHAR(2048)
READS SQL DATA
BEGIN

  DECLARE currentParent INTEGER;
  DECLARE currentPath VARCHAR(2048)  DEFAULT '';
  
  SELECT parentID, name 
  INTO currentParent, currentPath
  FROM catmarepository.tagdefinition 
  WHERE tagDefinitionID = curTagDefinitionID;
  
  WHILE currentParent is not null DO
    SELECT parentId, concat(name, '/', currentPath) 
    INTO currentParent, currentPath
    FROM catmarepository.tagdefinition 
    where tagDefinitionID = currentParent;
    
  END WHILE;
  
  RETURN concat('/', currentPath);
END$$

