// $ANTLR 3.5.1 /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g 2014-01-14 14:02:32

package de.catma.queryengine.parser;

import de.catma.queryengine.*;


import org.antlr.runtime.*;
import org.antlr.runtime.tree.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

/**
* CATMA Query Walker Tree Grammar
*
* Author: 	Malte Meister, Marco Petris
* Version: 	1.0
* About:	Tree Grammar for the CATMA Query Walker
*/
@SuppressWarnings("all")
public class CatmaQueryWalker extends TreeParser {
	public static final String[] tokenNames = new String[] {
		"<invalid>", "<EOR>", "<DOWN>", "<UP>", "EQUAL", "FREQ", "GROUPIDENT", 
		"INT", "LETTER", "LETTEREXTENDED", "ND_ADJACENCY", "ND_ANDREFINE", "ND_COLLOC", 
		"ND_EXCLUSION", "ND_FREQ", "ND_ORREFINE", "ND_PHRASE", "ND_PROPERTY", 
		"ND_QUERY", "ND_REFINE", "ND_REG", "ND_SIMIL", "ND_TAG", "ND_TAGPROPERTY", 
		"ND_UNION", "ND_WILD", "PROPERTY", "REG", "SIMIL", "TAG", "TAG_MATCH_MODE", 
		"TXT", "UNEQUAL", "VALUE", "WHITESPACE", "WILD", "'%'", "'&'", "'('", 
		"')'", "','", "'-'", "';'", "'CI'", "'EXCL'", "'where'", "'|'"
	};
	public static final int EOF=-1;
	public static final int T__36=36;
	public static final int T__37=37;
	public static final int T__38=38;
	public static final int T__39=39;
	public static final int T__40=40;
	public static final int T__41=41;
	public static final int T__42=42;
	public static final int T__43=43;
	public static final int T__44=44;
	public static final int T__45=45;
	public static final int T__46=46;
	public static final int EQUAL=4;
	public static final int FREQ=5;
	public static final int GROUPIDENT=6;
	public static final int INT=7;
	public static final int LETTER=8;
	public static final int LETTEREXTENDED=9;
	public static final int ND_ADJACENCY=10;
	public static final int ND_ANDREFINE=11;
	public static final int ND_COLLOC=12;
	public static final int ND_EXCLUSION=13;
	public static final int ND_FREQ=14;
	public static final int ND_ORREFINE=15;
	public static final int ND_PHRASE=16;
	public static final int ND_PROPERTY=17;
	public static final int ND_QUERY=18;
	public static final int ND_REFINE=19;
	public static final int ND_REG=20;
	public static final int ND_SIMIL=21;
	public static final int ND_TAG=22;
	public static final int ND_TAGPROPERTY=23;
	public static final int ND_UNION=24;
	public static final int ND_WILD=25;
	public static final int PROPERTY=26;
	public static final int REG=27;
	public static final int SIMIL=28;
	public static final int TAG=29;
	public static final int TAG_MATCH_MODE=30;
	public static final int TXT=31;
	public static final int UNEQUAL=32;
	public static final int VALUE=33;
	public static final int WHITESPACE=34;
	public static final int WILD=35;

	// delegates
	public TreeParser[] getDelegates() {
		return new TreeParser[] {};
	}

	// delegators


	public CatmaQueryWalker(TreeNodeStream input) {
		this(input, new RecognizerSharedState());
	}
	public CatmaQueryWalker(TreeNodeStream input, RecognizerSharedState state) {
		super(input, state);
	}

	@Override public String[] getTokenNames() { return CatmaQueryWalker.tokenNames; }
	@Override public String getGrammarFileName() { return "/Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g"; }



	// $ANTLR start "start"
	// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:27:1: start returns [Query finalQuery] : query ;
	public final Query start() throws RecognitionException {
		Query finalQuery = null;


		Query query1 =null;

		try {
			// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:28:2: ( query )
			// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:28:4: query
			{
			pushFollow(FOLLOW_query_in_start50);
			query1=query();
			state._fsp--;

			 finalQuery = query1; 
			}

		}
		catch (RecognitionException e) {
			throw e;
		}

		finally {
			// do for sure before leaving
		}
		return finalQuery;
	}
	// $ANTLR end "start"



	// $ANTLR start "query"
	// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:32:1: query returns [Query query] : ^( ND_QUERY queryExpression ( refinement[$queryExpression.query] )? ) ;
	public final Query query() throws RecognitionException {
		Query query = null;


		Query queryExpression2 =null;

		try {
			// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:33:2: ( ^( ND_QUERY queryExpression ( refinement[$queryExpression.query] )? ) )
			// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:33:4: ^( ND_QUERY queryExpression ( refinement[$queryExpression.query] )? )
			{
			match(input,ND_QUERY,FOLLOW_ND_QUERY_in_query75); 
			match(input, Token.DOWN, null); 
			pushFollow(FOLLOW_queryExpression_in_query77);
			queryExpression2=queryExpression();
			state._fsp--;

			// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:33:31: ( refinement[$queryExpression.query] )?
			int alt1=2;
			int LA1_0 = input.LA(1);
			if ( (LA1_0==ND_ANDREFINE||LA1_0==ND_ORREFINE||LA1_0==ND_REFINE) ) {
				alt1=1;
			}
			switch (alt1) {
				case 1 :
					// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:33:31: refinement[$queryExpression.query]
					{
					pushFollow(FOLLOW_refinement_in_query79);
					refinement(queryExpression2);
					state._fsp--;

					}
					break;

			}

			match(input, Token.UP, null); 

			 query = queryExpression2; 
			}

		}
		catch (RecognitionException e) {
			throw e;
		}

		finally {
			// do for sure before leaving
		}
		return query;
	}
	// $ANTLR end "query"



