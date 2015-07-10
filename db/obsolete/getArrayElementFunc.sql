delimiter $$

DROP FUNCTION IF EXISTS getArrayElement$$

CREATE DEFINER=`root`@`localhost` FUNCTION `getArrayElement`(array VARCHAR(3000), delim CHAR(1), pos INT) RETURNS varchar(500) CHARSET utf8
BEGIN
	/* originally posted as a comment by Bob Collins at http://dev.mysql.com/doc/refman/5.5/en/string-functions.html */

	DECLARE element VARCHAR(500);
	
	SELECT REPLACE(
		SUBSTRING(
			SUBSTRING_INDEX(array, delim, pos), 
			LENGTH(SUBSTRING_INDEX(array, delim, pos - 1)) + 1), delim, '')
	INTO element;

RETURN element;
END$$

