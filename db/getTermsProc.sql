
delimiter $$

DROP PROCEDURE IF EXISTS getTerms$$

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
