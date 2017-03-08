SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL,ALLOW_INVALID_DATES';

CREATE DATABASE `catmaindex` /*!40100 DEFAULT CHARACTER SET utf8 */;
CREATE DATABASE `catmarepository` /*!40100 DEFAULT CHARACTER SET utf8 COLLATE utf8_bin */;

USE catmaindex;

-- -----------------------------------------------------
-- Table `catmaindex`.`term`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `catmaindex`.`term` (
  `termID` INT(11) NOT NULL AUTO_INCREMENT COMMENT '',
  `documentID` VARCHAR(300) NOT NULL COMMENT '',
  `frequency` INT(11) NOT NULL COMMENT '',
  `term` VARCHAR(300) CHARACTER SET 'utf8' COLLATE 'utf8_bin' NOT NULL COMMENT '',
  PRIMARY KEY (`termID`)  COMMENT '',
  INDEX `TERM_IDX` USING BTREE (`documentID` ASC, `term` ASC)  COMMENT '')
ENGINE = InnoDB
AUTO_INCREMENT = 5874
DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `catmaindex`.`position`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `catmaindex`.`position` (
  `positionID` INT(11) NOT NULL AUTO_INCREMENT COMMENT '',
  `termID` INT(11) NOT NULL COMMENT '',
  `characterStart` INT(11) NOT NULL COMMENT '',
  `characterEnd` INT(11) NOT NULL COMMENT '',
  `tokenOffset` INT(11) NOT NULL COMMENT '',
  PRIMARY KEY (`positionID`)  COMMENT '',
  INDEX `FK_PositionTermID_idx` (`termID` ASC)  COMMENT '',
  INDEX `TOKENOFFS_IDX` USING HASH (`tokenOffset` ASC)  COMMENT '',
  INDEX `CHAROFFS_IDX` (`characterStart` ASC, `characterEnd` ASC)  COMMENT '',
  CONSTRAINT `FK_PositionTermID`
    FOREIGN KEY (`termID`)
    REFERENCES `catmaindex`.`term` (`termID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB
AUTO_INCREMENT = 47403
DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `catmaindex`.`tagreference`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `catmaindex`.`tagreference` (
  `tagReferenceID` INT(11) NOT NULL AUTO_INCREMENT COMMENT '',
  `documentID` VARCHAR(300) NOT NULL COMMENT '',
  `userMarkupCollectionID` VARCHAR(300) NOT NULL COMMENT '',
  `tagDefinitionPath` VARCHAR(2048) NOT NULL COMMENT '',
  `tagDefinitionID` BINARY(16) NOT NULL COMMENT '',
  `tagInstanceID` BINARY(16) NOT NULL COMMENT '',
  `tagDefinitionVersion` VARCHAR(28) NOT NULL COMMENT '',
  `characterStart` INT(11) NOT NULL COMMENT '',
  `characterEnd` INT(11) NOT NULL COMMENT '',
  PRIMARY KEY (`tagReferenceID`)  COMMENT '',
  INDEX `I_searchTr` (`userMarkupCollectionID`(12) ASC, `tagDefinitionID` ASC, `tagInstanceID` ASC)  COMMENT '',
  INDEX `I_searchTr2` (`userMarkupCollectionID`(12) ASC, `tagDefinitionPath`(150) ASC)  COMMENT '')
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `catmaindex`.`property`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `catmaindex`.`property` (
  `propertyID` INT NOT NULL AUTO_INCREMENT COMMENT '',
  `tagInstanceID` BINARY(16) NOT NULL COMMENT '',
  `propertyDefinitionID` BINARY(16) NOT NULL COMMENT '',
  `name` VARCHAR(45) NOT NULL COMMENT '',
  `value` VARCHAR(1024) NULL COMMENT '',
  PRIMARY KEY (`propertyID`)  COMMENT '',
  INDEX `I_pTagInstanceID` (`tagInstanceID` ASC, `propertyDefinitionID` ASC)  COMMENT '',
  INDEX `I_pValue` (`value`(200) ASC)  COMMENT '')
ENGINE = InnoDB;


DELIMITER $$
CREATE PROCEDURE `getTerms`(
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
        FROM catmaindex.term t
        JOIN catmaindex.position p ON p.termID = t.termID
        WHERE t.documentID=docID
        AND p.tokenOffset = basePos+counter;
        SET counter = counter+1;
    END WHILE;
    
    SELECT term, characterStart, characterEnd FROM result_getTerms;

    DROP TABLE result_getTerms;

END$$
DELIMITER ;

DELIMITER $$
CREATE PROCEDURE `searchPhrase`(
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
DELIMITER ;

DELIMITER $$
CREATE FUNCTION `getTagDefinitionPath`(curTagDefinitionID INT) RETURNS varchar(2048) CHARSET utf8
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
DELIMITER ;



USE catmarepository;

-- -----------------------------------------------------
-- Table `catmarepository`.`sourcedocument`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `catmarepository`.`sourcedocument` (
  `sourceDocumentID` INT NOT NULL AUTO_INCREMENT COMMENT '',
  `title` VARCHAR(300) NULL COMMENT '',
  `publisher` VARCHAR(300) NULL COMMENT '',
  `author` VARCHAR(300) NULL COMMENT '',
  `description` VARCHAR(300) NULL COMMENT '',
  `sourceUri` VARCHAR(300) NULL COMMENT '',
  `fileType` VARCHAR(5) NOT NULL COMMENT '',
  `charset` VARCHAR(50) NULL COMMENT '',
  `fileOSType` VARCHAR(15) NOT NULL COMMENT '',
  `checksum` BIGINT NOT NULL COMMENT '',
  `mimeType` VARCHAR(255) NULL COMMENT '',
  `xsltDocumentLocalUri` VARCHAR(300) NULL COMMENT '',
  `locale` VARCHAR(15) NOT NULL COMMENT '',
  `localUri` VARCHAR(300) NULL COMMENT '',
  PRIMARY KEY (`sourceDocumentID`)  COMMENT '',
  INDEX `IDX_localUri` (`localUri`(200) ASC)  COMMENT '')
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `catmarepository`.`userdefined_separatingcharacter`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `catmarepository`.`userdefined_separatingcharacter` (
  `udscID` INT NOT NULL AUTO_INCREMENT COMMENT '',
  `chr` VARCHAR(1) NOT NULL COMMENT '',
  `sourceDocumentID` INT NOT NULL COMMENT '',
  PRIMARY KEY (`udscID`)  COMMENT '',
  INDEX `FK_udsc_sourceDocumentID_idx` (`sourceDocumentID` ASC)  COMMENT '',
  CONSTRAINT `FK_udsc_sourceDocumentID`
    FOREIGN KEY (`sourceDocumentID`)
    REFERENCES `catmarepository`.`sourcedocument` (`sourceDocumentID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `catmarepository`.`unseparable_charsequence`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `catmarepository`.`unseparable_charsequence` (
  `uscID` INT NOT NULL AUTO_INCREMENT COMMENT '',
  `charsequence` VARCHAR(45) NOT NULL COMMENT '',
  `sourceDocumentID` INT NOT NULL COMMENT '',
  PRIMARY KEY (`uscID`)  COMMENT '',
  INDEX `FK_uscs_sourceDocumentID_idx` (`sourceDocumentID` ASC)  COMMENT '',
  CONSTRAINT `FK_uscs_sourceDocumentID`
    FOREIGN KEY (`sourceDocumentID`)
    REFERENCES `catmarepository`.`sourcedocument` (`sourceDocumentID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `catmarepository`.`taglibrary`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `catmarepository`.`taglibrary` (
  `tagLibraryID` INT NOT NULL AUTO_INCREMENT COMMENT '',
  `title` VARCHAR(300) NOT NULL COMMENT '',
  `publisher` VARCHAR(300) NULL COMMENT '',
  `author` VARCHAR(300) NULL COMMENT '',
  `description` VARCHAR(300) NULL COMMENT '',
  `independent` TINYINT(1) NOT NULL COMMENT '',
  PRIMARY KEY (`tagLibraryID`)  COMMENT '')
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `catmarepository`.`usermarkupcollection`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `catmarepository`.`usermarkupcollection` (
  `usermarkupCollectionID` INT NOT NULL AUTO_INCREMENT COMMENT '',
  `title` VARCHAR(300) NULL COMMENT '',
  `publisher` VARCHAR(300) NULL COMMENT '',
  `author` VARCHAR(300) NULL COMMENT '',
  `description` VARCHAR(300) NULL COMMENT '',
  `sourceDocumentID` INT NOT NULL COMMENT '',
  `tagLibraryID` INT NOT NULL COMMENT '',
  PRIMARY KEY (`usermarkupCollectionID`)  COMMENT '',
  INDEX `FK_umc_sourceDocumentID_idx` (`sourceDocumentID` ASC)  COMMENT '',
  INDEX `FK_umc_tagLibraryID_idx` (`tagLibraryID` ASC)  COMMENT '',
  CONSTRAINT `FK_umc_sourceDocumentID`
    FOREIGN KEY (`sourceDocumentID`)
    REFERENCES `catmarepository`.`sourcedocument` (`sourceDocumentID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `FK_umc_tagLibraryID`
    FOREIGN KEY (`tagLibraryID`)
    REFERENCES `catmarepository`.`taglibrary` (`tagLibraryID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `catmarepository`.`tagsetdefinition`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `catmarepository`.`tagsetdefinition` (
  `tagsetDefinitionID` INT NOT NULL AUTO_INCREMENT COMMENT '',
  `uuid` BINARY(16) NOT NULL COMMENT '',
  `version` DATETIME NOT NULL COMMENT '',
  `name` VARCHAR(255) NOT NULL COMMENT '',
  `tagLibraryID` INT NOT NULL COMMENT '',
  PRIMARY KEY (`tagsetDefinitionID`)  COMMENT '',
  UNIQUE INDEX `UK_tsDef_tLib_uuid` (`uuid` ASC, `tagLibraryID` ASC)  COMMENT '',
  INDEX `FK_tsDef_tagLibraryID_idx` (`tagLibraryID` ASC)  COMMENT '',
  CONSTRAINT `FK_tsDef_tagLibraryID`
    FOREIGN KEY (`tagLibraryID`)
    REFERENCES `catmarepository`.`taglibrary` (`tagLibraryID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `catmarepository`.`tagdefinition`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `catmarepository`.`tagdefinition` (
  `tagDefinitionID` INT NOT NULL AUTO_INCREMENT COMMENT '',
  `uuid` BINARY(16) NOT NULL COMMENT '',
  `version` DATETIME NOT NULL COMMENT '',
  `name` VARCHAR(255) NOT NULL COMMENT '',
  `tagsetDefinitionID` INT NOT NULL COMMENT '',
  `parentID` INT NULL COMMENT '',
  `parentUuid` BINARY(16) NULL COMMENT '',
  PRIMARY KEY (`tagDefinitionID`)  COMMENT '',
  INDEX `FK_tdef_tagsetDefinitionID_idx` (`tagsetDefinitionID` ASC)  COMMENT '',
  INDEX `FK_tdef_parentID_idx` (`parentID` ASC)  COMMENT '',
  UNIQUE INDEX `UK_tdef_tsdef_uuid` (`uuid` ASC, `tagsetDefinitionID` ASC)  COMMENT '',
  CONSTRAINT `FK_tdef_tagsetDefinitionID`
    FOREIGN KEY (`tagsetDefinitionID`)
    REFERENCES `catmarepository`.`tagsetdefinition` (`tagsetDefinitionID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `FK_tdef_parentID`
    FOREIGN KEY (`parentID`)
    REFERENCES `catmarepository`.`tagdefinition` (`tagDefinitionID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `catmarepository`.`propertydefinition`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `catmarepository`.`propertydefinition` (
  `propertyDefinitionID` INT NOT NULL AUTO_INCREMENT COMMENT '',
  `uuid` BINARY(16) NOT NULL COMMENT '',
  `name` VARCHAR(45) NOT NULL COMMENT '',
  `tagDefinitionID` INT NOT NULL COMMENT '',
  `systemproperty` TINYINT(1) NOT NULL COMMENT '',
  PRIMARY KEY (`propertyDefinitionID`)  COMMENT '',
  UNIQUE INDEX `UK_pd_uuid_tdef` (`uuid` ASC, `tagDefinitionID` ASC)  COMMENT '',
  INDEX `FK_pd_tagDefinitionID_idx` (`tagDefinitionID` ASC)  COMMENT '',
  CONSTRAINT `FK_pd_tagDefinitionID`
    FOREIGN KEY (`tagDefinitionID`)
    REFERENCES `catmarepository`.`tagdefinition` (`tagDefinitionID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `catmarepository`.`taginstance`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `catmarepository`.`taginstance` (
  `tagInstanceID` INT NOT NULL AUTO_INCREMENT COMMENT '',
  `uuid` BINARY(16) NOT NULL COMMENT '',
  `tagDefinitionID` INT NOT NULL COMMENT '',
  PRIMARY KEY (`tagInstanceID`)  COMMENT '',
  UNIQUE INDEX `UK_ti_uuid` (`uuid` ASC)  COMMENT '',
  INDEX `FK_ti_tagDefinitionID_idx` (`tagDefinitionID` ASC)  COMMENT '',
  CONSTRAINT `FK_ti_tagDefinitionID`
    FOREIGN KEY (`tagDefinitionID`)
    REFERENCES `catmarepository`.`tagdefinition` (`tagDefinitionID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `catmarepository`.`property`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `catmarepository`.`property` (
  `propertyID` INT NOT NULL AUTO_INCREMENT COMMENT '',
  `propertyDefinitionID` INT NOT NULL COMMENT '',
  `tagInstanceID` INT NOT NULL COMMENT '',
  PRIMARY KEY (`propertyID`)  COMMENT '',
  INDEX `FK_prop_propertyDefinitionID_idx` (`propertyDefinitionID` ASC)  COMMENT '',
  INDEX `FK_prop_tagInstanceID_idx` (`tagInstanceID` ASC)  COMMENT '',
  CONSTRAINT `FK_prop_propertyDefinitionID`
    FOREIGN KEY (`propertyDefinitionID`)
    REFERENCES `catmarepository`.`propertydefinition` (`propertyDefinitionID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `FK_prop_tagInstanceID`
    FOREIGN KEY (`tagInstanceID`)
    REFERENCES `catmarepository`.`taginstance` (`tagInstanceID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `catmarepository`.`tagreference`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `catmarepository`.`tagreference` (
  `tagReferenceID` INT NOT NULL AUTO_INCREMENT COMMENT '',
  `characterStart` INT NOT NULL COMMENT '',
  `characterEnd` INT NOT NULL COMMENT '',
  `userMarkupCollectionID` INT NOT NULL COMMENT '',
  `tagInstanceID` INT NOT NULL COMMENT '',
  PRIMARY KEY (`tagReferenceID`)  COMMENT '',
  INDEX `FK_tr_userMarkupCollectionID_idx` (`userMarkupCollectionID` ASC)  COMMENT '',
  INDEX `FK_tr_tagInstanceID_idx` (`tagInstanceID` ASC)  COMMENT '',
  CONSTRAINT `FK_tr_userMarkupCollectionID`
    FOREIGN KEY (`userMarkupCollectionID`)
    REFERENCES `catmarepository`.`usermarkupcollection` (`usermarkupCollectionID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `FK_tr_tagInstanceID`
    FOREIGN KEY (`tagInstanceID`)
    REFERENCES `catmarepository`.`taginstance` (`tagInstanceID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `catmarepository`.`propertydef_possiblevalue`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `catmarepository`.`propertydef_possiblevalue` (
  `propertydefPossibleValueID` INT NOT NULL AUTO_INCREMENT COMMENT '',
  `value` VARCHAR(255) NULL COMMENT '',
  `propertyDefinitionID` INT NOT NULL COMMENT '',
  PRIMARY KEY (`propertydefPossibleValueID`)  COMMENT '',
  INDEX `FK_ppv_propertyDefinitionID_idx` (`propertyDefinitionID` ASC)  COMMENT '',
  CONSTRAINT `FK_ppv_propertyDefinitionID`
    FOREIGN KEY (`propertyDefinitionID`)
    REFERENCES `catmarepository`.`propertydefinition` (`propertyDefinitionID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `catmarepository`.`staticmarkupcollection`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `catmarepository`.`staticmarkupcollection` (
  `staticMarkupCollectionID` INT NOT NULL AUTO_INCREMENT COMMENT '',
  `name` VARCHAR(45) NOT NULL COMMENT '',
  `sourceDocumentID` INT NOT NULL COMMENT '',
  PRIMARY KEY (`staticMarkupCollectionID`)  COMMENT '',
  INDEX `FK_staticmc_sourceDocumentID` (`sourceDocumentID` ASC)  COMMENT '',
  CONSTRAINT `FK_staticmc_sourceDocumentID00`
    FOREIGN KEY (`sourceDocumentID`)
    REFERENCES `catmarepository`.`sourcedocument` (`sourceDocumentID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `catmarepository`.`staticmarkupinstance`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `catmarepository`.`staticmarkupinstance` (
  `staticMarkupInstanceID` INT NOT NULL AUTO_INCREMENT COMMENT '',
  `name` VARCHAR(300) NOT NULL COMMENT '',
  `staticMarkupCollectionID` INT NULL COMMENT '',
  PRIMARY KEY (`staticMarkupInstanceID`)  COMMENT '',
  INDEX `FK_staticmi_staticMarkupCollectionID_idx` (`staticMarkupCollectionID` ASC)  COMMENT '',
  CONSTRAINT `FK_staticmi_staticMarkupCollectionID`
    FOREIGN KEY (`staticMarkupCollectionID`)
    REFERENCES `catmarepository`.`staticmarkupcollection` (`staticMarkupCollectionID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `catmarepository`.`staticmarkupattribute`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `catmarepository`.`staticmarkupattribute` (
  `staticMarkupAttributeID` INT NOT NULL AUTO_INCREMENT COMMENT '',
  `name` VARCHAR(45) NOT NULL COMMENT '',
  `value` VARCHAR(45) NOT NULL COMMENT '',
  `staticMarkupInstanceID` INT NOT NULL COMMENT '',
  PRIMARY KEY (`staticMarkupAttributeID`)  COMMENT '',
  INDEX `FK_statica_staticMarkupInstanceID_idx` (`staticMarkupInstanceID` ASC)  COMMENT '',
  CONSTRAINT `FK_statica_staticMarkupInstanceID`
    FOREIGN KEY (`staticMarkupInstanceID`)
    REFERENCES `catmarepository`.`staticmarkupinstance` (`staticMarkupInstanceID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `catmarepository`.`propertyvalue`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `catmarepository`.`propertyvalue` (
  `propertyValueID` INT NOT NULL AUTO_INCREMENT COMMENT '',
  `propertyID` INT NOT NULL COMMENT '',
  `value` VARCHAR(1024) NULL COMMENT '',
  PRIMARY KEY (`propertyValueID`)  COMMENT '',
  INDEX `FK_propv_propertyID_idx` (`propertyID` ASC)  COMMENT '',
  CONSTRAINT `FK_propv_propertyID`
    FOREIGN KEY (`propertyID`)
    REFERENCES `catmarepository`.`property` (`propertyID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `catmarepository`.`user`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `catmarepository`.`user` (
  `userID` INT NOT NULL AUTO_INCREMENT COMMENT '',
  `identifier` VARCHAR(300) NOT NULL COMMENT '',
  `locked` TINYINT(1) NOT NULL DEFAULT true COMMENT '',
  `email` VARCHAR(300) NULL COMMENT '',
  `lastlogin` DATETIME NULL COMMENT '',
  `firstlogin` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP COMMENT '',
  `guest` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '',
  `spawnable` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '',
  PRIMARY KEY (`userID`)  COMMENT '')
ENGINE = InnoDB;

-- -----------------------------------------------------
-- Table `catmarepository`.`user_sourcedocument`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `catmarepository`.`user_sourcedocument` (
  `user_sourcedocumentID` INT NOT NULL AUTO_INCREMENT COMMENT '',
  `userID` INT NOT NULL COMMENT '',
  `sourceDocumentID` INT NOT NULL COMMENT '',
  `accessMode` INT NOT NULL COMMENT '',
  `owner` TINYINT(1) NOT NULL COMMENT '',
  PRIMARY KEY (`user_sourcedocumentID`)  COMMENT '',
  INDEX `FK_usersd_userID_idx` (`userID` ASC)  COMMENT '',
  INDEX `FK_usersd_sourceDocumentID_idx` (`sourceDocumentID` ASC)  COMMENT '',
  CONSTRAINT `FK_usersd_userID`
    FOREIGN KEY (`userID`)
    REFERENCES `catmarepository`.`user` (`userID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `FK_usersd_sourceDocumentID`
    FOREIGN KEY (`sourceDocumentID`)
    REFERENCES `catmarepository`.`sourcedocument` (`sourceDocumentID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `catmarepository`.`user_staticmarkupcollection`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `catmarepository`.`user_staticmarkupcollection` (
  `user_staticmarkupcollectionID` INT NOT NULL AUTO_INCREMENT COMMENT '',
  `userID` INT NOT NULL COMMENT '',
  `staticMarkupCollectionID` INT NOT NULL COMMENT '',
  `accessMode` INT NOT NULL COMMENT '',
  `owner` TINYINT(1) NOT NULL COMMENT '',
  PRIMARY KEY (`user_staticmarkupcollectionID`)  COMMENT '',
  INDEX `FK_usersmc_userID_idx` (`userID` ASC)  COMMENT '',
  INDEX `FK_usersmc_staticMarkupCollectionID_idx` (`staticMarkupCollectionID` ASC)  COMMENT '',
  CONSTRAINT `FK_usersmc_userID`
    FOREIGN KEY (`userID`)
    REFERENCES `catmarepository`.`user` (`userID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `FK_usersmc_staticMarkupCollectionID`
    FOREIGN KEY (`staticMarkupCollectionID`)
    REFERENCES `catmarepository`.`staticmarkupcollection` (`staticMarkupCollectionID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `catmarepository`.`user_usermarkupcollection`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `catmarepository`.`user_usermarkupcollection` (
  `user_usermarkupcollectioID` INT NOT NULL AUTO_INCREMENT COMMENT '',
  `userID` INT NOT NULL COMMENT '',
  `userMarkupCollectionID` INT NOT NULL COMMENT '',
  `accessMode` INT NOT NULL COMMENT '',
  `owner` TINYINT(1) NOT NULL COMMENT '',
  PRIMARY KEY (`user_usermarkupcollectioID`)  COMMENT '',
  INDEX `FK_userumc_userID_idx` (`userID` ASC)  COMMENT '',
  INDEX `FK_userumc_userMarkupCollectionID_idx` (`userMarkupCollectionID` ASC)  COMMENT '',
  CONSTRAINT `FK_userumc_userID`
    FOREIGN KEY (`userID`)
    REFERENCES `catmarepository`.`user` (`userID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `FK_userumc_userMarkupCollectionID`
    FOREIGN KEY (`userMarkupCollectionID`)
    REFERENCES `catmarepository`.`usermarkupcollection` (`usermarkupCollectionID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `catmarepository`.`user_taglibrary`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `catmarepository`.`user_taglibrary` (
  `user_tagLibraryID` INT NOT NULL AUTO_INCREMENT COMMENT '',
  `userID` INT NOT NULL COMMENT '',
  `tagLibraryID` INT NOT NULL COMMENT '',
  `accessMode` INT NOT NULL COMMENT '',
  `owner` TINYINT(1) NOT NULL COMMENT '',
  PRIMARY KEY (`user_tagLibraryID`)  COMMENT '',
  INDEX `FK_usertl_userID_idx` (`userID` ASC)  COMMENT '',
  INDEX `FK_usertl_tagLibraryID_idx` (`tagLibraryID` ASC)  COMMENT '',
  CONSTRAINT `FK_usertl_userID`
    FOREIGN KEY (`userID`)
    REFERENCES `catmarepository`.`user` (`userID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `FK_usertl_tagLibraryID`
    FOREIGN KEY (`tagLibraryID`)
    REFERENCES `catmarepository`.`taglibrary` (`tagLibraryID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `catmarepository`.`corpus`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `catmarepository`.`corpus` (
  `corpusID` INT NOT NULL AUTO_INCREMENT COMMENT '',
  `name` VARCHAR(45) NOT NULL COMMENT '',
  PRIMARY KEY (`corpusID`)  COMMENT '')
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `catmarepository`.`corpus_sourcedocument`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `catmarepository`.`corpus_sourcedocument` (
  `corpus_sourcedocumentID` INT NOT NULL AUTO_INCREMENT COMMENT '',
  `corpusID` INT NOT NULL COMMENT '',
  `sourceDocumentID` INT NOT NULL COMMENT '',
  PRIMARY KEY (`corpus_sourcedocumentID`)  COMMENT '',
  INDEX `FK_corpussd_corpusID_idx` (`corpusID` ASC)  COMMENT '',
  INDEX `FK_corpussd_sourceDocumentID_idx` (`sourceDocumentID` ASC)  COMMENT '',
  UNIQUE INDEX `UK_corpussd_corpusID_sdID` (`corpusID` ASC, `sourceDocumentID` ASC)  COMMENT '',
  CONSTRAINT `FK_corpussd_corpusID`
    FOREIGN KEY (`corpusID`)
    REFERENCES `catmarepository`.`corpus` (`corpusID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `FK_corpussd_sourceDocumentID`
    FOREIGN KEY (`sourceDocumentID`)
    REFERENCES `catmarepository`.`sourcedocument` (`sourceDocumentID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `catmarepository`.`corpus_usermarkupcollection`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `catmarepository`.`corpus_usermarkupcollection` (
  `corpus_usermarkupcollectionID` INT NOT NULL AUTO_INCREMENT COMMENT '',
  `corpusID` INT NOT NULL COMMENT '',
  `userMarkupCollectionID` INT NOT NULL COMMENT '',
  PRIMARY KEY (`corpus_usermarkupcollectionID`)  COMMENT '',
  INDEX `FK_corpusumc_corpusID_idx` (`corpusID` ASC)  COMMENT '',
  INDEX `FK_corpusumc_userMarkupCollectionID_idx` (`userMarkupCollectionID` ASC)  COMMENT '',
  UNIQUE INDEX `UK_corpusumc_corpusID_umcID` (`corpusID` ASC, `userMarkupCollectionID` ASC)  COMMENT '',
  CONSTRAINT `FK_corpusumc_corpusID`
    FOREIGN KEY (`corpusID`)
    REFERENCES `catmarepository`.`corpus` (`corpusID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `FK_corpusumc_userMarkupCollectionID`
    FOREIGN KEY (`userMarkupCollectionID`)
    REFERENCES `catmarepository`.`usermarkupcollection` (`usermarkupCollectionID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `catmarepository`.`corpus_staticmarkupcollection`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `catmarepository`.`corpus_staticmarkupcollection` (
  `corpus_staticmarkupcollectionID` INT NOT NULL AUTO_INCREMENT COMMENT '',
  `corpusID` INT NOT NULL COMMENT '',
  `staticMarkupCollectionID` INT NOT NULL COMMENT '',
  PRIMARY KEY (`corpus_staticmarkupcollectionID`)  COMMENT '',
  INDEX `FK_corpussmc_corpusID_idx` (`corpusID` ASC)  COMMENT '',
  INDEX `FK_corpussmc_staticMarkupCollectionID_idx` (`staticMarkupCollectionID` ASC)  COMMENT '',
  UNIQUE INDEX `UK_corpussmc_corpusID_smcID` (`corpusID` ASC, `staticMarkupCollectionID` ASC)  COMMENT '',
  CONSTRAINT `FK_corpussmc_corpusID`
    FOREIGN KEY (`corpusID`)
    REFERENCES `catmarepository`.`corpus` (`corpusID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `FK_corpussmc_staticMarkupCollectionID`
    FOREIGN KEY (`staticMarkupCollectionID`)
    REFERENCES `catmarepository`.`staticmarkupcollection` (`staticMarkupCollectionID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `catmarepository`.`user_corpus`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `catmarepository`.`user_corpus` (
  `user_corpusID` INT NOT NULL AUTO_INCREMENT COMMENT '',
  `userID` INT NOT NULL COMMENT '',
  `corpusID` INT NOT NULL COMMENT '',
  `accessMode` INT NOT NULL COMMENT '',
  `owner` TINYINT(1) NOT NULL COMMENT '',
  PRIMARY KEY (`user_corpusID`)  COMMENT '',
  INDEX `FK_usercorpus_userID_idx` (`userID` ASC)  COMMENT '',
  INDEX `FK_usercorpus_corpusID_idx` (`corpusID` ASC)  COMMENT '',
  CONSTRAINT `FK_usercorpus_userID`
    FOREIGN KEY (`userID`)
    REFERENCES `catmarepository`.`user` (`userID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `FK_usercorpus_corpusID`
    FOREIGN KEY (`corpusID`)
    REFERENCES `catmarepository`.`corpus` (`corpusID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `catmarepository`.`maintenance_sem`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `catmarepository`.`maintenance_sem` (
  `maintenance_semID` INT NOT NULL AUTO_INCREMENT COMMENT '',
  `type` VARCHAR(45) NOT NULL COMMENT 'CLEANING\nIMPORT\nSYNCH',
  `starttime` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '',
  PRIMARY KEY (`maintenance_semID`)  COMMENT '')
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `catmarepository`.`permission`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `catmarepository`.`permission` (
  `permissionID` INT NOT NULL AUTO_INCREMENT COMMENT '',
  `identifier` VARCHAR(255) NOT NULL COMMENT '',
  PRIMARY KEY (`permissionID`)  COMMENT '',
  UNIQUE INDEX `UK_perm_identifier` (`identifier` ASC)  COMMENT '')
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `catmarepository`.`role`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `catmarepository`.`role` (
  `roleID` INT NOT NULL AUTO_INCREMENT COMMENT '',
  `identifier` VARCHAR(45) NOT NULL COMMENT '',
  PRIMARY KEY (`roleID`)  COMMENT '',
  UNIQUE INDEX `UK_role_identifier` (`identifier` ASC)  COMMENT '')
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `catmarepository`.`user_role`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `catmarepository`.`user_role` (
  `user_roleID` INT NOT NULL AUTO_INCREMENT COMMENT '',
  `userID` INT NOT NULL COMMENT '',
  `roleID` INT NOT NULL COMMENT '',
  PRIMARY KEY (`user_roleID`)  COMMENT '',
  INDEX `FK_user_role_userID_idx` (`userID` ASC)  COMMENT '',
  INDEX `FK_user_role_roleID_idx` (`roleID` ASC)  COMMENT '',
  CONSTRAINT `FK_user_role_userID`
    FOREIGN KEY (`userID`)
    REFERENCES `catmarepository`.`user` (`userID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `FK_user_role_roleID`
    FOREIGN KEY (`roleID`)
    REFERENCES `catmarepository`.`role` (`roleID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `catmarepository`.`role_permission`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `catmarepository`.`role_permission` (
  `role_permissionID` INT NOT NULL AUTO_INCREMENT COMMENT '',
  `roleID` INT NOT NULL COMMENT '',
  `permissionID` INT NOT NULL COMMENT '',
  PRIMARY KEY (`role_permissionID`)  COMMENT '',
  INDEX `FK_role_perm_roleID_idx` (`roleID` ASC)  COMMENT '',
  INDEX `FK_role_perm_permissionID_idx` (`permissionID` ASC)  COMMENT '',
  CONSTRAINT `FK_role_perm_roleID`
    FOREIGN KEY (`roleID`)
    REFERENCES `catmarepository`.`role` (`roleID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `FK_role_perm_permissionID`
    FOREIGN KEY (`permissionID`)
    REFERENCES `catmarepository`.`permission` (`permissionID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


DELIMITER $$
CREATE DEFINER=`root`@`localhost` PROCEDURE `getTagDefinitionChildren`(startTagDefinitionID INT)
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
DELIMITER ;


CREATE DATABASE `quartz` DEFAULT CHARACTER SET utf8 COLLATE utf8_bin;
USE `quartz`;

DROP TABLE IF EXISTS QRTZ_FIRED_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_PAUSED_TRIGGER_GRPS;
DROP TABLE IF EXISTS QRTZ_SCHEDULER_STATE;
DROP TABLE IF EXISTS QRTZ_LOCKS;
DROP TABLE IF EXISTS QRTZ_SIMPLE_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_SIMPROP_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_CRON_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_BLOB_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_JOB_DETAILS;
DROP TABLE IF EXISTS QRTZ_CALENDARS;

CREATE TABLE QRTZ_JOB_DETAILS(
SCHED_NAME VARCHAR(120) NOT NULL,
JOB_NAME VARCHAR(200) NOT NULL,
JOB_GROUP VARCHAR(200) NOT NULL,
DESCRIPTION VARCHAR(250) NULL,
JOB_CLASS_NAME VARCHAR(250) NOT NULL,
IS_DURABLE VARCHAR(1) NOT NULL,
IS_NONCONCURRENT VARCHAR(1) NOT NULL,
IS_UPDATE_DATA VARCHAR(1) NOT NULL,
REQUESTS_RECOVERY VARCHAR(1) NOT NULL,
JOB_DATA BLOB NULL,
PRIMARY KEY (SCHED_NAME,JOB_NAME,JOB_GROUP))
ENGINE=InnoDB;

CREATE TABLE QRTZ_TRIGGERS (
SCHED_NAME VARCHAR(120) NOT NULL,
TRIGGER_NAME VARCHAR(200) NOT NULL,
TRIGGER_GROUP VARCHAR(200) NOT NULL,
JOB_NAME VARCHAR(200) NOT NULL,
JOB_GROUP VARCHAR(200) NOT NULL,
DESCRIPTION VARCHAR(250) NULL,
NEXT_FIRE_TIME BIGINT(13) NULL,
PREV_FIRE_TIME BIGINT(13) NULL,
PRIORITY INTEGER NULL,
TRIGGER_STATE VARCHAR(16) NOT NULL,
TRIGGER_TYPE VARCHAR(8) NOT NULL,
START_TIME BIGINT(13) NOT NULL,
END_TIME BIGINT(13) NULL,
CALENDAR_NAME VARCHAR(200) NULL,
MISFIRE_INSTR SMALLINT(2) NULL,
JOB_DATA BLOB NULL,
PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
FOREIGN KEY (SCHED_NAME,JOB_NAME,JOB_GROUP)
REFERENCES QRTZ_JOB_DETAILS(SCHED_NAME,JOB_NAME,JOB_GROUP))
ENGINE=InnoDB;

CREATE TABLE QRTZ_SIMPLE_TRIGGERS (
SCHED_NAME VARCHAR(120) NOT NULL,
TRIGGER_NAME VARCHAR(200) NOT NULL,
TRIGGER_GROUP VARCHAR(200) NOT NULL,
REPEAT_COUNT BIGINT(7) NOT NULL,
REPEAT_INTERVAL BIGINT(12) NOT NULL,
TIMES_TRIGGERED BIGINT(10) NOT NULL,
PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP))
ENGINE=InnoDB;

CREATE TABLE QRTZ_CRON_TRIGGERS (
SCHED_NAME VARCHAR(120) NOT NULL,
TRIGGER_NAME VARCHAR(200) NOT NULL,
TRIGGER_GROUP VARCHAR(200) NOT NULL,
CRON_EXPRESSION VARCHAR(120) NOT NULL,
TIME_ZONE_ID VARCHAR(80),
PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP))
ENGINE=InnoDB;

CREATE TABLE QRTZ_SIMPROP_TRIGGERS
  (          
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    STR_PROP_1 VARCHAR(512) NULL,
    STR_PROP_2 VARCHAR(512) NULL,
    STR_PROP_3 VARCHAR(512) NULL,
    INT_PROP_1 INT NULL,
    INT_PROP_2 INT NULL,
    LONG_PROP_1 BIGINT NULL,
    LONG_PROP_2 BIGINT NULL,
    DEC_PROP_1 NUMERIC(13,4) NULL,
    DEC_PROP_2 NUMERIC(13,4) NULL,
    BOOL_PROP_1 VARCHAR(1) NULL,
    BOOL_PROP_2 VARCHAR(1) NULL,
    PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP) 
    REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP))
ENGINE=InnoDB;

CREATE TABLE QRTZ_BLOB_TRIGGERS (
SCHED_NAME VARCHAR(120) NOT NULL,
TRIGGER_NAME VARCHAR(200) NOT NULL,
TRIGGER_GROUP VARCHAR(200) NOT NULL,
BLOB_DATA BLOB NULL,
PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
INDEX (SCHED_NAME,TRIGGER_NAME, TRIGGER_GROUP),
FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP))
ENGINE=InnoDB;

CREATE TABLE QRTZ_CALENDARS (
SCHED_NAME VARCHAR(120) NOT NULL,
CALENDAR_NAME VARCHAR(200) NOT NULL,
CALENDAR BLOB NOT NULL,
PRIMARY KEY (SCHED_NAME,CALENDAR_NAME))
ENGINE=InnoDB;

CREATE TABLE QRTZ_PAUSED_TRIGGER_GRPS (
SCHED_NAME VARCHAR(120) NOT NULL,
TRIGGER_GROUP VARCHAR(200) NOT NULL,
PRIMARY KEY (SCHED_NAME,TRIGGER_GROUP))
ENGINE=InnoDB;

CREATE TABLE QRTZ_FIRED_TRIGGERS (
SCHED_NAME VARCHAR(120) NOT NULL,
ENTRY_ID VARCHAR(95) NOT NULL,
TRIGGER_NAME VARCHAR(200) NOT NULL,
TRIGGER_GROUP VARCHAR(200) NOT NULL,
INSTANCE_NAME VARCHAR(200) NOT NULL,
FIRED_TIME BIGINT(13) NOT NULL,
SCHED_TIME BIGINT(13) NOT NULL,
PRIORITY INTEGER NOT NULL,
STATE VARCHAR(16) NOT NULL,
JOB_NAME VARCHAR(200) NULL,
JOB_GROUP VARCHAR(200) NULL,
IS_NONCONCURRENT VARCHAR(1) NULL,
REQUESTS_RECOVERY VARCHAR(1) NULL,
PRIMARY KEY (SCHED_NAME,ENTRY_ID))
ENGINE=InnoDB;

CREATE TABLE QRTZ_SCHEDULER_STATE (
SCHED_NAME VARCHAR(120) NOT NULL,
INSTANCE_NAME VARCHAR(200) NOT NULL,
LAST_CHECKIN_TIME BIGINT(13) NOT NULL,
CHECKIN_INTERVAL BIGINT(13) NOT NULL,
PRIMARY KEY (SCHED_NAME,INSTANCE_NAME))
ENGINE=InnoDB;

CREATE TABLE QRTZ_LOCKS (
SCHED_NAME VARCHAR(120) NOT NULL,
LOCK_NAME VARCHAR(40) NOT NULL,
PRIMARY KEY (SCHED_NAME,LOCK_NAME))
ENGINE=InnoDB;

CREATE INDEX IDX_QRTZ_J_REQ_RECOVERY ON QRTZ_JOB_DETAILS(SCHED_NAME,REQUESTS_RECOVERY);
CREATE INDEX IDX_QRTZ_J_GRP ON QRTZ_JOB_DETAILS(SCHED_NAME,JOB_GROUP);

CREATE INDEX IDX_QRTZ_T_J ON QRTZ_TRIGGERS(SCHED_NAME,JOB_NAME,JOB_GROUP);
CREATE INDEX IDX_QRTZ_T_JG ON QRTZ_TRIGGERS(SCHED_NAME,JOB_GROUP);
CREATE INDEX IDX_QRTZ_T_C ON QRTZ_TRIGGERS(SCHED_NAME,CALENDAR_NAME);
CREATE INDEX IDX_QRTZ_T_G ON QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_GROUP);
CREATE INDEX IDX_QRTZ_T_STATE ON QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_N_STATE ON QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP,TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_N_G_STATE ON QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_GROUP,TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_NEXT_FIRE_TIME ON QRTZ_TRIGGERS(SCHED_NAME,NEXT_FIRE_TIME);
CREATE INDEX IDX_QRTZ_T_NFT_ST ON QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_STATE,NEXT_FIRE_TIME);
CREATE INDEX IDX_QRTZ_T_NFT_MISFIRE ON QRTZ_TRIGGERS(SCHED_NAME,MISFIRE_INSTR,NEXT_FIRE_TIME);
CREATE INDEX IDX_QRTZ_T_NFT_ST_MISFIRE ON QRTZ_TRIGGERS(SCHED_NAME,MISFIRE_INSTR,NEXT_FIRE_TIME,TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_NFT_ST_MISFIRE_GRP ON QRTZ_TRIGGERS(SCHED_NAME,MISFIRE_INSTR,NEXT_FIRE_TIME,TRIGGER_GROUP,TRIGGER_STATE);

CREATE INDEX IDX_QRTZ_FT_TRIG_INST_NAME ON QRTZ_FIRED_TRIGGERS(SCHED_NAME,INSTANCE_NAME);
CREATE INDEX IDX_QRTZ_FT_INST_JOB_REQ_RCVRY ON QRTZ_FIRED_TRIGGERS(SCHED_NAME,INSTANCE_NAME,REQUESTS_RECOVERY);
CREATE INDEX IDX_QRTZ_FT_J_G ON QRTZ_FIRED_TRIGGERS(SCHED_NAME,JOB_NAME,JOB_GROUP);
CREATE INDEX IDX_QRTZ_FT_JG ON QRTZ_FIRED_TRIGGERS(SCHED_NAME,JOB_GROUP);
CREATE INDEX IDX_QRTZ_FT_T_G ON QRTZ_FIRED_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP);
CREATE INDEX IDX_QRTZ_FT_TG ON QRTZ_FIRED_TRIGGERS(SCHED_NAME,TRIGGER_GROUP);


CREATE USER 'catma'@'localhost' IDENTIFIED BY 'test';

GRANT SELECT , INSERT , UPDATE , DELETE, EXECUTE ON `catmaindex` . * TO 'catma'@'localhost';
GRANT SELECT , INSERT , UPDATE , DELETE, EXECUTE ON `catmarepository` . * TO 'catma'@'localhost';

GRANT SELECT , INSERT , UPDATE , DELETE, EXECUTE ON `quartz` . * TO 'catma'@'localhost';

USE catmarepository;

INSERT INTO role(identifier) VALUES('admin');
INSERT INTO role(identifier) VALUES('user');
INSERT INTO role(identifier) VALUES('heureclea');

INSERT INTO permission(identifier) VALUES('adminwindow');
INSERT INTO permission(identifier) VALUES('autotagging');
INSERT INTO permission(identifier) VALUES('exportcorpus');

SELECT @adminRole:=roleID FROM role WHERE identifier = 'admin';
SELECT @heurecleaRole:=roleID FROM role WHERE identifier = 'heureclea';
SELECT @adminWindow:=permissionID FROM permission WHERE identifier = 'adminwindow';
SELECT @autotagging:=permissionID FROM permission WHERE identifier = 'autotagging';
SELECT @exportcorpus:=permissionID FROM permission WHERE identifier = 'exportcorpus';

INSERT INTO role_permission(roleID, permissionID) VALUES (@adminRole, @adminWindow);
INSERT INTO role_permission(roleID, permissionID) VALUES (@adminRole, @autotagging);
INSERT INTO role_permission(roleID, permissionID) VALUES (@adminRole, @exportcorpus);
INSERT INTO role_permission(roleID, permissionID) VALUES (@heurecleaRole, @autotagging);

SELECT @userRole := roleID FROM role WHERE identifier = 'user';

INSERT INTO user_role(userID, roleID) SELECT userID, @userRole FROM user;

SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
