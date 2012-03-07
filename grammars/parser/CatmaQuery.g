/**
* THIS IS LEGACY CODE! Please edit CatmaQuery.g in grammar.ast and CatmaQueryWalker.g in grammar.tree!
*
* CATMA Query Parser Grammar
*
* Author: 	Malte Meister, Marco Petris
* Version: 	2.0
* About:	EBNF Grammar for the CATMA Query Parser, which enables the use of custom queries to make a selection in CATMA.
*/

grammar CatmaQuery;

//************************************************
// package definition for generated code
//************************************************

@lexer::header {
package org.catma.parser;
}

@parser::header {
package org.catma.parser;
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

/************************************************
* grammar rules
*************************************************/

start	:	query
	;
	catch[RecognitionException e] {throw e;}

query	:	queryExpression refinement?
	;
	catch[RecognitionException e] {throw e;}
	

/************************************************
* the query expression definition
*************************************************/

queryExpression 
	:	term (unionQuery | collocQuery | exclusionQuery | adjacencyQuery)?
	;	
	catch[RecognitionException e] {throw e;}
	

/********************************************************
* query expression building blocks and their connectors
*********************************************************/


unionQuery 
	:	','{ System.out.println( "UNION" );} term
	;
	catch[RecognitionException e] {throw e;}
	
collocQuery 
	:	'&'{ System.out.println( "COLLOC" );} term INT?
	;
	catch[RecognitionException e] {throw e;}
	
exclusionQuery
	:	'-'{ System.out.println( "EXCLUDE" );} term
	;
	catch[RecognitionException e] {throw e;}
	
adjacencyQuery 
	:	';'{ System.out.println( "ADJACENCY" );} term // may not be needed 
	;
	catch[RecognitionException e] {throw e;}
	

term 	:	phrase | selector | '('query')'
	;
	catch[RecognitionException e] {throw e;}
	



phrase returns [String value]
	:	TXT { $value = $TXT.text; System.out.println("found phrase " + $TXT.text); }
	;
	catch[RecognitionException e] {throw e;}
	

	
/********************************************************
* special queries definition
********************************************************/

selector 
	:	tagQuery
	|	regQuery
	|	freqQuery
	|	groupQuery
	|	similQuery
	;
	catch[RecognitionException e] {throw e;}
	
	
	
tagQuery
	:	TAG EQUAL phrase { System.out.println("tag query with tag " + $phrase.value); }
	;
	catch[RecognitionException e] {throw e;}

regQuery
	:	REG EQUAL phrase { System.out.println("reg query with regex " + $phrase.value); }
	;
	catch[RecognitionException e] {throw e;}
	
freqQuery 
	:	FREQ (
			EQUAL { System.out.println("found comparator = for " ); } |
		 	UNEQUAL { System.out.println("found comparator " + $UNEQUAL.text + " for" ); })
		INT { System.out.println("freq query: freq " + $INT.text); }
	;	
	catch[RecognitionException e] {throw e;}
	
groupQuery
	:	GROUPIDENT { System.out.println("group query with group " + $GROUPIDENT.text); }
	; 
	catch[RecognitionException e] {throw e;}
	
similQuery
	:	SIMIL phrase INT '%' { System.out.println("simil query with " + $phrase.value + " " + $INT.text + "\%"); }
	; 
	catch[RecognitionException e] {throw e;}
	

/************************************************
* refinement definition
*************************************************/	

refinement 
	:	'where' { System.out.println( "refinement:" );} refinementExpression
	;
	catch[RecognitionException e] {throw e;}
	
refinementExpression
	:	refinementTerm ( orRefinement | andRefinement )?
	;
	catch[RecognitionException e] {throw e;}

orRefinement 
	:	'|' { System.out.println( "OR" );} refinementTerm
	;
	catch[RecognitionException e] {throw e;}
	
andRefinement 
	:	',' { System.out.println( "AND" );} refinementTerm
	;
	catch[RecognitionException e] {throw e;}
	
refinementTerm 
	: 	selector | '('refinementExpression')'
	;
	catch[RecognitionException e] {throw e;}
	
	
/************************************************
* lexer symbols
*************************************************/

TAG
    :	'tag'
	;
	
REG
    :	'reg'
	;
	
FREQ
    :	'freq'
	;
	
SIMIL
    :	'simil'
	;
	
WHITESPACE 
	:	(' '|'\t'|'\r'|'\n'|'\u000C') {skip();}
	;

EQUAL
    :	'='
	;
	
UNEQUAL
    :	( '<' | '>' | '<=' | '>=' )
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
	
