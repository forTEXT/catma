SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL,ALLOW_INVALID_DATES';

-- -----------------------------------------------------
-- Schema catmaindex
-- -----------------------------------------------------
-- CREATE SCHEMA IF NOT EXISTS `catmaindex` DEFAULT CHARACTER SET utf8 ;
-- -----------------------------------------------------
-- Schema catmarepository
-- -----------------------------------------------------
-- CREATE SCHEMA IF NOT EXISTS `catmarepository` DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ;
USE `catmaindex` ;

-- -----------------------------------------------------
-- Table `catmaindex`.`term`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `catmaindex`.`term` (
  `termID` INT(11) NOT NULL AUTO_INCREMENT,
  `documentID` VARCHAR(300) NOT NULL,
  `frequency` INT(11) NOT NULL,
  `term` VARCHAR(300) CHARACTER SET 'utf8' COLLATE 'utf8_bin' NOT NULL,
  PRIMARY KEY (`termID`),
  INDEX `TERM_IDX` USING BTREE (`documentID`(150) ASC, `term`(150) ASC))
ENGINE = InnoDB
AUTO_INCREMENT = 5874
DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `catmaindex`.`position`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `catmaindex`.`position` (
  `positionID` INT(11) NOT NULL AUTO_INCREMENT,
  `termID` INT(11) NOT NULL,
  `characterStart` INT(11) NOT NULL,
  `characterEnd` INT(11) NOT NULL,
  `tokenOffset` INT(11) NOT NULL,
  PRIMARY KEY (`positionID`),
  INDEX `FK_PositionTermID_idx` (`termID` ASC),
  INDEX `TOKENOFFS_IDX` USING HASH (`tokenOffset` ASC),
  INDEX `CHAROFFS_IDX` (`characterStart` ASC, `characterEnd` ASC),
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
  `tagReferenceID` INT(11) NOT NULL AUTO_INCREMENT,
  `documentID` VARCHAR(300) NOT NULL,
  `userMarkupCollectionID` VARCHAR(300) NOT NULL,
  `tagDefinitionPath` VARCHAR(2048) NOT NULL,
  `tagDefinitionID` BINARY(16) NOT NULL,
  `tagInstanceID` BINARY(16) NOT NULL,
  `tagDefinitionVersion` VARCHAR(28) NOT NULL,
  `characterStart` INT(11) NOT NULL,
  `characterEnd` INT(11) NOT NULL,
  PRIMARY KEY (`tagReferenceID`),
  INDEX `I_searchTr` (`userMarkupCollectionID`(12) ASC, `tagDefinitionID` ASC, `tagInstanceID` ASC),
  INDEX `I_searchTr2` (`userMarkupCollectionID`(12) ASC, `tagDefinitionPath`(150) ASC))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `catmaindex`.`property`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `catmaindex`.`property` (
  `propertyID` INT NOT NULL AUTO_INCREMENT,
  `tagInstanceID` BINARY(16) NOT NULL,
  `propertyDefinitionID` BINARY(16) NOT NULL,
  `name` VARCHAR(45) NOT NULL,
  `value` VARCHAR(300) NULL,
  PRIMARY KEY (`propertyID`),
  INDEX `I_pTagInstanceID` (`tagInstanceID` ASC, `propertyDefinitionID` ASC),
  INDEX `I_pValue` (`value`(200) ASC))
ENGINE = InnoDB;

USE `catmarepository` ;

