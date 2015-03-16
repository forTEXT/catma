// $ANTLR 3.4 C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g 2015-02-05 15:01:14

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
* About:	Tree Grammar for the CATMA Query Walker
*/
@SuppressWarnings({"all", "warnings", "unchecked"})
public class CatmaQueryWalker extends TreeParser {
    public static final String[] tokenNames = new String[] {
        "<invalid>", "<EOR>", "<DOWN>", "<UP>", "EQUAL", "FREQ", "GROUPIDENT", "INT", "LETTER", "LETTEREXTENDED", "MATCH_MODE", "ND_ADJACENCY", "ND_ANDREFINE", "ND_COLLOC", "ND_EXCLUSION", "ND_FREQ", "ND_ORREFINE", "ND_PHRASE", "ND_PROPERTY", "ND_QUERY", "ND_REFINE", "ND_REG", "ND_SIMIL", "ND_TAG", "ND_TAGPROPERTY", "ND_UNION", "ND_WILD", "PROPERTY", "REG", "SIMIL", "TAG", "TXT", "UNEQUAL", "VALUE", "WHITESPACE", "WILD", "'%'", "'&'", "'('", "')'", "','", "'-'", "';'", "'CI'", "'EXCL'", "'where'", "'|'"
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
    public static final int MATCH_MODE=10;
    public static final int ND_ADJACENCY=11;
    public static final int ND_ANDREFINE=12;
    public static final int ND_COLLOC=13;
    public static final int ND_EXCLUSION=14;
    public static final int ND_FREQ=15;
    public static final int ND_ORREFINE=16;
    public static final int ND_PHRASE=17;
    public static final int ND_PROPERTY=18;
    public static final int ND_QUERY=19;
    public static final int ND_REFINE=20;
    public static final int ND_REG=21;
    public static final int ND_SIMIL=22;
    public static final int ND_TAG=23;
    public static final int ND_TAGPROPERTY=24;
    public static final int ND_UNION=25;
    public static final int ND_WILD=26;
    public static final int PROPERTY=27;
    public static final int REG=28;
    public static final int SIMIL=29;
    public static final int TAG=30;
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

    public String[] getTokenNames() { return CatmaQueryWalker.tokenNames; }
    public String getGrammarFileName() { return "C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g"; }



    // $ANTLR start "start"
    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:26:1: start returns [Query finalQuery] : query ;
    public final Query start() throws RecognitionException {
        Query finalQuery = null;


        Query query1 =null;


        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:27:2: ( query )
            // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:27:4: query
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
    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:31:1: query returns [Query query] : ^( ND_QUERY queryExpression ( refinement[$queryExpression.query] )? ) ;
    public final Query query() throws RecognitionException {
        Query query = null;


        Query queryExpression2 =null;


        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:32:2: ( ^( ND_QUERY queryExpression ( refinement[$queryExpression.query] )? ) )
            // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:32:4: ^( ND_QUERY queryExpression ( refinement[$queryExpression.query] )? )
            {
            match(input,ND_QUERY,FOLLOW_ND_QUERY_in_query75); 

            match(input, Token.DOWN, null); 
            pushFollow(FOLLOW_queryExpression_in_query77);
            queryExpression2=queryExpression();

            state._fsp--;


            // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:32:31: ( refinement[$queryExpression.query] )?
            int alt1=2;
            int LA1_0 = input.LA(1);

            if ( (LA1_0==ND_ANDREFINE||LA1_0==ND_ORREFINE||LA1_0==ND_REFINE) ) {
                alt1=1;
            }
            switch (alt1) {
                case 1 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:32:31: refinement[$queryExpression.query]
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
    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:41:1: queryExpression returns [Query query] : ( unionQuery | collocQuery | exclusionQuery | adjacencyQuery | term );
    public final Query queryExpression() throws RecognitionException {
        Query query = null;


        Query unionQuery3 =null;

        Query collocQuery4 =null;

        Query exclusionQuery5 =null;

        Query adjacencyQuery6 =null;

        Query term7 =null;


        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:42:2: ( unionQuery | collocQuery | exclusionQuery | adjacencyQuery | term )
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
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:42:4: unionQuery
                    {
                    pushFollow(FOLLOW_unionQuery_in_queryExpression111);
                    unionQuery3=unionQuery();

                    state._fsp--;


                     query = unionQuery3; 

                    }
                    break;
                case 2 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:43:5: collocQuery
                    {
                    pushFollow(FOLLOW_collocQuery_in_queryExpression119);
                    collocQuery4=collocQuery();

                    state._fsp--;


                     query = collocQuery4; 

                    }
                    break;
                case 3 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:44:5: exclusionQuery
                    {
                    pushFollow(FOLLOW_exclusionQuery_in_queryExpression127);
                    exclusionQuery5=exclusionQuery();

                    state._fsp--;


                     query = exclusionQuery5; 

                    }
                    break;
                case 4 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:45:5: adjacencyQuery
                    {
                    pushFollow(FOLLOW_adjacencyQuery_in_queryExpression135);
                    adjacencyQuery6=adjacencyQuery();

                    state._fsp--;


                     query = adjacencyQuery6; 

                    }
                    break;
                case 5 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:46:5: term
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
    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:55:1: unionQuery returns [Query query] : ^( ND_UNION term1= term term2= term (exclusiveMarker= 'EXCL' )? ) ;
    public final Query unionQuery() throws RecognitionException {
        Query query = null;


        CommonTree exclusiveMarker=null;
        Query term1 =null;

        Query term2 =null;


        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:56:2: ( ^( ND_UNION term1= term term2= term (exclusiveMarker= 'EXCL' )? ) )
            // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:56:4: ^( ND_UNION term1= term term2= term (exclusiveMarker= 'EXCL' )? )
            {
            match(input,ND_UNION,FOLLOW_ND_UNION_in_unionQuery173); 

            match(input, Token.DOWN, null); 
            pushFollow(FOLLOW_term_in_unionQuery177);
            term1=term();

            state._fsp--;


            pushFollow(FOLLOW_term_in_unionQuery181);
            term2=term();

            state._fsp--;


            // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:56:52: (exclusiveMarker= 'EXCL' )?
            int alt3=2;
            int LA3_0 = input.LA(1);

            if ( (LA3_0==44) ) {
                alt3=1;
            }
            switch (alt3) {
                case 1 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:56:52: exclusiveMarker= 'EXCL'
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
    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:60:1: collocQuery returns [Query query] : ^( ND_COLLOC term1= term term2= term ( INT )? ) ;
    public final Query collocQuery() throws RecognitionException {
        Query query = null;


        CommonTree INT8=null;
        Query term1 =null;

        Query term2 =null;


        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:61:2: ( ^( ND_COLLOC term1= term term2= term ( INT )? ) )
            // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:61:4: ^( ND_COLLOC term1= term term2= term ( INT )? )
            {
            match(input,ND_COLLOC,FOLLOW_ND_COLLOC_in_collocQuery212); 

            match(input, Token.DOWN, null); 
            pushFollow(FOLLOW_term_in_collocQuery216);
            term1=term();

            state._fsp--;


            pushFollow(FOLLOW_term_in_collocQuery220);
            term2=term();

            state._fsp--;


            // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:61:38: ( INT )?
            int alt4=2;
            int LA4_0 = input.LA(1);

            if ( (LA4_0==INT) ) {
                alt4=1;
            }
            switch (alt4) {
                case 1 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:61:38: INT
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
    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:65:1: exclusionQuery returns [Query query] : ^( ND_EXCLUSION term1= term term2= term (matchMode= MATCH_MODE )? ) ;
    public final Query exclusionQuery() throws RecognitionException {
        Query query = null;


        CommonTree matchMode=null;
        Query term1 =null;

        Query term2 =null;


        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:66:2: ( ^( ND_EXCLUSION term1= term term2= term (matchMode= MATCH_MODE )? ) )
            // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:66:4: ^( ND_EXCLUSION term1= term term2= term (matchMode= MATCH_MODE )? )
            {
            match(input,ND_EXCLUSION,FOLLOW_ND_EXCLUSION_in_exclusionQuery249); 

            match(input, Token.DOWN, null); 
            pushFollow(FOLLOW_term_in_exclusionQuery253);
            term1=term();

            state._fsp--;


            pushFollow(FOLLOW_term_in_exclusionQuery257);
            term2=term();

            state._fsp--;


            // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:66:50: (matchMode= MATCH_MODE )?
            int alt5=2;
            int LA5_0 = input.LA(1);

            if ( (LA5_0==MATCH_MODE) ) {
                alt5=1;
            }
            switch (alt5) {
                case 1 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:66:50: matchMode= MATCH_MODE
                    {
                    matchMode=(CommonTree)match(input,MATCH_MODE,FOLLOW_MATCH_MODE_in_exclusionQuery261); 

                    }
                    break;

            }


            match(input, Token.UP, null); 


             query = new ExclusionQuery(term1, term2, (matchMode!=null?matchMode.getText():null)); 

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
    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:70:1: adjacencyQuery returns [Query query] : ^( ND_ADJACENCY term1= term term2= term ) ;
    public final Query adjacencyQuery() throws RecognitionException {
        Query query = null;


        Query term1 =null;

        Query term2 =null;


        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:71:2: ( ^( ND_ADJACENCY term1= term term2= term ) )
            // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:71:4: ^( ND_ADJACENCY term1= term term2= term )
            {
            match(input,ND_ADJACENCY,FOLLOW_ND_ADJACENCY_in_adjacencyQuery289); 

            match(input, Token.DOWN, null); 
            pushFollow(FOLLOW_term_in_adjacencyQuery293);
            term1=term();

            state._fsp--;


            pushFollow(FOLLOW_term_in_adjacencyQuery297);
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
    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:76:1: term returns [Query query] : ( phrase | selector |subQuery= query );
    public final Query term() throws RecognitionException {
        Query query = null;


        Query subQuery =null;

        Phrase phrase9 =null;

        Query selector10 =null;


        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:77:2: ( phrase | selector |subQuery= query )
            int alt6=3;
            switch ( input.LA(1) ) {
            case ND_PHRASE:
                {
                alt6=1;
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
                alt6=2;
                }
                break;
            case ND_QUERY:
                {
                alt6=3;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 6, 0, input);

                throw nvae;

            }

            switch (alt6) {
                case 1 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:77:4: phrase
                    {
                    pushFollow(FOLLOW_phrase_in_term323);
                    phrase9=phrase();

                    state._fsp--;


                     query = phrase9; 

                    }
                    break;
                case 2 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:78:5: selector
                    {
                    pushFollow(FOLLOW_selector_in_term331);
                    selector10=selector();

                    state._fsp--;


                     query = selector10; 

                    }
                    break;
                case 3 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:79:5: subQuery= query
                    {
                    pushFollow(FOLLOW_query_in_term341);
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
    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:86:1: phrase returns [Phrase query] : ^( ND_PHRASE TXT ) ;
    public final Phrase phrase() throws RecognitionException {
        Phrase query = null;


        CommonTree TXT11=null;

        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:87:2: ( ^( ND_PHRASE TXT ) )
            // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:87:4: ^( ND_PHRASE TXT )
            {
            match(input,ND_PHRASE,FOLLOW_ND_PHRASE_in_phrase369); 

            match(input, Token.DOWN, null); 
            TXT11=(CommonTree)match(input,TXT,FOLLOW_TXT_in_phrase371); 

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
    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:97:1: selector returns [Query query] : ( tagQuery | propertyQuery | regQuery | freqQuery | similQuery | wildQuery );
    public final Query selector() throws RecognitionException {
        Query query = null;


        Query tagQuery12 =null;

        Query propertyQuery13 =null;

        Query regQuery14 =null;

        Query freqQuery15 =null;

        Query similQuery16 =null;

        Query wildQuery17 =null;


        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:98:2: ( tagQuery | propertyQuery | regQuery | freqQuery | similQuery | wildQuery )
            int alt7=6;
            switch ( input.LA(1) ) {
            case ND_TAG:
                {
                alt7=1;
                }
                break;
            case ND_PROPERTY:
            case ND_TAGPROPERTY:
                {
                alt7=2;
                }
                break;
            case ND_REG:
                {
                alt7=3;
                }
                break;
            case ND_FREQ:
                {
                alt7=4;
                }
                break;
            case ND_SIMIL:
                {
                alt7=5;
                }
                break;
            case ND_WILD:
                {
                alt7=6;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 7, 0, input);

                throw nvae;

            }

            switch (alt7) {
                case 1 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:98:4: tagQuery
                    {
                    pushFollow(FOLLOW_tagQuery_in_selector402);
                    tagQuery12=tagQuery();

                    state._fsp--;


                     query = tagQuery12; 

                    }
                    break;
                case 2 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:99:4: propertyQuery
                    {
                    pushFollow(FOLLOW_propertyQuery_in_selector409);
                    propertyQuery13=propertyQuery();

                    state._fsp--;


                     query = propertyQuery13; 

                    }
                    break;
                case 3 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:100:4: regQuery
                    {
                    pushFollow(FOLLOW_regQuery_in_selector416);
                    regQuery14=regQuery();

                    state._fsp--;


                     query = regQuery14; 

                    }
                    break;
                case 4 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:101:4: freqQuery
                    {
                    pushFollow(FOLLOW_freqQuery_in_selector423);
                    freqQuery15=freqQuery();

                    state._fsp--;


                     query = freqQuery15; 

                    }
                    break;
                case 5 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:102:4: similQuery
                    {
                    pushFollow(FOLLOW_similQuery_in_selector430);
                    similQuery16=similQuery();

                    state._fsp--;


                     query = similQuery16; 

                    }
                    break;
                case 6 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:103:4: wildQuery
                    {
                    pushFollow(FOLLOW_wildQuery_in_selector437);
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
    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:108:1: tagQuery returns [Query query] : ^( ND_TAG phrase ) ;
    public final Query tagQuery() throws RecognitionException {
        Query query = null;


        Phrase phrase18 =null;


        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:109:2: ( ^( ND_TAG phrase ) )
            // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:109:4: ^( ND_TAG phrase )
            {
            match(input,ND_TAG,FOLLOW_ND_TAG_in_tagQuery464); 

            match(input, Token.DOWN, null); 
            pushFollow(FOLLOW_phrase_in_tagQuery466);
            phrase18=phrase();

            state._fsp--;


            match(input, Token.UP, null); 


             query = new TagQuery(phrase18); 

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
    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:113:1: propertyQuery returns [Query query] : ( ^( ND_PROPERTY property= phrase (value= phrase )? ) | ^( ND_TAGPROPERTY tag= phrase property= phrase (value= phrase )? ) );
    public final Query propertyQuery() throws RecognitionException {
        Query query = null;


        Phrase property =null;

        Phrase value =null;

        Phrase tag =null;


        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:114:2: ( ^( ND_PROPERTY property= phrase (value= phrase )? ) | ^( ND_TAGPROPERTY tag= phrase property= phrase (value= phrase )? ) )
            int alt10=2;
            int LA10_0 = input.LA(1);

            if ( (LA10_0==ND_PROPERTY) ) {
                alt10=1;
            }
            else if ( (LA10_0==ND_TAGPROPERTY) ) {
                alt10=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 10, 0, input);

                throw nvae;

            }
            switch (alt10) {
                case 1 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:114:4: ^( ND_PROPERTY property= phrase (value= phrase )? )
                    {
                    match(input,ND_PROPERTY,FOLLOW_ND_PROPERTY_in_propertyQuery491); 

                    match(input, Token.DOWN, null); 
                    pushFollow(FOLLOW_phrase_in_propertyQuery495);
                    property=phrase();

                    state._fsp--;


                    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:114:34: (value= phrase )?
                    int alt8=2;
                    int LA8_0 = input.LA(1);

                    if ( (LA8_0==ND_PHRASE) ) {
                        alt8=1;
                    }
                    switch (alt8) {
                        case 1 :
                            // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:114:35: value= phrase
                            {
                            pushFollow(FOLLOW_phrase_in_propertyQuery500);
                            value=phrase();

                            state._fsp--;


                            }
                            break;

                    }


                    match(input, Token.UP, null); 


                     query = new PropertyQuery(null, property, value); 

                    }
                    break;
                case 2 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:115:4: ^( ND_TAGPROPERTY tag= phrase property= phrase (value= phrase )? )
                    {
                    match(input,ND_TAGPROPERTY,FOLLOW_ND_TAGPROPERTY_in_propertyQuery511); 

                    match(input, Token.DOWN, null); 
                    pushFollow(FOLLOW_phrase_in_propertyQuery515);
                    tag=phrase();

                    state._fsp--;


                    pushFollow(FOLLOW_phrase_in_propertyQuery519);
                    property=phrase();

                    state._fsp--;


                    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:115:48: (value= phrase )?
                    int alt9=2;
                    int LA9_0 = input.LA(1);

                    if ( (LA9_0==ND_PHRASE) ) {
                        alt9=1;
                    }
                    switch (alt9) {
                        case 1 :
                            // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:115:49: value= phrase
                            {
                            pushFollow(FOLLOW_phrase_in_propertyQuery524);
                            value=phrase();

                            state._fsp--;


                            }
                            break;

                    }


                    match(input, Token.UP, null); 


                     query = new PropertyQuery(tag, property, value); 

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
    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:119:1: regQuery returns [Query query] : ^( ND_REG phrase (caseInsensitiveMarker= 'CI' )? ) ;
    public final Query regQuery() throws RecognitionException {
        Query query = null;


        CommonTree caseInsensitiveMarker=null;
        Phrase phrase19 =null;


        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:120:2: ( ^( ND_REG phrase (caseInsensitiveMarker= 'CI' )? ) )
            // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:120:4: ^( ND_REG phrase (caseInsensitiveMarker= 'CI' )? )
            {
            match(input,ND_REG,FOLLOW_ND_REG_in_regQuery551); 

            match(input, Token.DOWN, null); 
            pushFollow(FOLLOW_phrase_in_regQuery553);
            phrase19=phrase();

            state._fsp--;


            // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:120:41: (caseInsensitiveMarker= 'CI' )?
            int alt11=2;
            int LA11_0 = input.LA(1);

            if ( (LA11_0==43) ) {
                alt11=1;
            }
            switch (alt11) {
                case 1 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:120:41: caseInsensitiveMarker= 'CI'
                    {
                    caseInsensitiveMarker=(CommonTree)match(input,43,FOLLOW_43_in_regQuery557); 

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
    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:124:1: freqQuery returns [Query query] : ( ^( ND_FREQ EQUAL rInt1= INT (rInt2= INT )? ) | ^( ND_FREQ UNEQUAL INT ) );
    public final Query freqQuery() throws RecognitionException {
        Query query = null;


        CommonTree rInt1=null;
        CommonTree rInt2=null;
        CommonTree EQUAL20=null;
        CommonTree UNEQUAL21=null;
        CommonTree INT22=null;

        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:125:2: ( ^( ND_FREQ EQUAL rInt1= INT (rInt2= INT )? ) | ^( ND_FREQ UNEQUAL INT ) )
            int alt13=2;
            int LA13_0 = input.LA(1);

            if ( (LA13_0==ND_FREQ) ) {
                int LA13_1 = input.LA(2);

                if ( (LA13_1==DOWN) ) {
                    int LA13_2 = input.LA(3);

                    if ( (LA13_2==EQUAL) ) {
                        alt13=1;
                    }
                    else if ( (LA13_2==UNEQUAL) ) {
                        alt13=2;
                    }
                    else {
                        NoViableAltException nvae =
                            new NoViableAltException("", 13, 2, input);

                        throw nvae;

                    }
                }
                else {
                    NoViableAltException nvae =
                        new NoViableAltException("", 13, 1, input);

                    throw nvae;

                }
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 13, 0, input);

                throw nvae;

            }
            switch (alt13) {
                case 1 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:125:4: ^( ND_FREQ EQUAL rInt1= INT (rInt2= INT )? )
                    {
                    match(input,ND_FREQ,FOLLOW_ND_FREQ_in_freqQuery585); 

                    match(input, Token.DOWN, null); 
                    EQUAL20=(CommonTree)match(input,EQUAL,FOLLOW_EQUAL_in_freqQuery587); 

                    rInt1=(CommonTree)match(input,INT,FOLLOW_INT_in_freqQuery591); 

                    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:125:35: (rInt2= INT )?
                    int alt12=2;
                    int LA12_0 = input.LA(1);

                    if ( (LA12_0==INT) ) {
                        alt12=1;
                    }
                    switch (alt12) {
                        case 1 :
                            // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:125:35: rInt2= INT
                            {
                            rInt2=(CommonTree)match(input,INT,FOLLOW_INT_in_freqQuery595); 

                            }
                            break;

                    }


                    match(input, Token.UP, null); 


                     query = new FreqQuery((EQUAL20!=null?EQUAL20.getText():null), (rInt1!=null?rInt1.getText():null), (rInt2!=null?rInt2.getText():null)); 

                    }
                    break;
                case 2 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:126:5: ^( ND_FREQ UNEQUAL INT )
                    {
                    match(input,ND_FREQ,FOLLOW_ND_FREQ_in_freqQuery606); 

                    match(input, Token.DOWN, null); 
                    UNEQUAL21=(CommonTree)match(input,UNEQUAL,FOLLOW_UNEQUAL_in_freqQuery608); 

                    INT22=(CommonTree)match(input,INT,FOLLOW_INT_in_freqQuery610); 

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
    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:130:1: similQuery returns [Query query] : ^( ND_SIMIL phrase INT ) ;
    public final Query similQuery() throws RecognitionException {
        Query query = null;


        CommonTree INT24=null;
        Phrase phrase23 =null;


        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:131:2: ( ^( ND_SIMIL phrase INT ) )
            // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:131:4: ^( ND_SIMIL phrase INT )
            {
            match(input,ND_SIMIL,FOLLOW_ND_SIMIL_in_similQuery638); 

            match(input, Token.DOWN, null); 
            pushFollow(FOLLOW_phrase_in_similQuery640);
            phrase23=phrase();

            state._fsp--;


            INT24=(CommonTree)match(input,INT,FOLLOW_INT_in_similQuery642); 

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
    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:135:1: wildQuery returns [Query query] : ^( ND_WILD phrase ) ;
    public final Query wildQuery() throws RecognitionException {
        Query query = null;


        Phrase phrase25 =null;


        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:136:2: ( ^( ND_WILD phrase ) )
            // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:136:4: ^( ND_WILD phrase )
            {
            match(input,ND_WILD,FOLLOW_ND_WILD_in_wildQuery669); 

            match(input, Token.DOWN, null); 
            pushFollow(FOLLOW_phrase_in_wildQuery671);
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
    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:144:1: refinement[Query query] : refinementExpression ;
    public final void refinement(Query query) throws RecognitionException {
        Refinement refinementExpression26 =null;


        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:145:2: ( refinementExpression )
            // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:145:4: refinementExpression
            {
            pushFollow(FOLLOW_refinementExpression_in_refinement697);
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
        return ;
    }
    // $ANTLR end "refinement"



    // $ANTLR start "refinementExpression"
    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:150:1: refinementExpression returns [Refinement refinement] : ( orRefinement | andRefinement | ^( ND_REFINE refinementTerm ) );
    public final Refinement refinementExpression() throws RecognitionException {
        Refinement refinement = null;


        Refinement orRefinement27 =null;

        Refinement andRefinement28 =null;

        Refinement refinementTerm29 =null;


        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:151:2: ( orRefinement | andRefinement | ^( ND_REFINE refinementTerm ) )
            int alt14=3;
            switch ( input.LA(1) ) {
            case ND_ORREFINE:
                {
                alt14=1;
                }
                break;
            case ND_ANDREFINE:
                {
                alt14=2;
                }
                break;
            case ND_REFINE:
                {
                alt14=3;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 14, 0, input);

                throw nvae;

            }

            switch (alt14) {
                case 1 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:151:4: orRefinement
                    {
                    pushFollow(FOLLOW_orRefinement_in_refinementExpression723);
                    orRefinement27=orRefinement();

                    state._fsp--;


                     refinement = orRefinement27; 

                    }
                    break;
                case 2 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:152:5: andRefinement
                    {
                    pushFollow(FOLLOW_andRefinement_in_refinementExpression731);
                    andRefinement28=andRefinement();

                    state._fsp--;


                     refinement = andRefinement28; 

                    }
                    break;
                case 3 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:153:5: ^( ND_REFINE refinementTerm )
                    {
                    match(input,ND_REFINE,FOLLOW_ND_REFINE_in_refinementExpression740); 

                    match(input, Token.DOWN, null); 
                    pushFollow(FOLLOW_refinementTerm_in_refinementExpression742);
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
    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:157:1: orRefinement returns [Refinement refinement] : ^( ND_ORREFINE rTerm1= refinementTerm rTerm2= refinementTerm ) ;
    public final Refinement orRefinement() throws RecognitionException {
        Refinement refinement = null;


        Refinement rTerm1 =null;

        Refinement rTerm2 =null;


        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:158:2: ( ^( ND_ORREFINE rTerm1= refinementTerm rTerm2= refinementTerm ) )
            // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:158:4: ^( ND_ORREFINE rTerm1= refinementTerm rTerm2= refinementTerm )
            {
            match(input,ND_ORREFINE,FOLLOW_ND_ORREFINE_in_orRefinement767); 

            match(input, Token.DOWN, null); 
            pushFollow(FOLLOW_refinementTerm_in_orRefinement771);
            rTerm1=refinementTerm();

            state._fsp--;


            pushFollow(FOLLOW_refinementTerm_in_orRefinement775);
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
    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:163:1: andRefinement returns [Refinement refinement] : ^( ND_ANDREFINE rTerm1= refinementTerm rTerm2= refinementTerm ) ;
    public final Refinement andRefinement() throws RecognitionException {
        Refinement refinement = null;


        Refinement rTerm1 =null;

        Refinement rTerm2 =null;


        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:164:2: ( ^( ND_ANDREFINE rTerm1= refinementTerm rTerm2= refinementTerm ) )
            // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:164:4: ^( ND_ANDREFINE rTerm1= refinementTerm rTerm2= refinementTerm )
            {
            match(input,ND_ANDREFINE,FOLLOW_ND_ANDREFINE_in_andRefinement805); 

            match(input, Token.DOWN, null); 
            pushFollow(FOLLOW_refinementTerm_in_andRefinement809);
            rTerm1=refinementTerm();

            state._fsp--;


            pushFollow(FOLLOW_refinementTerm_in_andRefinement813);
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
    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:169:1: refinementTerm returns [Refinement refinement] : ( selector (matchMode= MATCH_MODE )? |subQuery= query (matchMode= MATCH_MODE )? | refinementExpression );
    public final Refinement refinementTerm() throws RecognitionException {
        Refinement refinement = null;


        CommonTree matchMode=null;
        Query subQuery =null;

        Query selector30 =null;

        Refinement refinementExpression31 =null;


        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:170:2: ( selector (matchMode= MATCH_MODE )? |subQuery= query (matchMode= MATCH_MODE )? | refinementExpression )
            int alt17=3;
            switch ( input.LA(1) ) {
            case ND_FREQ:
            case ND_PROPERTY:
            case ND_REG:
            case ND_SIMIL:
            case ND_TAG:
            case ND_TAGPROPERTY:
            case ND_WILD:
                {
                alt17=1;
                }
                break;
            case ND_QUERY:
                {
                alt17=2;
                }
                break;
            case ND_ANDREFINE:
            case ND_ORREFINE:
            case ND_REFINE:
                {
                alt17=3;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 17, 0, input);

                throw nvae;

            }

            switch (alt17) {
                case 1 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:170:5: selector (matchMode= MATCH_MODE )?
                    {
                    pushFollow(FOLLOW_selector_in_refinementTerm842);
                    selector30=selector();

                    state._fsp--;


                    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:170:23: (matchMode= MATCH_MODE )?
                    int alt15=2;
                    int LA15_0 = input.LA(1);

                    if ( (LA15_0==MATCH_MODE) ) {
                        alt15=1;
                    }
                    switch (alt15) {
                        case 1 :
                            // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:170:23: matchMode= MATCH_MODE
                            {
                            matchMode=(CommonTree)match(input,MATCH_MODE,FOLLOW_MATCH_MODE_in_refinementTerm846); 

                            }
                            break;

                    }


                     refinement = new QueryRefinement(selector30, (matchMode!=null?matchMode.getText():null)); 

                    }
                    break;
                case 2 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:171:5: subQuery= query (matchMode= MATCH_MODE )?
                    {
                    pushFollow(FOLLOW_query_in_refinementTerm857);
                    subQuery=query();

                    state._fsp--;


                    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:171:29: (matchMode= MATCH_MODE )?
                    int alt16=2;
                    int LA16_0 = input.LA(1);

                    if ( (LA16_0==MATCH_MODE) ) {
                        alt16=1;
                    }
                    switch (alt16) {
                        case 1 :
                            // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:171:29: matchMode= MATCH_MODE
                            {
                            matchMode=(CommonTree)match(input,MATCH_MODE,FOLLOW_MATCH_MODE_in_refinementTerm861); 

                            }
                            break;

                    }


                     refinement = new QueryRefinement(new SubQuery(subQuery), (matchMode!=null?matchMode.getText():null)); 

                    }
                    break;
                case 3 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\tree\\CatmaQueryWalker.g:172:5: refinementExpression
                    {
                    pushFollow(FOLLOW_refinementExpression_in_refinementTerm870);
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
    public static final BitSet FOLLOW_queryExpression_in_query77 = new BitSet(new long[]{0x0000000000111008L});
    public static final BitSet FOLLOW_refinement_in_query79 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_unionQuery_in_queryExpression111 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_collocQuery_in_queryExpression119 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_exclusionQuery_in_queryExpression127 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_adjacencyQuery_in_queryExpression135 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_term_in_queryExpression143 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ND_UNION_in_unionQuery173 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_term_in_unionQuery177 = new BitSet(new long[]{0x0000000005EE8000L});
    public static final BitSet FOLLOW_term_in_unionQuery181 = new BitSet(new long[]{0x0000100000000008L});
    public static final BitSet FOLLOW_44_in_unionQuery185 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_ND_COLLOC_in_collocQuery212 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_term_in_collocQuery216 = new BitSet(new long[]{0x0000000005EE8000L});
    public static final BitSet FOLLOW_term_in_collocQuery220 = new BitSet(new long[]{0x0000000000000088L});
    public static final BitSet FOLLOW_INT_in_collocQuery222 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_ND_EXCLUSION_in_exclusionQuery249 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_term_in_exclusionQuery253 = new BitSet(new long[]{0x0000000005EE8000L});
    public static final BitSet FOLLOW_term_in_exclusionQuery257 = new BitSet(new long[]{0x0000000000000408L});
    public static final BitSet FOLLOW_MATCH_MODE_in_exclusionQuery261 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_ND_ADJACENCY_in_adjacencyQuery289 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_term_in_adjacencyQuery293 = new BitSet(new long[]{0x0000000005EE8000L});
    public static final BitSet FOLLOW_term_in_adjacencyQuery297 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_phrase_in_term323 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_selector_in_term331 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_query_in_term341 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ND_PHRASE_in_phrase369 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_TXT_in_phrase371 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_tagQuery_in_selector402 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_propertyQuery_in_selector409 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_regQuery_in_selector416 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_freqQuery_in_selector423 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_similQuery_in_selector430 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_wildQuery_in_selector437 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ND_TAG_in_tagQuery464 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_phrase_in_tagQuery466 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_ND_PROPERTY_in_propertyQuery491 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_phrase_in_propertyQuery495 = new BitSet(new long[]{0x0000000000020008L});
    public static final BitSet FOLLOW_phrase_in_propertyQuery500 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_ND_TAGPROPERTY_in_propertyQuery511 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_phrase_in_propertyQuery515 = new BitSet(new long[]{0x0000000000020000L});
    public static final BitSet FOLLOW_phrase_in_propertyQuery519 = new BitSet(new long[]{0x0000000000020008L});
    public static final BitSet FOLLOW_phrase_in_propertyQuery524 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_ND_REG_in_regQuery551 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_phrase_in_regQuery553 = new BitSet(new long[]{0x0000080000000008L});
    public static final BitSet FOLLOW_43_in_regQuery557 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_ND_FREQ_in_freqQuery585 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_EQUAL_in_freqQuery587 = new BitSet(new long[]{0x0000000000000080L});
    public static final BitSet FOLLOW_INT_in_freqQuery591 = new BitSet(new long[]{0x0000000000000088L});
    public static final BitSet FOLLOW_INT_in_freqQuery595 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_ND_FREQ_in_freqQuery606 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_UNEQUAL_in_freqQuery608 = new BitSet(new long[]{0x0000000000000080L});
    public static final BitSet FOLLOW_INT_in_freqQuery610 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_ND_SIMIL_in_similQuery638 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_phrase_in_similQuery640 = new BitSet(new long[]{0x0000000000000080L});
    public static final BitSet FOLLOW_INT_in_similQuery642 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_ND_WILD_in_wildQuery669 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_phrase_in_wildQuery671 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_refinementExpression_in_refinement697 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_orRefinement_in_refinementExpression723 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_andRefinement_in_refinementExpression731 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ND_REFINE_in_refinementExpression740 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_refinementTerm_in_refinementExpression742 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_ND_ORREFINE_in_orRefinement767 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_refinementTerm_in_orRefinement771 = new BitSet(new long[]{0x0000000005FD9000L});
    public static final BitSet FOLLOW_refinementTerm_in_orRefinement775 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_ND_ANDREFINE_in_andRefinement805 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_refinementTerm_in_andRefinement809 = new BitSet(new long[]{0x0000000005FD9000L});
    public static final BitSet FOLLOW_refinementTerm_in_andRefinement813 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_selector_in_refinementTerm842 = new BitSet(new long[]{0x0000000000000402L});
    public static final BitSet FOLLOW_MATCH_MODE_in_refinementTerm846 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_query_in_refinementTerm857 = new BitSet(new long[]{0x0000000000000402L});
    public static final BitSet FOLLOW_MATCH_MODE_in_refinementTerm861 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_refinementExpression_in_refinementTerm870 = new BitSet(new long[]{0x0000000000000002L});

}