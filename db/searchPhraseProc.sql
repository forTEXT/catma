
delimiter $$

DROP PROCEDURE IF EXISTS searchPhrase$$

CREATE PROCEDURE searchPhrase (
    term1 VARCHAR(300), term2 VARCHAR(300), 
    term3 VARCHAR(300), term4 VARCHAR(300), term5 VARCHAR(300), 
    docID VARCHAR(300))
BEGIN

    CREATE temporary TABLE result (
        tokenOffset INT,
        characterStart INT,
        characterEnd INT
    );

    CREATE temporary TABLE termbuf1 (
        tokenOffset INT,
        characterEnd INT
    ); 
    
    INSERT INTO termbuf1(tokenOffset, characterEnd) 
    SELECT p.tokenOffset, p.characterEnd
    FROM CatmaIndex.term t
    JOIN CatmaIndex.position p ON p.termID = t.termID
    WHERE t.documentID=docID
    and t.term = term2;

    CREATE temporary TABLE termbuf2 (
        tokenOffset INT,
        characterEnd INT
    ); 

    IF (term3 IS NOT NULL)
    THEN
        INSERT INTO termbuf2(tokenOffset, characterEnd) 
        SELECT p.tokenOffset, p.characterEnd
        FROM CatmaIndex.term t
        JOIN CatmaIndex.position p ON p.termID = t.termID
        WHERE t.documentID=docID
        and t.term = term3;
    END IF;
   

    CREATE temporary TABLE termbuf3 (
        tokenOffset INT,
        characterEnd INT
    ); 

    IF (term4 IS NOT NULL)
    THEN
        INSERT INTO termbuf3(tokenOffset, characterEnd) 
        SELECT p.tokenOffset, p.characterEnd
        FROM CatmaIndex.term t
        JOIN CatmaIndex.position p ON p.termID = t.termID
        WHERE t.documentID=docID
        and t.term = term4;
    END IF;

    CREATE temporary TABLE termbuf4 (
        tokenOffset INT,
        characterEnd INT
    ); 

    IF (term5 IS NOT NULL)
    THEN
        INSERT INTO termbuf4(tokenOffset, characterEnd) 
        SELECT p.tokenOffset, p.characterEnd
        FROM CatmaIndex.term t
        JOIN CatmaIndex.position p ON p.termID = t.termID
        WHERE t.documentID=docID
        and t.term = term5;
    END IF;
    
    INSERT INTO result(tokenOffset, characterStart)
    SELECT
    pos.tokenOffset as tokenOffset,
    pos.characterStart as characterStart
    FROM CatmaIndex.term t 
    JOIN CatmaIndex.position pos on pos.termID=t.termID
    WHERE t.documentID=docID
    AND t.term = term1 
    AND pos.tokenOffset+1 IN (
        SELECT tm1.tokenOffset FROM termbuf1 tm1
    )
    AND CASE WHEN term3 IS NOT NULl THEN pos.tokenOffset+2 IN (
        SELECT tm2.tokenOffset FROM termbuf2 tm2
    ) ELSE 1=1 END
    AND CASE WHEN term4 IS NOT NULl THEN pos.tokenOffset+3 IN (
        SELECT tm3.tokenOffset FROM termbuf3 tm3
    ) ELSE 1=1 END
    AND CASE WHEN term5 IS NOT NULl THEN pos.tokenOffset+4 IN (
        SELECT tm4.tokenOffset FROM termbuf4 tm4
    ) ELSE 1=1 END;

    IF (term5 IS NOT NULL) THEN
        UPDATE result r, termbuf4 t SET r.characterEnd = t.characterEnd WHERE r.tokenOffset+4=t.tokenOffset;
    ELSEIF (term4 IS NOT NULL) THEN
        UPDATE result r, termbuf3 t SET r.characterEnd = t.characterEnd WHERE r.tokenOffset+3=t.tokenOffset;
    ELSEIF (term3 IS NOT NULL) THEN
        UPDATE result r, termbuf2 t SET r.characterEnd = t.characterEnd WHERE r.tokenOffset+2=t.tokenOffset;
    ELSE 
        UPDATE result r, termbuf1 t SET r.characterEnd = t.characterEnd WHERE r.tokenOffset+1=t.tokenOffset;
    END IF;

    SELECT tokenOffset, characterStart, characterEnd FROM result;

    DROP TABLE termbuf1;
    DROP TABLE termbuf2;
    DROP TABLE termbuf3;
    DROP TABLE termbuf4;
    DROP TABLE result;

END$$

delimiter ;
