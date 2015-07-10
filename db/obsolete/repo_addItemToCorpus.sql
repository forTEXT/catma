
delimiter $$

DROP PROCEDURE IF EXISTS addItemToCorpus$$

CREATE PROCEDURE addItemToCorpus (
    userMarkupCollectionID INT, sourceDocumentLocalUri VARCHAR(300), corpusID INT)
BEGIN
    DECLARE sourceDocumentID INT;
    
    IF sourceDocumentLocalUri IS NOT NULL THEN
        SELECT sd.sourceDocumentID INTO sourceDocumentID
        FROM sourcedocument sd 
        WHERE sd.localUri = sourceDocumentLocalUri;
    END IF;
    
    IF userMarkupCollectionID IS NOT NULL THEN
        INSERT INTO user_usermarkupcollection(userID, userMarkupCollectionID, accessMode, owner)
        SELECT uc.userID, userMarkupCollectionID, uc.accessMode, 0
        FROM user_corpus uc
        WHERE uc.corpusID = corpusID AND uc.userID NOT IN (
            SELECT uumc.userID 
            FROM user_usermarkupcollection uumc
            WHERE uumc.userMarkupCollectionID = userMarkupCollectionID
        );
        
        INSERT INTO corpus_usermarkupcollection(corpusID, userMarkupCollectionID) 
        VALUES(corpusID, userMarkupCollectionID);
    END IF;
    
    IF sourceDocumentID IS NOT NULL THEN
        INSERT INTO user_sourcedocument(userID, sourceDocumentID, accessMode, owner)
        SELECT uc.userID, sourceDocumentID, uc.accessMode, 0
        FROM user_corpus uc
        WHERE uc.corpusID = corpusID AND uc.userID NOT IN (
            SELECT usd.userID
            FROM user_sourcedocument usd
            WHERE usd.sourceDocumentID = sourceDocumentID
        );
        INSERT INTO corpus_sourcedocument(corpusID, sourceDocumentID)
        VALUES(corpusID, sourceDocumentID);
    END IF;
END$$

delimiter ;
