/**
* CATMA Query Walker Tree Grammar
*
* Author: 	Malte Meister, Marco Petris
* About:	Tree Grammar for the CATMA Query Walker
*/

tree grammar CatmaQueryWalker;

options {
	tokenVocab=CatmaQuery;
	ASTLabelType=CommonTree;
}

@treeparser::header {
package de.catma.queryengine.parser;

import de.catma.queryengine.*;
}


/************************************************
* grammar rules
*************************************************/

start returns [Query finalQuery]
	:	query { $finalQuery = $query.query; } 
	;
	catch[RecognitionException e] {throw e;}

query returns [Query query]
	:	^(ND_QUERY queryExpression refinement[$queryExpression.query]?) { $query = $queryExpression.query; } 
	;
	catch[RecognitionException e] {throw e;}
	

/************************************************
* the query expression definition
*************************************************/

queryExpression returns [Query query]
	:	unionQuery { $query = $unionQuery.query; }
		| collocQuery { $query = $collocQuery.query; }
		| exclusionQuery { $query = $exclusionQuery.query; }
		| adjacencyQuery { $query = $adjacencyQuery.query; }
		| term { $query = $term.query; }
	;	
	catch[RecognitionException e] {throw e;}
	
/********************************************************
* query expression building blocks and their connectors
*********************************************************/


unionQuery returns [Query query]
	:	^(ND_UNION term1=term term2=term exclusiveMarker='EXCL'?) { $query = new UnionQuery($term1.query, $term2.query, $exclusiveMarker.text); }
	;
	catch[RecognitionException e] {throw e;}
	
collocQuery returns [Query query]
	:	^(ND_COLLOC term1=term term2=term INT?) { $query = new CollocQuery($term1.query, $term2.query, $INT.text); }
	;
	catch[RecognitionException e] {throw e;}
	
exclusionQuery returns [Query query]
	:	^(ND_EXCLUSION term1=term term2=term matchMode=MATCH_MODE?) { $query = new ExclusionQuery($term1.query, $term2.query, $matchMode.text); }
	;
	catch[RecognitionException e] {throw e;}
	
adjacencyQuery returns [Query query] 
	:	^(ND_ADJACENCY term1=term term2=term) { $query = new AdjacencyQuery($term1.query, $term2.query); }
	;
	catch[RecognitionException e] {throw e;}
	

term returns [Query query]
	:	phrase { $query = $phrase.query; }
		| selector { $query = $selector.query; }
		| subQuery=query { $query = new SubQuery($subQuery.query); }
	;
	catch[RecognitionException e] {throw e;}
	



phrase returns [Phrase query]
	:	^(ND_PHRASE TXT) { $query = new Phrase($TXT.text); }
	;
	catch[RecognitionException e] {throw e;}
	

	
/********************************************************
* special queries definition
********************************************************/

selector returns [Query query]
	:	tagQuery { $query = $tagQuery.query; }
	|	tagdiffQuery { $query = $tagdiffQuery.query; }
	|	propertyQuery { $query = $propertyQuery.query; }
	|	regQuery { $query = $regQuery.query; }
	|	freqQuery { $query = $freqQuery.query; }
	|	similQuery { $query = $similQuery.query; }
	|	wildQuery { $query = $wildQuery.query; }
	;
	catch[RecognitionException e] {throw e;}
	
	
tagQuery returns [Query query]
	:	^(ND_TAG phrase) { $query = new TagQuery($phrase.query); }
	;
	catch[RecognitionException e] {throw e;}

tagdiffQuery returns [Query query]
	:	^(ND_TAGDIFF tagdiff=phrase (property=phrase)?) { $query = new TagDiffQuery($tagdiff.query, $property.query); }
	;
	catch[RecognitionException e] {throw e;}
propertyQuery returns [Query query]
	:	^(ND_PROPERTY property=phrase (value=phrase)?) { $query = new PropertyQuery(null, $property.query, $value.query); }
	|	^(ND_TAGPROPERTY tag=phrase property=phrase (value=phrase)?) { $query = new PropertyQuery($tag.query, $property.query, $value.query); }
	;
	catch[RecognitionException e] {throw e;}

regQuery returns [Query query]
	:	^(ND_REG phrase caseInsensitiveMarker='CI'?) { $query = new RegQuery($phrase.query, $caseInsensitiveMarker.text); }
	;
	catch[RecognitionException e] {throw e;}
	
freqQuery returns [Query query] 
	:	^(ND_FREQ EQUAL rInt1=INT rInt2=INT?) { $query = new FreqQuery($EQUAL.text, $rInt1.text, $rInt2.text); }
		| ^(ND_FREQ UNEQUAL INT) { $query = new FreqQuery($UNEQUAL.text, $INT.text); }
	;	
	catch[RecognitionException e] {throw e;}
	
similQuery returns [Query query] 
	:	^(ND_SIMIL phrase INT) { $query = new SimilQuery($phrase.query, $INT.text); }
	; 
	catch[RecognitionException e] {throw e;}
	
wildQuery returns [Query query]
	:	^(ND_WILD phrase) { $query = new WildcardQuery($phrase.query); }
	;
	catch[RecognitionException e] {throw e;}

/************************************************
* refinement definition
*************************************************/	

refinement[Query query] 
	:	refinementExpression { query.setRefinement($refinementExpression.refinement); }
	;
	catch[RecognitionException e] {throw e;}
	
	
refinementExpression returns [Refinement refinement]
	:	orRefinement { $refinement = $orRefinement.refinement; }
		| andRefinement { $refinement = $andRefinement.refinement; }
		| ^(ND_REFINE refinementTerm) { $refinement = $refinementTerm.refinement; }
	;
	catch[RecognitionException e] {throw e;}

orRefinement returns [Refinement refinement]
	:	^(ND_ORREFINE rTerm1=refinementTerm rTerm2=refinementTerm) 
			{ $refinement = new OrRefinement($rTerm1.refinement,$rTerm2.refinement); }
	;
	catch[RecognitionException e] {throw e;}
	
andRefinement returns [Refinement refinement]
	:	^(ND_ANDREFINE rTerm1=refinementTerm rTerm2=refinementTerm) 
			{ $refinement = new AndRefinement($rTerm1.refinement,$rTerm2.refinement); }
	;
	catch[RecognitionException e] {throw e;}

refinementTerm returns [Refinement refinement]
	: 	selector matchMode=MATCH_MODE? { $refinement = new QueryRefinement($selector.query, $matchMode.text); }
		| subQuery=query matchMode=MATCH_MODE? { $refinement = new QueryRefinement(new SubQuery($subQuery.query), $matchMode.text); }
		| refinementExpression { $refinement = $refinementExpression.refinement; }
	;
	catch[RecognitionException e] {throw e;}

/*
refinementTerm returns [Refinement refinement]
	: 	selector { $refinement = new QueryRefinement($selector.query); }
		| refinementExpression { $refinement = $refinementExpression.refinement; }
	;
	catch[RecognitionException e] {throw e;}
*/	