	// $ANTLR start "queryExpression"
	// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:42:1: queryExpression returns [Query query] : ( unionQuery | collocQuery | exclusionQuery | adjacencyQuery | term );
	public final Query queryExpression() throws RecognitionException {
		Query query = null;


		Query unionQuery3 =null;
		Query collocQuery4 =null;
		Query exclusionQuery5 =null;
		Query adjacencyQuery6 =null;
		Query term7 =null;

		try {
			// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:43:2: ( unionQuery | collocQuery | exclusionQuery | adjacencyQuery | term )
			int alt2=5;
			switch ( input.LA(1) ) {
			case ND_UNION:
				{
				alt2=1;
				}
				break;
			case ND_COLLOC:
				{
				alt2=2;
				}
				break;
			case ND_EXCLUSION:
				{
				alt2=3;
				}
				break;
			case ND_ADJACENCY:
				{
				alt2=4;
				}
				break;
			case ND_FREQ:
			case ND_PHRASE:
			case ND_PROPERTY:
			case ND_QUERY:
			case ND_REG:
			case ND_SIMIL:
			case ND_TAG:
			case ND_TAGPROPERTY:
			case ND_WILD:
				{
				alt2=5;
				}
				break;
			default:
				NoViableAltException nvae =
					new NoViableAltException("", 2, 0, input);
				throw nvae;
			}
			switch (alt2) {
				case 1 :
					// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:43:4: unionQuery
					{
					pushFollow(FOLLOW_unionQuery_in_queryExpression111);
					unionQuery3=unionQuery();
					state._fsp--;

					 query = unionQuery3; 
					}
					break;
				case 2 :
					// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:44:5: collocQuery
					{
					pushFollow(FOLLOW_collocQuery_in_queryExpression119);
					collocQuery4=collocQuery();
					state._fsp--;

					 query = collocQuery4; 
					}
					break;
				case 3 :
					// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:45:5: exclusionQuery
					{
					pushFollow(FOLLOW_exclusionQuery_in_queryExpression127);
					exclusionQuery5=exclusionQuery();
					state._fsp--;

					 query = exclusionQuery5; 
					}
					break;
				case 4 :
					// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:46:5: adjacencyQuery
					{
					pushFollow(FOLLOW_adjacencyQuery_in_queryExpression135);
					adjacencyQuery6=adjacencyQuery();
					state._fsp--;

					 query = adjacencyQuery6; 
					}
					break;
				case 5 :
					// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:47:5: term
					{
					pushFollow(FOLLOW_term_in_queryExpression143);
					term7=term();
					state._fsp--;

					 query = term7; 
					}
					break;

			}
		}
		catch (RecognitionException e) {
			throw e;
		}

		finally {
			// do for sure before leaving
		}
		return query;
	}
	// $ANTLR end "queryExpression"



	// $ANTLR start "unionQuery"
	// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:56:1: unionQuery returns [Query query] : ^( ND_UNION term1= term term2= term (exclusiveMarker= 'EXCL' )? ) ;
	public final Query unionQuery() throws RecognitionException {
		Query query = null;


		CommonTree exclusiveMarker=null;
		Query term1 =null;
		Query term2 =null;

		try {
			// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:57:2: ( ^( ND_UNION term1= term term2= term (exclusiveMarker= 'EXCL' )? ) )
			// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:57:4: ^( ND_UNION term1= term term2= term (exclusiveMarker= 'EXCL' )? )
			{
			match(input,ND_UNION,FOLLOW_ND_UNION_in_unionQuery173); 
			match(input, Token.DOWN, null); 
			pushFollow(FOLLOW_term_in_unionQuery177);
			term1=term();
			state._fsp--;

			pushFollow(FOLLOW_term_in_unionQuery181);
			term2=term();
			state._fsp--;

			// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:57:52: (exclusiveMarker= 'EXCL' )?
			int alt3=2;
			int LA3_0 = input.LA(1);
			if ( (LA3_0==44) ) {
				alt3=1;
			}
			switch (alt3) {
				case 1 :
					// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:57:52: exclusiveMarker= 'EXCL'
					{
					exclusiveMarker=(CommonTree)match(input,44,FOLLOW_44_in_unionQuery185); 
					}
					break;

			}

			match(input, Token.UP, null); 

			 query = new UnionQuery(term1, term2, (exclusiveMarker!=null?exclusiveMarker.getText():null)); 
			}

		}
		catch (RecognitionException e) {
			throw e;
		}

		finally {
			// do for sure before leaving
		}
		return query;
	}
	// $ANTLR end "unionQuery"



	// $ANTLR start "collocQuery"
	// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:61:1: collocQuery returns [Query query] : ^( ND_COLLOC term1= term term2= term ( INT )? ) ;
	public final Query collocQuery() throws RecognitionException {
		Query query = null;


		CommonTree INT8=null;
		Query term1 =null;
		Query term2 =null;

		try {
			// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:62:2: ( ^( ND_COLLOC term1= term term2= term ( INT )? ) )
			// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:62:4: ^( ND_COLLOC term1= term term2= term ( INT )? )
			{
			match(input,ND_COLLOC,FOLLOW_ND_COLLOC_in_collocQuery212); 
			match(input, Token.DOWN, null); 
			pushFollow(FOLLOW_term_in_collocQuery216);
			term1=term();
			state._fsp--;

			pushFollow(FOLLOW_term_in_collocQuery220);
			term2=term();
			state._fsp--;

			// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:62:38: ( INT )?
			int alt4=2;
			int LA4_0 = input.LA(1);
			if ( (LA4_0==INT) ) {
				alt4=1;
			}
			switch (alt4) {
				case 1 :
					// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:62:38: INT
					{
					INT8=(CommonTree)match(input,INT,FOLLOW_INT_in_collocQuery222); 
					}
					break;

			}

			match(input, Token.UP, null); 

			 query = new CollocQuery(term1, term2, (INT8!=null?INT8.getText():null)); 
			}

		}
		catch (RecognitionException e) {
			throw e;
		}

		finally {
			// do for sure before leaving
		}
		return query;
	}
	// $ANTLR end "collocQuery"



