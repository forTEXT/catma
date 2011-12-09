<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet
	version="2.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:tei="http://www.tei-c.org/ns/1.0"
	exclude-result-prefixes="tei">

	<xsl:output method="xml" indent="yes" encoding="UTF-8"/>
 
	<xsl:template match="*">
		<xsl:copy>
			<xsl:copy-of select="@*"/>
			<xsl:apply-templates/>
		</xsl:copy>
	</xsl:template>

	<xsl:template match="tei:teiHeader">
		<xsl:copy>
			<xsl:copy-of select="@*"/>
			<xsl:apply-templates/>
		</xsl:copy>
		<encodingDesc>
			<fsdDecl>
			<!--<xsl:for-each select="*/tei:text/tei:fs">
				
			</xsl:for-each>-->
			</fsdDecl>
		</encodingDesc>
	</xsl:template>
	
</xsl:stylesheet>