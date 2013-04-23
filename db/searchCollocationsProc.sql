delimiter $$

DROP PROCEDURE IF EXISTS searchCollocations$$

CREATE DEFINER=`root`@`localhost` PROCEDURE `searchCollocations`(
    base VARCHAR(15000), cond VARCHAR(15000), contextSize INT)
BEGIN
    DECLARE arrayElement VARCHAR(500);
	DECLARE arrayElementIndex INT;
	DECLARE sourceDocumentID VARCHAR(300);
	DECLARE characterStart INT;
	DECLARE characterEnd INT;
	DECLARE tokenMinOffset INT;
	DECLARE tokenMaxOffset INT;

	CREATE TEMPORARY TABLE log (
		msg VARCHAR(200),
		zeit TIMESTAMP
	);

	INSERT INTO log(msg, zeit)
	SELECT 'proc start', now();

    CREATE TEMPORARY TABLE baserows (
		baseID INT AUTO_INCREMENT PRIMARY KEY,
        sourceDocumentID VARCHAR(300),
        characterStart INT,
        characterEnd INT,
		tokenMinOffset INT,
		tokenMaxOffset INT
    );

	CREATE TEMPORARY TABLE basecontext (
		baseID INT,
		tokenOffset INT,
		term VARCHAR(300)
	);

    CREATE TEMPORARY TABLE condrows (
		condID INT AUTO_INCREMENT PRIMARY KEY,
        sourceDocumentID VARCHAR(300),
		characterStart INT,
        characterEnd INT,
		KEY condrows_char1 (characterStart),
		KEY condrows_char2 (characterEnd),
		KEY condrows_sourceDocID (sourceDocumentID)
    ); 

	CREATE TEMPORARY TABLE condtoken (
		condID INT,
		tokenOffset INT,
		term VARCHAR(300)
	);

    SET arrayElementIndex = 1;
	REPEAT 
		SELECT getArrayElement(base, ';', arrayElementIndex)
		INTO arrayElement;
		
		IF (LENGTH(arrayElement)>0) THEN
			SET sourceDocumentID = getArrayElement(arrayElement, ',', 1);
			SET characterStart = getArrayElement(arrayElement, ',', 2);
			SET characterEnd = getArrayElement(arrayElement, ',', 3);
			
			SELECT min(p.tokenOffset), max(p.tokenOffset)
			FROM CatmaIndex.position p 
			JOIN CatmaIndex.term t ON t.termID = p.termID 
			WHERE t.documentID = sourceDocumentID
			AND characterStart <= p.characterStart AND p.characterEnd <= characterEnd
			INTO tokenMinOffset, tokenMaxOffset;

			INSERT INTO baserows(sourceDocumentID, characterStart, characterEnd, tokenMinOffset, tokenMaxOffset)
			VALUES(sourceDocumentID, characterStart, characterEnd, tokenMinOffset, tokenMaxOffset);

		END IF;
		SET arrayElementIndex = arrayElementIndex+1;
	UNTIL LENGTH(arrayElement) <= 0
	END REPEAT;

	INSERT INTO log(msg, zeit)
	SELECT 'nach baserows fuellen', now();

    SET arrayElementIndex = 1;
	REPEAT 
		SELECT getArrayElement(cond, ';', arrayElementIndex)
		INTO arrayElement;
		
		IF (LENGTH(arrayElement)>0) THEN
			INSERT INTO condrows(sourceDocumentId, characterStart, characterEnd)
			VALUES (
				TRIM(getArrayElement(arrayElement, ',', 1)),
				TRIM(getArrayElement(arrayElement, ',', 2)),	
				TRIM(getArrayElement(arrayElement, ',', 3)));
		END IF;
		SET arrayElementIndex = arrayElementIndex+1;
	UNTIL LENGTH(arrayElement) <= 0
	END REPEAT;

	INSERT INTO log(msg, zeit)
	SELECT 'nach condrows fuellen', now();

	INSERT INTO basecontext(baseID, tokenOffset, term) 
	SELECT b.baseID, p.tokenOffset, t.term 
	FROM baserows b
	JOIN CatmaIndex.term t ON t.documentID = b.sourceDocumentID
	JOIN CatmaIndex.position p ON t.termID = p.termID 
	WHERE p.tokenOffset <= b.tokenMaxOffset+contextSize AND p.tokenOffset >= b.tokenMinOffset-contextSize;

	INSERT INTO log(msg, zeit)
	SELECT 'nach basecontext fuellen', now();

	INSERT INTO condtoken(condID, tokenOffset, term)
	SELECT c.condID, p.tokenOffset, t.term
	FROM condrows c
	JOIN CatmaIndex.term t ON t.documentID = c.sourceDocumentID
	JOIN CatmaIndex.position p ON t.termID = p.termID 
	WHERE c.characterStart <= p.characterStart AND p.characterEnd <= c.characterEnd;

	INSERT INTO log(msg, zeit)
	SELECT 'nach condtoken fuellen', now();

	SELECT DISTINCT b.sourceDocumentID, b.characterStart, b.characterEnd
	FROM baserows b 
	JOIN basecontext bc ON bc.baseID = b.baseID 
	JOIN condtoken ct ON ct.tokenOffset = bc.tokenOffset
	JOIN condrows c ON c.condID = ct.condID and c.sourceDocumentID = b.sourceDocumentID;

	INSERT INTO log(msg, zeit)
	SELECT 'nach end select', now();

	SELECT msg, zeit FROM log order by zeit asc;

	DROP TABLE condtoken;
	DROP TABLE basecontext;
    DROP TABLE baserows;
    DROP TABLE condrows;
	DROP TABLE log;
END$$