	// $ANTLR start "exclusionQuery"
	// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:66:1: exclusionQuery returns [Query query] : ^( ND_EXCLUSION term1= term term2= term ) ;
	public final Query exclusionQuery() throws RecognitionException {
		Query query = null;


		Query term1 =null;
		Query term2 =null;

		try {
			// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:67:2: ( ^( ND_EXCLUSION term1= term term2= term ) )
			// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:67:4: ^( ND_EXCLUSION term1= term term2= term )
			{
			match(input,ND_EXCLUSION,FOLLOW_ND_EXCLUSION_in_exclusionQuery249); 
			match(input, Token.DOWN, null); 
			pushFollow(FOLLOW_term_in_exclusionQuery253);
			term1=term();
			state._fsp--;

			pushFollow(FOLLOW_term_in_exclusionQuery257);
			term2=term();
			state._fsp--;

			match(input, Token.UP, null); 

			 query = new ExclusionQuery(term1, term2); 
			}

		}
		catch (RecognitionException e) {
			throw e;
		}

		finally {
			// do for sure before leaving
		}
		return query;
	}
	// $ANTLR end "exclusionQuery"



	// $ANTLR start "adjacencyQuery"
	// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:71:1: adjacencyQuery returns [Query query] : ^( ND_ADJACENCY term1= term term2= term ) ;
	public final Query adjacencyQuery() throws RecognitionException {
		Query query = null;


		Query term1 =null;
		Query term2 =null;

		try {
			// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:72:2: ( ^( ND_ADJACENCY term1= term term2= term ) )
			// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:72:4: ^( ND_ADJACENCY term1= term term2= term )
			{
			match(input,ND_ADJACENCY,FOLLOW_ND_ADJACENCY_in_adjacencyQuery284); 
			match(input, Token.DOWN, null); 
			pushFollow(FOLLOW_term_in_adjacencyQuery288);
			term1=term();
			state._fsp--;

			pushFollow(FOLLOW_term_in_adjacencyQuery292);
			term2=term();
			state._fsp--;

			match(input, Token.UP, null); 

			 query = new AdjacencyQuery(term1, term2); 
			}

		}
		catch (RecognitionException e) {
			throw e;
		}

		finally {
			// do for sure before leaving
		}
		return query;
	}
	// $ANTLR end "adjacencyQuery"



	// $ANTLR start "term"
	// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:77:1: term returns [Query query] : ( phrase | selector |subQuery= query );
	public final Query term() throws RecognitionException {
		Query query = null;


		Query subQuery =null;
		Phrase phrase9 =null;
		Query selector10 =null;

		try {
			// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:78:2: ( phrase | selector |subQuery= query )
			int alt5=3;
			switch ( input.LA(1) ) {
			case ND_PHRASE:
				{
				alt5=1;
				}
				break;
			case ND_FREQ:
			case ND_PROPERTY:
			case ND_REG:
			case ND_SIMIL:
			case ND_TAG:
			case ND_TAGPROPERTY:
			case ND_WILD:
				{
				alt5=2;
				}
				break;
			case ND_QUERY:
				{
				alt5=3;
				}
				break;
			default:
				NoViableAltException nvae =
					new NoViableAltException("", 5, 0, input);
				throw nvae;
			}
			switch (alt5) {
				case 1 :
					// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:78:4: phrase
					{
					pushFollow(FOLLOW_phrase_in_term318);
					phrase9=phrase();
					state._fsp--;

					 query = phrase9; 
					}
					break;
				case 2 :
					// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:79:5: selector
					{
					pushFollow(FOLLOW_selector_in_term326);
					selector10=selector();
					state._fsp--;

					 query = selector10; 
					}
					break;
				case 3 :
					// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:80:5: subQuery= query
					{
					pushFollow(FOLLOW_query_in_term336);
					subQuery=query();
					state._fsp--;

					 query = new SubQuery(subQuery); 
					}
					break;

			}
		}
		catch (RecognitionException e) {
			throw e;
		}

		finally {
			// do for sure before leaving
		}
		return query;
	}
	// $ANTLR end "term"



	// $ANTLR start "phrase"
	// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:87:1: phrase returns [Phrase query] : ^( ND_PHRASE TXT ) ;
	public final Phrase phrase() throws RecognitionException {
		Phrase query = null;


		CommonTree TXT11=null;

		try {
			// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:88:2: ( ^( ND_PHRASE TXT ) )
			// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:88:4: ^( ND_PHRASE TXT )
			{
			match(input,ND_PHRASE,FOLLOW_ND_PHRASE_in_phrase364); 
			match(input, Token.DOWN, null); 
			TXT11=(CommonTree)match(input,TXT,FOLLOW_TXT_in_phrase366); 
			match(input, Token.UP, null); 

			 query = new Phrase((TXT11!=null?TXT11.getText():null)); 
			}

		}
		catch (RecognitionException e) {
			throw e;
		}

		finally {
			// do for sure before leaving
		}
		return query;
	}
	// $ANTLR end "phrase"



	// $ANTLR start "selector"
	// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:98:1: selector returns [Query query] : ( tagQuery | propertyQuery | regQuery | freqQuery | similQuery | wildQuery );
	public final Query selector() throws RecognitionException {
		Query query = null;


		Query tagQuery12 =null;
		Query propertyQuery13 =null;
		Query regQuery14 =null;
		Query freqQuery15 =null;
		Query similQuery16 =null;
		Query wildQuery17 =null;

		try {
			// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:99:2: ( tagQuery | propertyQuery | regQuery | freqQuery | similQuery | wildQuery )
			int alt6=6;
			switch ( input.LA(1) ) {
			case ND_TAG:
				{
				alt6=1;
				}
				break;
			case ND_PROPERTY:
			case ND_TAGPROPERTY:
				{
				alt6=2;
				}
				break;
			case ND_REG:
				{
				alt6=3;
				}
				break;
			case ND_FREQ:
				{
				alt6=4;
				}
				break;
			case ND_SIMIL:
				{
				alt6=5;
				}
				break;
			case ND_WILD:
				{
				alt6=6;
				}
				break;
			default:
				NoViableAltException nvae =
					new NoViableAltException("", 6, 0, input);
				throw nvae;
			}
			switch (alt6) {
				case 1 :
					// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:99:4: tagQuery
					{
					pushFollow(FOLLOW_tagQuery_in_selector397);
					tagQuery12=tagQuery();
					state._fsp--;

					 query = tagQuery12; 
					}
					break;
				case 2 :
					// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:100:4: propertyQuery
					{
					pushFollow(FOLLOW_propertyQuery_in_selector404);
					propertyQuery13=propertyQuery();
					state._fsp--;

					 query = propertyQuery13; 
					}
					break;
				case 3 :
					// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:101:4: regQuery
					{
					pushFollow(FOLLOW_regQuery_in_selector411);
					regQuery14=regQuery();
					state._fsp--;

					 query = regQuery14; 
					}
					break;
				case 4 :
					// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:102:4: freqQuery
					{
					pushFollow(FOLLOW_freqQuery_in_selector418);
					freqQuery15=freqQuery();
					state._fsp--;

					 query = freqQuery15; 
					}
					break;
				case 5 :
					// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:103:4: similQuery
					{
					pushFollow(FOLLOW_similQuery_in_selector425);
					similQuery16=similQuery();
					state._fsp--;

					 query = similQuery16; 
					}
					break;
				case 6 :
					// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:104:4: wildQuery
					{
					pushFollow(FOLLOW_wildQuery_in_selector432);
					wildQuery17=wildQuery();
					state._fsp--;

					 query = wildQuery17; 
					}
					break;

			}
		}
		catch (RecognitionException e) {
			throw e;
		}

		finally {
			// do for sure before leaving
		}
		return query;
	}
	// $ANTLR end "selector"



