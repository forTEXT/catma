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
	DECLARE tokenOffset INT;
	DECLARE baseRowID INT;

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
		tokenOffset INT
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
		tokenOffset INT
	);

    SET arrayElementIndex = 1;
	REPEAT 
		SELECT getArrayElement(base, ';', arrayElementIndex)
		INTO arrayElement;
		
		IF (LENGTH(arrayElement)>0) THEN
			SET sourceDocumentID = getArrayElement(arrayElement, ',', 1);
			SET characterStart = getArrayElement(arrayElement, ',', 2);
			SET characterEnd = getArrayElement(arrayElement, ',', 3);

			INSERT INTO baserows(sourceDocumentID, characterStart, characterEnd, tokenMinOffset, tokenMaxOffset)
			SELECT sourceDocumentID, characterStart, characterEnd, 
				min(p.tokenOffset)-contextSize, max(p.tokenOffset)+contextSize
			FROM CatmaIndex.docposition p
			WHERE p.documentID = sourceDocumentID
			AND p.characterStart < characterEnd AND p.characterEnd > characterStart;

			SET baseRowID = LAST_INSERT_ID();

			SELECT b.tokenMinOffset, b.tokenMaxOffset 
			FROM baserows b
			WHERE b.baseID = baseRowID
			INTO tokenMinOffset, tokenMaxOffset;

			SET tokenOffset = tokenMinOffset;
			WHILE tokenOffset <= tokenMaxOffset DO
				INSERT INTO basecontext(baseID, tokenOffset) 
				VALUES (baseRowID, tokenOffset);
				SET tokenOffset = tokenOffset+1;
			END WHILE;

		END IF;
		SET arrayElementIndex = arrayElementIndex+1;
	UNTIL LENGTH(arrayElement) <= 0
	END REPEAT;

	INSERT INTO log(msg, zeit)
	SELECT 'nach baserows und basecontext fuellen', now();

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

	INSERT INTO condtoken(condID, tokenOffset)
	SELECT c.condID, p.tokenOffset
	FROM condrows c
	JOIN CatmaIndex.docposition p ON p.documentID = c.sourceDocumentID
	WHERE p.characterStart < c.characterEnd AND p.characterEnd > c.characterStart;


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

