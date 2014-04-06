/**
* CATMA Query Parser Grammar
*
* Author: 	Malte Meister, Marco Petris
* Version: 	2.0
* About:	EBNF Grammar for the CATMA Query Parser with AST generation rules
*/

grammar CatmaQuery;

options {output=AST;}

// AST node symbols
tokens {
	ND_QUERY;
	ND_UNION;
	ND_PHRASE;
	ND_COLLOC;
	ND_EXCLUSION;
	ND_ADJACENCY;
	ND_TAG;
	ND_PROPERTY;
	ND_TAGPROPERTY;
	ND_REG;
	ND_FREQ;
	ND_SIMIL;
	ND_WILD;
	ND_REFINE;
	ND_ORREFINE;
	ND_ANDREFINE;
}


//************************************************
// package definition for generated code
//************************************************

@lexer::header {
package de.catma.queryengine.parser;
}

@parser::header {
package de.catma.queryengine.parser;
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


/************************************************
* grammar rules
*************************************************/

start	:	query EOF
	;
	catch[RecognitionException e] {throw e;}

query	:	queryExpression refinement? -> ^(ND_QUERY queryExpression refinement?)
	;
	catch[RecognitionException e] {throw e;}
	

/************************************************
* the query expression definition
*************************************************/

queryExpression 
	:	startTerm=term (
			unionQuery[(CommonTree)$startTerm.tree] -> unionQuery
			| collocQuery [(CommonTree)$startTerm.tree] -> collocQuery
			| exclusionQuery [(CommonTree)$startTerm.tree] -> exclusionQuery
			| adjacencyQuery [(CommonTree)$startTerm.tree] -> adjacencyQuery
			| -> term )
	;	
	catch[RecognitionException e] {throw e;}

	
/********************************************************
* query expression building blocks and their connectors
*********************************************************/


unionQuery[CommonTree startTerm] 
	:	',' term 'EXCL'? -> ^(ND_UNION {$startTerm} term 'EXCL'?)
	;
	catch[RecognitionException e] {throw e;}
	
collocQuery[CommonTree startTerm]  
	:	'&' term INT? -> ^(ND_COLLOC {$startTerm} term INT?)
	;
	catch[RecognitionException e] {throw e;}
	
exclusionQuery[CommonTree startTerm] 
	:	'-' term -> ^(ND_EXCLUSION {$startTerm} term)
	;
	catch[RecognitionException e] {throw e;}
	
adjacencyQuery[CommonTree startTerm]  
	:	';' term -> ^(ND_ADJACENCY {$startTerm} term) 
	;
	catch[RecognitionException e] {throw e;}
	

term 	:	phrase -> phrase
		| selector -> selector 
		| '('query')' -> query
	;
	catch[RecognitionException e] {throw e;}
	



phrase :	TXT -> ^(ND_PHRASE TXT)
	;
	catch[RecognitionException e] {throw e;}
	

	
/********************************************************
* special queries definition
********************************************************/

selector 
	:	tagQuery
	|	propertyQuery
	|	regQuery
	|	freqQuery
	|	similQuery
	|	wildQuery
	;
	catch[RecognitionException e] {throw e;}
	
	
tagQuery
	:	TAG EQUAL phrase TAG_MATCH_MODE? -> ^(ND_TAG phrase TAG_MATCH_MODE?)
	;
	catch[RecognitionException e] {throw e;}
	
	
propertyQuery 
	:	PROPERTY EQUAL phrase (VALUE EQUAL phrase)? TAG_MATCH_MODE? -> ^(ND_PROPERTY phrase phrase? TAG_MATCH_MODE?)	
	|	TAG EQUAL phrase PROPERTY EQUAL phrase (VALUE EQUAL phrase)? TAG_MATCH_MODE? -> ^(ND_TAGPROPERTY phrase phrase phrase? TAG_MATCH_MODE?)
	;
	catch[RecognitionException e] {throw e;}
	
regQuery
	:	REG EQUAL phrase 'CI'? -> ^(ND_REG phrase 'CI'?)
	;
	catch[RecognitionException e] {throw e;}
	
freqQuery 
	:	FREQ 
		( EQUAL INT ('-' INT)? -> ^(ND_FREQ EQUAL INT INT?) 
		| UNEQUAL INT -> ^(ND_FREQ UNEQUAL INT) ) 
	;	
	catch[RecognitionException e] {throw e;}
	
similQuery
	:	SIMIL EQUAL phrase INT '%'? -> ^(ND_SIMIL phrase INT)
	; 
	catch[RecognitionException e] {throw e;}
	
wildQuery
	:	WILD EQUAL phrase -> ^(ND_WILD phrase)
	;
	catch[RecognitionException e] {throw e;}

	

/************************************************
* refinement definition
*************************************************/	

refinement 
	:	'where' refinementExpression -> refinementExpression
	;
	catch[RecognitionException e] {throw e;}
	
refinementExpression
	:	startRefinement=refinementTerm ( 
			orRefinement[(CommonTree)$startRefinement.tree] -> orRefinement
			| andRefinement[(CommonTree)$startRefinement.tree] -> andRefinement
			| -> ^(ND_REFINE refinementTerm) )
	;
	catch[RecognitionException e] {throw e;}

orRefinement[CommonTree startRefinement] 
	:	'|' refinementTerm -> ^(ND_ORREFINE {$startRefinement} refinementTerm)
	;
	catch[RecognitionException e] {throw e;}
	
andRefinement[CommonTree startRefinement] 
	:	',' refinementTerm -> ^(ND_ANDREFINE {$startRefinement} refinementTerm)
	;
	catch[RecognitionException e] {throw e;}
	
refinementTerm 
	: 	selector -> selector
		| '('refinementExpression')' -> refinementExpression
	;
	catch[RecognitionException e] {throw e;}
	
	
/************************************************
* lexer symbols
*************************************************/

TAG 	:	'tag'
	;
	
TAG_MATCH_MODE 
	:	'boundary' | 'overlap' | 'exact'
	;
	
PROPERTY 
	:	'property'
	;
	
VALUE	:	'value'
	;	
		
REG	:	'reg'
	;
	
FREQ	:	'freq'
	;
	
SIMIL	:	'simil'
	;
	
WILD	:	'wild'
	;
	
WHITESPACE 
	:	(' '|'\t'|'\r'|'\n'|'\u000C') {skip();}
	;

EQUAL 	:	'='
	;
	
UNEQUAL	:	( '<' | '>' | '<=' | '>=' )
	;

TXT	:	'"' (~('"')|'\\"')+ '"'
	;

fragment LETTER 	
	:	('a'..'z'|'A'..'Z')
	;
	
fragment LETTEREXTENDED
	:	('\u00C0'..'\u00D6'|'\u00D8'..'\u00F6'|'\u00F8'..'\u00FF')
	;

GROUPIDENT 
	:	'@' (LETTER|'_'|LETTEREXTENDED) (LETTER|'_'|LETTEREXTENDED|INT)+
	;

INT	:	('0'..'9')+
	;
	