	// $ANTLR start "tagQuery"
	// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:109:1: tagQuery returns [Query query] : ^( ND_TAG phrase (tagMatchMode= TAG_MATCH_MODE )? ) ;
	public final Query tagQuery() throws RecognitionException {
		Query query = null;


		CommonTree tagMatchMode=null;
		Phrase phrase18 =null;

		try {
			// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:110:2: ( ^( ND_TAG phrase (tagMatchMode= TAG_MATCH_MODE )? ) )
			// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:110:4: ^( ND_TAG phrase (tagMatchMode= TAG_MATCH_MODE )? )
			{
			match(input,ND_TAG,FOLLOW_ND_TAG_in_tagQuery459); 
			match(input, Token.DOWN, null); 
			pushFollow(FOLLOW_phrase_in_tagQuery461);
			phrase18=phrase();
			state._fsp--;

			// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:110:32: (tagMatchMode= TAG_MATCH_MODE )?
			int alt7=2;
			int LA7_0 = input.LA(1);
			if ( (LA7_0==TAG_MATCH_MODE) ) {
				alt7=1;
			}
			switch (alt7) {
				case 1 :
					// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:110:32: tagMatchMode= TAG_MATCH_MODE
					{
					tagMatchMode=(CommonTree)match(input,TAG_MATCH_MODE,FOLLOW_TAG_MATCH_MODE_in_tagQuery465); 
					}
					break;

			}

			match(input, Token.UP, null); 

			 query = new TagQuery(phrase18, (tagMatchMode!=null?tagMatchMode.getText():null)); 
			}

		}
		catch (RecognitionException e) {
			throw e;
		}

		finally {
			// do for sure before leaving
		}
		return query;
	}
	// $ANTLR end "tagQuery"



	// $ANTLR start "propertyQuery"
	// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:114:1: propertyQuery returns [Query query] : ( ^( ND_PROPERTY property= phrase (value= phrase )? (tagMatchMode= TAG_MATCH_MODE )? ) | ^( ND_TAGPROPERTY tag= phrase property= phrase (value= phrase )? (tagMatchMode= TAG_MATCH_MODE )? ) );
	public final Query propertyQuery() throws RecognitionException {
		Query query = null;


		CommonTree tagMatchMode=null;
		Phrase property =null;
		Phrase value =null;
		Phrase tag =null;

		try {
			// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:115:2: ( ^( ND_PROPERTY property= phrase (value= phrase )? (tagMatchMode= TAG_MATCH_MODE )? ) | ^( ND_TAGPROPERTY tag= phrase property= phrase (value= phrase )? (tagMatchMode= TAG_MATCH_MODE )? ) )
			int alt12=2;
			int LA12_0 = input.LA(1);
			if ( (LA12_0==ND_PROPERTY) ) {
				alt12=1;
			}
			else if ( (LA12_0==ND_TAGPROPERTY) ) {
				alt12=2;
			}

			else {
				NoViableAltException nvae =
					new NoViableAltException("", 12, 0, input);
				throw nvae;
			}

			switch (alt12) {
				case 1 :
					// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:115:4: ^( ND_PROPERTY property= phrase (value= phrase )? (tagMatchMode= TAG_MATCH_MODE )? )
					{
					match(input,ND_PROPERTY,FOLLOW_ND_PROPERTY_in_propertyQuery491); 
					match(input, Token.DOWN, null); 
					pushFollow(FOLLOW_phrase_in_propertyQuery495);
					property=phrase();
					state._fsp--;

					// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:115:34: (value= phrase )?
					int alt8=2;
					int LA8_0 = input.LA(1);
					if ( (LA8_0==ND_PHRASE) ) {
						alt8=1;
					}
					switch (alt8) {
						case 1 :
							// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:115:35: value= phrase
							{
							pushFollow(FOLLOW_phrase_in_propertyQuery500);
							value=phrase();
							state._fsp--;

							}
							break;

					}

					// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:115:62: (tagMatchMode= TAG_MATCH_MODE )?
					int alt9=2;
					int LA9_0 = input.LA(1);
					if ( (LA9_0==TAG_MATCH_MODE) ) {
						alt9=1;
					}
					switch (alt9) {
						case 1 :
							// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:115:62: tagMatchMode= TAG_MATCH_MODE
							{
							tagMatchMode=(CommonTree)match(input,TAG_MATCH_MODE,FOLLOW_TAG_MATCH_MODE_in_propertyQuery506); 
							}
							break;

					}

					match(input, Token.UP, null); 

					 query = new PropertyQuery(null, property, value, (tagMatchMode!=null?tagMatchMode.getText():null)); 
					}
					break;
				case 2 :
					// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:116:4: ^( ND_TAGPROPERTY tag= phrase property= phrase (value= phrase )? (tagMatchMode= TAG_MATCH_MODE )? )
					{
					match(input,ND_TAGPROPERTY,FOLLOW_ND_TAGPROPERTY_in_propertyQuery516); 
					match(input, Token.DOWN, null); 
					pushFollow(FOLLOW_phrase_in_propertyQuery520);
					tag=phrase();
					state._fsp--;

					pushFollow(FOLLOW_phrase_in_propertyQuery524);
					property=phrase();
					state._fsp--;

					// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:116:48: (value= phrase )?
					int alt10=2;
					int LA10_0 = input.LA(1);
					if ( (LA10_0==ND_PHRASE) ) {
						alt10=1;
					}
					switch (alt10) {
						case 1 :
							// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:116:49: value= phrase
							{
							pushFollow(FOLLOW_phrase_in_propertyQuery529);
							value=phrase();
							state._fsp--;

							}
							break;

					}

					// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:116:76: (tagMatchMode= TAG_MATCH_MODE )?
					int alt11=2;
					int LA11_0 = input.LA(1);
					if ( (LA11_0==TAG_MATCH_MODE) ) {
						alt11=1;
					}
					switch (alt11) {
						case 1 :
							// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:116:76: tagMatchMode= TAG_MATCH_MODE
							{
							tagMatchMode=(CommonTree)match(input,TAG_MATCH_MODE,FOLLOW_TAG_MATCH_MODE_in_propertyQuery535); 
							}
							break;

					}

					match(input, Token.UP, null); 

					 query = new PropertyQuery(tag, property, value, (tagMatchMode!=null?tagMatchMode.getText():null)); 
					}
					break;

			}
		}
		catch (RecognitionException e) {
			throw e;
		}

		finally {
			// do for sure before leaving
		}
		return query;
	}
	// $ANTLR end "propertyQuery"



