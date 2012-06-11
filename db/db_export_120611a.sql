CREATE DATABASE  IF NOT EXISTS `CatmaIndex` /*!40100 DEFAULT CHARACTER SET utf8 */;
USE `CatmaIndex`;
-- MySQL dump 10.13  Distrib 5.5.16, for Win32 (x86)
--
-- Host: 127.0.0.1    Database: CatmaIndex
-- ------------------------------------------------------
-- Server version	5.5.8

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `position`
--

DROP TABLE IF EXISTS `position`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `position` (
  `positionID` int(11) NOT NULL AUTO_INCREMENT,
  `termID` int(11) NOT NULL,
  `characterStart` int(11) NOT NULL,
  `characterEnd` int(11) NOT NULL,
  `tokenOffset` int(11) NOT NULL,
  PRIMARY KEY (`positionID`),
  KEY `FK_PositionTermID` (`termID`),
  KEY `TOKENOFFS_IDX` (`tokenOffset`) USING HASH,
  KEY `CHAROFFS_IDX` (`characterStart`,`characterEnd`) USING BTREE,
  CONSTRAINT `FK_PositionTermID` FOREIGN KEY (`termID`) REFERENCES `term` (`termID`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=106293 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `term`
--

DROP TABLE IF EXISTS `term`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `term` (
  `termID` int(11) NOT NULL AUTO_INCREMENT,
  `documentID` varchar(300) NOT NULL,
  `frequency` int(11) NOT NULL,
  `term` varchar(300) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  PRIMARY KEY (`termID`),
  KEY `TERM_IDX` (`term`(255),`documentID`(255)) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=15232 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tagreference`
--

DROP TABLE IF EXISTS `tagreference`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tagreference` (
  `tagReferenceID` int(11) NOT NULL AUTO_INCREMENT,
  `documentID` varchar(300) NOT NULL,
  `userMarkupCollectionID` varchar(300) NOT NULL,
  `tagDefinitionPath` varchar(2048) NOT NULL,
  `tagDefinitionID` binary(16) NOT NULL,
  `tagInstanceID` binary(16) NOT NULL,
  `tagDefinitionVersion` varchar(28) NOT NULL,
  `characterStart` int(11) NOT NULL,
  `characterEnd` int(11) NOT NULL,
  PRIMARY KEY (`tagReferenceID`)
) ENGINE=InnoDB AUTO_INCREMENT=122 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping routines for database 'CatmaIndex'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2012-06-11 19:30:09
CREATE DATABASE  IF NOT EXISTS `CatmaRepository` /*!40100 DEFAULT CHARACTER SET utf8 COLLATE utf8_bin */;
USE `CatmaRepository`;
-- MySQL dump 10.13  Distrib 5.5.16, for Win32 (x86)
--
-- Host: 127.0.0.1    Database: CatmaRepository
-- ------------------------------------------------------
-- Server version	5.5.8

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `propertydef_possiblevalue`
--

DROP TABLE IF EXISTS `propertydef_possiblevalue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `propertydef_possiblevalue` (
  `propertydefPossibleValueID` int(11) NOT NULL AUTO_INCREMENT,
  `value` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `propertyDefinitionID` int(11) NOT NULL,
  PRIMARY KEY (`propertydefPossibleValueID`),
  KEY `FK_ppv_propertyDefinitionID` (`propertyDefinitionID`),
  CONSTRAINT `FK_ppv_propertyDefinitionID` FOREIGN KEY (`propertyDefinitionID`) REFERENCES `propertydefinition` (`propertyDefinitionID`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=1797 DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_taglibrary`
--

DROP TABLE IF EXISTS `user_taglibrary`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user_taglibrary` (
  `user_tagLibraryID` int(11) NOT NULL AUTO_INCREMENT,
  `userID` int(11) NOT NULL,
  `tagLibraryID` int(11) NOT NULL,
  `accessMode` int(11) NOT NULL,
  `owner` tinyint(1) NOT NULL,
  PRIMARY KEY (`user_tagLibraryID`),
  KEY `FK_usertl_userID` (`userID`),
  KEY `FK_usertl_tagLibraryID` (`tagLibraryID`),
  CONSTRAINT `FK_usertl_tagLibraryID` FOREIGN KEY (`tagLibraryID`) REFERENCES `taglibrary` (`tagLibraryID`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `FK_usertl_userID` FOREIGN KEY (`userID`) REFERENCES `user` (`userID`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=50 DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tagsetdefinition`
--

DROP TABLE IF EXISTS `tagsetdefinition`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tagsetdefinition` (
  `tagsetDefinitionID` int(11) NOT NULL AUTO_INCREMENT,
  `uuid` binary(16) NOT NULL,
  `version` datetime NOT NULL,
  `name` varchar(255) COLLATE utf8_bin NOT NULL,
  `tagLibraryID` int(11) NOT NULL,
  PRIMARY KEY (`tagsetDefinitionID`),
  UNIQUE KEY `UK_tsDef_tLib_uuid` (`uuid`,`tagLibraryID`),
  KEY `FK_tsDef_tagLibraryID` (`tagLibraryID`),
  CONSTRAINT `FK_tsDef_tagLibraryID` FOREIGN KEY (`tagLibraryID`) REFERENCES `taglibrary` (`tagLibraryID`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=138 DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `propertydefinition`
--

DROP TABLE IF EXISTS `propertydefinition`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `propertydefinition` (
  `propertyDefinitionID` int(11) NOT NULL AUTO_INCREMENT,
  `uuid` binary(16) NOT NULL,
  `name` varchar(45) COLLATE utf8_bin NOT NULL,
  `tagDefinitionID` int(11) NOT NULL,
  `systemproperty` tinyint(1) NOT NULL,
  PRIMARY KEY (`propertyDefinitionID`),
  UNIQUE KEY `UK_pd_uuid_tdef` (`uuid`,`tagDefinitionID`),
  KEY `FK_pd_tagDefinitionID` (`tagDefinitionID`),
  CONSTRAINT `FK_pd_tagDefinitionID` FOREIGN KEY (`tagDefinitionID`) REFERENCES `tagdefinition` (`tagDefinitionID`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=708 DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `corpus_sourcedocument`
--

DROP TABLE IF EXISTS `corpus_sourcedocument`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `corpus_sourcedocument` (
  `corpus_sourcedocumentID` int(11) NOT NULL AUTO_INCREMENT,
  `corpusID` int(11) NOT NULL,
  `sourceDocumentID` int(11) NOT NULL,
  PRIMARY KEY (`corpus_sourcedocumentID`),
  KEY `FK_corpussd_corpusID` (`corpusID`),
  KEY `FK_corpussd_sourceDocumentID` (`sourceDocumentID`),
  CONSTRAINT `FK_corpussd_corpusID` FOREIGN KEY (`corpusID`) REFERENCES `corpus` (`corpusID`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `FK_corpussd_sourceDocumentID` FOREIGN KEY (`sourceDocumentID`) REFERENCES `sourcedocument` (`sourceDocumentID`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_sourcedocument`
--

DROP TABLE IF EXISTS `user_sourcedocument`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user_sourcedocument` (
  `user_sourcedocumentID` int(11) NOT NULL AUTO_INCREMENT,
  `userID` int(11) NOT NULL,
  `sourceDocumentID` int(11) NOT NULL,
  `accessMode` int(11) NOT NULL,
  `owner` tinyint(1) NOT NULL,
  PRIMARY KEY (`user_sourcedocumentID`),
  KEY `FK_usersd_userID` (`userID`),
  KEY `FK_usersd_sourceDocumentID` (`sourceDocumentID`),
  CONSTRAINT `FK_usersd_sourceDocumentID` FOREIGN KEY (`sourceDocumentID`) REFERENCES `sourcedocument` (`sourceDocumentID`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `FK_usersd_userID` FOREIGN KEY (`userID`) REFERENCES `user` (`userID`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `taginstance`
--

DROP TABLE IF EXISTS `taginstance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `taginstance` (
  `tagInstanceID` int(11) NOT NULL AUTO_INCREMENT,
  `uuid` binary(16) NOT NULL,
  `tagDefinitionID` int(11) NOT NULL,
  PRIMARY KEY (`tagInstanceID`),
  UNIQUE KEY `UK_ti_uuid` (`uuid`),
  KEY `FK_ti_tagDefinitionID` (`tagDefinitionID`),
  CONSTRAINT `FK_ti_tagDefinitionID` FOREIGN KEY (`tagDefinitionID`) REFERENCES `tagdefinition` (`tagDefinitionID`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=155 DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `propertyvalue`
--

DROP TABLE IF EXISTS `propertyvalue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `propertyvalue` (
  `propertyValueID` int(11) NOT NULL AUTO_INCREMENT,
  `propertyID` int(11) NOT NULL,
  `value` varchar(1024) COLLATE utf8_bin DEFAULT NULL,
  PRIMARY KEY (`propertyValueID`),
  KEY `FK_propv_propertyID` (`propertyID`),
  CONSTRAINT `FK_propv_propertyID` FOREIGN KEY (`propertyID`) REFERENCES `property` (`propertyID`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=314 DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `taglibrary`
--

DROP TABLE IF EXISTS `taglibrary`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `taglibrary` (
  `tagLibraryID` int(11) NOT NULL AUTO_INCREMENT,
  `title` varchar(300) COLLATE utf8_bin NOT NULL,
  `independent` tinyint(1) NOT NULL,
  `publisher` varchar(300) COLLATE utf8_bin DEFAULT NULL,
  `author` varchar(300) COLLATE utf8_bin DEFAULT NULL,
  `description` varchar(300) COLLATE utf8_bin DEFAULT NULL,
  PRIMARY KEY (`tagLibraryID`)
) ENGINE=InnoDB AUTO_INCREMENT=45 DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user` (
  `userID` int(11) NOT NULL AUTO_INCREMENT,
  `identifier` varchar(300) COLLATE utf8_bin NOT NULL,
  `locked` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`userID`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tagdefinition`
--

DROP TABLE IF EXISTS `tagdefinition`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tagdefinition` (
  `tagDefinitionID` int(11) NOT NULL AUTO_INCREMENT,
  `uuid` binary(16) NOT NULL,
  `version` datetime NOT NULL,
  `name` varchar(255) COLLATE utf8_bin NOT NULL,
  `tagsetDefinitionID` int(11) NOT NULL,
  `parentID` int(11) DEFAULT NULL,
  `parentUuid` binary(16) DEFAULT NULL,
  PRIMARY KEY (`tagDefinitionID`),
  UNIQUE KEY `UK_tdef_tsdef_uuid` (`uuid`,`tagsetDefinitionID`),
  KEY `FK_tdef_tagsetDefinitionID` (`tagsetDefinitionID`),
  KEY `FK_tdef_parentID` (`parentID`),
  CONSTRAINT `FK_tdef_parentID` FOREIGN KEY (`parentID`) REFERENCES `tagdefinition` (`tagDefinitionID`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `FK_tdef_tagsetDefinitionID` FOREIGN KEY (`tagsetDefinitionID`) REFERENCES `tagsetdefinition` (`tagsetDefinitionID`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=623 DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `userdefined_separatingcharacter`
--

DROP TABLE IF EXISTS `userdefined_separatingcharacter`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `userdefined_separatingcharacter` (
  `udscID` int(11) NOT NULL AUTO_INCREMENT,
  `character` varchar(5) COLLATE utf8_bin NOT NULL,
  `sourceDocumentID` int(11) NOT NULL,
  PRIMARY KEY (`udscID`),
  KEY `FK_udsc_sourceDocumentID` (`sourceDocumentID`),
  CONSTRAINT `FK_udsc_sourceDocumentID` FOREIGN KEY (`sourceDocumentID`) REFERENCES `sourcedocument` (`sourceDocumentID`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `staticmarkupattribute`
--

DROP TABLE IF EXISTS `staticmarkupattribute`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `staticmarkupattribute` (
  `staticMarkupAttributeID` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(45) COLLATE utf8_bin NOT NULL,
  `value` varchar(45) COLLATE utf8_bin NOT NULL,
  `staticMarkupInstanceID` int(11) NOT NULL,
  PRIMARY KEY (`staticMarkupAttributeID`),
  KEY `FK_statica_staticMarkupInstanceID` (`staticMarkupInstanceID`),
  CONSTRAINT `FK_statica_staticMarkupInstanceID` FOREIGN KEY (`staticMarkupInstanceID`) REFERENCES `staticmarkupinstance` (`staticMarkupInstanceID`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sourcedocument`
--

DROP TABLE IF EXISTS `sourcedocument`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sourcedocument` (
  `sourceDocumentID` int(11) NOT NULL AUTO_INCREMENT,
  `title` varchar(300) COLLATE utf8_bin DEFAULT NULL,
  `publisher` varchar(300) COLLATE utf8_bin DEFAULT NULL,
  `author` varchar(300) COLLATE utf8_bin DEFAULT NULL,
  `description` varchar(300) COLLATE utf8_bin DEFAULT NULL,
  `sourceUri` varchar(300) COLLATE utf8_bin DEFAULT NULL,
  `fileType` varchar(5) COLLATE utf8_bin NOT NULL,
  `charset` varchar(50) COLLATE utf8_bin NOT NULL,
  `fileOSType` varchar(15) COLLATE utf8_bin NOT NULL,
  `checksum` bigint(20) NOT NULL,
  `mimeType` varchar(45) COLLATE utf8_bin DEFAULT NULL,
  `xsltDocumentLocalUri` varchar(300) COLLATE utf8_bin DEFAULT NULL,
  `locale` varchar(15) COLLATE utf8_bin NOT NULL,
  `localUri` varchar(300) COLLATE utf8_bin DEFAULT NULL,
  PRIMARY KEY (`sourceDocumentID`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `corpus_usermarkupcollection`
--

DROP TABLE IF EXISTS `corpus_usermarkupcollection`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `corpus_usermarkupcollection` (
  `corpus_usermarkupcollectionID` int(11) NOT NULL AUTO_INCREMENT,
  `corpusID` int(11) NOT NULL,
  `userMarkupCollectionID` int(11) NOT NULL,
  PRIMARY KEY (`corpus_usermarkupcollectionID`),
  KEY `FK_corpusumc_corpusID` (`corpusID`),
  KEY `FK_corpusumc_userMarkupCollectionID` (`userMarkupCollectionID`),
  CONSTRAINT `FK_corpusumc_corpusID` FOREIGN KEY (`corpusID`) REFERENCES `corpus` (`corpusID`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `FK_corpusumc_userMarkupCollectionID` FOREIGN KEY (`userMarkupCollectionID`) REFERENCES `usermarkupcollection` (`usermarkupCollectionID`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `staticmarkupinstance`
--

DROP TABLE IF EXISTS `staticmarkupinstance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `staticmarkupinstance` (
  `staticMarkupInstanceID` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(300) COLLATE utf8_bin NOT NULL,
  `staticMarkupCollectionID` int(11) DEFAULT NULL,
  PRIMARY KEY (`staticMarkupInstanceID`),
  KEY `FK_staticmi_staticMarkupCollectionID` (`staticMarkupCollectionID`),
  CONSTRAINT `FK_staticmi_staticMarkupCollectionID` FOREIGN KEY (`staticMarkupCollectionID`) REFERENCES `staticmarkupcollection` (`staticMarkupCollectionID`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `staticmarkupcollection`
--

DROP TABLE IF EXISTS `staticmarkupcollection`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `staticmarkupcollection` (
  `staticMarkupCollectionID` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(45) COLLATE utf8_bin NOT NULL,
  `sourceDocumentID` int(11) NOT NULL,
  PRIMARY KEY (`staticMarkupCollectionID`),
  KEY `FK_staticmc_sourceDocumentID` (`sourceDocumentID`),
  CONSTRAINT `FK_staticmc_sourceDocumentID00` FOREIGN KEY (`sourceDocumentID`) REFERENCES `sourcedocument` (`sourceDocumentID`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_staticmarkupcollection`
--

DROP TABLE IF EXISTS `user_staticmarkupcollection`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user_staticmarkupcollection` (
  `user_staticmarkupcollectionID` int(11) NOT NULL AUTO_INCREMENT,
  `userID` int(11) NOT NULL,
  `staticMarkupCollectionID` int(11) NOT NULL,
  `accessMode` int(11) NOT NULL,
  `owner` tinyint(1) NOT NULL,
  PRIMARY KEY (`user_staticmarkupcollectionID`),
  KEY `FK_usersmc_userID` (`userID`),
  KEY `FK_usersmc_staticMarkupCollectionID` (`staticMarkupCollectionID`),
  CONSTRAINT `FK_usersmc_staticMarkupCollectionID` FOREIGN KEY (`staticMarkupCollectionID`) REFERENCES `staticmarkupcollection` (`staticMarkupCollectionID`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `FK_usersmc_userID` FOREIGN KEY (`userID`) REFERENCES `user` (`userID`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_corpus`
--

DROP TABLE IF EXISTS `user_corpus`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user_corpus` (
  `user_corpusID` int(11) NOT NULL AUTO_INCREMENT,
  `userID` int(11) NOT NULL,
  `corpusID` int(11) NOT NULL,
  `accessMode` int(11) NOT NULL,
  `owner` tinyint(1) NOT NULL,
  PRIMARY KEY (`user_corpusID`),
  KEY `FK_usercorpus_userID` (`userID`),
  KEY `FK_usercorpus_corpusID` (`corpusID`),
  CONSTRAINT `FK_usercorpus_corpusID` FOREIGN KEY (`corpusID`) REFERENCES `corpus` (`corpusID`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `FK_usercorpus_userID` FOREIGN KEY (`userID`) REFERENCES `user` (`userID`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tagreference`
--

DROP TABLE IF EXISTS `tagreference`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tagreference` (
  `tagReferenceID` int(11) NOT NULL AUTO_INCREMENT,
  `characterStart` int(11) NOT NULL,
  `characterEnd` int(11) NOT NULL,
  `userMarkupCollectionID` int(11) NOT NULL,
  `tagInstanceID` int(11) NOT NULL,
  PRIMARY KEY (`tagReferenceID`),
  KEY `FK_tr_userMarkupCollectionID` (`userMarkupCollectionID`),
  KEY `FK_tr_tagInstanceID` (`tagInstanceID`),
  CONSTRAINT `FK_tr_tagInstanceID` FOREIGN KEY (`tagInstanceID`) REFERENCES `taginstance` (`tagInstanceID`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `FK_tr_userMarkupCollectionID` FOREIGN KEY (`userMarkupCollectionID`) REFERENCES `usermarkupcollection` (`usermarkupCollectionID`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=319 DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `corpus_staticmarkupcollection`
--

DROP TABLE IF EXISTS `corpus_staticmarkupcollection`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `corpus_staticmarkupcollection` (
  `corpus_staticmarkupcollectionID` int(11) NOT NULL AUTO_INCREMENT,
  `corpusID` int(11) NOT NULL,
  `staticMarkupCollectionID` int(11) NOT NULL,
  PRIMARY KEY (`corpus_staticmarkupcollectionID`),
  KEY `FK_corpussmc_corpusID` (`corpusID`),
  KEY `FK_corpussmc_staticMarkupCollectionID` (`staticMarkupCollectionID`),
  CONSTRAINT `FK_corpussmc_corpusID` FOREIGN KEY (`corpusID`) REFERENCES `corpus` (`corpusID`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `FK_corpussmc_staticMarkupCollectionID` FOREIGN KEY (`staticMarkupCollectionID`) REFERENCES `staticmarkupcollection` (`staticMarkupCollectionID`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `corpus`
--

DROP TABLE IF EXISTS `corpus`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `corpus` (
  `corpusID` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(45) COLLATE utf8_bin NOT NULL,
  PRIMARY KEY (`corpusID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `property`
--

DROP TABLE IF EXISTS `property`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `property` (
  `propertyID` int(11) NOT NULL AUTO_INCREMENT,
  `propertyDefinitionID` int(11) NOT NULL,
  `tagInstanceID` int(11) NOT NULL,
  PRIMARY KEY (`propertyID`),
  KEY `FK_prop_propertyDefinitionID` (`propertyDefinitionID`),
  KEY `FK_prop_tagInstanceID` (`tagInstanceID`),
  CONSTRAINT `FK_prop_propertyDefinitionID` FOREIGN KEY (`propertyDefinitionID`) REFERENCES `propertydefinition` (`propertyDefinitionID`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `FK_prop_tagInstanceID` FOREIGN KEY (`tagInstanceID`) REFERENCES `taginstance` (`tagInstanceID`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=274 DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_usermarkupcollection`
--

DROP TABLE IF EXISTS `user_usermarkupcollection`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user_usermarkupcollection` (
  `user_usermarkupcollectioID` int(11) NOT NULL AUTO_INCREMENT,
  `userID` int(11) NOT NULL,
  `userMarkupCollectionID` int(11) NOT NULL,
  `accessMode` int(11) NOT NULL,
  `owner` tinyint(1) NOT NULL,
  PRIMARY KEY (`user_usermarkupcollectioID`),
  KEY `FK_userumc_userID` (`userID`),
  KEY `FK_userumc_userMarkupCollectionID` (`userMarkupCollectionID`),
  CONSTRAINT `FK_userumc_userID` FOREIGN KEY (`userID`) REFERENCES `user` (`userID`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `FK_userumc_userMarkupCollectionID` FOREIGN KEY (`userMarkupCollectionID`) REFERENCES `usermarkupcollection` (`usermarkupCollectionID`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `unseparable_charsequence`
--

DROP TABLE IF EXISTS `unseparable_charsequence`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `unseparable_charsequence` (
  `uscID` int(11) NOT NULL AUTO_INCREMENT,
  `charsequence` varchar(45) COLLATE utf8_bin NOT NULL,
  `sourceDocumentID` int(11) NOT NULL,
  PRIMARY KEY (`uscID`),
  KEY `FK_uscs_sourceDocumentID` (`sourceDocumentID`),
  CONSTRAINT `FK_uscs_sourceDocumentID` FOREIGN KEY (`sourceDocumentID`) REFERENCES `sourcedocument` (`sourceDocumentID`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `usermarkupcollection`
--

DROP TABLE IF EXISTS `usermarkupcollection`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `usermarkupcollection` (
  `usermarkupCollectionID` int(11) NOT NULL AUTO_INCREMENT,
  `title` varchar(300) COLLATE utf8_bin DEFAULT NULL,
  `publisher` varchar(300) COLLATE utf8_bin DEFAULT NULL,
  `author` varchar(300) COLLATE utf8_bin DEFAULT NULL,
  `description` varchar(300) COLLATE utf8_bin DEFAULT NULL,
  `sourceDocumentID` int(11) NOT NULL,
  `tagLibraryID` int(11) NOT NULL,
  PRIMARY KEY (`usermarkupCollectionID`),
  KEY `FK_umc_sourceDocumentID` (`sourceDocumentID`),
  KEY `FK_umc_tagLibraryID` (`tagLibraryID`),
  CONSTRAINT `FK_umc_sourceDocumentID` FOREIGN KEY (`sourceDocumentID`) REFERENCES `sourcedocument` (`sourceDocumentID`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `FK_umc_tagLibraryID` FOREIGN KEY (`tagLibraryID`) REFERENCES `taglibrary` (`tagLibraryID`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=36 DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping routines for database 'CatmaRepository'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2012-06-11 19:30:09

CREATE USER 'catma'@'localhost' IDENTIFIED BY 'roaring_like_thunder';

GRANT USAGE ON * . * TO 'catma'@'localhost' IDENTIFIED BY 'roaring_like_thunder' WITH MAX_QUERIES_PER_HOUR 0 MAX_CONNECTIONS_PER_HOUR 0 MAX_UPDATES_PER_HOUR 0 MAX_USER_CONNECTIONS 0 ;

GRANT SELECT , INSERT , UPDATE , DELETE ON `CatmaIndex` . * TO 'catma'@'localhost';
GRANT SELECT , INSERT , UPDATE , DELETE ON `CatmaRepository` . * TO 'catma'@'localhost';