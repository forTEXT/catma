// $ANTLR 3.4 C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g 2014-09-22 22:06:46

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
        "<invalid>", "<EOR>", "<DOWN>", "<UP>", "EQUAL", "FREQ", "GROUPIDENT", "INT", "LETTER", "LETTEREXTENDED", "ND_ADJACENCY", "ND_ANDREFINE", "ND_COLLOC", "ND_EXCLUSION", "ND_FREQ", "ND_ORREFINE", "ND_PHRASE", "ND_PROPERTY", "ND_QUERY", "ND_REFINE", "ND_REG", "ND_SIMIL", "ND_TAG", "ND_TAGPROPERTY", "ND_UNION", "ND_WILD", "PROPERTY", "REG", "SIMIL", "TAG", "TAG_MATCH_MODE", "TXT", "UNEQUAL", "VALUE", "WHITESPACE", "WILD", "'%'", "'&'", "'('", "')'", "','", "'-'", "';'", "'CI'", "'EXCL'", "'where'", "'|'"
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


        RewriteRuleSubtreeStream stream_refinement=new RewriteRuleSubtreeStream(adaptor,"rule refinement");
        RewriteRuleSubtreeStream stream_queryExpression=new RewriteRuleSubtreeStream(adaptor,"rule queryExpression");
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


        RewriteRuleSubtreeStream stream_adjacencyQuery=new RewriteRuleSubtreeStream(adaptor,"rule adjacencyQuery");
        RewriteRuleSubtreeStream stream_unionQuery=new RewriteRuleSubtreeStream(adaptor,"rule unionQuery");
        RewriteRuleSubtreeStream stream_term=new RewriteRuleSubtreeStream(adaptor,"rule term");
        RewriteRuleSubtreeStream stream_exclusionQuery=new RewriteRuleSubtreeStream(adaptor,"rule exclusionQuery");
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
        RewriteRuleTokenStream stream_INT=new RewriteRuleTokenStream(adaptor,"token INT");
        RewriteRuleTokenStream stream_37=new RewriteRuleTokenStream(adaptor,"token 37");
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
    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:144:1: exclusionQuery[CommonTree startTerm] : '-' term -> ^( ND_EXCLUSION term ) ;
    public final CatmaQueryParser.exclusionQuery_return exclusionQuery(CommonTree startTerm) throws RecognitionException {
        CatmaQueryParser.exclusionQuery_return retval = new CatmaQueryParser.exclusionQuery_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token char_literal15=null;
        CatmaQueryParser.term_return term16 =null;


        Object char_literal15_tree=null;
        RewriteRuleTokenStream stream_41=new RewriteRuleTokenStream(adaptor,"token 41");
        RewriteRuleSubtreeStream stream_term=new RewriteRuleSubtreeStream(adaptor,"rule term");
        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:145:2: ( '-' term -> ^( ND_EXCLUSION term ) )
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:145:4: '-' term
            {
            char_literal15=(Token)match(input,41,FOLLOW_41_in_exclusionQuery373);  
            stream_41.add(char_literal15);


            pushFollow(FOLLOW_term_in_exclusionQuery375);
            term16=term();

            state._fsp--;

            stream_term.add(term16.getTree());

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
            // 145:13: -> ^( ND_EXCLUSION term )
            {
                // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:145:16: ^( ND_EXCLUSION term )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(ND_EXCLUSION, "ND_EXCLUSION")
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

        Token char_literal17=null;
        CatmaQueryParser.term_return term18 =null;


        Object char_literal17_tree=null;
        RewriteRuleTokenStream stream_42=new RewriteRuleTokenStream(adaptor,"token 42");
        RewriteRuleSubtreeStream stream_term=new RewriteRuleSubtreeStream(adaptor,"rule term");
        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:150:2: ( ';' term -> ^( ND_ADJACENCY term ) )
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:150:4: ';' term
            {
            char_literal17=(Token)match(input,42,FOLLOW_42_in_adjacencyQuery406);  
            stream_42.add(char_literal17);


            pushFollow(FOLLOW_term_in_adjacencyQuery408);
            term18=term();

            state._fsp--;

            stream_term.add(term18.getTree());

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

        Token char_literal21=null;
        Token char_literal23=null;
        CatmaQueryParser.phrase_return phrase19 =null;

        CatmaQueryParser.selector_return selector20 =null;

        CatmaQueryParser.query_return query22 =null;


        Object char_literal21_tree=null;
        Object char_literal23_tree=null;
        RewriteRuleTokenStream stream_39=new RewriteRuleTokenStream(adaptor,"token 39");
        RewriteRuleTokenStream stream_38=new RewriteRuleTokenStream(adaptor,"token 38");
        RewriteRuleSubtreeStream stream_selector=new RewriteRuleSubtreeStream(adaptor,"rule selector");
        RewriteRuleSubtreeStream stream_query=new RewriteRuleSubtreeStream(adaptor,"rule query");
        RewriteRuleSubtreeStream stream_phrase=new RewriteRuleSubtreeStream(adaptor,"rule phrase");
        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:155:7: ( phrase -> phrase | selector -> selector | '(' query ')' -> query )
            int alt5=3;
            switch ( input.LA(1) ) {
            case TXT:
                {
                alt5=1;
                }
                break;
            case FREQ:
            case PROPERTY:
            case REG:
            case SIMIL:
            case TAG:
            case WILD:
                {
                alt5=2;
                }
                break;
            case 38:
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
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:155:9: phrase
                    {
                    pushFollow(FOLLOW_phrase_in_term438);
                    phrase19=phrase();

                    state._fsp--;

                    stream_phrase.add(phrase19.getTree());

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
                    pushFollow(FOLLOW_selector_in_term448);
                    selector20=selector();

                    state._fsp--;

                    stream_selector.add(selector20.getTree());

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
                    char_literal21=(Token)match(input,38,FOLLOW_38_in_term459);  
                    stream_38.add(char_literal21);


                    pushFollow(FOLLOW_query_in_term460);
                    query22=query();

                    state._fsp--;

                    stream_query.add(query22.getTree());

                    char_literal23=(Token)match(input,39,FOLLOW_39_in_term461);  
                    stream_39.add(char_literal23);


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

        Token TXT24=null;

        Object TXT24_tree=null;
        RewriteRuleTokenStream stream_TXT=new RewriteRuleTokenStream(adaptor,"token TXT");

        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:164:8: ( TXT -> ^( ND_PHRASE TXT ) )
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:164:10: TXT
            {
            TXT24=(Token)match(input,TXT,FOLLOW_TXT_in_phrase485);  
            stream_TXT.add(TXT24);


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

        CatmaQueryParser.tagQuery_return tagQuery25 =null;

        CatmaQueryParser.propertyQuery_return propertyQuery26 =null;

        CatmaQueryParser.regQuery_return regQuery27 =null;

        CatmaQueryParser.freqQuery_return freqQuery28 =null;

        CatmaQueryParser.similQuery_return similQuery29 =null;

        CatmaQueryParser.wildQuery_return wildQuery30 =null;



        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:175:2: ( tagQuery | propertyQuery | regQuery | freqQuery | similQuery | wildQuery )
            int alt6=6;
            switch ( input.LA(1) ) {
            case TAG:
                {
                int LA6_1 = input.LA(2);

                if ( (LA6_1==EQUAL) ) {
                    int LA6_7 = input.LA(3);

                    if ( (LA6_7==TXT) ) {
                        int LA6_8 = input.LA(4);

                        if ( (LA6_8==EOF||LA6_8==INT||LA6_8==TAG_MATCH_MODE||LA6_8==37||(LA6_8 >= 39 && LA6_8 <= 42)||(LA6_8 >= 44 && LA6_8 <= 46)) ) {
                            alt6=1;
                        }
                        else if ( (LA6_8==PROPERTY) ) {
                            alt6=2;
                        }
                        else {
                            NoViableAltException nvae =
                                new NoViableAltException("", 6, 8, input);

                            throw nvae;

                        }
                    }
                    else {
                        NoViableAltException nvae =
                            new NoViableAltException("", 6, 7, input);

                        throw nvae;

                    }
                }
                else {
                    NoViableAltException nvae =
                        new NoViableAltException("", 6, 1, input);

                    throw nvae;

                }
                }
                break;
            case PROPERTY:
                {
                alt6=2;
                }
                break;
            case REG:
                {
                alt6=3;
                }
                break;
            case FREQ:
                {
                alt6=4;
                }
                break;
            case SIMIL:
                {
                alt6=5;
                }
                break;
            case WILD:
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
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:175:4: tagQuery
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_tagQuery_in_selector518);
                    tagQuery25=tagQuery();

                    state._fsp--;

                    adaptor.addChild(root_0, tagQuery25.getTree());

                    }
                    break;
                case 2 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:176:4: propertyQuery
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_propertyQuery_in_selector523);
                    propertyQuery26=propertyQuery();

                    state._fsp--;

                    adaptor.addChild(root_0, propertyQuery26.getTree());

                    }
                    break;
                case 3 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:177:4: regQuery
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_regQuery_in_selector528);
                    regQuery27=regQuery();

                    state._fsp--;

                    adaptor.addChild(root_0, regQuery27.getTree());

                    }
                    break;
                case 4 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:178:4: freqQuery
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_freqQuery_in_selector533);
                    freqQuery28=freqQuery();

                    state._fsp--;

                    adaptor.addChild(root_0, freqQuery28.getTree());

                    }
                    break;
                case 5 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:179:4: similQuery
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_similQuery_in_selector538);
                    similQuery29=similQuery();

                    state._fsp--;

                    adaptor.addChild(root_0, similQuery29.getTree());

                    }
                    break;
                case 6 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:180:4: wildQuery
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_wildQuery_in_selector543);
                    wildQuery30=wildQuery();

                    state._fsp--;

                    adaptor.addChild(root_0, wildQuery30.getTree());

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
    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:185:1: tagQuery : TAG EQUAL phrase ( TAG_MATCH_MODE )? -> ^( ND_TAG phrase ( TAG_MATCH_MODE )? ) ;
    public final CatmaQueryParser.tagQuery_return tagQuery() throws RecognitionException {
        CatmaQueryParser.tagQuery_return retval = new CatmaQueryParser.tagQuery_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token TAG31=null;
        Token EQUAL32=null;
        Token TAG_MATCH_MODE34=null;
        CatmaQueryParser.phrase_return phrase33 =null;


        Object TAG31_tree=null;
        Object EQUAL32_tree=null;
        Object TAG_MATCH_MODE34_tree=null;
        RewriteRuleTokenStream stream_TAG_MATCH_MODE=new RewriteRuleTokenStream(adaptor,"token TAG_MATCH_MODE");
        RewriteRuleTokenStream stream_EQUAL=new RewriteRuleTokenStream(adaptor,"token EQUAL");
        RewriteRuleTokenStream stream_TAG=new RewriteRuleTokenStream(adaptor,"token TAG");
        RewriteRuleSubtreeStream stream_phrase=new RewriteRuleSubtreeStream(adaptor,"rule phrase");
        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:186:2: ( TAG EQUAL phrase ( TAG_MATCH_MODE )? -> ^( ND_TAG phrase ( TAG_MATCH_MODE )? ) )
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:186:4: TAG EQUAL phrase ( TAG_MATCH_MODE )?
            {
            TAG31=(Token)match(input,TAG,FOLLOW_TAG_in_tagQuery563);  
            stream_TAG.add(TAG31);


            EQUAL32=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_tagQuery565);  
            stream_EQUAL.add(EQUAL32);


            pushFollow(FOLLOW_phrase_in_tagQuery567);
            phrase33=phrase();

            state._fsp--;

            stream_phrase.add(phrase33.getTree());

            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:186:21: ( TAG_MATCH_MODE )?
            int alt7=2;
            int LA7_0 = input.LA(1);

            if ( (LA7_0==TAG_MATCH_MODE) ) {
                alt7=1;
            }
            switch (alt7) {
                case 1 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:186:21: TAG_MATCH_MODE
                    {
                    TAG_MATCH_MODE34=(Token)match(input,TAG_MATCH_MODE,FOLLOW_TAG_MATCH_MODE_in_tagQuery569);  
                    stream_TAG_MATCH_MODE.add(TAG_MATCH_MODE34);


                    }
                    break;

            }


            // AST REWRITE
            // elements: TAG_MATCH_MODE, phrase
            // token labels: 
            // rule labels: retval
            // token list labels: 
            // rule list labels: 
            // wildcard labels: 
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 186:37: -> ^( ND_TAG phrase ( TAG_MATCH_MODE )? )
            {
                // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:186:40: ^( ND_TAG phrase ( TAG_MATCH_MODE )? )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(ND_TAG, "ND_TAG")
                , root_1);

                adaptor.addChild(root_1, stream_phrase.nextTree());

                // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:186:56: ( TAG_MATCH_MODE )?
                if ( stream_TAG_MATCH_MODE.hasNext() ) {
                    adaptor.addChild(root_1, 
                    stream_TAG_MATCH_MODE.nextNode()
                    );

                }
                stream_TAG_MATCH_MODE.reset();

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
    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:191:1: propertyQuery : ( PROPERTY EQUAL phrase ( VALUE EQUAL phrase )? ( TAG_MATCH_MODE )? -> ^( ND_PROPERTY phrase ( phrase )? ( TAG_MATCH_MODE )? ) | TAG EQUAL phrase PROPERTY EQUAL phrase ( VALUE EQUAL phrase )? ( TAG_MATCH_MODE )? -> ^( ND_TAGPROPERTY phrase phrase ( phrase )? ( TAG_MATCH_MODE )? ) );
    public final CatmaQueryParser.propertyQuery_return propertyQuery() throws RecognitionException {
        CatmaQueryParser.propertyQuery_return retval = new CatmaQueryParser.propertyQuery_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token PROPERTY35=null;
        Token EQUAL36=null;
        Token VALUE38=null;
        Token EQUAL39=null;
        Token TAG_MATCH_MODE41=null;
        Token TAG42=null;
        Token EQUAL43=null;
        Token PROPERTY45=null;
        Token EQUAL46=null;
        Token VALUE48=null;
        Token EQUAL49=null;
        Token TAG_MATCH_MODE51=null;
        CatmaQueryParser.phrase_return phrase37 =null;

        CatmaQueryParser.phrase_return phrase40 =null;

        CatmaQueryParser.phrase_return phrase44 =null;

        CatmaQueryParser.phrase_return phrase47 =null;

        CatmaQueryParser.phrase_return phrase50 =null;


        Object PROPERTY35_tree=null;
        Object EQUAL36_tree=null;
        Object VALUE38_tree=null;
        Object EQUAL39_tree=null;
        Object TAG_MATCH_MODE41_tree=null;
        Object TAG42_tree=null;
        Object EQUAL43_tree=null;
        Object PROPERTY45_tree=null;
        Object EQUAL46_tree=null;
        Object VALUE48_tree=null;
        Object EQUAL49_tree=null;
        Object TAG_MATCH_MODE51_tree=null;
        RewriteRuleTokenStream stream_TAG_MATCH_MODE=new RewriteRuleTokenStream(adaptor,"token TAG_MATCH_MODE");
        RewriteRuleTokenStream stream_VALUE=new RewriteRuleTokenStream(adaptor,"token VALUE");
        RewriteRuleTokenStream stream_PROPERTY=new RewriteRuleTokenStream(adaptor,"token PROPERTY");
        RewriteRuleTokenStream stream_EQUAL=new RewriteRuleTokenStream(adaptor,"token EQUAL");
        RewriteRuleTokenStream stream_TAG=new RewriteRuleTokenStream(adaptor,"token TAG");
        RewriteRuleSubtreeStream stream_phrase=new RewriteRuleSubtreeStream(adaptor,"rule phrase");
        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:192:2: ( PROPERTY EQUAL phrase ( VALUE EQUAL phrase )? ( TAG_MATCH_MODE )? -> ^( ND_PROPERTY phrase ( phrase )? ( TAG_MATCH_MODE )? ) | TAG EQUAL phrase PROPERTY EQUAL phrase ( VALUE EQUAL phrase )? ( TAG_MATCH_MODE )? -> ^( ND_TAGPROPERTY phrase phrase ( phrase )? ( TAG_MATCH_MODE )? ) )
            int alt12=2;
            int LA12_0 = input.LA(1);

            if ( (LA12_0==PROPERTY) ) {
                alt12=1;
            }
            else if ( (LA12_0==TAG) ) {
                alt12=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 12, 0, input);

                throw nvae;

            }
            switch (alt12) {
                case 1 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:192:4: PROPERTY EQUAL phrase ( VALUE EQUAL phrase )? ( TAG_MATCH_MODE )?
                    {
                    PROPERTY35=(Token)match(input,PROPERTY,FOLLOW_PROPERTY_in_propertyQuery602);  
                    stream_PROPERTY.add(PROPERTY35);


                    EQUAL36=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_propertyQuery604);  
                    stream_EQUAL.add(EQUAL36);


                    pushFollow(FOLLOW_phrase_in_propertyQuery606);
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
                            VALUE38=(Token)match(input,VALUE,FOLLOW_VALUE_in_propertyQuery609);  
                            stream_VALUE.add(VALUE38);


                            EQUAL39=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_propertyQuery611);  
                            stream_EQUAL.add(EQUAL39);


                            pushFollow(FOLLOW_phrase_in_propertyQuery613);
                            phrase40=phrase();

                            state._fsp--;

                            stream_phrase.add(phrase40.getTree());

                            }
                            break;

                    }


                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:192:48: ( TAG_MATCH_MODE )?
                    int alt9=2;
                    int LA9_0 = input.LA(1);

                    if ( (LA9_0==TAG_MATCH_MODE) ) {
                        alt9=1;
                    }
                    switch (alt9) {
                        case 1 :
                            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:192:48: TAG_MATCH_MODE
                            {
                            TAG_MATCH_MODE41=(Token)match(input,TAG_MATCH_MODE,FOLLOW_TAG_MATCH_MODE_in_propertyQuery617);  
                            stream_TAG_MATCH_MODE.add(TAG_MATCH_MODE41);


                            }
                            break;

                    }


                    // AST REWRITE
                    // elements: phrase, TAG_MATCH_MODE, phrase
                    // token labels: 
                    // rule labels: retval
                    // token list labels: 
                    // rule list labels: 
                    // wildcard labels: 
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

                    root_0 = (Object)adaptor.nil();
                    // 192:64: -> ^( ND_PROPERTY phrase ( phrase )? ( TAG_MATCH_MODE )? )
                    {
                        // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:192:67: ^( ND_PROPERTY phrase ( phrase )? ( TAG_MATCH_MODE )? )
                        {
                        Object root_1 = (Object)adaptor.nil();
                        root_1 = (Object)adaptor.becomeRoot(
                        (Object)adaptor.create(ND_PROPERTY, "ND_PROPERTY")
                        , root_1);

                        adaptor.addChild(root_1, stream_phrase.nextTree());

                        // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:192:88: ( phrase )?
                        if ( stream_phrase.hasNext() ) {
                            adaptor.addChild(root_1, stream_phrase.nextTree());

                        }
                        stream_phrase.reset();

                        // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:192:96: ( TAG_MATCH_MODE )?
                        if ( stream_TAG_MATCH_MODE.hasNext() ) {
                            adaptor.addChild(root_1, 
                            stream_TAG_MATCH_MODE.nextNode()
                            );

                        }
                        stream_TAG_MATCH_MODE.reset();

                        adaptor.addChild(root_0, root_1);
                        }

                    }


                    retval.tree = root_0;

                    }
                    break;
                case 2 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:193:4: TAG EQUAL phrase PROPERTY EQUAL phrase ( VALUE EQUAL phrase )? ( TAG_MATCH_MODE )?
                    {
                    TAG42=(Token)match(input,TAG,FOLLOW_TAG_in_propertyQuery638);  
                    stream_TAG.add(TAG42);


                    EQUAL43=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_propertyQuery640);  
                    stream_EQUAL.add(EQUAL43);


                    pushFollow(FOLLOW_phrase_in_propertyQuery642);
                    phrase44=phrase();

                    state._fsp--;

                    stream_phrase.add(phrase44.getTree());

                    PROPERTY45=(Token)match(input,PROPERTY,FOLLOW_PROPERTY_in_propertyQuery644);  
                    stream_PROPERTY.add(PROPERTY45);


                    EQUAL46=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_propertyQuery646);  
                    stream_EQUAL.add(EQUAL46);


                    pushFollow(FOLLOW_phrase_in_propertyQuery648);
                    phrase47=phrase();

                    state._fsp--;

                    stream_phrase.add(phrase47.getTree());

                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:193:43: ( VALUE EQUAL phrase )?
                    int alt10=2;
                    int LA10_0 = input.LA(1);

                    if ( (LA10_0==VALUE) ) {
                        alt10=1;
                    }
                    switch (alt10) {
                        case 1 :
                            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:193:44: VALUE EQUAL phrase
                            {
                            VALUE48=(Token)match(input,VALUE,FOLLOW_VALUE_in_propertyQuery651);  
                            stream_VALUE.add(VALUE48);


                            EQUAL49=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_propertyQuery653);  
                            stream_EQUAL.add(EQUAL49);


                            pushFollow(FOLLOW_phrase_in_propertyQuery655);
                            phrase50=phrase();

                            state._fsp--;

                            stream_phrase.add(phrase50.getTree());

                            }
                            break;

                    }


                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:193:65: ( TAG_MATCH_MODE )?
                    int alt11=2;
                    int LA11_0 = input.LA(1);

                    if ( (LA11_0==TAG_MATCH_MODE) ) {
                        alt11=1;
                    }
                    switch (alt11) {
                        case 1 :
                            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:193:65: TAG_MATCH_MODE
                            {
                            TAG_MATCH_MODE51=(Token)match(input,TAG_MATCH_MODE,FOLLOW_TAG_MATCH_MODE_in_propertyQuery659);  
                            stream_TAG_MATCH_MODE.add(TAG_MATCH_MODE51);


                            }
                            break;

                    }


                    // AST REWRITE
                    // elements: TAG_MATCH_MODE, phrase, phrase, phrase
                    // token labels: 
                    // rule labels: retval
                    // token list labels: 
                    // rule list labels: 
                    // wildcard labels: 
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

                    root_0 = (Object)adaptor.nil();
                    // 193:81: -> ^( ND_TAGPROPERTY phrase phrase ( phrase )? ( TAG_MATCH_MODE )? )
                    {
                        // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:193:84: ^( ND_TAGPROPERTY phrase phrase ( phrase )? ( TAG_MATCH_MODE )? )
                        {
                        Object root_1 = (Object)adaptor.nil();
                        root_1 = (Object)adaptor.becomeRoot(
                        (Object)adaptor.create(ND_TAGPROPERTY, "ND_TAGPROPERTY")
                        , root_1);

                        adaptor.addChild(root_1, stream_phrase.nextTree());

                        adaptor.addChild(root_1, stream_phrase.nextTree());

                        // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:193:115: ( phrase )?
                        if ( stream_phrase.hasNext() ) {
                            adaptor.addChild(root_1, stream_phrase.nextTree());

                        }
                        stream_phrase.reset();

                        // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:193:123: ( TAG_MATCH_MODE )?
                        if ( stream_TAG_MATCH_MODE.hasNext() ) {
                            adaptor.addChild(root_1, 
                            stream_TAG_MATCH_MODE.nextNode()
                            );

                        }
                        stream_TAG_MATCH_MODE.reset();

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

        Token REG52=null;
        Token EQUAL53=null;
        Token string_literal55=null;
        CatmaQueryParser.phrase_return phrase54 =null;


        Object REG52_tree=null;
        Object EQUAL53_tree=null;
        Object string_literal55_tree=null;
        RewriteRuleTokenStream stream_43=new RewriteRuleTokenStream(adaptor,"token 43");
        RewriteRuleTokenStream stream_REG=new RewriteRuleTokenStream(adaptor,"token REG");
        RewriteRuleTokenStream stream_EQUAL=new RewriteRuleTokenStream(adaptor,"token EQUAL");
        RewriteRuleSubtreeStream stream_phrase=new RewriteRuleSubtreeStream(adaptor,"rule phrase");
        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:198:2: ( REG EQUAL phrase ( 'CI' )? -> ^( ND_REG phrase ( 'CI' )? ) )
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:198:4: REG EQUAL phrase ( 'CI' )?
            {
            REG52=(Token)match(input,REG,FOLLOW_REG_in_regQuery694);  
            stream_REG.add(REG52);


            EQUAL53=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_regQuery696);  
            stream_EQUAL.add(EQUAL53);


            pushFollow(FOLLOW_phrase_in_regQuery698);
            phrase54=phrase();

            state._fsp--;

            stream_phrase.add(phrase54.getTree());

            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:198:21: ( 'CI' )?
            int alt13=2;
            int LA13_0 = input.LA(1);

            if ( (LA13_0==43) ) {
                alt13=1;
            }
            switch (alt13) {
                case 1 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:198:21: 'CI'
                    {
                    string_literal55=(Token)match(input,43,FOLLOW_43_in_regQuery700);  
                    stream_43.add(string_literal55);


                    }
                    break;

            }


            // AST REWRITE
            // elements: phrase, 43
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

        Token FREQ56=null;
        Token EQUAL57=null;
        Token INT58=null;
        Token char_literal59=null;
        Token INT60=null;
        Token UNEQUAL61=null;
        Token INT62=null;

        Object FREQ56_tree=null;
        Object EQUAL57_tree=null;
        Object INT58_tree=null;
        Object char_literal59_tree=null;
        Object INT60_tree=null;
        Object UNEQUAL61_tree=null;
        Object INT62_tree=null;
        RewriteRuleTokenStream stream_FREQ=new RewriteRuleTokenStream(adaptor,"token FREQ");
        RewriteRuleTokenStream stream_INT=new RewriteRuleTokenStream(adaptor,"token INT");
        RewriteRuleTokenStream stream_41=new RewriteRuleTokenStream(adaptor,"token 41");
        RewriteRuleTokenStream stream_EQUAL=new RewriteRuleTokenStream(adaptor,"token EQUAL");
        RewriteRuleTokenStream stream_UNEQUAL=new RewriteRuleTokenStream(adaptor,"token UNEQUAL");

        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:203:2: ( FREQ ( EQUAL INT ( '-' INT )? -> ^( ND_FREQ EQUAL INT ( INT )? ) | UNEQUAL INT -> ^( ND_FREQ UNEQUAL INT ) ) )
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:203:4: FREQ ( EQUAL INT ( '-' INT )? -> ^( ND_FREQ EQUAL INT ( INT )? ) | UNEQUAL INT -> ^( ND_FREQ UNEQUAL INT ) )
            {
            FREQ56=(Token)match(input,FREQ,FOLLOW_FREQ_in_freqQuery731);  
            stream_FREQ.add(FREQ56);


            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:204:3: ( EQUAL INT ( '-' INT )? -> ^( ND_FREQ EQUAL INT ( INT )? ) | UNEQUAL INT -> ^( ND_FREQ UNEQUAL INT ) )
            int alt15=2;
            int LA15_0 = input.LA(1);

            if ( (LA15_0==EQUAL) ) {
                alt15=1;
            }
            else if ( (LA15_0==UNEQUAL) ) {
                alt15=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 15, 0, input);

                throw nvae;

            }
            switch (alt15) {
                case 1 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:204:5: EQUAL INT ( '-' INT )?
                    {
                    EQUAL57=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_freqQuery738);  
                    stream_EQUAL.add(EQUAL57);


                    INT58=(Token)match(input,INT,FOLLOW_INT_in_freqQuery740);  
                    stream_INT.add(INT58);


                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:204:15: ( '-' INT )?
                    int alt14=2;
                    int LA14_0 = input.LA(1);

                    if ( (LA14_0==41) ) {
                        int LA14_1 = input.LA(2);

                        if ( (LA14_1==INT) ) {
                            alt14=1;
                        }
                    }
                    switch (alt14) {
                        case 1 :
                            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:204:16: '-' INT
                            {
                            char_literal59=(Token)match(input,41,FOLLOW_41_in_freqQuery743);  
                            stream_41.add(char_literal59);


                            INT60=(Token)match(input,INT,FOLLOW_INT_in_freqQuery745);  
                            stream_INT.add(INT60);


                            }
                            break;

                    }


                    // AST REWRITE
                    // elements: INT, INT, EQUAL
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
                    UNEQUAL61=(Token)match(input,UNEQUAL,FOLLOW_UNEQUAL_in_freqQuery767);  
                    stream_UNEQUAL.add(UNEQUAL61);


                    INT62=(Token)match(input,INT,FOLLOW_INT_in_freqQuery769);  
                    stream_INT.add(INT62);


                    // AST REWRITE
                    // elements: INT, UNEQUAL
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

        Token SIMIL63=null;
        Token EQUAL64=null;
        Token INT66=null;
        Token char_literal67=null;
        CatmaQueryParser.phrase_return phrase65 =null;


        Object SIMIL63_tree=null;
        Object EQUAL64_tree=null;
        Object INT66_tree=null;
        Object char_literal67_tree=null;
        RewriteRuleTokenStream stream_INT=new RewriteRuleTokenStream(adaptor,"token INT");
        RewriteRuleTokenStream stream_36=new RewriteRuleTokenStream(adaptor,"token 36");
        RewriteRuleTokenStream stream_EQUAL=new RewriteRuleTokenStream(adaptor,"token EQUAL");
        RewriteRuleTokenStream stream_SIMIL=new RewriteRuleTokenStream(adaptor,"token SIMIL");
        RewriteRuleSubtreeStream stream_phrase=new RewriteRuleSubtreeStream(adaptor,"rule phrase");
        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:210:2: ( SIMIL EQUAL phrase INT ( '%' )? -> ^( ND_SIMIL phrase INT ) )
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:210:4: SIMIL EQUAL phrase INT ( '%' )?
            {
            SIMIL63=(Token)match(input,SIMIL,FOLLOW_SIMIL_in_similQuery801);  
            stream_SIMIL.add(SIMIL63);


            EQUAL64=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_similQuery803);  
            stream_EQUAL.add(EQUAL64);


            pushFollow(FOLLOW_phrase_in_similQuery805);
            phrase65=phrase();

            state._fsp--;

            stream_phrase.add(phrase65.getTree());

            INT66=(Token)match(input,INT,FOLLOW_INT_in_similQuery807);  
            stream_INT.add(INT66);


            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:210:27: ( '%' )?
            int alt16=2;
            int LA16_0 = input.LA(1);

            if ( (LA16_0==36) ) {
                alt16=1;
            }
            switch (alt16) {
                case 1 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:210:27: '%'
                    {
                    char_literal67=(Token)match(input,36,FOLLOW_36_in_similQuery809);  
                    stream_36.add(char_literal67);


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

        Token WILD68=null;
        Token EQUAL69=null;
        CatmaQueryParser.phrase_return phrase70 =null;


        Object WILD68_tree=null;
        Object EQUAL69_tree=null;
        RewriteRuleTokenStream stream_WILD=new RewriteRuleTokenStream(adaptor,"token WILD");
        RewriteRuleTokenStream stream_EQUAL=new RewriteRuleTokenStream(adaptor,"token EQUAL");
        RewriteRuleSubtreeStream stream_phrase=new RewriteRuleSubtreeStream(adaptor,"rule phrase");
        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:215:2: ( WILD EQUAL phrase -> ^( ND_WILD phrase ) )
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:215:4: WILD EQUAL phrase
            {
            WILD68=(Token)match(input,WILD,FOLLOW_WILD_in_wildQuery839);  
            stream_WILD.add(WILD68);


            EQUAL69=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_wildQuery841);  
            stream_EQUAL.add(EQUAL69);


            pushFollow(FOLLOW_phrase_in_wildQuery843);
            phrase70=phrase();

            state._fsp--;

            stream_phrase.add(phrase70.getTree());

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

        Token string_literal71=null;
        CatmaQueryParser.refinementExpression_return refinementExpression72 =null;


        Object string_literal71_tree=null;
        RewriteRuleTokenStream stream_45=new RewriteRuleTokenStream(adaptor,"token 45");
        RewriteRuleSubtreeStream stream_refinementExpression=new RewriteRuleSubtreeStream(adaptor,"rule refinementExpression");
        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:226:2: ( 'where' refinementExpression -> refinementExpression )
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:226:4: 'where' refinementExpression
            {
            string_literal71=(Token)match(input,45,FOLLOW_45_in_refinement876);  
            stream_45.add(string_literal71);


            pushFollow(FOLLOW_refinementExpression_in_refinement878);
            refinementExpression72=refinementExpression();

            state._fsp--;

            stream_refinementExpression.add(refinementExpression72.getTree());

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
    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:230:1: refinementExpression : startRefinement= refinementTerm ( orRefinement[(CommonTree)$startRefinement.tree] -> orRefinement | andRefinement[(CommonTree)$startRefinement.tree] -> andRefinement | -> ^( ND_REFINE refinementTerm ) ) ;
    public final CatmaQueryParser.refinementExpression_return refinementExpression() throws RecognitionException {
        CatmaQueryParser.refinementExpression_return retval = new CatmaQueryParser.refinementExpression_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        CatmaQueryParser.refinementTerm_return startRefinement =null;

        CatmaQueryParser.orRefinement_return orRefinement73 =null;

        CatmaQueryParser.andRefinement_return andRefinement74 =null;


        RewriteRuleSubtreeStream stream_orRefinement=new RewriteRuleSubtreeStream(adaptor,"rule orRefinement");
        RewriteRuleSubtreeStream stream_andRefinement=new RewriteRuleSubtreeStream(adaptor,"rule andRefinement");
        RewriteRuleSubtreeStream stream_refinementTerm=new RewriteRuleSubtreeStream(adaptor,"rule refinementTerm");
        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:231:2: (startRefinement= refinementTerm ( orRefinement[(CommonTree)$startRefinement.tree] -> orRefinement | andRefinement[(CommonTree)$startRefinement.tree] -> andRefinement | -> ^( ND_REFINE refinementTerm ) ) )
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:231:4: startRefinement= refinementTerm ( orRefinement[(CommonTree)$startRefinement.tree] -> orRefinement | andRefinement[(CommonTree)$startRefinement.tree] -> andRefinement | -> ^( ND_REFINE refinementTerm ) )
            {
            pushFollow(FOLLOW_refinementTerm_in_refinementExpression902);
            startRefinement=refinementTerm();

            state._fsp--;

            stream_refinementTerm.add(startRefinement.getTree());

            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:231:35: ( orRefinement[(CommonTree)$startRefinement.tree] -> orRefinement | andRefinement[(CommonTree)$startRefinement.tree] -> andRefinement | -> ^( ND_REFINE refinementTerm ) )
            int alt17=3;
            switch ( input.LA(1) ) {
            case 46:
                {
                alt17=1;
                }
                break;
            case 40:
                {
                alt17=2;
                }
                break;
            case EOF:
            case 39:
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
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:232:4: orRefinement[(CommonTree)$startRefinement.tree]
                    {
                    pushFollow(FOLLOW_orRefinement_in_refinementExpression910);
                    orRefinement73=orRefinement((CommonTree)(startRefinement!=null?((Object)startRefinement.tree):null));

                    state._fsp--;

                    stream_orRefinement.add(orRefinement73.getTree());

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
                    pushFollow(FOLLOW_andRefinement_in_refinementExpression922);
                    andRefinement74=andRefinement((CommonTree)(startRefinement!=null?((Object)startRefinement.tree):null));

                    state._fsp--;

                    stream_andRefinement.add(andRefinement74.getTree());

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
                    // elements: refinementTerm
                    // token labels: 
                    // rule labels: retval
                    // token list labels: 
                    // rule list labels: 
                    // wildcard labels: 
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

                    root_0 = (Object)adaptor.nil();
                    // 234:6: -> ^( ND_REFINE refinementTerm )
                    {
                        // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:234:9: ^( ND_REFINE refinementTerm )
                        {
                        Object root_1 = (Object)adaptor.nil();
                        root_1 = (Object)adaptor.becomeRoot(
                        (Object)adaptor.create(ND_REFINE, "ND_REFINE")
                        , root_1);

                        adaptor.addChild(root_1, stream_refinementTerm.nextTree());

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
    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:238:1: orRefinement[CommonTree startRefinement] : '|' refinementTerm -> ^( ND_ORREFINE refinementTerm ) ;
    public final CatmaQueryParser.orRefinement_return orRefinement(CommonTree startRefinement) throws RecognitionException {
        CatmaQueryParser.orRefinement_return retval = new CatmaQueryParser.orRefinement_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token char_literal75=null;
        CatmaQueryParser.refinementTerm_return refinementTerm76 =null;


        Object char_literal75_tree=null;
        RewriteRuleTokenStream stream_46=new RewriteRuleTokenStream(adaptor,"token 46");
        RewriteRuleSubtreeStream stream_refinementTerm=new RewriteRuleSubtreeStream(adaptor,"rule refinementTerm");
        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:239:2: ( '|' refinementTerm -> ^( ND_ORREFINE refinementTerm ) )
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:239:4: '|' refinementTerm
            {
            char_literal75=(Token)match(input,46,FOLLOW_46_in_orRefinement961);  
            stream_46.add(char_literal75);


            pushFollow(FOLLOW_refinementTerm_in_orRefinement963);
            refinementTerm76=refinementTerm();

            state._fsp--;

            stream_refinementTerm.add(refinementTerm76.getTree());

            // AST REWRITE
            // elements: refinementTerm
            // token labels: 
            // rule labels: retval
            // token list labels: 
            // rule list labels: 
            // wildcard labels: 
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 239:23: -> ^( ND_ORREFINE refinementTerm )
            {
                // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:239:26: ^( ND_ORREFINE refinementTerm )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(ND_ORREFINE, "ND_ORREFINE")
                , root_1);

                adaptor.addChild(root_1, startRefinement);

                adaptor.addChild(root_1, stream_refinementTerm.nextTree());

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
    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:243:1: andRefinement[CommonTree startRefinement] : ',' refinementTerm -> ^( ND_ANDREFINE refinementTerm ) ;
    public final CatmaQueryParser.andRefinement_return andRefinement(CommonTree startRefinement) throws RecognitionException {
        CatmaQueryParser.andRefinement_return retval = new CatmaQueryParser.andRefinement_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token char_literal77=null;
        CatmaQueryParser.refinementTerm_return refinementTerm78 =null;


        Object char_literal77_tree=null;
        RewriteRuleTokenStream stream_40=new RewriteRuleTokenStream(adaptor,"token 40");
        RewriteRuleSubtreeStream stream_refinementTerm=new RewriteRuleSubtreeStream(adaptor,"rule refinementTerm");
        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:244:2: ( ',' refinementTerm -> ^( ND_ANDREFINE refinementTerm ) )
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:244:4: ',' refinementTerm
            {
            char_literal77=(Token)match(input,40,FOLLOW_40_in_andRefinement993);  
            stream_40.add(char_literal77);


            pushFollow(FOLLOW_refinementTerm_in_andRefinement995);
            refinementTerm78=refinementTerm();

            state._fsp--;

            stream_refinementTerm.add(refinementTerm78.getTree());

            // AST REWRITE
            // elements: refinementTerm
            // token labels: 
            // rule labels: retval
            // token list labels: 
            // rule list labels: 
            // wildcard labels: 
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 244:23: -> ^( ND_ANDREFINE refinementTerm )
            {
                // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:244:26: ^( ND_ANDREFINE refinementTerm )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(ND_ANDREFINE, "ND_ANDREFINE")
                , root_1);

                adaptor.addChild(root_1, startRefinement);

                adaptor.addChild(root_1, stream_refinementTerm.nextTree());

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


    public static class refinementTerm_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "refinementTerm"
    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:248:1: refinementTerm : ( selector -> selector | '(' refinementExpression ')' -> refinementExpression );
    public final CatmaQueryParser.refinementTerm_return refinementTerm() throws RecognitionException {
        CatmaQueryParser.refinementTerm_return retval = new CatmaQueryParser.refinementTerm_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token char_literal80=null;
        Token char_literal82=null;
        CatmaQueryParser.selector_return selector79 =null;

        CatmaQueryParser.refinementExpression_return refinementExpression81 =null;


        Object char_literal80_tree=null;
        Object char_literal82_tree=null;
        RewriteRuleTokenStream stream_39=new RewriteRuleTokenStream(adaptor,"token 39");
        RewriteRuleTokenStream stream_38=new RewriteRuleTokenStream(adaptor,"token 38");
        RewriteRuleSubtreeStream stream_selector=new RewriteRuleSubtreeStream(adaptor,"rule selector");
        RewriteRuleSubtreeStream stream_refinementExpression=new RewriteRuleSubtreeStream(adaptor,"rule refinementExpression");
        try {
            // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:249:2: ( selector -> selector | '(' refinementExpression ')' -> refinementExpression )
            int alt18=2;
            int LA18_0 = input.LA(1);

            if ( (LA18_0==FREQ||(LA18_0 >= PROPERTY && LA18_0 <= TAG)||LA18_0==WILD) ) {
                alt18=1;
            }
            else if ( (LA18_0==38) ) {
                alt18=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 18, 0, input);

                throw nvae;

            }
            switch (alt18) {
                case 1 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:249:5: selector
                    {
                    pushFollow(FOLLOW_selector_in_refinementTerm1025);
                    selector79=selector();

                    state._fsp--;

                    stream_selector.add(selector79.getTree());

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
                    // 249:14: -> selector
                    {
                        adaptor.addChild(root_0, stream_selector.nextTree());

                    }


                    retval.tree = root_0;

                    }
                    break;
                case 2 :
                    // C:\\data\\eclipse_kepler\\catma\\grammars\\ast\\CatmaQuery.g:250:5: '(' refinementExpression ')'
                    {
                    char_literal80=(Token)match(input,38,FOLLOW_38_in_refinementTerm1035);  
                    stream_38.add(char_literal80);


                    pushFollow(FOLLOW_refinementExpression_in_refinementTerm1036);
                    refinementExpression81=refinementExpression();

                    state._fsp--;

                    stream_refinementExpression.add(refinementExpression81.getTree());

                    char_literal82=(Token)match(input,39,FOLLOW_39_in_refinementTerm1037);  
                    stream_39.add(char_literal82);


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
                    // 250:32: -> refinementExpression
                    {
                        adaptor.addChild(root_0, stream_refinementExpression.nextTree());

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
    // $ANTLR end "refinementTerm"

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
    public static final BitSet FOLLOW_40_in_unionQuery296 = new BitSet(new long[]{0x00000048BC000020L});
    public static final BitSet FOLLOW_term_in_unionQuery298 = new BitSet(new long[]{0x0000100000000002L});
    public static final BitSet FOLLOW_44_in_unionQuery300 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_37_in_collocQuery335 = new BitSet(new long[]{0x00000048BC000020L});
    public static final BitSet FOLLOW_term_in_collocQuery337 = new BitSet(new long[]{0x0000000000000082L});
    public static final BitSet FOLLOW_INT_in_collocQuery339 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_41_in_exclusionQuery373 = new BitSet(new long[]{0x00000048BC000020L});
    public static final BitSet FOLLOW_term_in_exclusionQuery375 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_42_in_adjacencyQuery406 = new BitSet(new long[]{0x00000048BC000020L});
    public static final BitSet FOLLOW_term_in_adjacencyQuery408 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_phrase_in_term438 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_selector_in_term448 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_38_in_term459 = new BitSet(new long[]{0x00000048BC000020L});
    public static final BitSet FOLLOW_query_in_term460 = new BitSet(new long[]{0x0000008000000000L});
    public static final BitSet FOLLOW_39_in_term461 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_TXT_in_phrase485 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_tagQuery_in_selector518 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_propertyQuery_in_selector523 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_regQuery_in_selector528 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_freqQuery_in_selector533 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_similQuery_in_selector538 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_wildQuery_in_selector543 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_TAG_in_tagQuery563 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_EQUAL_in_tagQuery565 = new BitSet(new long[]{0x0000000080000000L});
    public static final BitSet FOLLOW_phrase_in_tagQuery567 = new BitSet(new long[]{0x0000000040000002L});
    public static final BitSet FOLLOW_TAG_MATCH_MODE_in_tagQuery569 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_PROPERTY_in_propertyQuery602 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_EQUAL_in_propertyQuery604 = new BitSet(new long[]{0x0000000080000000L});
    public static final BitSet FOLLOW_phrase_in_propertyQuery606 = new BitSet(new long[]{0x0000000240000002L});
    public static final BitSet FOLLOW_VALUE_in_propertyQuery609 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_EQUAL_in_propertyQuery611 = new BitSet(new long[]{0x0000000080000000L});
    public static final BitSet FOLLOW_phrase_in_propertyQuery613 = new BitSet(new long[]{0x0000000040000002L});
    public static final BitSet FOLLOW_TAG_MATCH_MODE_in_propertyQuery617 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_TAG_in_propertyQuery638 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_EQUAL_in_propertyQuery640 = new BitSet(new long[]{0x0000000080000000L});
    public static final BitSet FOLLOW_phrase_in_propertyQuery642 = new BitSet(new long[]{0x0000000004000000L});
    public static final BitSet FOLLOW_PROPERTY_in_propertyQuery644 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_EQUAL_in_propertyQuery646 = new BitSet(new long[]{0x0000000080000000L});
    public static final BitSet FOLLOW_phrase_in_propertyQuery648 = new BitSet(new long[]{0x0000000240000002L});
    public static final BitSet FOLLOW_VALUE_in_propertyQuery651 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_EQUAL_in_propertyQuery653 = new BitSet(new long[]{0x0000000080000000L});
    public static final BitSet FOLLOW_phrase_in_propertyQuery655 = new BitSet(new long[]{0x0000000040000002L});
    public static final BitSet FOLLOW_TAG_MATCH_MODE_in_propertyQuery659 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_REG_in_regQuery694 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_EQUAL_in_regQuery696 = new BitSet(new long[]{0x0000000080000000L});
    public static final BitSet FOLLOW_phrase_in_regQuery698 = new BitSet(new long[]{0x0000080000000002L});
    public static final BitSet FOLLOW_43_in_regQuery700 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_FREQ_in_freqQuery731 = new BitSet(new long[]{0x0000000100000010L});
    public static final BitSet FOLLOW_EQUAL_in_freqQuery738 = new BitSet(new long[]{0x0000000000000080L});
    public static final BitSet FOLLOW_INT_in_freqQuery740 = new BitSet(new long[]{0x0000020000000002L});
    public static final BitSet FOLLOW_41_in_freqQuery743 = new BitSet(new long[]{0x0000000000000080L});
    public static final BitSet FOLLOW_INT_in_freqQuery745 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_UNEQUAL_in_freqQuery767 = new BitSet(new long[]{0x0000000000000080L});
    public static final BitSet FOLLOW_INT_in_freqQuery769 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_SIMIL_in_similQuery801 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_EQUAL_in_similQuery803 = new BitSet(new long[]{0x0000000080000000L});
    public static final BitSet FOLLOW_phrase_in_similQuery805 = new BitSet(new long[]{0x0000000000000080L});
    public static final BitSet FOLLOW_INT_in_similQuery807 = new BitSet(new long[]{0x0000001000000002L});
    public static final BitSet FOLLOW_36_in_similQuery809 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_WILD_in_wildQuery839 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_EQUAL_in_wildQuery841 = new BitSet(new long[]{0x0000000080000000L});
    public static final BitSet FOLLOW_phrase_in_wildQuery843 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_45_in_refinement876 = new BitSet(new long[]{0x000000483C000020L});
    public static final BitSet FOLLOW_refinementExpression_in_refinement878 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_refinementTerm_in_refinementExpression902 = new BitSet(new long[]{0x0000410000000002L});
    public static final BitSet FOLLOW_orRefinement_in_refinementExpression910 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_andRefinement_in_refinementExpression922 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_46_in_orRefinement961 = new BitSet(new long[]{0x000000483C000020L});
    public static final BitSet FOLLOW_refinementTerm_in_orRefinement963 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_40_in_andRefinement993 = new BitSet(new long[]{0x000000483C000020L});
    public static final BitSet FOLLOW_refinementTerm_in_andRefinement995 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_selector_in_refinementTerm1025 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_38_in_refinementTerm1035 = new BitSet(new long[]{0x000000483C000020L});
    public static final BitSet FOLLOW_refinementExpression_in_refinementTerm1036 = new BitSet(new long[]{0x0000008000000000L});
    public static final BitSet FOLLOW_39_in_refinementTerm1037 = new BitSet(new long[]{0x0000000000000002L});

}