	// $ANTLR start "regQuery"
	// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:120:1: regQuery returns [Query query] : ^( ND_REG phrase (caseInsensitiveMarker= 'CI' )? ) ;
	public final Query regQuery() throws RecognitionException {
		Query query = null;


		CommonTree caseInsensitiveMarker=null;
		Phrase phrase19 =null;

		try {
			// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:121:2: ( ^( ND_REG phrase (caseInsensitiveMarker= 'CI' )? ) )
			// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:121:4: ^( ND_REG phrase (caseInsensitiveMarker= 'CI' )? )
			{
			match(input,ND_REG,FOLLOW_ND_REG_in_regQuery561); 
			match(input, Token.DOWN, null); 
			pushFollow(FOLLOW_phrase_in_regQuery563);
			phrase19=phrase();
			state._fsp--;

			// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:121:41: (caseInsensitiveMarker= 'CI' )?
			int alt13=2;
			int LA13_0 = input.LA(1);
			if ( (LA13_0==43) ) {
				alt13=1;
			}
			switch (alt13) {
				case 1 :
					// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:121:41: caseInsensitiveMarker= 'CI'
					{
					caseInsensitiveMarker=(CommonTree)match(input,43,FOLLOW_43_in_regQuery567); 
					}
					break;

			}

			match(input, Token.UP, null); 

			 query = new RegQuery(phrase19, (caseInsensitiveMarker!=null?caseInsensitiveMarker.getText():null)); 
			}

		}
		catch (RecognitionException e) {
			throw e;
		}

		finally {
			// do for sure before leaving
		}
		return query;
	}
	// $ANTLR end "regQuery"



	// $ANTLR start "freqQuery"
	// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:125:1: freqQuery returns [Query query] : ( ^( ND_FREQ EQUAL rInt1= INT (rInt2= INT )? ) | ^( ND_FREQ UNEQUAL INT ) );
	public final Query freqQuery() throws RecognitionException {
		Query query = null;


		CommonTree rInt1=null;
		CommonTree rInt2=null;
		CommonTree EQUAL20=null;
		CommonTree UNEQUAL21=null;
		CommonTree INT22=null;

		try {
			// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:126:2: ( ^( ND_FREQ EQUAL rInt1= INT (rInt2= INT )? ) | ^( ND_FREQ UNEQUAL INT ) )
			int alt15=2;
			int LA15_0 = input.LA(1);
			if ( (LA15_0==ND_FREQ) ) {
				int LA15_1 = input.LA(2);
				if ( (LA15_1==DOWN) ) {
					int LA15_2 = input.LA(3);
					if ( (LA15_2==EQUAL) ) {
						alt15=1;
					}
					else if ( (LA15_2==UNEQUAL) ) {
						alt15=2;
					}

					else {
						int nvaeMark = input.mark();
						try {
							for (int nvaeConsume = 0; nvaeConsume < 3 - 1; nvaeConsume++) {
								input.consume();
							}
							NoViableAltException nvae =
								new NoViableAltException("", 15, 2, input);
							throw nvae;
						} finally {
							input.rewind(nvaeMark);
						}
					}

				}

				else {
					int nvaeMark = input.mark();
					try {
						input.consume();
						NoViableAltException nvae =
							new NoViableAltException("", 15, 1, input);
						throw nvae;
					} finally {
						input.rewind(nvaeMark);
					}
				}

			}

			else {
				NoViableAltException nvae =
					new NoViableAltException("", 15, 0, input);
				throw nvae;
			}

			switch (alt15) {
				case 1 :
					// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:126:4: ^( ND_FREQ EQUAL rInt1= INT (rInt2= INT )? )
					{
					match(input,ND_FREQ,FOLLOW_ND_FREQ_in_freqQuery595); 
					match(input, Token.DOWN, null); 
					EQUAL20=(CommonTree)match(input,EQUAL,FOLLOW_EQUAL_in_freqQuery597); 
					rInt1=(CommonTree)match(input,INT,FOLLOW_INT_in_freqQuery601); 
					// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:126:35: (rInt2= INT )?
					int alt14=2;
					int LA14_0 = input.LA(1);
					if ( (LA14_0==INT) ) {
						alt14=1;
					}
					switch (alt14) {
						case 1 :
							// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:126:35: rInt2= INT
							{
							rInt2=(CommonTree)match(input,INT,FOLLOW_INT_in_freqQuery605); 
							}
							break;

					}

					match(input, Token.UP, null); 

					 query = new FreqQuery((EQUAL20!=null?EQUAL20.getText():null), (rInt1!=null?rInt1.getText():null), (rInt2!=null?rInt2.getText():null)); 
					}
					break;
				case 2 :
					// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:127:5: ^( ND_FREQ UNEQUAL INT )
					{
					match(input,ND_FREQ,FOLLOW_ND_FREQ_in_freqQuery616); 
					match(input, Token.DOWN, null); 
					UNEQUAL21=(CommonTree)match(input,UNEQUAL,FOLLOW_UNEQUAL_in_freqQuery618); 
					INT22=(CommonTree)match(input,INT,FOLLOW_INT_in_freqQuery620); 
					match(input, Token.UP, null); 

					 query = new FreqQuery((UNEQUAL21!=null?UNEQUAL21.getText():null), (INT22!=null?INT22.getText():null)); 
					}
					break;

			}
		}
		catch (RecognitionException e) {
			throw e;
		}

		finally {
			// do for sure before leaving
		}
		return query;
	}
	// $ANTLR end "freqQuery"



