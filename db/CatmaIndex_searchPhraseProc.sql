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

DROP PROCEDURE IF EXISTS searchPhrase$$
/**
 * USE catmaindex
 * 
 * Used by de.catma.indexer.db.SpSearchPhrase
 * 
 * Searches a phrase consisting of up to five tokens. Each token can contain 
 * SQL wildcards.
 * 
 * term1 - first token in the phrase
 * term2 - second token in the phrase
 * ...
 * docID - localUri of the sourceDocument
 * wild - true->with wildcards
 * limitresult - limit of the result set
 * 
 * author: marco.petris@web.de
 */
CREATE PROCEDURE searchPhrase (
    term1 VARCHAR(300), term2 VARCHAR(300), 
    term3 VARCHAR(300), term4 VARCHAR(300), term5 VARCHAR(300), 
    docID VARCHAR(300),
    wild BOOLEAN, limitresult INT)
BEGIN
    DECLARE dynSQL VARCHAR(200);
    
    CREATE temporary TABLE result (
        tokenOffset INT,
        characterStart INT,
        characterEnd INT
    );

    /* create term buffers for each non null term2 to term5*/
    CREATE temporary TABLE termbuf1 (
        tokenOffset INT,
        characterEnd INT
    ); 
    
    /* fill each term buffer termbufX with offset and endpositions for terms that
     * match termX */
    IF (term2 IS NOT NULL)
    THEN
        INSERT INTO termbuf1(tokenOffset, characterEnd) 
        SELECT p.tokenOffset, p.characterEnd
        FROM catmaindex.term t
        JOIN catmaindex.position p ON p.termID = t.termID
        WHERE t.documentID=docID
        AND CASE WHEN wild = 0 THEN
            t.term = term2
        ELSE t.term like term2 END;
    END IF;
    
    CREATE temporary TABLE termbuf2 (
        tokenOffset INT,
        characterEnd INT
    ); 

    IF (term3 IS NOT NULL)
    THEN
        INSERT INTO termbuf2(tokenOffset, characterEnd) 
        SELECT p.tokenOffset, p.characterEnd
        FROM catmaindex.term t
        JOIN catmaindex.position p ON p.termID = t.termID
        WHERE t.documentID=docID
        AND CASE WHEN wild = 0 THEN
            t.term = term3
        ELSE t.term like term3 END;
    END IF;
   

    CREATE temporary TABLE termbuf3 (
        tokenOffset INT,
        characterEnd INT
    ); 

    IF (term4 IS NOT NULL)
    THEN
        INSERT INTO termbuf3(tokenOffset, characterEnd) 
        SELECT p.tokenOffset, p.characterEnd
        FROM catmaindex.term t
        JOIN catmaindex.position p ON p.termID = t.termID
        WHERE t.documentID=docID
        AND CASE WHEN wild = 0 THEN
            t.term = term4
        ELSE t.term like term4 END;
    END IF;

    CREATE temporary TABLE termbuf4 (
        tokenOffset INT,
        characterEnd INT
    ); 

    IF (term5 IS NOT NULL)
    THEN
        INSERT INTO termbuf4(tokenOffset, characterEnd) 
        SELECT p.tokenOffset, p.characterEnd
        FROM catmaindex.term t
        JOIN catmaindex.position p ON p.termID = t.termID
        WHERE t.documentID=docID
        AND CASE WHEN wild = 0 THEN
            t.term = term5
        ELSE t.term like term5 END;
    END IF;
    
    /* fill result with all terms matching term1 that have an entry with 
     * the corresponding tokenoffset for all non null term2-term5
     * this gives us all tokenOffsets and characterStarts for valid term1 entries*/
    INSERT INTO result(tokenOffset, characterStart, characterEnd)
    SELECT
    pos.tokenOffset as tokenOffset,
    pos.characterStart as characterStart,
    pos.characterEnd as characterEnd
    FROM catmaindex.term t 
    JOIN catmaindex.position pos on pos.termID=t.termID
    WHERE t.documentID=docID
    AND CASE WHEN wild = 0 THEN
        t.term = term1
    ELSE 
        CASE WHEN term1 IS NOT NULL THEN t.term like term1 
        ELSE 1=1 END
    END
    AND CASE WHEN term2 IS NOT NULL THEN pos.tokenOffset+1 IN (
        SELECT tm1.tokenOffset FROM termbuf1 tm1
    ) ELSE 1=1 END
    AND CASE WHEN term3 IS NOT NULl THEN pos.tokenOffset+2 IN (
        SELECT tm2.tokenOffset FROM termbuf2 tm2
    ) ELSE 1=1 END
    AND CASE WHEN term4 IS NOT NULl THEN pos.tokenOffset+3 IN (
        SELECT tm3.tokenOffset FROM termbuf3 tm3
    ) ELSE 1=1 END
    AND CASE WHEN term5 IS NOT NULl THEN pos.tokenOffset+4 IN (
        SELECT tm4.tokenOffset FROM termbuf4 tm4
    ) ELSE 1=1 END;

    /* now we fill the result with the end positions of the matching phrases, i. e. the characterEnd of the 
     * terms that matched the last non null term and belong to a matching phrase*/
    IF (term5 IS NOT NULL) THEN
        UPDATE result r, termbuf4 t SET r.characterEnd = t.characterEnd WHERE r.tokenOffset+4=t.tokenOffset;
    ELSEIF (term4 IS NOT NULL) THEN
        UPDATE result r, termbuf3 t SET r.characterEnd = t.characterEnd WHERE r.tokenOffset+3=t.tokenOffset;
    ELSEIF (term3 IS NOT NULL) THEN
        UPDATE result r, termbuf2 t SET r.characterEnd = t.characterEnd WHERE r.tokenOffset+2=t.tokenOffset;
    ELSEIF (term2 IS NOT NULL) THEN
        UPDATE result r, termbuf1 t SET r.characterEnd = t.characterEnd WHERE r.tokenOffset+1=t.tokenOffset;
    END IF;

    /* respect the limit */ 
    IF (limitresult > 0) THEN
        SET @dynSQL = CONCAT('SELECT tokenOffset, characterStart, characterEnd FROM result LIMIT 0,', limitresult);
        PREPARE stmt FROM @dynSQL;
        EXECUTE stmt;
    ELSE
        SELECT tokenOffset, characterStart, characterEnd FROM result;
    END IF;

    DROP TABLE termbuf1;
    DROP TABLE termbuf2;
    DROP TABLE termbuf3;
    DROP TABLE termbuf4;
    DROP TABLE result;

END$$

delimiter ;
