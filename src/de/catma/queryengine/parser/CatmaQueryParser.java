// $ANTLR 3.4 C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g 2015-02-05 14:57:44

package de.catma.queryengine.parser;


import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

import org.antlr.runtime.tree.*;


/**
* CATMA Query Parser Grammar
*
* Author: 	Malte Meister, Marco Petris
* Version: 	2.0
* About:	EBNF Grammar for the CATMA Query Parser with AST generation rules
*/
@SuppressWarnings({"all", "warnings", "unchecked"})
public class CatmaQueryParser extends Parser {
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
    public Parser[] getDelegates() {
        return new Parser[] {};
    }

    // delegators


    public CatmaQueryParser(TokenStream input) {
        this(input, new RecognizerSharedState());
    }
    public CatmaQueryParser(TokenStream input, RecognizerSharedState state) {
        super(input, state);
    }

protected TreeAdaptor adaptor = new CommonTreeAdaptor();

public void setTreeAdaptor(TreeAdaptor adaptor) {
    this.adaptor = adaptor;
}
public TreeAdaptor getTreeAdaptor() {
    return adaptor;
}
    public String[] getTokenNames() { return CatmaQueryParser.tokenNames; }
    public String getGrammarFileName() { return "C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g"; }



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


    public static class start_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "start"
    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:105:1: start : query EOF ;
    public final CatmaQueryParser.start_return start() throws RecognitionException {
        CatmaQueryParser.start_return retval = new CatmaQueryParser.start_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token EOF2=null;
        CatmaQueryParser.query_return query1 =null;


        Object EOF2_tree=null;

        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:105:7: ( query EOF )
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:105:9: query EOF
            {
            root_0 = (Object)adaptor.nil();


            pushFollow(FOLLOW_query_in_start151);
            query1=query();

            state._fsp--;

            adaptor.addChild(root_0, query1.getTree());

            EOF2=(Token)match(input,EOF,FOLLOW_EOF_in_start153); 
            EOF2_tree = 
            (Object)adaptor.create(EOF2)
            ;
            adaptor.addChild(root_0, EOF2_tree);


            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException e) {
            throw e;
        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "start"


    public static class query_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "query"
    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:109:1: query : queryExpression ( refinement )? -> ^( ND_QUERY queryExpression ( refinement )? ) ;
    public final CatmaQueryParser.query_return query() throws RecognitionException {
        CatmaQueryParser.query_return retval = new CatmaQueryParser.query_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        CatmaQueryParser.queryExpression_return queryExpression3 =null;

        CatmaQueryParser.refinement_return refinement4 =null;


        RewriteRuleSubtreeStream stream_queryExpression=new RewriteRuleSubtreeStream(adaptor,"rule queryExpression");
        RewriteRuleSubtreeStream stream_refinement=new RewriteRuleSubtreeStream(adaptor,"rule refinement");
        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:109:7: ( queryExpression ( refinement )? -> ^( ND_QUERY queryExpression ( refinement )? ) )
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:109:9: queryExpression ( refinement )?
            {
            pushFollow(FOLLOW_queryExpression_in_query169);
            queryExpression3=queryExpression();

            state._fsp--;

            stream_queryExpression.add(queryExpression3.getTree());

            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:109:25: ( refinement )?
            int alt1=2;
            int LA1_0 = input.LA(1);

            if ( (LA1_0==45) ) {
                alt1=1;
            }
            switch (alt1) {
                case 1 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:109:25: refinement
                    {
                    pushFollow(FOLLOW_refinement_in_query171);
                    refinement4=refinement();

                    state._fsp--;

                    stream_refinement.add(refinement4.getTree());

                    }
                    break;

            }


            // AST REWRITE
            // elements: queryExpression, refinement
            // token labels: 
            // rule labels: retval
            // token list labels: 
            // rule list labels: 
            // wildcard labels: 
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 109:37: -> ^( ND_QUERY queryExpression ( refinement )? )
            {
                // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:109:40: ^( ND_QUERY queryExpression ( refinement )? )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(ND_QUERY, "ND_QUERY")
                , root_1);

                adaptor.addChild(root_1, stream_queryExpression.nextTree());

                // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:109:67: ( refinement )?
                if ( stream_refinement.hasNext() ) {
                    adaptor.addChild(root_1, stream_refinement.nextTree());

                }
                stream_refinement.reset();

                adaptor.addChild(root_0, root_1);
                }

            }


            retval.tree = root_0;

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException e) {
            throw e;
        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "query"


    public static class queryExpression_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "queryExpression"
    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:118:1: queryExpression : startTerm= term ( unionQuery[(CommonTree)$startTerm.tree] -> unionQuery | collocQuery[(CommonTree)$startTerm.tree] -> collocQuery | exclusionQuery[(CommonTree)$startTerm.tree] -> exclusionQuery | adjacencyQuery[(CommonTree)$startTerm.tree] -> adjacencyQuery | -> term ) ;
    public final CatmaQueryParser.queryExpression_return queryExpression() throws RecognitionException {
        CatmaQueryParser.queryExpression_return retval = new CatmaQueryParser.queryExpression_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        CatmaQueryParser.term_return startTerm =null;

        CatmaQueryParser.unionQuery_return unionQuery5 =null;

        CatmaQueryParser.collocQuery_return collocQuery6 =null;

        CatmaQueryParser.exclusionQuery_return exclusionQuery7 =null;

        CatmaQueryParser.adjacencyQuery_return adjacencyQuery8 =null;


        RewriteRuleSubtreeStream stream_unionQuery=new RewriteRuleSubtreeStream(adaptor,"rule unionQuery");
        RewriteRuleSubtreeStream stream_exclusionQuery=new RewriteRuleSubtreeStream(adaptor,"rule exclusionQuery");
        RewriteRuleSubtreeStream stream_adjacencyQuery=new RewriteRuleSubtreeStream(adaptor,"rule adjacencyQuery");
        RewriteRuleSubtreeStream stream_term=new RewriteRuleSubtreeStream(adaptor,"rule term");
        RewriteRuleSubtreeStream stream_collocQuery=new RewriteRuleSubtreeStream(adaptor,"rule collocQuery");
        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:119:2: (startTerm= term ( unionQuery[(CommonTree)$startTerm.tree] -> unionQuery | collocQuery[(CommonTree)$startTerm.tree] -> collocQuery | exclusionQuery[(CommonTree)$startTerm.tree] -> exclusionQuery | adjacencyQuery[(CommonTree)$startTerm.tree] -> adjacencyQuery | -> term ) )
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:119:4: startTerm= term ( unionQuery[(CommonTree)$startTerm.tree] -> unionQuery | collocQuery[(CommonTree)$startTerm.tree] -> collocQuery | exclusionQuery[(CommonTree)$startTerm.tree] -> exclusionQuery | adjacencyQuery[(CommonTree)$startTerm.tree] -> adjacencyQuery | -> term )
            {
            pushFollow(FOLLOW_term_in_queryExpression208);
            startTerm=term();

            state._fsp--;

            stream_term.add(startTerm.getTree());

            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:119:19: ( unionQuery[(CommonTree)$startTerm.tree] -> unionQuery | collocQuery[(CommonTree)$startTerm.tree] -> collocQuery | exclusionQuery[(CommonTree)$startTerm.tree] -> exclusionQuery | adjacencyQuery[(CommonTree)$startTerm.tree] -> adjacencyQuery | -> term )
            int alt2=5;
            switch ( input.LA(1) ) {
            case 40:
                {
                alt2=1;
                }
                break;
            case 37:
                {
                alt2=2;
                }
                break;
            case 41:
                {
                alt2=3;
                }
                break;
            case 42:
                {
                alt2=4;
                }
                break;
            case EOF:
            case 39:
            case 45:
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
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:120:4: unionQuery[(CommonTree)$startTerm.tree]
                    {
                    pushFollow(FOLLOW_unionQuery_in_queryExpression215);
                    unionQuery5=unionQuery((CommonTree)(startTerm!=null?((Object)startTerm.tree):null));

                    state._fsp--;

                    stream_unionQuery.add(unionQuery5.getTree());

                    // AST REWRITE
                    // elements: unionQuery
                    // token labels: 
                    // rule labels: retval
                    // token list labels: 
                    // rule list labels: 
                    // wildcard labels: 
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

                    root_0 = (Object)adaptor.nil();
                    // 120:44: -> unionQuery
                    {
                        adaptor.addChild(root_0, stream_unionQuery.nextTree());

                    }


                    retval.tree = root_0;

                    }
                    break;
                case 2 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:121:6: collocQuery[(CommonTree)$startTerm.tree]
                    {
                    pushFollow(FOLLOW_collocQuery_in_queryExpression227);
                    collocQuery6=collocQuery((CommonTree)(startTerm!=null?((Object)startTerm.tree):null));

                    state._fsp--;

                    stream_collocQuery.add(collocQuery6.getTree());

                    // AST REWRITE
                    // elements: collocQuery
                    // token labels: 
                    // rule labels: retval
                    // token list labels: 
                    // rule list labels: 
                    // wildcard labels: 
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

                    root_0 = (Object)adaptor.nil();
                    // 121:48: -> collocQuery
                    {
                        adaptor.addChild(root_0, stream_collocQuery.nextTree());

                    }


                    retval.tree = root_0;

                    }
                    break;
                case 3 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:122:6: exclusionQuery[(CommonTree)$startTerm.tree]
                    {
                    pushFollow(FOLLOW_exclusionQuery_in_queryExpression240);
                    exclusionQuery7=exclusionQuery((CommonTree)(startTerm!=null?((Object)startTerm.tree):null));

                    state._fsp--;

                    stream_exclusionQuery.add(exclusionQuery7.getTree());

                    // AST REWRITE
                    // elements: exclusionQuery
                    // token labels: 
                    // rule labels: retval
                    // token list labels: 
                    // rule list labels: 
                    // wildcard labels: 
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

                    root_0 = (Object)adaptor.nil();
                    // 122:51: -> exclusionQuery
                    {
                        adaptor.addChild(root_0, stream_exclusionQuery.nextTree());

                    }


                    retval.tree = root_0;

                    }
                    break;
                case 4 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:123:6: adjacencyQuery[(CommonTree)$startTerm.tree]
                    {
                    pushFollow(FOLLOW_adjacencyQuery_in_queryExpression253);
                    adjacencyQuery8=adjacencyQuery((CommonTree)(startTerm!=null?((Object)startTerm.tree):null));

                    state._fsp--;

                    stream_adjacencyQuery.add(adjacencyQuery8.getTree());

                    // AST REWRITE
                    // elements: adjacencyQuery
                    // token labels: 
                    // rule labels: retval
                    // token list labels: 
                    // rule list labels: 
                    // wildcard labels: 
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

                    root_0 = (Object)adaptor.nil();
                    // 123:51: -> adjacencyQuery
                    {
                        adaptor.addChild(root_0, stream_adjacencyQuery.nextTree());

                    }


                    retval.tree = root_0;

                    }
                    break;
                case 5 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:124:6: 
                    {
                    // AST REWRITE
                    // elements: term
                    // token labels: 
                    // rule labels: retval
                    // token list labels: 
                    // rule list labels: 
                    // wildcard labels: 
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

                    root_0 = (Object)adaptor.nil();
                    // 124:6: -> term
                    {
                        adaptor.addChild(root_0, stream_term.nextTree());

                    }


                    retval.tree = root_0;

                    }
                    break;

            }


            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException e) {
            throw e;
        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "queryExpression"


    public static class unionQuery_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "unionQuery"
    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:134:1: unionQuery[CommonTree startTerm] : ',' term ( 'EXCL' )? -> ^( ND_UNION term ( 'EXCL' )? ) ;
    public final CatmaQueryParser.unionQuery_return unionQuery(CommonTree startTerm) throws RecognitionException {
        CatmaQueryParser.unionQuery_return retval = new CatmaQueryParser.unionQuery_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token char_literal9=null;
        Token string_literal11=null;
        CatmaQueryParser.term_return term10 =null;


        Object char_literal9_tree=null;
        Object string_literal11_tree=null;
        RewriteRuleTokenStream stream_44=new RewriteRuleTokenStream(adaptor,"token 44");
        RewriteRuleTokenStream stream_40=new RewriteRuleTokenStream(adaptor,"token 40");
        RewriteRuleSubtreeStream stream_term=new RewriteRuleSubtreeStream(adaptor,"rule term");
        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:135:2: ( ',' term ( 'EXCL' )? -> ^( ND_UNION term ( 'EXCL' )? ) )
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:135:4: ',' term ( 'EXCL' )?
            {
            char_literal9=(Token)match(input,40,FOLLOW_40_in_unionQuery296);  
            stream_40.add(char_literal9);


            pushFollow(FOLLOW_term_in_unionQuery298);
            term10=term();

            state._fsp--;

            stream_term.add(term10.getTree());

            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:135:13: ( 'EXCL' )?
            int alt3=2;
            int LA3_0 = input.LA(1);

            if ( (LA3_0==44) ) {
                alt3=1;
            }
            switch (alt3) {
                case 1 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:135:13: 'EXCL'
                    {
                    string_literal11=(Token)match(input,44,FOLLOW_44_in_unionQuery300);  
                    stream_44.add(string_literal11);


                    }
                    break;

            }


            // AST REWRITE
            // elements: 44, term
            // token labels: 
            // rule labels: retval
            // token list labels: 
            // rule list labels: 
            // wildcard labels: 
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 135:21: -> ^( ND_UNION term ( 'EXCL' )? )
            {
                // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:135:24: ^( ND_UNION term ( 'EXCL' )? )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(ND_UNION, "ND_UNION")
                , root_1);

                adaptor.addChild(root_1, startTerm);

                adaptor.addChild(root_1, stream_term.nextTree());

                // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:135:53: ( 'EXCL' )?
                if ( stream_44.hasNext() ) {
                    adaptor.addChild(root_1, 
                    stream_44.nextNode()
                    );

                }
                stream_44.reset();

                adaptor.addChild(root_0, root_1);
                }

            }


            retval.tree = root_0;

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException e) {
            throw e;
        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "unionQuery"


    public static class collocQuery_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "collocQuery"
    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:139:1: collocQuery[CommonTree startTerm] : '&' term ( INT )? -> ^( ND_COLLOC term ( INT )? ) ;
    public final CatmaQueryParser.collocQuery_return collocQuery(CommonTree startTerm) throws RecognitionException {
        CatmaQueryParser.collocQuery_return retval = new CatmaQueryParser.collocQuery_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token char_literal12=null;
        Token INT14=null;
        CatmaQueryParser.term_return term13 =null;


        Object char_literal12_tree=null;
        Object INT14_tree=null;
        RewriteRuleTokenStream stream_37=new RewriteRuleTokenStream(adaptor,"token 37");
        RewriteRuleTokenStream stream_INT=new RewriteRuleTokenStream(adaptor,"token INT");
        RewriteRuleSubtreeStream stream_term=new RewriteRuleSubtreeStream(adaptor,"rule term");
        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:140:2: ( '&' term ( INT )? -> ^( ND_COLLOC term ( INT )? ) )
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:140:4: '&' term ( INT )?
            {
            char_literal12=(Token)match(input,37,FOLLOW_37_in_collocQuery335);  
            stream_37.add(char_literal12);


            pushFollow(FOLLOW_term_in_collocQuery337);
            term13=term();

            state._fsp--;

            stream_term.add(term13.getTree());

            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:140:13: ( INT )?
            int alt4=2;
            int LA4_0 = input.LA(1);

            if ( (LA4_0==INT) ) {
                alt4=1;
            }
            switch (alt4) {
                case 1 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:140:13: INT
                    {
                    INT14=(Token)match(input,INT,FOLLOW_INT_in_collocQuery339);  
                    stream_INT.add(INT14);


                    }
                    break;

            }


            // AST REWRITE
            // elements: INT, term
            // token labels: 
            // rule labels: retval
            // token list labels: 
            // rule list labels: 
            // wildcard labels: 
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 140:18: -> ^( ND_COLLOC term ( INT )? )
            {
                // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:140:21: ^( ND_COLLOC term ( INT )? )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(ND_COLLOC, "ND_COLLOC")
                , root_1);

                adaptor.addChild(root_1, startTerm);

                adaptor.addChild(root_1, stream_term.nextTree());

                // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:140:51: ( INT )?
                if ( stream_INT.hasNext() ) {
                    adaptor.addChild(root_1, 
                    stream_INT.nextNode()
                    );

                }
                stream_INT.reset();

                adaptor.addChild(root_0, root_1);
                }

            }


            retval.tree = root_0;

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException e) {
            throw e;
        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "collocQuery"


    public static class exclusionQuery_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "exclusionQuery"
    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:144:1: exclusionQuery[CommonTree startTerm] : '-' term ( MATCH_MODE )? -> ^( ND_EXCLUSION term ( MATCH_MODE )? ) ;
    public final CatmaQueryParser.exclusionQuery_return exclusionQuery(CommonTree startTerm) throws RecognitionException {
        CatmaQueryParser.exclusionQuery_return retval = new CatmaQueryParser.exclusionQuery_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token char_literal15=null;
        Token MATCH_MODE17=null;
        CatmaQueryParser.term_return term16 =null;


        Object char_literal15_tree=null;
        Object MATCH_MODE17_tree=null;
        RewriteRuleTokenStream stream_MATCH_MODE=new RewriteRuleTokenStream(adaptor,"token MATCH_MODE");
        RewriteRuleTokenStream stream_41=new RewriteRuleTokenStream(adaptor,"token 41");
        RewriteRuleSubtreeStream stream_term=new RewriteRuleSubtreeStream(adaptor,"rule term");
        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:145:2: ( '-' term ( MATCH_MODE )? -> ^( ND_EXCLUSION term ( MATCH_MODE )? ) )
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:145:4: '-' term ( MATCH_MODE )?
            {
            char_literal15=(Token)match(input,41,FOLLOW_41_in_exclusionQuery373);  
            stream_41.add(char_literal15);


            pushFollow(FOLLOW_term_in_exclusionQuery375);
            term16=term();

            state._fsp--;

            stream_term.add(term16.getTree());

            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:145:13: ( MATCH_MODE )?
            int alt5=2;
            int LA5_0 = input.LA(1);

            if ( (LA5_0==MATCH_MODE) ) {
                alt5=1;
            }
            switch (alt5) {
                case 1 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:145:13: MATCH_MODE
                    {
                    MATCH_MODE17=(Token)match(input,MATCH_MODE,FOLLOW_MATCH_MODE_in_exclusionQuery377);  
                    stream_MATCH_MODE.add(MATCH_MODE17);


                    }
                    break;

            }


            // AST REWRITE
            // elements: MATCH_MODE, term
            // token labels: 
            // rule labels: retval
            // token list labels: 
            // rule list labels: 
            // wildcard labels: 
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 145:24: -> ^( ND_EXCLUSION term ( MATCH_MODE )? )
            {
                // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:145:27: ^( ND_EXCLUSION term ( MATCH_MODE )? )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(ND_EXCLUSION, "ND_EXCLUSION")
                , root_1);

                adaptor.addChild(root_1, startTerm);

                adaptor.addChild(root_1, stream_term.nextTree());

                // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:145:60: ( MATCH_MODE )?
                if ( stream_MATCH_MODE.hasNext() ) {
                    adaptor.addChild(root_1, 
                    stream_MATCH_MODE.nextNode()
                    );

                }
                stream_MATCH_MODE.reset();

                adaptor.addChild(root_0, root_1);
                }

            }


            retval.tree = root_0;

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException e) {
            throw e;
        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "exclusionQuery"


    public static class adjacencyQuery_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "adjacencyQuery"
    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:149:1: adjacencyQuery[CommonTree startTerm] : ';' term -> ^( ND_ADJACENCY term ) ;
    public final CatmaQueryParser.adjacencyQuery_return adjacencyQuery(CommonTree startTerm) throws RecognitionException {
        CatmaQueryParser.adjacencyQuery_return retval = new CatmaQueryParser.adjacencyQuery_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token char_literal18=null;
        CatmaQueryParser.term_return term19 =null;


        Object char_literal18_tree=null;
        RewriteRuleTokenStream stream_42=new RewriteRuleTokenStream(adaptor,"token 42");
        RewriteRuleSubtreeStream stream_term=new RewriteRuleSubtreeStream(adaptor,"rule term");
        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:150:2: ( ';' term -> ^( ND_ADJACENCY term ) )
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:150:4: ';' term
            {
            char_literal18=(Token)match(input,42,FOLLOW_42_in_adjacencyQuery411);  
            stream_42.add(char_literal18);


            pushFollow(FOLLOW_term_in_adjacencyQuery413);
            term19=term();

            state._fsp--;

            stream_term.add(term19.getTree());

            // AST REWRITE
            // elements: term
            // token labels: 
            // rule labels: retval
            // token list labels: 
            // rule list labels: 
            // wildcard labels: 
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 150:13: -> ^( ND_ADJACENCY term )
            {
                // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:150:16: ^( ND_ADJACENCY term )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(ND_ADJACENCY, "ND_ADJACENCY")
                , root_1);

                adaptor.addChild(root_1, startTerm);

                adaptor.addChild(root_1, stream_term.nextTree());

                adaptor.addChild(root_0, root_1);
                }

            }


            retval.tree = root_0;

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException e) {
            throw e;
        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "adjacencyQuery"


    public static class term_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "term"
    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:155:1: term : ( phrase -> phrase | selector -> selector | '(' query ')' -> query );
    public final CatmaQueryParser.term_return term() throws RecognitionException {
        CatmaQueryParser.term_return retval = new CatmaQueryParser.term_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token char_literal22=null;
        Token char_literal24=null;
        CatmaQueryParser.phrase_return phrase20 =null;

        CatmaQueryParser.selector_return selector21 =null;

        CatmaQueryParser.query_return query23 =null;


        Object char_literal22_tree=null;
        Object char_literal24_tree=null;
        RewriteRuleTokenStream stream_38=new RewriteRuleTokenStream(adaptor,"token 38");
        RewriteRuleTokenStream stream_39=new RewriteRuleTokenStream(adaptor,"token 39");
        RewriteRuleSubtreeStream stream_phrase=new RewriteRuleSubtreeStream(adaptor,"rule phrase");
        RewriteRuleSubtreeStream stream_query=new RewriteRuleSubtreeStream(adaptor,"rule query");
        RewriteRuleSubtreeStream stream_selector=new RewriteRuleSubtreeStream(adaptor,"rule selector");
        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:155:7: ( phrase -> phrase | selector -> selector | '(' query ')' -> query )
            int alt6=3;
            switch ( input.LA(1) ) {
            case TXT:
                {
                alt6=1;
                }
                break;
            case FREQ:
            case PROPERTY:
            case REG:
            case SIMIL:
            case TAG:
            case WILD:
                {
                alt6=2;
                }
                break;
            case 38:
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
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:155:9: phrase
                    {
                    pushFollow(FOLLOW_phrase_in_term443);
                    phrase20=phrase();

                    state._fsp--;

                    stream_phrase.add(phrase20.getTree());

                    // AST REWRITE
                    // elements: phrase
                    // token labels: 
                    // rule labels: retval
                    // token list labels: 
                    // rule list labels: 
                    // wildcard labels: 
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

                    root_0 = (Object)adaptor.nil();
                    // 155:16: -> phrase
                    {
                        adaptor.addChild(root_0, stream_phrase.nextTree());

                    }


                    retval.tree = root_0;

                    }
                    break;
                case 2 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:156:5: selector
                    {
                    pushFollow(FOLLOW_selector_in_term453);
                    selector21=selector();

                    state._fsp--;

                    stream_selector.add(selector21.getTree());

                    // AST REWRITE
                    // elements: selector
                    // token labels: 
                    // rule labels: retval
                    // token list labels: 
                    // rule list labels: 
                    // wildcard labels: 
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

                    root_0 = (Object)adaptor.nil();
                    // 156:14: -> selector
                    {
                        adaptor.addChild(root_0, stream_selector.nextTree());

                    }


                    retval.tree = root_0;

                    }
                    break;
                case 3 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:157:5: '(' query ')'
                    {
                    char_literal22=(Token)match(input,38,FOLLOW_38_in_term464);  
                    stream_38.add(char_literal22);


                    pushFollow(FOLLOW_query_in_term465);
                    query23=query();

                    state._fsp--;

                    stream_query.add(query23.getTree());

                    char_literal24=(Token)match(input,39,FOLLOW_39_in_term466);  
                    stream_39.add(char_literal24);


                    // AST REWRITE
                    // elements: query
                    // token labels: 
                    // rule labels: retval
                    // token list labels: 
                    // rule list labels: 
                    // wildcard labels: 
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

                    root_0 = (Object)adaptor.nil();
                    // 157:17: -> query
                    {
                        adaptor.addChild(root_0, stream_query.nextTree());

                    }


                    retval.tree = root_0;

                    }
                    break;

            }
            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException e) {
            throw e;
        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "term"


    public static class phrase_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "phrase"
    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:164:1: phrase : TXT -> ^( ND_PHRASE TXT ) ;
    public final CatmaQueryParser.phrase_return phrase() throws RecognitionException {
        CatmaQueryParser.phrase_return retval = new CatmaQueryParser.phrase_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token TXT25=null;

        Object TXT25_tree=null;
        RewriteRuleTokenStream stream_TXT=new RewriteRuleTokenStream(adaptor,"token TXT");

        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:164:8: ( TXT -> ^( ND_PHRASE TXT ) )
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:164:10: TXT
            {
            TXT25=(Token)match(input,TXT,FOLLOW_TXT_in_phrase490);  
            stream_TXT.add(TXT25);


            // AST REWRITE
            // elements: TXT
            // token labels: 
            // rule labels: retval
            // token list labels: 
            // rule list labels: 
            // wildcard labels: 
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 164:14: -> ^( ND_PHRASE TXT )
            {
                // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:164:17: ^( ND_PHRASE TXT )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(ND_PHRASE, "ND_PHRASE")
                , root_1);

                adaptor.addChild(root_1, 
                stream_TXT.nextNode()
                );

                adaptor.addChild(root_0, root_1);
                }

            }


            retval.tree = root_0;

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException e) {
            throw e;
        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "phrase"


    public static class selector_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "selector"
    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:174:1: selector : ( tagQuery | propertyQuery | regQuery | freqQuery | similQuery | wildQuery );
    public final CatmaQueryParser.selector_return selector() throws RecognitionException {
        CatmaQueryParser.selector_return retval = new CatmaQueryParser.selector_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        CatmaQueryParser.tagQuery_return tagQuery26 =null;

        CatmaQueryParser.propertyQuery_return propertyQuery27 =null;

        CatmaQueryParser.regQuery_return regQuery28 =null;

        CatmaQueryParser.freqQuery_return freqQuery29 =null;

        CatmaQueryParser.similQuery_return similQuery30 =null;

        CatmaQueryParser.wildQuery_return wildQuery31 =null;



        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:175:2: ( tagQuery | propertyQuery | regQuery | freqQuery | similQuery | wildQuery )
            int alt7=6;
            switch ( input.LA(1) ) {
            case TAG:
                {
                int LA7_1 = input.LA(2);

                if ( (LA7_1==EQUAL) ) {
                    int LA7_7 = input.LA(3);

                    if ( (LA7_7==TXT) ) {
                        int LA7_8 = input.LA(4);

                        if ( (LA7_8==EOF||LA7_8==INT||LA7_8==MATCH_MODE||LA7_8==37||(LA7_8 >= 39 && LA7_8 <= 42)||(LA7_8 >= 44 && LA7_8 <= 46)) ) {
                            alt7=1;
                        }
                        else if ( (LA7_8==PROPERTY) ) {
                            alt7=2;
                        }
                        else {
                            NoViableAltException nvae =
                                new NoViableAltException("", 7, 8, input);

                            throw nvae;

                        }
                    }
                    else {
                        NoViableAltException nvae =
                            new NoViableAltException("", 7, 7, input);

                        throw nvae;

                    }
                }
                else {
                    NoViableAltException nvae =
                        new NoViableAltException("", 7, 1, input);

                    throw nvae;

                }
                }
                break;
            case PROPERTY:
                {
                alt7=2;
                }
                break;
            case REG:
                {
                alt7=3;
                }
                break;
            case FREQ:
                {
                alt7=4;
                }
                break;
            case SIMIL:
                {
                alt7=5;
                }
                break;
            case WILD:
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
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:175:4: tagQuery
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_tagQuery_in_selector523);
                    tagQuery26=tagQuery();

                    state._fsp--;

                    adaptor.addChild(root_0, tagQuery26.getTree());

                    }
                    break;
                case 2 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:176:4: propertyQuery
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_propertyQuery_in_selector528);
                    propertyQuery27=propertyQuery();

                    state._fsp--;

                    adaptor.addChild(root_0, propertyQuery27.getTree());

                    }
                    break;
                case 3 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:177:4: regQuery
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_regQuery_in_selector533);
                    regQuery28=regQuery();

                    state._fsp--;

                    adaptor.addChild(root_0, regQuery28.getTree());

                    }
                    break;
                case 4 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:178:4: freqQuery
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_freqQuery_in_selector538);
                    freqQuery29=freqQuery();