	// $ANTLR start "similQuery"
	// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:131:1: similQuery returns [Query query] : ^( ND_SIMIL phrase INT ) ;
	public final Query similQuery() throws RecognitionException {
		Query query = null;


		CommonTree INT24=null;
		Phrase phrase23 =null;

		try {
			// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:132:2: ( ^( ND_SIMIL phrase INT ) )
			// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:132:4: ^( ND_SIMIL phrase INT )
			{
			match(input,ND_SIMIL,FOLLOW_ND_SIMIL_in_similQuery648); 
			match(input, Token.DOWN, null); 
			pushFollow(FOLLOW_phrase_in_similQuery650);
			phrase23=phrase();
			state._fsp--;

			INT24=(CommonTree)match(input,INT,FOLLOW_INT_in_similQuery652); 
			match(input, Token.UP, null); 

			 query = new SimilQuery(phrase23, (INT24!=null?INT24.getText():null)); 
			}

		}
		catch (RecognitionException e) {
			throw e;
		}

		finally {
			// do for sure before leaving
		}
		return query;
	}
	// $ANTLR end "similQuery"



	// $ANTLR start "wildQuery"
	// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:136:1: wildQuery returns [Query query] : ^( ND_WILD phrase ) ;
	public final Query wildQuery() throws RecognitionException {
		Query query = null;


		Phrase phrase25 =null;

		try {
			// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:137:2: ( ^( ND_WILD phrase ) )
			// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:137:4: ^( ND_WILD phrase )
			{
			match(input,ND_WILD,FOLLOW_ND_WILD_in_wildQuery679); 
			match(input, Token.DOWN, null); 
			pushFollow(FOLLOW_phrase_in_wildQuery681);
			phrase25=phrase();
			state._fsp--;

			match(input, Token.UP, null); 

			 query = new WildcardQuery(phrase25); 
			}

		}
		catch (RecognitionException e) {
			throw e;
		}

		finally {
			// do for sure before leaving
		}
		return query;
	}
	// $ANTLR end "wildQuery"



	// $ANTLR start "refinement"
	// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:145:1: refinement[Query query] : refinementExpression ;
	public final void refinement(Query query) throws RecognitionException {
		Refinement refinementExpression26 =null;

		try {
			// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:146:2: ( refinementExpression )
			// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:146:4: refinementExpression
			{
			pushFollow(FOLLOW_refinementExpression_in_refinement707);
			refinementExpression26=refinementExpression();
			state._fsp--;

			 query.setRefinement(refinementExpression26); 
			}

		}
		catch (RecognitionException e) {
			throw e;
		}

		finally {
			// do for sure before leaving
		}
	}
	// $ANTLR end "refinement"



	// $ANTLR start "refinementExpression"
	// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:151:1: refinementExpression returns [Refinement refinement] : ( orRefinement | andRefinement | ^( ND_REFINE refinementTerm ) );
	public final Refinement refinementExpression() throws RecognitionException {
		Refinement refinement = null;


		Refinement orRefinement27 =null;
		Refinement andRefinement28 =null;
		Refinement refinementTerm29 =null;

		try {
			// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:152:2: ( orRefinement | andRefinement | ^( ND_REFINE refinementTerm ) )
			int alt16=3;
			switch ( input.LA(1) ) {
			case ND_ORREFINE:
				{
				alt16=1;
				}
				break;
			case ND_ANDREFINE:
				{
				alt16=2;
				}
				break;
			case ND_REFINE:
				{
				alt16=3;
				}
				break;
			default:
				NoViableAltException nvae =
					new NoViableAltException("", 16, 0, input);
				throw nvae;
			}
			switch (alt16) {
				case 1 :
					// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:152:4: orRefinement
					{
					pushFollow(FOLLOW_orRefinement_in_refinementExpression733);
					orRefinement27=orRefinement();
					state._fsp--;

					 refinement = orRefinement27; 
					}
					break;
				case 2 :
					// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:153:5: andRefinement
					{
					pushFollow(FOLLOW_andRefinement_in_refinementExpression741);
					andRefinement28=andRefinement();
					state._fsp--;

					 refinement = andRefinement28; 
					}
					break;
				case 3 :
					// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:154:5: ^( ND_REFINE refinementTerm )
					{
					match(input,ND_REFINE,FOLLOW_ND_REFINE_in_refinementExpression750); 
					match(input, Token.DOWN, null); 
					pushFollow(FOLLOW_refinementTerm_in_refinementExpression752);
					refinementTerm29=refinementTerm();
					state._fsp--;

					match(input, Token.UP, null); 

					 refinement = refinementTerm29; 
					}
					break;

			}
		}
		catch (RecognitionException e) {
			throw e;
		}

		finally {
			// do for sure before leaving
		}
		return refinement;
	}
	// $ANTLR end "refinementExpression"



