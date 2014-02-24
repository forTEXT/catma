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

DROP PROCEDURE IF EXISTS getTerms$$
/**
 * Gives all terms that belong to the given token range (basePos, baePos+termCount) in 
 * the document with the given ID.
 * 
 * docID - localUri of the sourceDocument
 * basePos - the base tokenoffset where to start counting
 * termCount - number of tokens to count
 * 
 * author: marco.petris@web.de
 */
CREATE PROCEDURE getTerms (
    docID VARCHAR(300), basePos INT, 
    termCount INT)
BEGIN

    DECLARE counter INT;
    
    SELECT 1 INTO counter;

    CREATE temporary TABLE result_getTerms (
        term VARCHAR(300),
        characterStart INT,
        characterEnd INT
    );

    /* collect terms that belong to the token range (basePos, basePos+counter) */
    WHILE counter <= termCount DO
    
        INSERT INTO result_getTerms(term, characterStart, characterEnd)
        SELECT t.term, p.characterStart, p.characterEnd
        FROM CatmaIndex.term t
        JOIN CatmaIndex.position p ON p.termID = t.termID
        WHERE t.documentID=docID
        AND p.tokenOffset = basePos+counter;
        SET counter = counter+1;
    END WHILE;
    
    SELECT term, characterStart, characterEnd FROM result_getTerms;

    DROP TABLE result_getTerms;

END$$

delimiter ;