                    state._fsp--;

                    adaptor.addChild(root_0, freqQuery29.getTree());

                    }
                    break;
                case 5 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:179:4: similQuery
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_similQuery_in_selector543);
                    similQuery30=similQuery();

                    state._fsp--;

                    adaptor.addChild(root_0, similQuery30.getTree());

                    }
                    break;
                case 6 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:180:4: wildQuery
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_wildQuery_in_selector548);
                    wildQuery31=wildQuery();

                    state._fsp--;

                    adaptor.addChild(root_0, wildQuery31.getTree());

                    }
                    break;

            }
            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException e) {
            throw e;
        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "selector"


    public static class tagQuery_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "tagQuery"
    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:185:1: tagQuery : TAG EQUAL phrase -> ^( ND_TAG phrase ) ;
    public final CatmaQueryParser.tagQuery_return tagQuery() throws RecognitionException {
        CatmaQueryParser.tagQuery_return retval = new CatmaQueryParser.tagQuery_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token TAG32=null;
        Token EQUAL33=null;
        CatmaQueryParser.phrase_return phrase34 =null;


        Object TAG32_tree=null;
        Object EQUAL33_tree=null;
        RewriteRuleTokenStream stream_EQUAL=new RewriteRuleTokenStream(adaptor,"token EQUAL");
        RewriteRuleTokenStream stream_TAG=new RewriteRuleTokenStream(adaptor,"token TAG");
        RewriteRuleSubtreeStream stream_phrase=new RewriteRuleSubtreeStream(adaptor,"rule phrase");
        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:186:2: ( TAG EQUAL phrase -> ^( ND_TAG phrase ) )
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:186:4: TAG EQUAL phrase
            {
            TAG32=(Token)match(input,TAG,FOLLOW_TAG_in_tagQuery568);  
            stream_TAG.add(TAG32);


            EQUAL33=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_tagQuery570);  
            stream_EQUAL.add(EQUAL33);


            pushFollow(FOLLOW_phrase_in_tagQuery572);
            phrase34=phrase();

            state._fsp--;

            stream_phrase.add(phrase34.getTree());

            // AST REWRITE
            // elements: phrase
            // token labels: 
            // rule labels: retval
            // token list labels: 
            // rule list labels: 
            // wildcard labels: 
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 186:21: -> ^( ND_TAG phrase )
            {
                // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:186:24: ^( ND_TAG phrase )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(ND_TAG, "ND_TAG")
                , root_1);

                adaptor.addChild(root_1, stream_phrase.nextTree());

                adaptor.addChild(root_0, root_1);
                }

            }


            retval.tree = root_0;

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException e) {
            throw e;
        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "tagQuery"


    public static class propertyQuery_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "propertyQuery"
    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:191:1: propertyQuery : ( PROPERTY EQUAL phrase ( VALUE EQUAL phrase )? -> ^( ND_PROPERTY phrase ( phrase )? ) | TAG EQUAL phrase PROPERTY EQUAL phrase ( VALUE EQUAL phrase )? -> ^( ND_TAGPROPERTY phrase phrase ( phrase )? ) );
    public final CatmaQueryParser.propertyQuery_return propertyQuery() throws RecognitionException {
        CatmaQueryParser.propertyQuery_return retval = new CatmaQueryParser.propertyQuery_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token PROPERTY35=null;
        Token EQUAL36=null;
        Token VALUE38=null;
        Token EQUAL39=null;
        Token TAG41=null;
        Token EQUAL42=null;
        Token PROPERTY44=null;
        Token EQUAL45=null;
        Token VALUE47=null;
        Token EQUAL48=null;
        CatmaQueryParser.phrase_return phrase37 =null;

        CatmaQueryParser.phrase_return phrase40 =null;

        CatmaQueryParser.phrase_return phrase43 =null;

        CatmaQueryParser.phrase_return phrase46 =null;

        CatmaQueryParser.phrase_return phrase49 =null;


        Object PROPERTY35_tree=null;
        Object EQUAL36_tree=null;
        Object VALUE38_tree=null;
        Object EQUAL39_tree=null;
        Object TAG41_tree=null;
        Object EQUAL42_tree=null;
        Object PROPERTY44_tree=null;
        Object EQUAL45_tree=null;
        Object VALUE47_tree=null;
        Object EQUAL48_tree=null;
        RewriteRuleTokenStream stream_PROPERTY=new RewriteRuleTokenStream(adaptor,"token PROPERTY");
        RewriteRuleTokenStream stream_EQUAL=new RewriteRuleTokenStream(adaptor,"token EQUAL");
        RewriteRuleTokenStream stream_VALUE=new RewriteRuleTokenStream(adaptor,"token VALUE");
        RewriteRuleTokenStream stream_TAG=new RewriteRuleTokenStream(adaptor,"token TAG");
        RewriteRuleSubtreeStream stream_phrase=new RewriteRuleSubtreeStream(adaptor,"rule phrase");
        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:192:2: ( PROPERTY EQUAL phrase ( VALUE EQUAL phrase )? -> ^( ND_PROPERTY phrase ( phrase )? ) | TAG EQUAL phrase PROPERTY EQUAL phrase ( VALUE EQUAL phrase )? -> ^( ND_TAGPROPERTY phrase phrase ( phrase )? ) )
            int alt10=2;
            int LA10_0 = input.LA(1);

            if ( (LA10_0==PROPERTY) ) {
                alt10=1;
            }
            else if ( (LA10_0==TAG) ) {
                alt10=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 10, 0, input);

                throw nvae;

            }
            switch (alt10) {
                case 1 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:192:4: PROPERTY EQUAL phrase ( VALUE EQUAL phrase )?
                    {
                    PROPERTY35=(Token)match(input,PROPERTY,FOLLOW_PROPERTY_in_propertyQuery601);  
                    stream_PROPERTY.add(PROPERTY35);


                    EQUAL36=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_propertyQuery603);  
                    stream_EQUAL.add(EQUAL36);


                    pushFollow(FOLLOW_phrase_in_propertyQuery605);
                    phrase37=phrase();

                    state._fsp--;

                    stream_phrase.add(phrase37.getTree());

                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:192:26: ( VALUE EQUAL phrase )?
                    int alt8=2;
                    int LA8_0 = input.LA(1);

                    if ( (LA8_0==VALUE) ) {
                        alt8=1;
                    }
                    switch (alt8) {
                        case 1 :
                            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:192:27: VALUE EQUAL phrase
                            {
                            VALUE38=(Token)match(input,VALUE,FOLLOW_VALUE_in_propertyQuery608);  
                            stream_VALUE.add(VALUE38);


                            EQUAL39=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_propertyQuery610);  
                            stream_EQUAL.add(EQUAL39);


                            pushFollow(FOLLOW_phrase_in_propertyQuery612);
                            phrase40=phrase();

                            state._fsp--;

                            stream_phrase.add(phrase40.getTree());

                            }
                            break;

                    }


                    // AST REWRITE
                    // elements: phrase, phrase
                    // token labels: 
                    // rule labels: retval
                    // token list labels: 
                    // rule list labels: 
                    // wildcard labels: 
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

                    root_0 = (Object)adaptor.nil();
                    // 192:48: -> ^( ND_PROPERTY phrase ( phrase )? )
                    {
                        // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:192:51: ^( ND_PROPERTY phrase ( phrase )? )
                        {
                        Object root_1 = (Object)adaptor.nil();
                        root_1 = (Object)adaptor.becomeRoot(
                        (Object)adaptor.create(ND_PROPERTY, "ND_PROPERTY")
                        , root_1);

                        adaptor.addChild(root_1, stream_phrase.nextTree());

                        // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:192:72: ( phrase )?
                        if ( stream_phrase.hasNext() ) {
                            adaptor.addChild(root_1, stream_phrase.nextTree());

                        }
                        stream_phrase.reset();

                        adaptor.addChild(root_0, root_1);
                        }

                    }


                    retval.tree = root_0;

                    }
                    break;
                case 2 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:193:4: TAG EQUAL phrase PROPERTY EQUAL phrase ( VALUE EQUAL phrase )?
                    {
                    TAG41=(Token)match(input,TAG,FOLLOW_TAG_in_propertyQuery631);  
                    stream_TAG.add(TAG41);


                    EQUAL42=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_propertyQuery633);  
                    stream_EQUAL.add(EQUAL42);


                    pushFollow(FOLLOW_phrase_in_propertyQuery635);
                    phrase43=phrase();

                    state._fsp--;

                    stream_phrase.add(phrase43.getTree());

                    PROPERTY44=(Token)match(input,PROPERTY,FOLLOW_PROPERTY_in_propertyQuery637);  
                    stream_PROPERTY.add(PROPERTY44);


                    EQUAL45=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_propertyQuery639);  
                    stream_EQUAL.add(EQUAL45);


                    pushFollow(FOLLOW_phrase_in_propertyQuery641);
                    phrase46=phrase();

                    state._fsp--;

                    stream_phrase.add(phrase46.getTree());

                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:193:43: ( VALUE EQUAL phrase )?
                    int alt9=2;
                    int LA9_0 = input.LA(1);

                    if ( (LA9_0==VALUE) ) {
                        alt9=1;
                    }
                    switch (alt9) {
                        case 1 :
                            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:193:44: VALUE EQUAL phrase
                            {
                            VALUE47=(Token)match(input,VALUE,FOLLOW_VALUE_in_propertyQuery644);  
                            stream_VALUE.add(VALUE47);


                            EQUAL48=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_propertyQuery646);  
                            stream_EQUAL.add(EQUAL48);


                            pushFollow(FOLLOW_phrase_in_propertyQuery648);
                            phrase49=phrase();

                            state._fsp--;

                            stream_phrase.add(phrase49.getTree());

                            }
                            break;

                    }


                    // AST REWRITE
                    // elements: phrase, phrase, phrase
                    // token labels: 
                    // rule labels: retval
                    // token list labels: 
                    // rule list labels: 
                    // wildcard labels: 
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

                    root_0 = (Object)adaptor.nil();
                    // 193:65: -> ^( ND_TAGPROPERTY phrase phrase ( phrase )? )
                    {
                        // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:193:68: ^( ND_TAGPROPERTY phrase phrase ( phrase )? )
                        {
                        Object root_1 = (Object)adaptor.nil();
                        root_1 = (Object)adaptor.becomeRoot(
                        (Object)adaptor.create(ND_TAGPROPERTY, "ND_TAGPROPERTY")
                        , root_1);

                        adaptor.addChild(root_1, stream_phrase.nextTree());

                        adaptor.addChild(root_1, stream_phrase.nextTree());

                        // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:193:99: ( phrase )?
                        if ( stream_phrase.hasNext() ) {
                            adaptor.addChild(root_1, stream_phrase.nextTree());

                        }
                        stream_phrase.reset();

                        adaptor.addChild(root_0, root_1);
                        }

                    }


                    retval.tree = root_0;

                    }
                    break;

            }
            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException e) {
            throw e;
        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "propertyQuery"


    public static class regQuery_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "regQuery"
    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:197:1: regQuery : REG EQUAL phrase ( 'CI' )? -> ^( ND_REG phrase ( 'CI' )? ) ;
    public final CatmaQueryParser.regQuery_return regQuery() throws RecognitionException {
        CatmaQueryParser.regQuery_return retval = new CatmaQueryParser.regQuery_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token REG50=null;
        Token EQUAL51=null;
        Token string_literal53=null;
        CatmaQueryParser.phrase_return phrase52 =null;


        Object REG50_tree=null;
        Object EQUAL51_tree=null;
        Object string_literal53_tree=null;
        RewriteRuleTokenStream stream_REG=new RewriteRuleTokenStream(adaptor,"token REG");
        RewriteRuleTokenStream stream_EQUAL=new RewriteRuleTokenStream(adaptor,"token EQUAL");
        RewriteRuleTokenStream stream_43=new RewriteRuleTokenStream(adaptor,"token 43");
        RewriteRuleSubtreeStream stream_phrase=new RewriteRuleSubtreeStream(adaptor,"rule phrase");
        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:198:2: ( REG EQUAL phrase ( 'CI' )? -> ^( ND_REG phrase ( 'CI' )? ) )
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:198:4: REG EQUAL phrase ( 'CI' )?
            {
            REG50=(Token)match(input,REG,FOLLOW_REG_in_regQuery681);  
            stream_REG.add(REG50);


            EQUAL51=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_regQuery683);  
            stream_EQUAL.add(EQUAL51);


            pushFollow(FOLLOW_phrase_in_regQuery685);
            phrase52=phrase();

            state._fsp--;

            stream_phrase.add(phrase52.getTree());

            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:198:21: ( 'CI' )?
            int alt11=2;
            int LA11_0 = input.LA(1);

            if ( (LA11_0==43) ) {
                alt11=1;
            }
            switch (alt11) {
                case 1 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:198:21: 'CI'
                    {
                    string_literal53=(Token)match(input,43,FOLLOW_43_in_regQuery687);  
                    stream_43.add(string_literal53);


                    }
                    break;

            }


            // AST REWRITE
            // elements: 43, phrase
            // token labels: 
            // rule labels: retval
            // token list labels: 
            // rule list labels: 
            // wildcard labels: 
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 198:27: -> ^( ND_REG phrase ( 'CI' )? )
            {
                // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:198:30: ^( ND_REG phrase ( 'CI' )? )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(ND_REG, "ND_REG")
                , root_1);

                adaptor.addChild(root_1, stream_phrase.nextTree());

                // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:198:46: ( 'CI' )?
                if ( stream_43.hasNext() ) {
                    adaptor.addChild(root_1, 
                    stream_43.nextNode()
                    );

                }
                stream_43.reset();

                adaptor.addChild(root_0, root_1);
                }

            }


            retval.tree = root_0;

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException e) {
            throw e;
        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "regQuery"


    public static class freqQuery_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "freqQuery"
    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:202:1: freqQuery : FREQ ( EQUAL INT ( '-' INT )? -> ^( ND_FREQ EQUAL INT ( INT )? ) | UNEQUAL INT -> ^( ND_FREQ UNEQUAL INT ) ) ;
    public final CatmaQueryParser.freqQuery_return freqQuery() throws RecognitionException {
        CatmaQueryParser.freqQuery_return retval = new CatmaQueryParser.freqQuery_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token FREQ54=null;
        Token EQUAL55=null;
        Token INT56=null;
        Token char_literal57=null;
        Token INT58=null;
        Token UNEQUAL59=null;
        Token INT60=null;

        Object FREQ54_tree=null;
        Object EQUAL55_tree=null;
        Object INT56_tree=null;
        Object char_literal57_tree=null;
        Object INT58_tree=null;
        Object UNEQUAL59_tree=null;
        Object INT60_tree=null;
        RewriteRuleTokenStream stream_UNEQUAL=new RewriteRuleTokenStream(adaptor,"token UNEQUAL");
        RewriteRuleTokenStream stream_EQUAL=new RewriteRuleTokenStream(adaptor,"token EQUAL");
        RewriteRuleTokenStream stream_FREQ=new RewriteRuleTokenStream(adaptor,"token FREQ");
        RewriteRuleTokenStream stream_41=new RewriteRuleTokenStream(adaptor,"token 41");
        RewriteRuleTokenStream stream_INT=new RewriteRuleTokenStream(adaptor,"token INT");

        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:203:2: ( FREQ ( EQUAL INT ( '-' INT )? -> ^( ND_FREQ EQUAL INT ( INT )? ) | UNEQUAL INT -> ^( ND_FREQ UNEQUAL INT ) ) )
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:203:4: FREQ ( EQUAL INT ( '-' INT )? -> ^( ND_FREQ EQUAL INT ( INT )? ) | UNEQUAL INT -> ^( ND_FREQ UNEQUAL INT ) )
            {
            FREQ54=(Token)match(input,FREQ,FOLLOW_FREQ_in_freqQuery718);  
            stream_FREQ.add(FREQ54);


            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:204:3: ( EQUAL INT ( '-' INT )? -> ^( ND_FREQ EQUAL INT ( INT )? ) | UNEQUAL INT -> ^( ND_FREQ UNEQUAL INT ) )
            int alt13=2;
            int LA13_0 = input.LA(1);

            if ( (LA13_0==EQUAL) ) {
                alt13=1;
            }
            else if ( (LA13_0==UNEQUAL) ) {
                alt13=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 13, 0, input);

                throw nvae;

            }
            switch (alt13) {
                case 1 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:204:5: EQUAL INT ( '-' INT )?
                    {
                    EQUAL55=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_freqQuery725);  
                    stream_EQUAL.add(EQUAL55);


                    INT56=(Token)match(input,INT,FOLLOW_INT_in_freqQuery727);  
                    stream_INT.add(INT56);


                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:204:15: ( '-' INT )?
                    int alt12=2;
                    int LA12_0 = input.LA(1);

                    if ( (LA12_0==41) ) {
                        int LA12_1 = input.LA(2);

                        if ( (LA12_1==INT) ) {
                            alt12=1;
                        }
                    }
                    switch (alt12) {
                        case 1 :
                            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:204:16: '-' INT
                            {
                            char_literal57=(Token)match(input,41,FOLLOW_41_in_freqQuery730);  
                            stream_41.add(char_literal57);


                            INT58=(Token)match(input,INT,FOLLOW_INT_in_freqQuery732);  
                            stream_INT.add(INT58);


                            }
                            break;

                    }


                    // AST REWRITE
                    // elements: EQUAL, INT, INT
                    // token labels: 
                    // rule labels: retval
                    // token list labels: 
                    // rule list labels: 
                    // wildcard labels: 
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

                    root_0 = (Object)adaptor.nil();
                    // 204:26: -> ^( ND_FREQ EQUAL INT ( INT )? )
                    {
                        // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:204:29: ^( ND_FREQ EQUAL INT ( INT )? )
                        {
                        Object root_1 = (Object)adaptor.nil();
                        root_1 = (Object)adaptor.becomeRoot(
                        (Object)adaptor.create(ND_FREQ, "ND_FREQ")
                        , root_1);

                        adaptor.addChild(root_1, 
                        stream_EQUAL.nextNode()
                        );

                        adaptor.addChild(root_1, 
                        stream_INT.nextNode()
                        );

                        // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:204:49: ( INT )?
                        if ( stream_INT.hasNext() ) {
                            adaptor.addChild(root_1, 
                            stream_INT.nextNode()
                            );

                        }
                        stream_INT.reset();

                        adaptor.addChild(root_0, root_1);
                        }

                    }


                    retval.tree = root_0;

                    }
                    break;
                case 2 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:205:5: UNEQUAL INT
                    {
                    UNEQUAL59=(Token)match(input,UNEQUAL,FOLLOW_UNEQUAL_in_freqQuery754);  
                    stream_UNEQUAL.add(UNEQUAL59);


                    INT60=(Token)match(input,INT,FOLLOW_INT_in_freqQuery756);  
                    stream_INT.add(INT60);


                    // AST REWRITE
                    // elements: UNEQUAL, INT
                    // token labels: 
                    // rule labels: retval
                    // token list labels: 
                    // rule list labels: 
                    // wildcard labels: 
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

                    root_0 = (Object)adaptor.nil();
                    // 205:17: -> ^( ND_FREQ UNEQUAL INT )
                    {
                        // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:205:20: ^( ND_FREQ UNEQUAL INT )
                        {
                        Object root_1 = (Object)adaptor.nil();
                        root_1 = (Object)adaptor.becomeRoot(
                        (Object)adaptor.create(ND_FREQ, "ND_FREQ")
                        , root_1);

                        adaptor.addChild(root_1, 
                        stream_UNEQUAL.nextNode()
                        );

                        adaptor.addChild(root_1, 
                        stream_INT.nextNode()
                        );

                        adaptor.addChild(root_0, root_1);
                        }

                    }


                    retval.tree = root_0;

                    }
                    break;

            }


            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException e) {
            throw e;
        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "freqQuery"


    public static class similQuery_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "similQuery"
    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:209:1: similQuery : SIMIL EQUAL phrase INT ( '%' )? -> ^( ND_SIMIL phrase INT ) ;
    public final CatmaQueryParser.similQuery_return similQuery() throws RecognitionException {
        CatmaQueryParser.similQuery_return retval = new CatmaQueryParser.similQuery_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token SIMIL61=null;
        Token EQUAL62=null;
        Token INT64=null;
        Token char_literal65=null;
        CatmaQueryParser.phrase_return phrase63 =null;


        Object SIMIL61_tree=null;
        Object EQUAL62_tree=null;
        Object INT64_tree=null;
        Object char_literal65_tree=null;
        RewriteRuleTokenStream stream_36=new RewriteRuleTokenStream(adaptor,"token 36");
        RewriteRuleTokenStream stream_EQUAL=new RewriteRuleTokenStream(adaptor,"token EQUAL");
        RewriteRuleTokenStream stream_SIMIL=new RewriteRuleTokenStream(adaptor,"token SIMIL");
        RewriteRuleTokenStream stream_INT=new RewriteRuleTokenStream(adaptor,"token INT");
        RewriteRuleSubtreeStream stream_phrase=new RewriteRuleSubtreeStream(adaptor,"rule phrase");
        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:210:2: ( SIMIL EQUAL phrase INT ( '%' )? -> ^( ND_SIMIL phrase INT ) )
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:210:4: SIMIL EQUAL phrase INT ( '%' )?
            {
            SIMIL61=(Token)match(input,SIMIL,FOLLOW_SIMIL_in_similQuery788);  
            stream_SIMIL.add(SIMIL61);


            EQUAL62=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_similQuery790);  
            stream_EQUAL.add(EQUAL62);


            pushFollow(FOLLOW_phrase_in_similQuery792);
            phrase63=phrase();

            state._fsp--;

            stream_phrase.add(phrase63.getTree());

            INT64=(Token)match(input,INT,FOLLOW_INT_in_similQuery794);  
            stream_INT.add(INT64);


            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:210:27: ( '%' )?
            int alt14=2;
            int LA14_0 = input.LA(1);

            if ( (LA14_0==36) ) {
                alt14=1;
            }
            switch (alt14) {
                case 1 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:210:27: '%'
                    {
                    char_literal65=(Token)match(input,36,FOLLOW_36_in_similQuery796);  
                    stream_36.add(char_literal65);


                    }
                    break;

            }


            // AST REWRITE
            // elements: phrase, INT
            // token labels: 
            // rule labels: retval
            // token list labels: 
            // rule list labels: 
            // wildcard labels: 
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 210:32: -> ^( ND_SIMIL phrase INT )
            {
                // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:210:35: ^( ND_SIMIL phrase INT )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(ND_SIMIL, "ND_SIMIL")
                , root_1);

                adaptor.addChild(root_1, stream_phrase.nextTree());

                adaptor.addChild(root_1, 
                stream_INT.nextNode()
                );

                adaptor.addChild(root_0, root_1);
                }

            }


            retval.tree = root_0;

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException e) {
            throw e;
        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "similQuery"


    public static class wildQuery_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "wildQuery"
    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:214:1: wildQuery : WILD EQUAL phrase -> ^( ND_WILD phrase ) ;
    public final CatmaQueryParser.wildQuery_return wildQuery() throws RecognitionException {
        CatmaQueryParser.wildQuery_return retval = new CatmaQueryParser.wildQuery_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token WILD66=null;
        Token EQUAL67=null;
        CatmaQueryParser.phrase_return phrase68 =null;


        Object WILD66_tree=null;
        Object EQUAL67_tree=null;
        RewriteRuleTokenStream stream_WILD=new RewriteRuleTokenStream(adaptor,"token WILD");
        RewriteRuleTokenStream stream_EQUAL=new RewriteRuleTokenStream(adaptor,"token EQUAL");
        RewriteRuleSubtreeStream stream_phrase=new RewriteRuleSubtreeStream(adaptor,"rule phrase");
        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:215:2: ( WILD EQUAL phrase -> ^( ND_WILD phrase ) )
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:215:4: WILD EQUAL phrase
            {
            WILD66=(Token)match(input,WILD,FOLLOW_WILD_in_wildQuery826);  
            stream_WILD.add(WILD66);


            EQUAL67=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_wildQuery828);  
            stream_EQUAL.add(EQUAL67);


            pushFollow(FOLLOW_phrase_in_wildQuery830);
            phrase68=phrase();

            state._fsp--;

            stream_phrase.add(phrase68.getTree());

            // AST REWRITE
            // elements: phrase
            // token labels: 
            // rule labels: retval
            // token list labels: 
            // rule list labels: 
            // wildcard labels: 
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 215:22: -> ^( ND_WILD phrase )
            {
                // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:215:25: ^( ND_WILD phrase )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(ND_WILD, "ND_WILD")
                , root_1);

                adaptor.addChild(root_1, stream_phrase.nextTree());

                adaptor.addChild(root_0, root_1);
                }

            }


            retval.tree = root_0;

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException e) {
            throw e;
        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "wildQuery"


    public static class refinement_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "refinement"
    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:225:1: refinement : 'where' refinementExpression -> refinementExpression ;
    public final CatmaQueryParser.refinement_return refinement() throws RecognitionException {
        CatmaQueryParser.refinement_return retval = new CatmaQueryParser.refinement_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token string_literal69=null;
        CatmaQueryParser.refinementExpression_return refinementExpression70 =null;


        Object string_literal69_tree=null;
        RewriteRuleTokenStream stream_45=new RewriteRuleTokenStream(adaptor,"token 45");
        RewriteRuleSubtreeStream stream_refinementExpression=new RewriteRuleSubtreeStream(adaptor,"rule refinementExpression");
        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:226:2: ( 'where' refinementExpression -> refinementExpression )
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:226:4: 'where' refinementExpression
            {
            string_literal69=(Token)match(input,45,FOLLOW_45_in_refinement863);  
            stream_45.add(string_literal69);


            pushFollow(FOLLOW_refinementExpression_in_refinement865);
            refinementExpression70=refinementExpression();

            state._fsp--;

            stream_refinementExpression.add(refinementExpression70.getTree());

            // AST REWRITE
            // elements: refinementExpression
            // token labels: 
            // rule labels: retval
            // token list labels: 
            // rule list labels: 
            // wildcard labels: 
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 226:33: -> refinementExpression
            {
                adaptor.addChild(root_0, stream_refinementExpression.nextTree());

            }


            retval.tree = root_0;

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException e) {
            throw e;
        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "refinement"


    public static class refinementExpression_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "refinementExpression"
    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:230:1: refinementExpression : startRefinement= term ( MATCH_MODE )? ( orRefinement[(CommonTree)$startRefinement.tree] -> orRefinement | andRefinement[(CommonTree)$startRefinement.tree] -> andRefinement | -> ^( ND_REFINE term ( MATCH_MODE )? ) ) ;
    public final CatmaQueryParser.refinementExpression_return refinementExpression() throws RecognitionException {
        CatmaQueryParser.refinementExpression_return retval = new CatmaQueryParser.refinementExpression_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token MATCH_MODE71=null;
        CatmaQueryParser.term_return startRefinement =null;

        CatmaQueryParser.orRefinement_return orRefinement72 =null;

        CatmaQueryParser.andRefinement_return andRefinement73 =null;


        Object MATCH_MODE71_tree=null;
        RewriteRuleTokenStream stream_MATCH_MODE=new RewriteRuleTokenStream(adaptor,"token MATCH_MODE");
        RewriteRuleSubtreeStream stream_andRefinement=new RewriteRuleSubtreeStream(adaptor,"rule andRefinement");
        RewriteRuleSubtreeStream stream_term=new RewriteRuleSubtreeStream(adaptor,"rule term");
        RewriteRuleSubtreeStream stream_orRefinement=new RewriteRuleSubtreeStream(adaptor,"rule orRefinement");
        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:231:2: (startRefinement= term ( MATCH_MODE )? ( orRefinement[(CommonTree)$startRefinement.tree] -> orRefinement | andRefinement[(CommonTree)$startRefinement.tree] -> andRefinement | -> ^( ND_REFINE term ( MATCH_MODE )? ) ) )
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:231:4: startRefinement= term ( MATCH_MODE )? ( orRefinement[(CommonTree)$startRefinement.tree] -> orRefinement | andRefinement[(CommonTree)$startRefinement.tree] -> andRefinement | -> ^( ND_REFINE term ( MATCH_MODE )? ) )
            {
            pushFollow(FOLLOW_term_in_refinementExpression889);
            startRefinement=term();

            state._fsp--;

            stream_term.add(startRefinement.getTree());

            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:231:25: ( MATCH_MODE )?
            int alt15=2;
            int LA15_0 = input.LA(1);

            if ( (LA15_0==MATCH_MODE) ) {
                alt15=1;
            }
            switch (alt15) {
                case 1 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:231:25: MATCH_MODE
                    {
                    MATCH_MODE71=(Token)match(input,MATCH_MODE,FOLLOW_MATCH_MODE_in_refinementExpression891);  
                    stream_MATCH_MODE.add(MATCH_MODE71);


                    }
                    break;

            }


            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:231:37: ( orRefinement[(CommonTree)$startRefinement.tree] -> orRefinement | andRefinement[(CommonTree)$startRefinement.tree] -> andRefinement | -> ^( ND_REFINE term ( MATCH_MODE )? ) )
            int alt16=3;
            switch ( input.LA(1) ) {
            case 46:
                {
                alt16=1;
                }
                break;
            case 40:
                {
                alt16=2;
                }
                break;
            case EOF:
            case 39:
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
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:232:4: orRefinement[(CommonTree)$startRefinement.tree]
                    {
                    pushFollow(FOLLOW_orRefinement_in_refinementExpression900);
                    orRefinement72=orRefinement((CommonTree)(startRefinement!=null?((Object)startRefinement.tree):null));

                    state._fsp--;

                    stream_orRefinement.add(orRefinement72.getTree());

                    // AST REWRITE
                    // elements: orRefinement
                    // token labels: 
                    // rule labels: retval
                    // token list labels: 
                    // rule list labels: 
                    // wildcard labels: 
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

                    root_0 = (Object)adaptor.nil();
                    // 232:52: -> orRefinement
                    {
                        adaptor.addChild(root_0, stream_orRefinement.nextTree());

                    }


                    retval.tree = root_0;

                    }
                    break;
                case 2 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:233:6: andRefinement[(CommonTree)$startRefinement.tree]
                    {
                    pushFollow(FOLLOW_andRefinement_in_refinementExpression912);
                    andRefinement73=andRefinement((CommonTree)(startRefinement!=null?((Object)startRefinement.tree):null));

                    state._fsp--;

                    stream_andRefinement.add(andRefinement73.getTree());

                    // AST REWRITE
                    // elements: andRefinement
                    // token labels: 
                    // rule labels: retval
                    // token list labels: 
                    // rule list labels: 
                    // wildcard labels: 
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

                    root_0 = (Object)adaptor.nil();
                    // 233:55: -> andRefinement
                    {
                        adaptor.addChild(root_0, stream_andRefinement.nextTree());

                    }


                    retval.tree = root_0;

                    }
                    break;
                case 3 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:234:6: 
                    {
                    // AST REWRITE
                    // elements: term, MATCH_MODE
                    // token labels: 
                    // rule labels: retval
                    // token list labels: 
                    // rule list labels: 
                    // wildcard labels: 
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

                    root_0 = (Object)adaptor.nil();
                    // 234:6: -> ^( ND_REFINE term ( MATCH_MODE )? )
                    {
                        // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:234:9: ^( ND_REFINE term ( MATCH_MODE )? )
                        {
                        Object root_1 = (Object)adaptor.nil();
                        root_1 = (Object)adaptor.becomeRoot(
                        (Object)adaptor.create(ND_REFINE, "ND_REFINE")
                        , root_1);

                        adaptor.addChild(root_1, stream_term.nextTree());

                        // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:234:26: ( MATCH_MODE )?
                        if ( stream_MATCH_MODE.hasNext() ) {
                            adaptor.addChild(root_1, 
                            stream_MATCH_MODE.nextNode()
                            );

                        }
                        stream_MATCH_MODE.reset();

                        adaptor.addChild(root_0, root_1);
                        }

                    }


                    retval.tree = root_0;

                    }
                    break;

            }


            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException e) {
            throw e;
        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "refinementExpression"


    public static class orRefinement_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "orRefinement"
    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:238:1: orRefinement[CommonTree startRefinement] : '|' term ( MATCH_MODE )? -> ^( ND_ORREFINE term ( MATCH_MODE )? ) ;
    public final CatmaQueryParser.orRefinement_return orRefinement(CommonTree startRefinement) throws RecognitionException {
        CatmaQueryParser.orRefinement_return retval = new CatmaQueryParser.orRefinement_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token char_literal74=null;
        Token MATCH_MODE76=null;
        CatmaQueryParser.term_return term75 =null;


        Object char_literal74_tree=null;
        Object MATCH_MODE76_tree=null;
        RewriteRuleTokenStream stream_46=new RewriteRuleTokenStream(adaptor,"token 46");
        RewriteRuleTokenStream stream_MATCH_MODE=new RewriteRuleTokenStream(adaptor,"token MATCH_MODE");
        RewriteRuleSubtreeStream stream_term=new RewriteRuleSubtreeStream(adaptor,"rule term");
        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:239:2: ( '|' term ( MATCH_MODE )? -> ^( ND_ORREFINE term ( MATCH_MODE )? ) )
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:239:4: '|' term ( MATCH_MODE )?
            {
            char_literal74=(Token)match(input,46,FOLLOW_46_in_orRefinement954);  
            stream_46.add(char_literal74);


            pushFollow(FOLLOW_term_in_orRefinement956);
            term75=term();

            state._fsp--;

            stream_term.add(term75.getTree());

            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:239:13: ( MATCH_MODE )?
            int alt17=2;
            int LA17_0 = input.LA(1);

            if ( (LA17_0==MATCH_MODE) ) {
                alt17=1;
            }
            switch (alt17) {
                case 1 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:239:13: MATCH_MODE
                    {
                    MATCH_MODE76=(Token)match(input,MATCH_MODE,FOLLOW_MATCH_MODE_in_orRefinement958);  
                    stream_MATCH_MODE.add(MATCH_MODE76);


                    }
                    break;

            }


            // AST REWRITE
            // elements: MATCH_MODE, term
            // token labels: 
            // rule labels: retval
            // token list labels: 
            // rule list labels: 
            // wildcard labels: 
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 239:25: -> ^( ND_ORREFINE term ( MATCH_MODE )? )
            {
                // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:239:28: ^( ND_ORREFINE term ( MATCH_MODE )? )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(ND_ORREFINE, "ND_ORREFINE")
                , root_1);

                adaptor.addChild(root_1, startRefinement);

                adaptor.addChild(root_1, stream_term.nextTree());

                // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:239:66: ( MATCH_MODE )?
                if ( stream_MATCH_MODE.hasNext() ) {
                    adaptor.addChild(root_1, 
                    stream_MATCH_MODE.nextNode()
                    );

                }
                stream_MATCH_MODE.reset();

                adaptor.addChild(root_0, root_1);
                }

            }


            retval.tree = root_0;

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException e) {
            throw e;
        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "orRefinement"


    public static class andRefinement_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "andRefinement"
    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:243:1: andRefinement[CommonTree startRefinement] : ',' term ( MATCH_MODE )? -> ^( ND_ANDREFINE term ( MATCH_MODE )? ) ;
    public final CatmaQueryParser.andRefinement_return andRefinement(CommonTree startRefinement) throws RecognitionException {
        CatmaQueryParser.andRefinement_return retval = new CatmaQueryParser.andRefinement_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token char_literal77=null;
        Token MATCH_MODE79=null;
        CatmaQueryParser.term_return term78 =null;


        Object char_literal77_tree=null;
        Object MATCH_MODE79_tree=null;
        RewriteRuleTokenStream stream_MATCH_MODE=new RewriteRuleTokenStream(adaptor,"token MATCH_MODE");
        RewriteRuleTokenStream stream_40=new RewriteRuleTokenStream(adaptor,"token 40");
        RewriteRuleSubtreeStream stream_term=new RewriteRuleSubtreeStream(adaptor,"rule term");
        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:244:2: ( ',' term ( MATCH_MODE )? -> ^( ND_ANDREFINE term ( MATCH_MODE )? ) )
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:244:4: ',' term ( MATCH_MODE )?
            {
            char_literal77=(Token)match(input,40,FOLLOW_40_in_andRefinement992);  
            stream_40.add(char_literal77);


            pushFollow(FOLLOW_term_in_andRefinement994);
            term78=term();

            state._fsp--;

            stream_term.add(term78.getTree());

            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:244:13: ( MATCH_MODE )?
            int alt18=2;
            int LA18_0 = input.LA(1);

            if ( (LA18_0==MATCH_MODE) ) {
                alt18=1;
            }
            switch (alt18) {
                case 1 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:244:13: MATCH_MODE
                    {
                    MATCH_MODE79=(Token)match(input,MATCH_MODE,FOLLOW_MATCH_MODE_in_andRefinement996);  
                    stream_MATCH_MODE.add(MATCH_MODE79);


                    }
                    break;

            }


            // AST REWRITE
            // elements: term, MATCH_MODE
            // token labels: 
            // rule labels: retval
            // token list labels: 
            // rule list labels: 
            // wildcard labels: 
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 244:24: -> ^( ND_ANDREFINE term ( MATCH_MODE )? )
            {
                // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:244:27: ^( ND_ANDREFINE term ( MATCH_MODE )? )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(ND_ANDREFINE, "ND_ANDREFINE")
                , root_1);

                adaptor.addChild(root_1, startRefinement);

                adaptor.addChild(root_1, stream_term.nextTree());

                // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:244:66: ( MATCH_MODE )?
                if ( stream_MATCH_MODE.hasNext() ) {
                    adaptor.addChild(root_1, 
                    stream_MATCH_MODE.nextNode()
                    );

                }
                stream_MATCH_MODE.reset();

                adaptor.addChild(root_0, root_1);
                }

            }


            retval.tree = root_0;

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException e) {
            throw e;
        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "andRefinement"

    // Delegated rules


 

    public static final BitSet FOLLOW_query_in_start151 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_start153 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_queryExpression_in_query169 = new BitSet(new long[]{0x0000200000000002L});
    public static final BitSet FOLLOW_refinement_in_query171 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_term_in_queryExpression208 = new BitSet(new long[]{0x0000072000000002L});
    public static final BitSet FOLLOW_unionQuery_in_queryExpression215 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_collocQuery_in_queryExpression227 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_exclusionQuery_in_queryExpression240 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_adjacencyQuery_in_queryExpression253 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_40_in_unionQuery296 = new BitSet(new long[]{0x00000048F8000020L});
    public static final BitSet FOLLOW_term_in_unionQuery298 = new BitSet(new long[]{0x0000100000000002L});
    public static final BitSet FOLLOW_44_in_unionQuery300 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_37_in_collocQuery335 = new BitSet(new long[]{0x00000048F8000020L});
    public static final BitSet FOLLOW_term_in_collocQuery337 = new BitSet(new long[]{0x0000000000000082L});
    public static final BitSet FOLLOW_INT_in_collocQuery339 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_41_in_exclusionQuery373 = new BitSet(new long[]{0x00000048F8000020L});
    public static final BitSet FOLLOW_term_in_exclusionQuery375 = new BitSet(new long[]{0x0000000000000402L});
    public static final BitSet FOLLOW_MATCH_MODE_in_exclusionQuery377 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_42_in_adjacencyQuery411 = new BitSet(new long[]{0x00000048F8000020L});
    public static final BitSet FOLLOW_term_in_adjacencyQuery413 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_phrase_in_term443 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_selector_in_term453 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_38_in_term464 = new BitSet(new long[]{0x00000048F8000020L});
    public static final BitSet FOLLOW_query_in_term465 = new BitSet(new long[]{0x0000008000000000L});
    public static final BitSet FOLLOW_39_in_term466 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_TXT_in_phrase490 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_tagQuery_in_selector523 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_propertyQuery_in_selector528 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_regQuery_in_selector533 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_freqQuery_in_selector538 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_similQuery_in_selector543 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_wildQuery_in_selector548 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_TAG_in_tagQuery568 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_EQUAL_in_tagQuery570 = new BitSet(new long[]{0x0000000080000000L});
    public static final BitSet FOLLOW_phrase_in_tagQuery572 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_PROPERTY_in_propertyQuery601 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_EQUAL_in_propertyQuery603 = new BitSet(new long[]{0x0000000080000000L});
    public static final BitSet FOLLOW_phrase_in_propertyQuery605 = new BitSet(new long[]{0x0000000200000002L});
    public static final BitSet FOLLOW_VALUE_in_propertyQuery608 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_EQUAL_in_propertyQuery610 = new BitSet(new long[]{0x0000000080000000L});
    public static final BitSet FOLLOW_phrase_in_propertyQuery612 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_TAG_in_propertyQuery631 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_EQUAL_in_propertyQuery633 = new BitSet(new long[]{0x0000000080000000L});
    public static final BitSet FOLLOW_phrase_in_propertyQuery635 = new BitSet(new long[]{0x0000000008000000L});
    public static final BitSet FOLLOW_PROPERTY_in_propertyQuery637 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_EQUAL_in_propertyQuery639 = new BitSet(new long[]{0x0000000080000000L});
    public static final BitSet FOLLOW_phrase_in_propertyQuery641 = new BitSet(new long[]{0x0000000200000002L});
    public static final BitSet FOLLOW_VALUE_in_propertyQuery644 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_EQUAL_in_propertyQuery646 = new BitSet(new long[]{0x0000000080000000L});
    public static final BitSet FOLLOW_phrase_in_propertyQuery648 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_REG_in_regQuery681 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_EQUAL_in_regQuery683 = new BitSet(new long[]{0x0000000080000000L});
    public static final BitSet FOLLOW_phrase_in_regQuery685 = new BitSet(new long[]{0x0000080000000002L});
    public static final BitSet FOLLOW_43_in_regQuery687 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_FREQ_in_freqQuery718 = new BitSet(new long[]{0x0000000100000010L});
    public static final BitSet FOLLOW_EQUAL_in_freqQuery725 = new BitSet(new long[]{0x0000000000000080L});
    public static final BitSet FOLLOW_INT_in_freqQuery727 = new BitSet(new long[]{0x0000020000000002L});
    public static final BitSet FOLLOW_41_in_freqQuery730 = new BitSet(new long[]{0x0000000000000080L});
    public static final BitSet FOLLOW_INT_in_freqQuery732 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_UNEQUAL_in_freqQuery754 = new BitSet(new long[]{0x0000000000000080L});
    public static final BitSet FOLLOW_INT_in_freqQuery756 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_SIMIL_in_similQuery788 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_EQUAL_in_similQuery790 = new BitSet(new long[]{0x0000000080000000L});
    public static final BitSet FOLLOW_phrase_in_similQuery792 = new BitSet(new long[]{0x0000000000000080L});
    public static final BitSet FOLLOW_INT_in_similQuery794 = new BitSet(new long[]{0x0000001000000002L});
    public static final BitSet FOLLOW_36_in_similQuery796 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_WILD_in_wildQuery826 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_EQUAL_in_wildQuery828 = new BitSet(new long[]{0x0000000080000000L});
    public static final BitSet FOLLOW_phrase_in_wildQuery830 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_45_in_refinement863 = new BitSet(new long[]{0x00000048F8000020L});
    public static final BitSet FOLLOW_refinementExpression_in_refinement865 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_term_in_refinementExpression889 = new BitSet(new long[]{0x0000410000000402L});
    public static final BitSet FOLLOW_MATCH_MODE_in_refinementExpression891 = new BitSet(new long[]{0x0000410000000002L});
    public static final BitSet FOLLOW_orRefinement_in_refinementExpression900 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_andRefinement_in_refinementExpression912 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_46_in_orRefinement954 = new BitSet(new long[]{0x00000048F8000020L});
    public static final BitSet FOLLOW_term_in_orRefinement956 = new BitSet(new long[]{0x0000000000000402L});
    public static final BitSet FOLLOW_MATCH_MODE_in_orRefinement958 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_40_in_andRefinement992 = new BitSet(new long[]{0x00000048F8000020L});
    public static final BitSet FOLLOW_term_in_andRefinement994 = new BitSet(new long[]{0x0000000000000402L});
    public static final BitSet FOLLOW_MATCH_MODE_in_andRefinement996 = new BitSet(new long[]{0x0000000000000002L});

}