	// $ANTLR start "orRefinement"
	// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:158:1: orRefinement returns [Refinement refinement] : ^( ND_ORREFINE rTerm1= refinementTerm rTerm2= refinementTerm ) ;
	public final Refinement orRefinement() throws RecognitionException {
		Refinement refinement = null;


		Refinement rTerm1 =null;
		Refinement rTerm2 =null;

		try {
			// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:159:2: ( ^( ND_ORREFINE rTerm1= refinementTerm rTerm2= refinementTerm ) )
			// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:159:4: ^( ND_ORREFINE rTerm1= refinementTerm rTerm2= refinementTerm )
			{
			match(input,ND_ORREFINE,FOLLOW_ND_ORREFINE_in_orRefinement777); 
			match(input, Token.DOWN, null); 
			pushFollow(FOLLOW_refinementTerm_in_orRefinement781);
			rTerm1=refinementTerm();
			state._fsp--;

			pushFollow(FOLLOW_refinementTerm_in_orRefinement785);
			rTerm2=refinementTerm();
			state._fsp--;

			match(input, Token.UP, null); 

			 refinement = new OrRefinement(rTerm1,rTerm2); 
			}

		}
		catch (RecognitionException e) {
			throw e;
		}

		finally {
			// do for sure before leaving
		}
		return refinement;
	}
	// $ANTLR end "orRefinement"



	// $ANTLR start "andRefinement"
	// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:164:1: andRefinement returns [Refinement refinement] : ^( ND_ANDREFINE rTerm1= refinementTerm rTerm2= refinementTerm ) ;
	public final Refinement andRefinement() throws RecognitionException {
		Refinement refinement = null;


		Refinement rTerm1 =null;
		Refinement rTerm2 =null;

		try {
			// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:165:2: ( ^( ND_ANDREFINE rTerm1= refinementTerm rTerm2= refinementTerm ) )
			// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:165:4: ^( ND_ANDREFINE rTerm1= refinementTerm rTerm2= refinementTerm )
			{
			match(input,ND_ANDREFINE,FOLLOW_ND_ANDREFINE_in_andRefinement815); 
			match(input, Token.DOWN, null); 
			pushFollow(FOLLOW_refinementTerm_in_andRefinement819);
			rTerm1=refinementTerm();
			state._fsp--;

			pushFollow(FOLLOW_refinementTerm_in_andRefinement823);
			rTerm2=refinementTerm();
			state._fsp--;

			match(input, Token.UP, null); 

			 refinement = new AndRefinement(rTerm1,rTerm2); 
			}

		}
		catch (RecognitionException e) {
			throw e;
		}

		finally {
			// do for sure before leaving
		}
		return refinement;
	}
	// $ANTLR end "andRefinement"



	// $ANTLR start "refinementTerm"
	// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:170:1: refinementTerm returns [Refinement refinement] : ( selector | refinementExpression );
	public final Refinement refinementTerm() throws RecognitionException {
		Refinement refinement = null;


		Query selector30 =null;
		Refinement refinementExpression31 =null;

		try {
			// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:171:2: ( selector | refinementExpression )
			int alt17=2;
			int LA17_0 = input.LA(1);
			if ( (LA17_0==ND_FREQ||LA17_0==ND_PROPERTY||(LA17_0 >= ND_REG && LA17_0 <= ND_TAGPROPERTY)||LA17_0==ND_WILD) ) {
				alt17=1;
			}
			else if ( (LA17_0==ND_ANDREFINE||LA17_0==ND_ORREFINE||LA17_0==ND_REFINE) ) {
				alt17=2;
			}

			else {
				NoViableAltException nvae =
					new NoViableAltException("", 17, 0, input);
				throw nvae;
			}

			switch (alt17) {
				case 1 :
					// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:171:5: selector
					{
					pushFollow(FOLLOW_selector_in_refinementTerm853);
					selector30=selector();
					state._fsp--;

					 refinement = new QueryRefinement(selector30); 
					}
					break;
				case 2 :
					// /Users/evelyn/Documents/workspace/catma/grammars/tree/CatmaQueryWalker.g:172:5: refinementExpression
					{
					pushFollow(FOLLOW_refinementExpression_in_refinementTerm861);
					refinementExpression31=refinementExpression();
					state._fsp--;

					 refinement = refinementExpression31; 
					}
					break;

			}
		}
		catch (RecognitionException e) {
			throw e;
		}

		finally {
			// do for sure before leaving
		}
		return refinement;
	}
	// $ANTLR end "refinementTerm"

	// Delegated rules