-- -----------------------------------------------------
-- Table `catmarepository`.`sourcedocument`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `catmarepository`.`sourcedocument` (
  `sourceDocumentID` INT NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(300) NULL,
  `publisher` VARCHAR(300) NULL,
  `author` VARCHAR(300) NULL,
  `description` VARCHAR(300) NULL,
  `sourceUri` VARCHAR(300) NULL,
  `fileType` VARCHAR(5) NOT NULL,
  `charset` VARCHAR(50) NULL,
  `fileOSType` VARCHAR(15) NOT NULL,
  `checksum` BIGINT NOT NULL,
  `mimeType` VARCHAR(255) NULL,
  `xsltDocumentLocalUri` VARCHAR(300) NULL,
  `locale` VARCHAR(15) NOT NULL,
  `localUri` VARCHAR(300) NULL,
  PRIMARY KEY (`sourceDocumentID`),
  INDEX `IDX_localUri` (`localUri`(200) ASC))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `catmarepository`.`userdefined_separatingcharacter`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `catmarepository`.`userdefined_separatingcharacter` (
  `udscID` INT NOT NULL AUTO_INCREMENT,
  `chr` VARCHAR(1) NOT NULL,
  `sourceDocumentID` INT NOT NULL,
  PRIMARY KEY (`udscID`),
  INDEX `FK_udsc_sourceDocumentID_idx` (`sourceDocumentID` ASC),
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
  `uscID` INT NOT NULL AUTO_INCREMENT,
  `charsequence` VARCHAR(45) NOT NULL,
  `sourceDocumentID` INT NOT NULL,
  PRIMARY KEY (`uscID`),
  INDEX `FK_uscs_sourceDocumentID_idx` (`sourceDocumentID` ASC),
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
  `tagLibraryID` INT NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(300) NOT NULL,
  `publisher` VARCHAR(300) NULL,
  `author` VARCHAR(300) NULL,
  `description` VARCHAR(300) NULL,
  `independent` TINYINT(1) NOT NULL,
  PRIMARY KEY (`tagLibraryID`))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `catmarepository`.`usermarkupcollection`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `catmarepository`.`usermarkupcollection` (
  `usermarkupCollectionID` INT NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(300) NULL,
  `publisher` VARCHAR(300) NULL,
  `author` VARCHAR(300) NULL,
  `description` VARCHAR(300) NULL,
  `sourceDocumentID` INT NOT NULL,
  `tagLibraryID` INT NOT NULL,
  PRIMARY KEY (`usermarkupCollectionID`),
  INDEX `FK_umc_sourceDocumentID_idx` (`sourceDocumentID` ASC),
  INDEX `FK_umc_tagLibraryID_idx` (`tagLibraryID` ASC),
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
  `tagsetDefinitionID` INT NOT NULL AUTO_INCREMENT,
  `uuid` BINARY(16) NOT NULL,
  `version` DATETIME NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `tagLibraryID` INT NOT NULL,
  PRIMARY KEY (`tagsetDefinitionID`),
  UNIQUE INDEX `UK_tsDef_tLib_uuid` (`uuid` ASC, `tagLibraryID` ASC),
  INDEX `FK_tsDef_tagLibraryID_idx` (`tagLibraryID` ASC),
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
  `tagDefinitionID` INT NOT NULL AUTO_INCREMENT,
  `uuid` BINARY(16) NOT NULL,
  `version` DATETIME NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `tagsetDefinitionID` INT NOT NULL,
  `parentID` INT NULL,
  `parentUuid` BINARY(16) NULL,
  PRIMARY KEY (`tagDefinitionID`),
  INDEX `FK_tdef_tagsetDefinitionID_idx` (`tagsetDefinitionID` ASC),
  INDEX `FK_tdef_parentID_idx` (`parentID` ASC),
  UNIQUE INDEX `UK_tdef_tsdef_uuid` (`uuid` ASC, `tagsetDefinitionID` ASC),
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
  `propertyDefinitionID` INT NOT NULL AUTO_INCREMENT,
  `uuid` BINARY(16) NOT NULL,
  `name` VARCHAR(45) NOT NULL,
  `tagDefinitionID` INT NOT NULL,
  `systemproperty` TINYINT(1) NOT NULL,
  PRIMARY KEY (`propertyDefinitionID`),
  UNIQUE INDEX `UK_pd_uuid_tdef` (`uuid` ASC, `tagDefinitionID` ASC),
  INDEX `FK_pd_tagDefinitionID_idx` (`tagDefinitionID` ASC),
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
  `tagInstanceID` INT NOT NULL AUTO_INCREMENT,
  `uuid` BINARY(16) NOT NULL,
  `tagDefinitionID` INT NOT NULL,
  PRIMARY KEY (`tagInstanceID`),
  UNIQUE INDEX `UK_ti_uuid` (`uuid` ASC),
  INDEX `FK_ti_tagDefinitionID_idx` (`tagDefinitionID` ASC),
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
  `propertyID` INT NOT NULL AUTO_INCREMENT,
  `propertyDefinitionID` INT NOT NULL,
  `tagInstanceID` INT NOT NULL,
  PRIMARY KEY (`propertyID`),
  INDEX `FK_prop_propertyDefinitionID_idx` (`propertyDefinitionID` ASC),
  INDEX `FK_prop_tagInstanceID_idx` (`tagInstanceID` ASC),
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
  `tagReferenceID` INT NOT NULL AUTO_INCREMENT,
  `characterStart` INT NOT NULL,
  `characterEnd` INT NOT NULL,
  `userMarkupCollectionID` INT NOT NULL,
  `tagInstanceID` INT NOT NULL,
  PRIMARY KEY (`tagReferenceID`),
  INDEX `FK_tr_userMarkupCollectionID_idx` (`userMarkupCollectionID` ASC),
  INDEX `FK_tr_tagInstanceID_idx` (`tagInstanceID` ASC),
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
  `propertydefPossibleValueID` INT NOT NULL AUTO_INCREMENT,
  `value` VARCHAR(255) NULL,
  `propertyDefinitionID` INT NOT NULL,
  PRIMARY KEY (`propertydefPossibleValueID`),
  INDEX `FK_ppv_propertyDefinitionID_idx` (`propertyDefinitionID` ASC),
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
  `staticMarkupCollectionID` INT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(45) NOT NULL,
  `sourceDocumentID` INT NOT NULL,
  PRIMARY KEY (`staticMarkupCollectionID`),
  INDEX `FK_staticmc_sourceDocumentID` (`sourceDocumentID` ASC),
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
  `staticMarkupInstanceID` INT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(300) NOT NULL,
  `staticMarkupCollectionID` INT NULL,
  PRIMARY KEY (`staticMarkupInstanceID`),
  INDEX `FK_staticmi_staticMarkupCollectionID_idx` (`staticMarkupCollectionID` ASC),
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
  `staticMarkupAttributeID` INT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(45) NOT NULL,
  `value` VARCHAR(45) NOT NULL,
  `staticMarkupInstanceID` INT NOT NULL,
  PRIMARY KEY (`staticMarkupAttributeID`),
  INDEX `FK_statica_staticMarkupInstanceID_idx` (`staticMarkupInstanceID` ASC),
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
  `propertyValueID` INT NOT NULL AUTO_INCREMENT,
  `propertyID` INT NOT NULL,
  `value` VARCHAR(1024) NULL,
  PRIMARY KEY (`propertyValueID`),
  INDEX `FK_propv_propertyID_idx` (`propertyID` ASC),
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
  `userID` INT NOT NULL AUTO_INCREMENT,
  `identifier` VARCHAR(300) NOT NULL,
  `locked` TINYINT(1) NOT NULL DEFAULT true,
  `role` INT NULL DEFAULT 0,
  PRIMARY KEY (`userID`))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `catmarepository`.`user_sourcedocument`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `catmarepository`.`user_sourcedocument` (
  `user_sourcedocumentID` INT NOT NULL AUTO_INCREMENT,
  `userID` INT NOT NULL,
  `sourceDocumentID` INT NOT NULL,
  `accessMode` INT NOT NULL,
  `owner` TINYINT(1) NOT NULL,
  PRIMARY KEY (`user_sourcedocumentID`),
  INDEX `FK_usersd_userID_idx` (`userID` ASC),
  INDEX `FK_usersd_sourceDocumentID_idx` (`sourceDocumentID` ASC),
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
  `user_staticmarkupcollectionID` INT NOT NULL AUTO_INCREMENT,
  `userID` INT NOT NULL,
  `staticMarkupCollectionID` INT NOT NULL,
  `accessMode` INT NOT NULL,
  `owner` TINYINT(1) NOT NULL,
  PRIMARY KEY (`user_staticmarkupcollectionID`),
  INDEX `FK_usersmc_userID_idx` (`userID` ASC),
  INDEX `FK_usersmc_staticMarkupCollectionID_idx` (`staticMarkupCollectionID` ASC),
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
  `user_usermarkupcollectioID` INT NOT NULL AUTO_INCREMENT,
  `userID` INT NOT NULL,
  `userMarkupCollectionID` INT NOT NULL,
  `accessMode` INT NOT NULL,
  `owner` TINYINT(1) NOT NULL,
  PRIMARY KEY (`user_usermarkupcollectioID`),
  INDEX `FK_userumc_userID_idx` (`userID` ASC),
  INDEX `FK_userumc_userMarkupCollectionID_idx` (`userMarkupCollectionID` ASC),
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
  `user_tagLibraryID` INT NOT NULL AUTO_INCREMENT,
  `userID` INT NOT NULL,
  `tagLibraryID` INT NOT NULL,
  `accessMode` INT NOT NULL,
  `owner` TINYINT(1) NOT NULL,
  PRIMARY KEY (`user_tagLibraryID`),
  INDEX `FK_usertl_userID_idx` (`userID` ASC),
  INDEX `FK_usertl_tagLibraryID_idx` (`tagLibraryID` ASC),
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
  `corpusID` INT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(45) NOT NULL,
  PRIMARY KEY (`corpusID`))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `catmarepository`.`corpus_sourcedocument`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `catmarepository`.`corpus_sourcedocument` (
  `corpus_sourcedocumentID` INT NOT NULL AUTO_INCREMENT,
  `corpusID` INT NOT NULL,
  `sourceDocumentID` INT NOT NULL,
  PRIMARY KEY (`corpus_sourcedocumentID`),
  INDEX `FK_corpussd_corpusID_idx` (`corpusID` ASC),
  INDEX `FK_corpussd_sourceDocumentID_idx` (`sourceDocumentID` ASC),
  UNIQUE INDEX `UK_corpussd_corpusID_sdID` (`corpusID` ASC, `sourceDocumentID` ASC),
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
  `corpus_usermarkupcollectionID` INT NOT NULL AUTO_INCREMENT,
  `corpusID` INT NOT NULL,
  `userMarkupCollectionID` INT NOT NULL,
  PRIMARY KEY (`corpus_usermarkupcollectionID`),
  INDEX `FK_corpusumc_corpusID_idx` (`corpusID` ASC),
  INDEX `FK_corpusumc_userMarkupCollectionID_idx` (`userMarkupCollectionID` ASC),
  UNIQUE INDEX `UK_corpusumc_corpusID_umcID` (`corpusID` ASC, `userMarkupCollectionID` ASC),
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
  `corpus_staticmarkupcollectionID` INT NOT NULL AUTO_INCREMENT,
  `corpusID` INT NOT NULL,
  `staticMarkupCollectionID` INT NOT NULL,
  PRIMARY KEY (`corpus_staticmarkupcollectionID`),
  INDEX `FK_corpussmc_corpusID_idx` (`corpusID` ASC),
  INDEX `FK_corpussmc_staticMarkupCollectionID_idx` (`staticMarkupCollectionID` ASC),
  UNIQUE INDEX `UK_corpussmc_corpusID_smcID` (`corpusID` ASC, `staticMarkupCollectionID` ASC),
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
  `user_corpusID` INT NOT NULL AUTO_INCREMENT,
  `userID` INT NOT NULL,
  `corpusID` INT NOT NULL,
  `accessMode` INT NOT NULL,
  `owner` TINYINT(1) NOT NULL,
  PRIMARY KEY (`user_corpusID`),
  INDEX `FK_usercorpus_userID_idx` (`userID` ASC),
  INDEX `FK_usercorpus_corpusID_idx` (`corpusID` ASC),
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
  `maintenance_semID` INT NOT NULL AUTO_INCREMENT,
  `type` VARCHAR(45) NOT NULL COMMENT 'CLEANING' /* comment truncated */ /*IMPORT
SYNCH*/,
  `starttime` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`maintenance_semID`))
ENGINE = InnoDB;


SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
