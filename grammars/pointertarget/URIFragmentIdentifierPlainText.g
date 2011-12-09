grammar URIFragmentIdentifierPlainText;


//************************************************
// package definition for generated code
//************************************************

@lexer::header {
package de.catma.serialization.tei.pointer.parser;
}

@parser::header {
package de.catma.serialization.tei.pointer.parser;
import de.catma.serialization.tei.pointer.*;
import de.catma.core.document.Range;
}

//************************************************
// plain code generation for the lexer
//************************************************

@lexer::members {

/**
* overrides the default error handling. enables immediate failure
*/
public void reportError(RecognitionException e) {
	throw new RuntimeException(e);
}

}


//************************************************
// plain code generation for the parser
//************************************************

@parser::members {

class IntegrityCheckParams {
	String md5;
	Integer length;
	String mimeCharset;
	public IntegrityCheckParams(Integer length, String mimeCharset) {
		super();
		this.length = length;
		this.mimeCharset = mimeCharset;
	}
	public IntegrityCheckParams(String md5, String mimeCharset) {
		super();
		this.md5 = md5;
		this.mimeCharset = mimeCharset;
	}
	
}

private TextFragmentIdentifier textfragmentIdentifier = null;

public TextFragmentIdentifier getTextFragmentIdentifier() {
	return textfragmentIdentifier;
}


/**
* overrides the default error handling. enables immediate failure
*/
protected void mismatch(IntStream input, int ttype, BitSet follow)
	throws RecognitionException {
	throw new MismatchedTokenException(ttype,input);
}

/**
* overrides the default error handling. enables immediate failure
*/
public Object recoverFromMismatchedSet(IntStream input, RecognitionException e, BitSet follow)
	throws RecognitionException {
	throw e;
}

/**
* overrides the default error handling. enables immediate failure
*/
protected Object recoverFromMismatchedToken(IntStream input, int ttype, BitSet follow)
	throws RecognitionException
{
	throw new MismatchedTokenException(ttype,input);
}
}

@rulecatch {
catch (RecognitionException e) {
	throw e;
}
}


start :	textFragment EOF
	;
	catch[RecognitionException e] {throw e;}
	
textFragment
	:	textScheme (';' integrityCheck)*
		{
			textfragmentIdentifier = $textScheme.textfragmentIdentifier;
			if ($integrityCheck.integrityCheckParams != null) {
				textfragmentIdentifier.setLength($integrityCheck.integrityCheckParams.length);
				textfragmentIdentifier.setMd5HexValue($integrityCheck.integrityCheckParams.md5);
				textfragmentIdentifier.setMimeCharset($integrityCheck.integrityCheckParams.mimeCharset);
			}
		}
	;	
	catch[RecognitionException e] {throw e;}
	
textScheme returns [TextFragmentIdentifier textfragmentIdentifier]
	:	lineScheme
			{
		 	$textfragmentIdentifier = $lineScheme.textfragmentIdentifier;
		 	}
	| charScheme
			{
		 	$textfragmentIdentifier = $charScheme.textfragmentIdentifier;
		 	}

	;
	catch[RecognitionException e] {throw e;}
	
charScheme returns [TextFragmentIdentifier textfragmentIdentifier]
	:	CHAR_S '=' range
			{
			$textfragmentIdentifier = new CharFragmentIdentifier($range.range);
			}
	;
	catch[RecognitionException e] {throw e;}
			
lineScheme returns [TextFragmentIdentifier textfragmentIdentifier]
	:	LINE_S '=' range
			{
			$textfragmentIdentifier = new LineFragmentIdentifier($range.range);
			}
	;	
	catch[RecognitionException e] {throw e;}
	
integrityCheck returns [IntegrityCheckParams integrityCheckParams]
	:	(lengthScheme  (',' mimeCharset)?) 
			{
			$integrityCheckParams = new IntegrityCheckParams($lengthScheme.length, $mimeCharset.text);
			}
		| (md5Scheme (',' mimeCharset)?)
			{
			$integrityCheckParams = new IntegrityCheckParams($md5Scheme.md5, $mimeCharset.text);
			}
	;
	catch[RecognitionException e] {throw e;}

position 
	:	INT
	;
	catch[RecognitionException e] {throw e;}
	
range 	returns [Range range]
	:	(position1=position (comma1=',' | (comma2=',' position2=position))?)
		{
		int pos1 = Integer.valueOf($position1.text);
		int pos2 = -1;
	
		if ($comma2.text != null) {
			pos2 = Integer.valueOf($position2.text);
		}
	
	
		$range = new Range(pos1,pos2);
		
		}
		| (',' position) 
		{
		int pos1 = Integer.valueOf($position1.text);
		
		$range = new Range(0,pos1);
		
		}
		
	;
	catch[RecognitionException e] {throw e;}
	
lengthScheme returns [Integer length] 
	:	'length=' INT
		{
			$length = Integer.valueOf($INT.text);
		}
	;
	catch[RecognitionException e] {throw e;}
	
md5Scheme returns [String md5]
	:	'md5=' MD5VALUE
		{
			$md5 = $MD5VALUE.text;
		}
	;	
	catch[RecognitionException e] {throw e;}
	
mimeCharset 
	:	MIMECHARS+
	;
	catch[RecognitionException e] {throw e;}

INT	:	DIGIT+
	;

MIMECHARS 
	:	(ALPHA | INT | '!' | '#' | '$' | '%' | '&' | '\'' | '+' | '-' | '^' | '_' | '`' | '{' | '}' | '~')+
	;
	
MD5VALUE 
	:	HEXDIGIT+
	;

fragment HEXDIGIT 
	:	 DIGIT | 'a' .. 'f' | 'A' .. 'F'
	;

fragment ALPHA 	:	'A'..'Z'
	;

fragment DIGIT	:	'0'..'9'
	;	
	
CHAR_S 	:	'char'
	;
	
LINE_S 	:	'line'
	;