	public static final BitSet FOLLOW_query_in_start50 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_ND_QUERY_in_query75 = new BitSet(new long[]{0x0000000000000004L});
	public static final BitSet FOLLOW_queryExpression_in_query77 = new BitSet(new long[]{0x0000000000088808L});
	public static final BitSet FOLLOW_refinement_in_query79 = new BitSet(new long[]{0x0000000000000008L});
	public static final BitSet FOLLOW_unionQuery_in_queryExpression111 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_collocQuery_in_queryExpression119 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_exclusionQuery_in_queryExpression127 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_adjacencyQuery_in_queryExpression135 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_term_in_queryExpression143 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_ND_UNION_in_unionQuery173 = new BitSet(new long[]{0x0000000000000004L});
	public static final BitSet FOLLOW_term_in_unionQuery177 = new BitSet(new long[]{0x0000000002F74000L});
	public static final BitSet FOLLOW_term_in_unionQuery181 = new BitSet(new long[]{0x0000100000000008L});
	public static final BitSet FOLLOW_44_in_unionQuery185 = new BitSet(new long[]{0x0000000000000008L});
	public static final BitSet FOLLOW_ND_COLLOC_in_collocQuery212 = new BitSet(new long[]{0x0000000000000004L});
	public static final BitSet FOLLOW_term_in_collocQuery216 = new BitSet(new long[]{0x0000000002F74000L});
	public static final BitSet FOLLOW_term_in_collocQuery220 = new BitSet(new long[]{0x0000000000000088L});
	public static final BitSet FOLLOW_INT_in_collocQuery222 = new BitSet(new long[]{0x0000000000000008L});
	public static final BitSet FOLLOW_ND_EXCLUSION_in_exclusionQuery249 = new BitSet(new long[]{0x0000000000000004L});
	public static final BitSet FOLLOW_term_in_exclusionQuery253 = new BitSet(new long[]{0x0000000002F74000L});
	public static final BitSet FOLLOW_term_in_exclusionQuery257 = new BitSet(new long[]{0x0000000000000008L});
	public static final BitSet FOLLOW_ND_ADJACENCY_in_adjacencyQuery284 = new BitSet(new long[]{0x0000000000000004L});
	public static final BitSet FOLLOW_term_in_adjacencyQuery288 = new BitSet(new long[]{0x0000000002F74000L});
	public static final BitSet FOLLOW_term_in_adjacencyQuery292 = new BitSet(new long[]{0x0000000000000008L});
	public static final BitSet FOLLOW_phrase_in_term318 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_selector_in_term326 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_query_in_term336 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_ND_PHRASE_in_phrase364 = new BitSet(new long[]{0x0000000000000004L});
	public static final BitSet FOLLOW_TXT_in_phrase366 = new BitSet(new long[]{0x0000000000000008L});
	public static final BitSet FOLLOW_tagQuery_in_selector397 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_propertyQuery_in_selector404 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_regQuery_in_selector411 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_freqQuery_in_selector418 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_similQuery_in_selector425 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_wildQuery_in_selector432 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_ND_TAG_in_tagQuery459 = new BitSet(new long[]{0x0000000000000004L});
	public static final BitSet FOLLOW_phrase_in_tagQuery461 = new BitSet(new long[]{0x0000000040000008L});
	public static final BitSet FOLLOW_TAG_MATCH_MODE_in_tagQuery465 = new BitSet(new long[]{0x0000000000000008L});
	public static final BitSet FOLLOW_ND_PROPERTY_in_propertyQuery491 = new BitSet(new long[]{0x0000000000000004L});
	public static final BitSet FOLLOW_phrase_in_propertyQuery495 = new BitSet(new long[]{0x0000000040010008L});
	public static final BitSet FOLLOW_phrase_in_propertyQuery500 = new BitSet(new long[]{0x0000000040000008L});
	public static final BitSet FOLLOW_TAG_MATCH_MODE_in_propertyQuery506 = new BitSet(new long[]{0x0000000000000008L});
	public static final BitSet FOLLOW_ND_TAGPROPERTY_in_propertyQuery516 = new BitSet(new long[]{0x0000000000000004L});
	public static final BitSet FOLLOW_phrase_in_propertyQuery520 = new BitSet(new long[]{0x0000000000010000L});
	public static final BitSet FOLLOW_phrase_in_propertyQuery524 = new BitSet(new long[]{0x0000000040010008L});
	public static final BitSet FOLLOW_phrase_in_propertyQuery529 = new BitSet(new long[]{0x0000000040000008L});
	public static final BitSet FOLLOW_TAG_MATCH_MODE_in_propertyQuery535 = new BitSet(new long[]{0x0000000000000008L});
	public static final BitSet FOLLOW_ND_REG_in_regQuery561 = new BitSet(new long[]{0x0000000000000004L});
	public static final BitSet FOLLOW_phrase_in_regQuery563 = new BitSet(new long[]{0x0000080000000008L});
	public static final BitSet FOLLOW_43_in_regQuery567 = new BitSet(new long[]{0x0000000000000008L});
	public static final BitSet FOLLOW_ND_FREQ_in_freqQuery595 = new BitSet(new long[]{0x0000000000000004L});
	public static final BitSet FOLLOW_EQUAL_in_freqQuery597 = new BitSet(new long[]{0x0000000000000080L});
	public static final BitSet FOLLOW_INT_in_freqQuery601 = new BitSet(new long[]{0x0000000000000088L});
	public static final BitSet FOLLOW_INT_in_freqQuery605 = new BitSet(new long[]{0x0000000000000008L});
	public static final BitSet FOLLOW_ND_FREQ_in_freqQuery616 = new BitSet(new long[]{0x0000000000000004L});
	public static final BitSet FOLLOW_UNEQUAL_in_freqQuery618 = new BitSet(new long[]{0x0000000000000080L});
	public static final BitSet FOLLOW_INT_in_freqQuery620 = new BitSet(new long[]{0x0000000000000008L});
	public static final BitSet FOLLOW_ND_SIMIL_in_similQuery648 = new BitSet(new long[]{0x0000000000000004L});
	public static final BitSet FOLLOW_phrase_in_similQuery650 = new BitSet(new long[]{0x0000000000000080L});
	public static final BitSet FOLLOW_INT_in_similQuery652 = new BitSet(new long[]{0x0000000000000008L});
	public static final BitSet FOLLOW_ND_WILD_in_wildQuery679 = new BitSet(new long[]{0x0000000000000004L});
	public static final BitSet FOLLOW_phrase_in_wildQuery681 = new BitSet(new long[]{0x0000000000000008L});
	public static final BitSet FOLLOW_refinementExpression_in_refinement707 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_orRefinement_in_refinementExpression733 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_andRefinement_in_refinementExpression741 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_ND_REFINE_in_refinementExpression750 = new BitSet(new long[]{0x0000000000000004L});
	public static final BitSet FOLLOW_refinementTerm_in_refinementExpression752 = new BitSet(new long[]{0x0000000000000008L});
	public static final BitSet FOLLOW_ND_ORREFINE_in_orRefinement777 = new BitSet(new long[]{0x0000000000000004L});
	public static final BitSet FOLLOW_refinementTerm_in_orRefinement781 = new BitSet(new long[]{0x0000000002FAC800L});
	public static final BitSet FOLLOW_refinementTerm_in_orRefinement785 = new BitSet(new long[]{0x0000000000000008L});
	public static final BitSet FOLLOW_ND_ANDREFINE_in_andRefinement815 = new BitSet(new long[]{0x0000000000000004L});
	public static final BitSet FOLLOW_refinementTerm_in_andRefinement819 = new BitSet(new long[]{0x0000000002FAC800L});
	public static final BitSet FOLLOW_refinementTerm_in_andRefinement823 = new BitSet(new long[]{0x0000000000000008L});
	public static final BitSet FOLLOW_selector_in_refinementTerm853 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_refinementExpression_in_refinementTerm861 = new BitSet(new long[]{0x0000000000000002L});
}
