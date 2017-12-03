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
delimiter $$
DROP PROCEDURE IF EXISTS `getTagDefinitionChildren`$$

/**
 * USE catmarepository
 *  
 * Gives all TagDefinition children for the given tagDefinitionID
 * Used by de.catma.repository.db.TagLibraryHandler#removeTagDefinition
 * 
 * author: marco.petris@web.de
 */
CREATE PROCEDURE `getTagDefinitionChildren`(startTagDefinitionID INT)
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

