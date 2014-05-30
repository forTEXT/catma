// $ANTLR 3.4 C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g 2014-05-30 15:24:20

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
        "<invalid>", "<EOR>", "<DOWN>", "<UP>", "EQUAL", "FREQ", "GROUPIDENT", "INT", "LETTER", "LETTEREXTENDED", "ND_ADJACENCY", "ND_ANDREFINE", "ND_COLLOC", "ND_EXCLUSION", "ND_FREQ", "ND_ORREFINE", "ND_PHRASE", "ND_PROPERTY", "ND_QUERY", "ND_REFINE", "ND_REG", "ND_SIMIL", "ND_TAG", "ND_TAGPROPERTY", "ND_UNION", "ND_WILD", "PROPERTY", "REG", "SIMIL", "TAG", "TAG_MATCH_MODE", "TXT", "UNEQUAL", "VALUE", "WHITESPACE", "WILD", "'%'", "'&'", "'('", "')'", "','", "'-'", "';'", "'CI'", "'EXCL'", "'GR'", "'where'", "'|'"
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
    public static final int T__47=47;
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
    public String getGrammarFileName() { return "C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g"; }



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
    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:105:1: start : query EOF ;
    public final CatmaQueryParser.start_return start() throws RecognitionException {
        CatmaQueryParser.start_return retval = new CatmaQueryParser.start_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token EOF2=null;
        CatmaQueryParser.query_return query1 =null;


        Object EOF2_tree=null;

        try {
            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:105:7: ( query EOF )
            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:105:9: query EOF
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
    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:109:1: query : queryExpression ( refinement )? -> ^( ND_QUERY queryExpression ( refinement )? ) ;
    public final CatmaQueryParser.query_return query() throws RecognitionException {
        CatmaQueryParser.query_return retval = new CatmaQueryParser.query_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        CatmaQueryParser.queryExpression_return queryExpression3 =null;

        CatmaQueryParser.refinement_return refinement4 =null;


        RewriteRuleSubtreeStream stream_refinement=new RewriteRuleSubtreeStream(adaptor,"rule refinement");
        RewriteRuleSubtreeStream stream_queryExpression=new RewriteRuleSubtreeStream(adaptor,"rule queryExpression");
        try {
            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:109:7: ( queryExpression ( refinement )? -> ^( ND_QUERY queryExpression ( refinement )? ) )
            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:109:9: queryExpression ( refinement )?
            {
            pushFollow(FOLLOW_queryExpression_in_query169);
            queryExpression3=queryExpression();

            state._fsp--;

            stream_queryExpression.add(queryExpression3.getTree());

            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:109:25: ( refinement )?
            int alt1=2;
            int LA1_0 = input.LA(1);

            if ( (LA1_0==46) ) {
                alt1=1;
            }
            switch (alt1) {
                case 1 :
                    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:109:25: refinement
                    {
                    pushFollow(FOLLOW_refinement_in_query171);
                    refinement4=refinement();

                    state._fsp--;

                    stream_refinement.add(refinement4.getTree());

                    }
                    break;

            }


            // AST REWRITE
            // elements: refinement, queryExpression
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
                // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:109:40: ^( ND_QUERY queryExpression ( refinement )? )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(ND_QUERY, "ND_QUERY")
                , root_1);

                adaptor.addChild(root_1, stream_queryExpression.nextTree());

                // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:109:67: ( refinement )?
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
    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:118:1: queryExpression : startTerm= term ( unionQuery[(CommonTree)$startTerm.tree] -> unionQuery | collocQuery[(CommonTree)$startTerm.tree] -> collocQuery | exclusionQuery[(CommonTree)$startTerm.tree] -> exclusionQuery | adjacencyQuery[(CommonTree)$startTerm.tree] -> adjacencyQuery | -> term ) ;
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
            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:119:2: (startTerm= term ( unionQuery[(CommonTree)$startTerm.tree] -> unionQuery | collocQuery[(CommonTree)$startTerm.tree] -> collocQuery | exclusionQuery[(CommonTree)$startTerm.tree] -> exclusionQuery | adjacencyQuery[(CommonTree)$startTerm.tree] -> adjacencyQuery | -> term ) )
            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:119:4: startTerm= term ( unionQuery[(CommonTree)$startTerm.tree] -> unionQuery | collocQuery[(CommonTree)$startTerm.tree] -> collocQuery | exclusionQuery[(CommonTree)$startTerm.tree] -> exclusionQuery | adjacencyQuery[(CommonTree)$startTerm.tree] -> adjacencyQuery | -> term )
            {
            pushFollow(FOLLOW_term_in_queryExpression208);
            startTerm=term();

            state._fsp--;

            stream_term.add(startTerm.getTree());

            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:119:19: ( unionQuery[(CommonTree)$startTerm.tree] -> unionQuery | collocQuery[(CommonTree)$startTerm.tree] -> collocQuery | exclusionQuery[(CommonTree)$startTerm.tree] -> exclusionQuery | adjacencyQuery[(CommonTree)$startTerm.tree] -> adjacencyQuery | -> term )
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
            case 46:
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
                    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:120:4: unionQuery[(CommonTree)$startTerm.tree]
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
                    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:121:6: collocQuery[(CommonTree)$startTerm.tree]
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
                    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:122:6: exclusionQuery[(CommonTree)$startTerm.tree]
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
                    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:123:6: adjacencyQuery[(CommonTree)$startTerm.tree]
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
                    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:124:6: 
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
    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:134:1: unionQuery[CommonTree startTerm] : ',' term ( 'EXCL' )? -> ^( ND_UNION term ( 'EXCL' )? ) ;
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
            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:135:2: ( ',' term ( 'EXCL' )? -> ^( ND_UNION term ( 'EXCL' )? ) )
            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:135:4: ',' term ( 'EXCL' )?
            {
            char_literal9=(Token)match(input,40,FOLLOW_40_in_unionQuery296);  
            stream_40.add(char_literal9);


            pushFollow(FOLLOW_term_in_unionQuery298);
            term10=term();

            state._fsp--;

            stream_term.add(term10.getTree());

            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:135:13: ( 'EXCL' )?
            int alt3=2;
            int LA3_0 = input.LA(1);

            if ( (LA3_0==44) ) {
                alt3=1;
            }
            switch (alt3) {
                case 1 :
                    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:135:13: 'EXCL'
                    {
                    string_literal11=(Token)match(input,44,FOLLOW_44_in_unionQuery300);  
                    stream_44.add(string_literal11);


                    }
                    break;

            }


            // AST REWRITE
            // elements: term, 44
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
                // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:135:24: ^( ND_UNION term ( 'EXCL' )? )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(ND_UNION, "ND_UNION")
                , root_1);

                adaptor.addChild(root_1, startTerm);

                adaptor.addChild(root_1, stream_term.nextTree());

                // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:135:53: ( 'EXCL' )?
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
    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:139:1: collocQuery[CommonTree startTerm] : '&' term ( INT )? ( 'GR' )? -> ^( ND_COLLOC term ( INT )? ( 'GR' )? ) ;
    public final CatmaQueryParser.collocQuery_return collocQuery(CommonTree startTerm) throws RecognitionException {
        CatmaQueryParser.collocQuery_return retval = new CatmaQueryParser.collocQuery_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token char_literal12=null;
        Token INT14=null;
        Token string_literal15=null;
        CatmaQueryParser.term_return term13 =null;


        Object char_literal12_tree=null;
        Object INT14_tree=null;
        Object string_literal15_tree=null;
        RewriteRuleTokenStream stream_45=new RewriteRuleTokenStream(adaptor,"token 45");
        RewriteRuleTokenStream stream_INT=new RewriteRuleTokenStream(adaptor,"token INT");
        RewriteRuleTokenStream stream_37=new RewriteRuleTokenStream(adaptor,"token 37");
        RewriteRuleSubtreeStream stream_term=new RewriteRuleSubtreeStream(adaptor,"rule term");
        try {
            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:140:2: ( '&' term ( INT )? ( 'GR' )? -> ^( ND_COLLOC term ( INT )? ( 'GR' )? ) )
            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:140:4: '&' term ( INT )? ( 'GR' )?
            {
            char_literal12=(Token)match(input,37,FOLLOW_37_in_collocQuery335);  
            stream_37.add(char_literal12);


            pushFollow(FOLLOW_term_in_collocQuery337);
            term13=term();

            state._fsp--;

            stream_term.add(term13.getTree());

            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:140:13: ( INT )?
            int alt4=2;
            int LA4_0 = input.LA(1);

            if ( (LA4_0==INT) ) {
                alt4=1;
            }
            switch (alt4) {
                case 1 :
                    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:140:13: INT
                    {
                    INT14=(Token)match(input,INT,FOLLOW_INT_in_collocQuery339);  
                    stream_INT.add(INT14);


                    }
                    break;

            }


            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:140:18: ( 'GR' )?
            int alt5=2;
            int LA5_0 = input.LA(1);

            if ( (LA5_0==45) ) {
                alt5=1;
            }
            switch (alt5) {
                case 1 :
                    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:140:18: 'GR'
                    {
                    string_literal15=(Token)match(input,45,FOLLOW_45_in_collocQuery342);  
                    stream_45.add(string_literal15);


                    }
                    break;

            }


            // AST REWRITE
            // elements: term, 45, INT
            // token labels: 
            // rule labels: retval
            // token list labels: 
            // rule list labels: 
            // wildcard labels: 
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 140:24: -> ^( ND_COLLOC term ( INT )? ( 'GR' )? )
            {
                // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:140:27: ^( ND_COLLOC term ( INT )? ( 'GR' )? )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(ND_COLLOC, "ND_COLLOC")
                , root_1);

                adaptor.addChild(root_1, startTerm);

                adaptor.addChild(root_1, stream_term.nextTree());

                // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:140:57: ( INT )?
                if ( stream_INT.hasNext() ) {
                    adaptor.addChild(root_1, 
                    stream_INT.nextNode()
                    );

                }
                stream_INT.reset();

                // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:140:62: ( 'GR' )?
                if ( stream_45.hasNext() ) {
                    adaptor.addChild(root_1, 
                    stream_45.nextNode()
                    );

                }
                stream_45.reset();

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
    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:144:1: exclusionQuery[CommonTree startTerm] : '-' term -> ^( ND_EXCLUSION term ) ;
    public final CatmaQueryParser.exclusionQuery_return exclusionQuery(CommonTree startTerm) throws RecognitionException {
        CatmaQueryParser.exclusionQuery_return retval = new CatmaQueryParser.exclusionQuery_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token char_literal16=null;
        CatmaQueryParser.term_return term17 =null;


        Object char_literal16_tree=null;
        RewriteRuleTokenStream stream_41=new RewriteRuleTokenStream(adaptor,"token 41");
        RewriteRuleSubtreeStream stream_term=new RewriteRuleSubtreeStream(adaptor,"rule term");
        try {
            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:145:2: ( '-' term -> ^( ND_EXCLUSION term ) )
            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:145:4: '-' term
            {
            char_literal16=(Token)match(input,41,FOLLOW_41_in_exclusionQuery379);  
            stream_41.add(char_literal16);


            pushFollow(FOLLOW_term_in_exclusionQuery381);
            term17=term();

            state._fsp--;

            stream_term.add(term17.getTree());

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
                // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:145:16: ^( ND_EXCLUSION term )
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
    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:149:1: adjacencyQuery[CommonTree startTerm] : ';' term -> ^( ND_ADJACENCY term ) ;
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
            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:150:2: ( ';' term -> ^( ND_ADJACENCY term ) )
            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:150:4: ';' term
            {
            char_literal18=(Token)match(input,42,FOLLOW_42_in_adjacencyQuery412);  
            stream_42.add(char_literal18);


            pushFollow(FOLLOW_term_in_adjacencyQuery414);
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
                // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:150:16: ^( ND_ADJACENCY term )
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
    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:155:1: term : ( phrase -> phrase | selector -> selector | '(' query ')' -> query );
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
        RewriteRuleTokenStream stream_39=new RewriteRuleTokenStream(adaptor,"token 39");
        RewriteRuleTokenStream stream_38=new RewriteRuleTokenStream(adaptor,"token 38");
        RewriteRuleSubtreeStream stream_selector=new RewriteRuleSubtreeStream(adaptor,"rule selector");
        RewriteRuleSubtreeStream stream_query=new RewriteRuleSubtreeStream(adaptor,"rule query");
        RewriteRuleSubtreeStream stream_phrase=new RewriteRuleSubtreeStream(adaptor,"rule phrase");
        try {
            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:155:7: ( phrase -> phrase | selector -> selector | '(' query ')' -> query )
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
                    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:155:9: phrase
                    {
                    pushFollow(FOLLOW_phrase_in_term444);
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
                    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:156:5: selector
                    {
                    pushFollow(FOLLOW_selector_in_term454);
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
                    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:157:5: '(' query ')'
                    {
                    char_literal22=(Token)match(input,38,FOLLOW_38_in_term465);  
                    stream_38.add(char_literal22);


                    pushFollow(FOLLOW_query_in_term466);
                    query23=query();

                    state._fsp--;

                    stream_query.add(query23.getTree());

                    char_literal24=(Token)match(input,39,FOLLOW_39_in_term467);  
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
    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:164:1: phrase : TXT -> ^( ND_PHRASE TXT ) ;
    public final CatmaQueryParser.phrase_return phrase() throws RecognitionException {
        CatmaQueryParser.phrase_return retval = new CatmaQueryParser.phrase_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token TXT25=null;

        Object TXT25_tree=null;
        RewriteRuleTokenStream stream_TXT=new RewriteRuleTokenStream(adaptor,"token TXT");

        try {
            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:164:8: ( TXT -> ^( ND_PHRASE TXT ) )
            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:164:10: TXT
            {
            TXT25=(Token)match(input,TXT,FOLLOW_TXT_in_phrase491);  
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
                // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:164:17: ^( ND_PHRASE TXT )
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
    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:174:1: selector : ( tagQuery | propertyQuery | regQuery | freqQuery | similQuery | wildQuery );
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
            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:175:2: ( tagQuery | propertyQuery | regQuery | freqQuery | similQuery | wildQuery )
            int alt7=6;
            switch ( input.LA(1) ) {
            case TAG:
                {
                int LA7_1 = input.LA(2);

                if ( (LA7_1==EQUAL) ) {
                    int LA7_7 = input.LA(3);

                    if ( (LA7_7==TXT) ) {
                        int LA7_8 = input.LA(4);

                        if ( (LA7_8==EOF||LA7_8==INT||LA7_8==TAG_MATCH_MODE||LA7_8==37||(LA7_8 >= 39 && LA7_8 <= 42)||(LA7_8 >= 44 && LA7_8 <= 47)) ) {
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
                    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:175:4: tagQuery
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_tagQuery_in_selector524);
                    tagQuery26=tagQuery();

                    state._fsp--;

                    adaptor.addChild(root_0, tagQuery26.getTree());

                    }
                    break;
                case 2 :
                    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:176:4: propertyQuery
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_propertyQuery_in_selector529);
                    propertyQuery27=propertyQuery();

                    state._fsp--;

                    adaptor.addChild(root_0, propertyQuery27.getTree());

                    }
                    break;
                case 3 :
                    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:177:4: regQuery
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_regQuery_in_selector534);
                    regQuery28=regQuery();

                    state._fsp--;

                    adaptor.addChild(root_0, regQuery28.getTree());

                    }
                    break;
                case 4 :
                    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:178:4: freqQuery
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_freqQuery_in_selector539);
                    freqQuery29=freqQuery();

                    state._fsp--;

                    adaptor.addChild(root_0, freqQuery29.getTree());

                    }
                    break;
                case 5 :
                    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:179:4: similQuery
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_similQuery_in_selector544);
                    similQuery30=similQuery();

                    state._fsp--;

                    adaptor.addChild(root_0, similQuery30.getTree());

                    }
                    break;
                case 6 :
                    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:180:4: wildQuery
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_wildQuery_in_selector549);
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
    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:185:1: tagQuery : TAG EQUAL phrase ( TAG_MATCH_MODE )? -> ^( ND_TAG phrase ( TAG_MATCH_MODE )? ) ;
    public final CatmaQueryParser.tagQuery_return tagQuery() throws RecognitionException {
        CatmaQueryParser.tagQuery_return retval = new CatmaQueryParser.tagQuery_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token TAG32=null;
        Token EQUAL33=null;
        Token TAG_MATCH_MODE35=null;
        CatmaQueryParser.phrase_return phrase34 =null;


        Object TAG32_tree=null;
        Object EQUAL33_tree=null;
        Object TAG_MATCH_MODE35_tree=null;
        RewriteRuleTokenStream stream_TAG_MATCH_MODE=new RewriteRuleTokenStream(adaptor,"token TAG_MATCH_MODE");
        RewriteRuleTokenStream stream_EQUAL=new RewriteRuleTokenStream(adaptor,"token EQUAL");
        RewriteRuleTokenStream stream_TAG=new RewriteRuleTokenStream(adaptor,"token TAG");
        RewriteRuleSubtreeStream stream_phrase=new RewriteRuleSubtreeStream(adaptor,"rule phrase");
        try {
            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:186:2: ( TAG EQUAL phrase ( TAG_MATCH_MODE )? -> ^( ND_TAG phrase ( TAG_MATCH_MODE )? ) )
            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:186:4: TAG EQUAL phrase ( TAG_MATCH_MODE )?
            {
            TAG32=(Token)match(input,TAG,FOLLOW_TAG_in_tagQuery569);  
            stream_TAG.add(TAG32);


            EQUAL33=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_tagQuery571);  
            stream_EQUAL.add(EQUAL33);


            pushFollow(FOLLOW_phrase_in_tagQuery573);
            phrase34=phrase();

            state._fsp--;

            stream_phrase.add(phrase34.getTree());

            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:186:21: ( TAG_MATCH_MODE )?
            int alt8=2;
            int LA8_0 = input.LA(1);

            if ( (LA8_0==TAG_MATCH_MODE) ) {
                alt8=1;
            }
            switch (alt8) {
                case 1 :
                    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:186:21: TAG_MATCH_MODE
                    {
                    TAG_MATCH_MODE35=(Token)match(input,TAG_MATCH_MODE,FOLLOW_TAG_MATCH_MODE_in_tagQuery575);  
                    stream_TAG_MATCH_MODE.add(TAG_MATCH_MODE35);


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
                // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:186:40: ^( ND_TAG phrase ( TAG_MATCH_MODE )? )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(ND_TAG, "ND_TAG")
                , root_1);

                adaptor.addChild(root_1, stream_phrase.nextTree());

                // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:186:56: ( TAG_MATCH_MODE )?
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
    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:191:1: propertyQuery : ( PROPERTY EQUAL phrase ( VALUE EQUAL phrase )? ( TAG_MATCH_MODE )? -> ^( ND_PROPERTY phrase ( phrase )? ( TAG_MATCH_MODE )? ) | TAG EQUAL phrase PROPERTY EQUAL phrase ( VALUE EQUAL phrase )? ( TAG_MATCH_MODE )? -> ^( ND_TAGPROPERTY phrase phrase ( phrase )? ( TAG_MATCH_MODE )? ) );
    public final CatmaQueryParser.propertyQuery_return propertyQuery() throws RecognitionException {
        CatmaQueryParser.propertyQuery_return retval = new CatmaQueryParser.propertyQuery_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token PROPERTY36=null;
        Token EQUAL37=null;
        Token VALUE39=null;
        Token EQUAL40=null;
        Token TAG_MATCH_MODE42=null;
        Token TAG43=null;
        Token EQUAL44=null;
        Token PROPERTY46=null;
        Token EQUAL47=null;
        Token VALUE49=null;
        Token EQUAL50=null;
        Token TAG_MATCH_MODE52=null;
        CatmaQueryParser.phrase_return phrase38 =null;

        CatmaQueryParser.phrase_return phrase41 =null;

        CatmaQueryParser.phrase_return phrase45 =null;

        CatmaQueryParser.phrase_return phrase48 =null;

        CatmaQueryParser.phrase_return phrase51 =null;


        Object PROPERTY36_tree=null;
        Object EQUAL37_tree=null;
        Object VALUE39_tree=null;
        Object EQUAL40_tree=null;
        Object TAG_MATCH_MODE42_tree=null;
        Object TAG43_tree=null;
        Object EQUAL44_tree=null;
        Object PROPERTY46_tree=null;
        Object EQUAL47_tree=null;
        Object VALUE49_tree=null;
        Object EQUAL50_tree=null;
        Object TAG_MATCH_MODE52_tree=null;
        RewriteRuleTokenStream stream_TAG_MATCH_MODE=new RewriteRuleTokenStream(adaptor,"token TAG_MATCH_MODE");
        RewriteRuleTokenStream stream_VALUE=new RewriteRuleTokenStream(adaptor,"token VALUE");
        RewriteRuleTokenStream stream_PROPERTY=new RewriteRuleTokenStream(adaptor,"token PROPERTY");
        RewriteRuleTokenStream stream_EQUAL=new RewriteRuleTokenStream(adaptor,"token EQUAL");
        RewriteRuleTokenStream stream_TAG=new RewriteRuleTokenStream(adaptor,"token TAG");
        RewriteRuleSubtreeStream stream_phrase=new RewriteRuleSubtreeStream(adaptor,"rule phrase");
        try {
            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:192:2: ( PROPERTY EQUAL phrase ( VALUE EQUAL phrase )? ( TAG_MATCH_MODE )? -> ^( ND_PROPERTY phrase ( phrase )? ( TAG_MATCH_MODE )? ) | TAG EQUAL phrase PROPERTY EQUAL phrase ( VALUE EQUAL phrase )? ( TAG_MATCH_MODE )? -> ^( ND_TAGPROPERTY phrase phrase ( phrase )? ( TAG_MATCH_MODE )? ) )
            int alt13=2;
            int LA13_0 = input.LA(1);

            if ( (LA13_0==PROPERTY) ) {
                alt13=1;
            }
            else if ( (LA13_0==TAG) ) {
                alt13=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 13, 0, input);

                throw nvae;

            }
            switch (alt13) {
                case 1 :
                    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:192:4: PROPERTY EQUAL phrase ( VALUE EQUAL phrase )? ( TAG_MATCH_MODE )?
                    {
                    PROPERTY36=(Token)match(input,PROPERTY,FOLLOW_PROPERTY_in_propertyQuery608);  
                    stream_PROPERTY.add(PROPERTY36);


                    EQUAL37=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_propertyQuery610);  
                    stream_EQUAL.add(EQUAL37);


                    pushFollow(FOLLOW_phrase_in_propertyQuery612);
                    phrase38=phrase();

                    state._fsp--;

                    stream_phrase.add(phrase38.getTree());

                    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:192:26: ( VALUE EQUAL phrase )?
                    int alt9=2;
                    int LA9_0 = input.LA(1);

                    if ( (LA9_0==VALUE) ) {
                        alt9=1;
                    }
                    switch (alt9) {
                        case 1 :
                            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:192:27: VALUE EQUAL phrase
                            {
                            VALUE39=(Token)match(input,VALUE,FOLLOW_VALUE_in_propertyQuery615);  
                            stream_VALUE.add(VALUE39);


                            EQUAL40=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_propertyQuery617);  
                            stream_EQUAL.add(EQUAL40);


                            pushFollow(FOLLOW_phrase_in_propertyQuery619);
                            phrase41=phrase();

                            state._fsp--;

                            stream_phrase.add(phrase41.getTree());

                            }
                            break;

                    }


                    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:192:48: ( TAG_MATCH_MODE )?
                    int alt10=2;
                    int LA10_0 = input.LA(1);

                    if ( (LA10_0==TAG_MATCH_MODE) ) {
                        alt10=1;
                    }
                    switch (alt10) {
                        case 1 :
                            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:192:48: TAG_MATCH_MODE
                            {
                            TAG_MATCH_MODE42=(Token)match(input,TAG_MATCH_MODE,FOLLOW_TAG_MATCH_MODE_in_propertyQuery623);  
                            stream_TAG_MATCH_MODE.add(TAG_MATCH_MODE42);


                            }
                            break;

                    }


                    // AST REWRITE
                    // elements: phrase, phrase, TAG_MATCH_MODE
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
                        // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:192:67: ^( ND_PROPERTY phrase ( phrase )? ( TAG_MATCH_MODE )? )
                        {
                        Object root_1 = (Object)adaptor.nil();
                        root_1 = (Object)adaptor.becomeRoot(
                        (Object)adaptor.create(ND_PROPERTY, "ND_PROPERTY")
                        , root_1);

                        adaptor.addChild(root_1, stream_phrase.nextTree());

                        // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:192:88: ( phrase )?
                        if ( stream_phrase.hasNext() ) {
                            adaptor.addChild(root_1, stream_phrase.nextTree());

                        }
                        stream_phrase.reset();

                        // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:192:96: ( TAG_MATCH_MODE )?
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
                    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:193:4: TAG EQUAL phrase PROPERTY EQUAL phrase ( VALUE EQUAL phrase )? ( TAG_MATCH_MODE )?
                    {
                    TAG43=(Token)match(input,TAG,FOLLOW_TAG_in_propertyQuery644);  
                    stream_TAG.add(TAG43);


                    EQUAL44=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_propertyQuery646);  
                    stream_EQUAL.add(EQUAL44);


                    pushFollow(FOLLOW_phrase_in_propertyQuery648);
                    phrase45=phrase();

                    state._fsp--;

                    stream_phrase.add(phrase45.getTree());

                    PROPERTY46=(Token)match(input,PROPERTY,FOLLOW_PROPERTY_in_propertyQuery650);  
                    stream_PROPERTY.add(PROPERTY46);


                    EQUAL47=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_propertyQuery652);  
                    stream_EQUAL.add(EQUAL47);


                    pushFollow(FOLLOW_phrase_in_propertyQuery654);
                    phrase48=phrase();

                    state._fsp--;

                    stream_phrase.add(phrase48.getTree());

                    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:193:43: ( VALUE EQUAL phrase )?
                    int alt11=2;
                    int LA11_0 = input.LA(1);

                    if ( (LA11_0==VALUE) ) {
                        alt11=1;
                    }
                    switch (alt11) {
                        case 1 :
                            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:193:44: VALUE EQUAL phrase
                            {
                            VALUE49=(Token)match(input,VALUE,FOLLOW_VALUE_in_propertyQuery657);  
                            stream_VALUE.add(VALUE49);


                            EQUAL50=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_propertyQuery659);  
                            stream_EQUAL.add(EQUAL50);


                            pushFollow(FOLLOW_phrase_in_propertyQuery661);
                            phrase51=phrase();

                            state._fsp--;

                            stream_phrase.add(phrase51.getTree());

                            }
                            break;

                    }


                    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:193:65: ( TAG_MATCH_MODE )?
                    int alt12=2;
                    int LA12_0 = input.LA(1);

                    if ( (LA12_0==TAG_MATCH_MODE) ) {
                        alt12=1;
                    }
                    switch (alt12) {
                        case 1 :
                            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:193:65: TAG_MATCH_MODE
                            {
                            TAG_MATCH_MODE52=(Token)match(input,TAG_MATCH_MODE,FOLLOW_TAG_MATCH_MODE_in_propertyQuery665);  
                            stream_TAG_MATCH_MODE.add(TAG_MATCH_MODE52);


                            }
                            break;

                    }


                    // AST REWRITE
                    // elements: phrase, TAG_MATCH_MODE, phrase, phrase
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
                        // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:193:84: ^( ND_TAGPROPERTY phrase phrase ( phrase )? ( TAG_MATCH_MODE )? )
                        {
                        Object root_1 = (Object)adaptor.nil();
                        root_1 = (Object)adaptor.becomeRoot(
                        (Object)adaptor.create(ND_TAGPROPERTY, "ND_TAGPROPERTY")
                        , root_1);

                        adaptor.addChild(root_1, stream_phrase.nextTree());

                        adaptor.addChild(root_1, stream_phrase.nextTree());

                        // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:193:115: ( phrase )?
                        if ( stream_phrase.hasNext() ) {
                            adaptor.addChild(root_1, stream_phrase.nextTree());

                        }
                        stream_phrase.reset();

                        // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:193:123: ( TAG_MATCH_MODE )?
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
    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:197:1: regQuery : REG EQUAL phrase ( 'CI' )? -> ^( ND_REG phrase ( 'CI' )? ) ;
    public final CatmaQueryParser.regQuery_return regQuery() throws RecognitionException {
        CatmaQueryParser.regQuery_return retval = new CatmaQueryParser.regQuery_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token REG53=null;
        Token EQUAL54=null;
        Token string_literal56=null;
        CatmaQueryParser.phrase_return phrase55 =null;


        Object REG53_tree=null;
        Object EQUAL54_tree=null;
        Object string_literal56_tree=null;
        RewriteRuleTokenStream stream_43=new RewriteRuleTokenStream(adaptor,"token 43");
        RewriteRuleTokenStream stream_REG=new RewriteRuleTokenStream(adaptor,"token REG");
        RewriteRuleTokenStream stream_EQUAL=new RewriteRuleTokenStream(adaptor,"token EQUAL");
        RewriteRuleSubtreeStream stream_phrase=new RewriteRuleSubtreeStream(adaptor,"rule phrase");
        try {
            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:198:2: ( REG EQUAL phrase ( 'CI' )? -> ^( ND_REG phrase ( 'CI' )? ) )
            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:198:4: REG EQUAL phrase ( 'CI' )?
            {
            REG53=(Token)match(input,REG,FOLLOW_REG_in_regQuery700);  
            stream_REG.add(REG53);


            EQUAL54=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_regQuery702);  
            stream_EQUAL.add(EQUAL54);


            pushFollow(FOLLOW_phrase_in_regQuery704);
            phrase55=phrase();

            state._fsp--;

            stream_phrase.add(phrase55.getTree());

            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:198:21: ( 'CI' )?
            int alt14=2;
            int LA14_0 = input.LA(1);

            if ( (LA14_0==43) ) {
                alt14=1;
            }
            switch (alt14) {
                case 1 :
                    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:198:21: 'CI'
                    {
                    string_literal56=(Token)match(input,43,FOLLOW_43_in_regQuery706);  
                    stream_43.add(string_literal56);


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
                // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:198:30: ^( ND_REG phrase ( 'CI' )? )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(ND_REG, "ND_REG")
                , root_1);

                adaptor.addChild(root_1, stream_phrase.nextTree());

                // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:198:46: ( 'CI' )?
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
    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:202:1: freqQuery : FREQ ( EQUAL INT ( '-' INT )? -> ^( ND_FREQ EQUAL INT ( INT )? ) | UNEQUAL INT -> ^( ND_FREQ UNEQUAL INT ) ) ;
    public final CatmaQueryParser.freqQuery_return freqQuery() throws RecognitionException {
        CatmaQueryParser.freqQuery_return retval = new CatmaQueryParser.freqQuery_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token FREQ57=null;
        Token EQUAL58=null;
        Token INT59=null;
        Token char_literal60=null;
        Token INT61=null;
        Token UNEQUAL62=null;
        Token INT63=null;

        Object FREQ57_tree=null;
        Object EQUAL58_tree=null;
        Object INT59_tree=null;
        Object char_literal60_tree=null;
        Object INT61_tree=null;
        Object UNEQUAL62_tree=null;
        Object INT63_tree=null;
        RewriteRuleTokenStream stream_FREQ=new RewriteRuleTokenStream(adaptor,"token FREQ");
        RewriteRuleTokenStream stream_INT=new RewriteRuleTokenStream(adaptor,"token INT");
        RewriteRuleTokenStream stream_41=new RewriteRuleTokenStream(adaptor,"token 41");
        RewriteRuleTokenStream stream_EQUAL=new RewriteRuleTokenStream(adaptor,"token EQUAL");
        RewriteRuleTokenStream stream_UNEQUAL=new RewriteRuleTokenStream(adaptor,"token UNEQUAL");

        try {
            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:203:2: ( FREQ ( EQUAL INT ( '-' INT )? -> ^( ND_FREQ EQUAL INT ( INT )? ) | UNEQUAL INT -> ^( ND_FREQ UNEQUAL INT ) ) )
            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:203:4: FREQ ( EQUAL INT ( '-' INT )? -> ^( ND_FREQ EQUAL INT ( INT )? ) | UNEQUAL INT -> ^( ND_FREQ UNEQUAL INT ) )
            {
            FREQ57=(Token)match(input,FREQ,FOLLOW_FREQ_in_freqQuery737);  
            stream_FREQ.add(FREQ57);


            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:204:3: ( EQUAL INT ( '-' INT )? -> ^( ND_FREQ EQUAL INT ( INT )? ) | UNEQUAL INT -> ^( ND_FREQ UNEQUAL INT ) )
            int alt16=2;
            int LA16_0 = input.LA(1);

            if ( (LA16_0==EQUAL) ) {
                alt16=1;
            }
            else if ( (LA16_0==UNEQUAL) ) {
                alt16=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 16, 0, input);

                throw nvae;

            }
            switch (alt16) {
                case 1 :
                    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:204:5: EQUAL INT ( '-' INT )?
                    {
                    EQUAL58=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_freqQuery744);  
                    stream_EQUAL.add(EQUAL58);


                    INT59=(Token)match(input,INT,FOLLOW_INT_in_freqQuery746);  
                    stream_INT.add(INT59);


                    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:204:15: ( '-' INT )?
                    int alt15=2;
                    int LA15_0 = input.LA(1);

                    if ( (LA15_0==41) ) {
                        int LA15_1 = input.LA(2);

                        if ( (LA15_1==INT) ) {
                            alt15=1;
                        }
                    }
                    switch (alt15) {
                        case 1 :
                            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:204:16: '-' INT
                            {
                            char_literal60=(Token)match(input,41,FOLLOW_41_in_freqQuery749);  
                            stream_41.add(char_literal60);


                            INT61=(Token)match(input,INT,FOLLOW_INT_in_freqQuery751);  
                            stream_INT.add(INT61);


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
                        // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:204:29: ^( ND_FREQ EQUAL INT ( INT )? )
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

                        // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:204:49: ( INT )?
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
                    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:205:5: UNEQUAL INT
                    {
                    UNEQUAL62=(Token)match(input,UNEQUAL,FOLLOW_UNEQUAL_in_freqQuery773);  
                    stream_UNEQUAL.add(UNEQUAL62);


                    INT63=(Token)match(input,INT,FOLLOW_INT_in_freqQuery775);  
                    stream_INT.add(INT63);


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
                        // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:205:20: ^( ND_FREQ UNEQUAL INT )
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
    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:209:1: similQuery : SIMIL EQUAL phrase INT ( '%' )? -> ^( ND_SIMIL phrase INT ) ;
    public final CatmaQueryParser.similQuery_return similQuery() throws RecognitionException {
        CatmaQueryParser.similQuery_return retval = new CatmaQueryParser.similQuery_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token SIMIL64=null;
        Token EQUAL65=null;
        Token INT67=null;
        Token char_literal68=null;
        CatmaQueryParser.phrase_return phrase66 =null;


        Object SIMIL64_tree=null;
        Object EQUAL65_tree=null;
        Object INT67_tree=null;
        Object char_literal68_tree=null;
        RewriteRuleTokenStream stream_INT=new RewriteRuleTokenStream(adaptor,"token INT");
        RewriteRuleTokenStream stream_36=new RewriteRuleTokenStream(adaptor,"token 36");
        RewriteRuleTokenStream stream_EQUAL=new RewriteRuleTokenStream(adaptor,"token EQUAL");
        RewriteRuleTokenStream stream_SIMIL=new RewriteRuleTokenStream(adaptor,"token SIMIL");
        RewriteRuleSubtreeStream stream_phrase=new RewriteRuleSubtreeStream(adaptor,"rule phrase");
        try {
            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:210:2: ( SIMIL EQUAL phrase INT ( '%' )? -> ^( ND_SIMIL phrase INT ) )
            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:210:4: SIMIL EQUAL phrase INT ( '%' )?
            {
            SIMIL64=(Token)match(input,SIMIL,FOLLOW_SIMIL_in_similQuery807);  
            stream_SIMIL.add(SIMIL64);


            EQUAL65=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_similQuery809);  
            stream_EQUAL.add(EQUAL65);


            pushFollow(FOLLOW_phrase_in_similQuery811);
            phrase66=phrase();

            state._fsp--;

            stream_phrase.add(phrase66.getTree());

            INT67=(Token)match(input,INT,FOLLOW_INT_in_similQuery813);  
            stream_INT.add(INT67);


            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:210:27: ( '%' )?
            int alt17=2;
            int LA17_0 = input.LA(1);

            if ( (LA17_0==36) ) {
                alt17=1;
            }
            switch (alt17) {
                case 1 :
                    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:210:27: '%'
                    {
                    char_literal68=(Token)match(input,36,FOLLOW_36_in_similQuery815);  
                    stream_36.add(char_literal68);


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
                // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:210:35: ^( ND_SIMIL phrase INT )
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
    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:214:1: wildQuery : WILD EQUAL phrase -> ^( ND_WILD phrase ) ;
    public final CatmaQueryParser.wildQuery_return wildQuery() throws RecognitionException {
        CatmaQueryParser.wildQuery_return retval = new CatmaQueryParser.wildQuery_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token WILD69=null;
        Token EQUAL70=null;
        CatmaQueryParser.phrase_return phrase71 =null;


        Object WILD69_tree=null;
        Object EQUAL70_tree=null;
        RewriteRuleTokenStream stream_WILD=new RewriteRuleTokenStream(adaptor,"token WILD");
        RewriteRuleTokenStream stream_EQUAL=new RewriteRuleTokenStream(adaptor,"token EQUAL");
        RewriteRuleSubtreeStream stream_phrase=new RewriteRuleSubtreeStream(adaptor,"rule phrase");
        try {
            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:215:2: ( WILD EQUAL phrase -> ^( ND_WILD phrase ) )
            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:215:4: WILD EQUAL phrase
            {
            WILD69=(Token)match(input,WILD,FOLLOW_WILD_in_wildQuery845);  
            stream_WILD.add(WILD69);


            EQUAL70=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_wildQuery847);  
            stream_EQUAL.add(EQUAL70);


            pushFollow(FOLLOW_phrase_in_wildQuery849);
            phrase71=phrase();

            state._fsp--;

            stream_phrase.add(phrase71.getTree());

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
                // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:215:25: ^( ND_WILD phrase )
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
    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:225:1: refinement : 'where' refinementExpression -> refinementExpression ;
    public final CatmaQueryParser.refinement_return refinement() throws RecognitionException {
        CatmaQueryParser.refinement_return retval = new CatmaQueryParser.refinement_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token string_literal72=null;
        CatmaQueryParser.refinementExpression_return refinementExpression73 =null;


        Object string_literal72_tree=null;
        RewriteRuleTokenStream stream_46=new RewriteRuleTokenStream(adaptor,"token 46");
        RewriteRuleSubtreeStream stream_refinementExpression=new RewriteRuleSubtreeStream(adaptor,"rule refinementExpression");
        try {
            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:226:2: ( 'where' refinementExpression -> refinementExpression )
            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:226:4: 'where' refinementExpression
            {
            string_literal72=(Token)match(input,46,FOLLOW_46_in_refinement882);  
            stream_46.add(string_literal72);


            pushFollow(FOLLOW_refinementExpression_in_refinement884);
            refinementExpression73=refinementExpression();

            state._fsp--;

            stream_refinementExpression.add(refinementExpression73.getTree());

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
    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:230:1: refinementExpression : startRefinement= refinementTerm ( orRefinement[(CommonTree)$startRefinement.tree] -> orRefinement | andRefinement[(CommonTree)$startRefinement.tree] -> andRefinement | -> ^( ND_REFINE refinementTerm ) ) ;
    public final CatmaQueryParser.refinementExpression_return refinementExpression() throws RecognitionException {
        CatmaQueryParser.refinementExpression_return retval = new CatmaQueryParser.refinementExpression_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        CatmaQueryParser.refinementTerm_return startRefinement =null;

        CatmaQueryParser.orRefinement_return orRefinement74 =null;

        CatmaQueryParser.andRefinement_return andRefinement75 =null;


        RewriteRuleSubtreeStream stream_orRefinement=new RewriteRuleSubtreeStream(adaptor,"rule orRefinement");
        RewriteRuleSubtreeStream stream_andRefinement=new RewriteRuleSubtreeStream(adaptor,"rule andRefinement");
        RewriteRuleSubtreeStream stream_refinementTerm=new RewriteRuleSubtreeStream(adaptor,"rule refinementTerm");
        try {
            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:231:2: (startRefinement= refinementTerm ( orRefinement[(CommonTree)$startRefinement.tree] -> orRefinement | andRefinement[(CommonTree)$startRefinement.tree] -> andRefinement | -> ^( ND_REFINE refinementTerm ) ) )
            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:231:4: startRefinement= refinementTerm ( orRefinement[(CommonTree)$startRefinement.tree] -> orRefinement | andRefinement[(CommonTree)$startRefinement.tree] -> andRefinement | -> ^( ND_REFINE refinementTerm ) )
            {
            pushFollow(FOLLOW_refinementTerm_in_refinementExpression908);
            startRefinement=refinementTerm();

            state._fsp--;

            stream_refinementTerm.add(startRefinement.getTree());

            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:231:35: ( orRefinement[(CommonTree)$startRefinement.tree] -> orRefinement | andRefinement[(CommonTree)$startRefinement.tree] -> andRefinement | -> ^( ND_REFINE refinementTerm ) )
            int alt18=3;
            switch ( input.LA(1) ) {
            case 47:
                {
                alt18=1;
                }
                break;
            case 40:
                {
                alt18=2;
                }
                break;
            case EOF:
            case 39:
                {
                alt18=3;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 18, 0, input);

                throw nvae;

            }

            switch (alt18) {
                case 1 :
                    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:232:4: orRefinement[(CommonTree)$startRefinement.tree]
                    {
                    pushFollow(FOLLOW_orRefinement_in_refinementExpression916);
                    orRefinement74=orRefinement((CommonTree)(startRefinement!=null?((Object)startRefinement.tree):null));

                    state._fsp--;

                    stream_orRefinement.add(orRefinement74.getTree());

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
                    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:233:6: andRefinement[(CommonTree)$startRefinement.tree]
                    {
                    pushFollow(FOLLOW_andRefinement_in_refinementExpression928);
                    andRefinement75=andRefinement((CommonTree)(startRefinement!=null?((Object)startRefinement.tree):null));

                    state._fsp--;

                    stream_andRefinement.add(andRefinement75.getTree());

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
                    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:234:6: 
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
                        // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:234:9: ^( ND_REFINE refinementTerm )
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
    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:238:1: orRefinement[CommonTree startRefinement] : '|' refinementTerm -> ^( ND_ORREFINE refinementTerm ) ;
    public final CatmaQueryParser.orRefinement_return orRefinement(CommonTree startRefinement) throws RecognitionException {
        CatmaQueryParser.orRefinement_return retval = new CatmaQueryParser.orRefinement_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token char_literal76=null;
        CatmaQueryParser.refinementTerm_return refinementTerm77 =null;


        Object char_literal76_tree=null;
        RewriteRuleTokenStream stream_47=new RewriteRuleTokenStream(adaptor,"token 47");
        RewriteRuleSubtreeStream stream_refinementTerm=new RewriteRuleSubtreeStream(adaptor,"rule refinementTerm");
        try {
            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:239:2: ( '|' refinementTerm -> ^( ND_ORREFINE refinementTerm ) )
            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:239:4: '|' refinementTerm
            {
            char_literal76=(Token)match(input,47,FOLLOW_47_in_orRefinement967);  
            stream_47.add(char_literal76);


            pushFollow(FOLLOW_refinementTerm_in_orRefinement969);
            refinementTerm77=refinementTerm();

            state._fsp--;

            stream_refinementTerm.add(refinementTerm77.getTree());

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
                // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:239:26: ^( ND_ORREFINE refinementTerm )
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
    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:243:1: andRefinement[CommonTree startRefinement] : ',' refinementTerm -> ^( ND_ANDREFINE refinementTerm ) ;
    public final CatmaQueryParser.andRefinement_return andRefinement(CommonTree startRefinement) throws RecognitionException {
        CatmaQueryParser.andRefinement_return retval = new CatmaQueryParser.andRefinement_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token char_literal78=null;
        CatmaQueryParser.refinementTerm_return refinementTerm79 =null;


        Object char_literal78_tree=null;
        RewriteRuleTokenStream stream_40=new RewriteRuleTokenStream(adaptor,"token 40");
        RewriteRuleSubtreeStream stream_refinementTerm=new RewriteRuleSubtreeStream(adaptor,"rule refinementTerm");
        try {
            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:244:2: ( ',' refinementTerm -> ^( ND_ANDREFINE refinementTerm ) )
            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:244:4: ',' refinementTerm
            {
            char_literal78=(Token)match(input,40,FOLLOW_40_in_andRefinement999);  
            stream_40.add(char_literal78);


            pushFollow(FOLLOW_refinementTerm_in_andRefinement1001);
            refinementTerm79=refinementTerm();

            state._fsp--;

            stream_refinementTerm.add(refinementTerm79.getTree());

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
                // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:244:26: ^( ND_ANDREFINE refinementTerm )
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
    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:248:1: refinementTerm : ( selector -> selector | '(' refinementExpression ')' -> refinementExpression );
    public final CatmaQueryParser.refinementTerm_return refinementTerm() throws RecognitionException {
        CatmaQueryParser.refinementTerm_return retval = new CatmaQueryParser.refinementTerm_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token char_literal81=null;
        Token char_literal83=null;
        CatmaQueryParser.selector_return selector80 =null;

        CatmaQueryParser.refinementExpression_return refinementExpression82 =null;


        Object char_literal81_tree=null;
        Object char_literal83_tree=null;
        RewriteRuleTokenStream stream_39=new RewriteRuleTokenStream(adaptor,"token 39");
        RewriteRuleTokenStream stream_38=new RewriteRuleTokenStream(adaptor,"token 38");
        RewriteRuleSubtreeStream stream_selector=new RewriteRuleSubtreeStream(adaptor,"rule selector");
        RewriteRuleSubtreeStream stream_refinementExpression=new RewriteRuleSubtreeStream(adaptor,"rule refinementExpression");
        try {
            // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:249:2: ( selector -> selector | '(' refinementExpression ')' -> refinementExpression )
            int alt19=2;
            int LA19_0 = input.LA(1);

            if ( (LA19_0==FREQ||(LA19_0 >= PROPERTY && LA19_0 <= TAG)||LA19_0==WILD) ) {
                alt19=1;
            }
            else if ( (LA19_0==38) ) {
                alt19=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 19, 0, input);

                throw nvae;

            }
            switch (alt19) {
                case 1 :
                    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:249:5: selector
                    {
                    pushFollow(FOLLOW_selector_in_refinementTerm1031);
                    selector80=selector();

                    state._fsp--;

                    stream_selector.add(selector80.getTree());

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
                    // C:\\data\\eclipse_workspace\\catma\\grammars\\ast\\CatmaQuery.g:250:5: '(' refinementExpression ')'
                    {
                    char_literal81=(Token)match(input,38,FOLLOW_38_in_refinementTerm1041);  
                    stream_38.add(char_literal81);


                    pushFollow(FOLLOW_refinementExpression_in_refinementTerm1042);
                    refinementExpression82=refinementExpression();

                    state._fsp--;

                    stream_refinementExpression.add(refinementExpression82.getTree());

                    char_literal83=(Token)match(input,39,FOLLOW_39_in_refinementTerm1043);  
                    stream_39.add(char_literal83);


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
    public static final BitSet FOLLOW_queryExpression_in_query169 = new BitSet(new long[]{0x0000400000000002L});
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
    public static final BitSet FOLLOW_term_in_collocQuery337 = new BitSet(new long[]{0x0000200000000082L});
    public static final BitSet FOLLOW_INT_in_collocQuery339 = new BitSet(new long[]{0x0000200000000002L});
    public static final BitSet FOLLOW_45_in_collocQuery342 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_41_in_exclusionQuery379 = new BitSet(new long[]{0x00000048BC000020L});
    public static final BitSet FOLLOW_term_in_exclusionQuery381 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_42_in_adjacencyQuery412 = new BitSet(new long[]{0x00000048BC000020L});
    public static final BitSet FOLLOW_term_in_adjacencyQuery414 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_phrase_in_term444 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_selector_in_term454 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_38_in_term465 = new BitSet(new long[]{0x00000048BC000020L});
    public static final BitSet FOLLOW_query_in_term466 = new BitSet(new long[]{0x0000008000000000L});
    public static final BitSet FOLLOW_39_in_term467 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_TXT_in_phrase491 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_tagQuery_in_selector524 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_propertyQuery_in_selector529 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_regQuery_in_selector534 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_freqQuery_in_selector539 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_similQuery_in_selector544 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_wildQuery_in_selector549 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_TAG_in_tagQuery569 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_EQUAL_in_tagQuery571 = new BitSet(new long[]{0x0000000080000000L});
    public static final BitSet FOLLOW_phrase_in_tagQuery573 = new BitSet(new long[]{0x0000000040000002L});
    public static final BitSet FOLLOW_TAG_MATCH_MODE_in_tagQuery575 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_PROPERTY_in_propertyQuery608 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_EQUAL_in_propertyQuery610 = new BitSet(new long[]{0x0000000080000000L});
    public static final BitSet FOLLOW_phrase_in_propertyQuery612 = new BitSet(new long[]{0x0000000240000002L});
    public static final BitSet FOLLOW_VALUE_in_propertyQuery615 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_EQUAL_in_propertyQuery617 = new BitSet(new long[]{0x0000000080000000L});
    public static final BitSet FOLLOW_phrase_in_propertyQuery619 = new BitSet(new long[]{0x0000000040000002L});
    public static final BitSet FOLLOW_TAG_MATCH_MODE_in_propertyQuery623 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_TAG_in_propertyQuery644 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_EQUAL_in_propertyQuery646 = new BitSet(new long[]{0x0000000080000000L});
    public static final BitSet FOLLOW_phrase_in_propertyQuery648 = new BitSet(new long[]{0x0000000004000000L});
    public static final BitSet FOLLOW_PROPERTY_in_propertyQuery650 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_EQUAL_in_propertyQuery652 = new BitSet(new long[]{0x0000000080000000L});
    public static final BitSet FOLLOW_phrase_in_propertyQuery654 = new BitSet(new long[]{0x0000000240000002L});
    public static final BitSet FOLLOW_VALUE_in_propertyQuery657 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_EQUAL_in_propertyQuery659 = new BitSet(new long[]{0x0000000080000000L});
    public static final BitSet FOLLOW_phrase_in_propertyQuery661 = new BitSet(new long[]{0x0000000040000002L});
    public static final BitSet FOLLOW_TAG_MATCH_MODE_in_propertyQuery665 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_REG_in_regQuery700 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_EQUAL_in_regQuery702 = new BitSet(new long[]{0x0000000080000000L});
    public static final BitSet FOLLOW_phrase_in_regQuery704 = new BitSet(new long[]{0x0000080000000002L});
    public static final BitSet FOLLOW_43_in_regQuery706 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_FREQ_in_freqQuery737 = new BitSet(new long[]{0x0000000100000010L});
    public static final BitSet FOLLOW_EQUAL_in_freqQuery744 = new BitSet(new long[]{0x0000000000000080L});
    public static final BitSet FOLLOW_INT_in_freqQuery746 = new BitSet(new long[]{0x0000020000000002L});
    public static final BitSet FOLLOW_41_in_freqQuery749 = new BitSet(new long[]{0x0000000000000080L});
    public static final BitSet FOLLOW_INT_in_freqQuery751 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_UNEQUAL_in_freqQuery773 = new BitSet(new long[]{0x0000000000000080L});
    public static final BitSet FOLLOW_INT_in_freqQuery775 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_SIMIL_in_similQuery807 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_EQUAL_in_similQuery809 = new BitSet(new long[]{0x0000000080000000L});
    public static final BitSet FOLLOW_phrase_in_similQuery811 = new BitSet(new long[]{0x0000000000000080L});
    public static final BitSet FOLLOW_INT_in_similQuery813 = new BitSet(new long[]{0x0000001000000002L});
    public static final BitSet FOLLOW_36_in_similQuery815 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_WILD_in_wildQuery845 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_EQUAL_in_wildQuery847 = new BitSet(new long[]{0x0000000080000000L});
    public static final BitSet FOLLOW_phrase_in_wildQuery849 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_46_in_refinement882 = new BitSet(new long[]{0x000000483C000020L});
    public static final BitSet FOLLOW_refinementExpression_in_refinement884 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_refinementTerm_in_refinementExpression908 = new BitSet(new long[]{0x0000810000000002L});
    public static final BitSet FOLLOW_orRefinement_in_refinementExpression916 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_andRefinement_in_refinementExpression928 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_47_in_orRefinement967 = new BitSet(new long[]{0x000000483C000020L});
    public static final BitSet FOLLOW_refinementTerm_in_orRefinement969 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_40_in_andRefinement999 = new BitSet(new long[]{0x000000483C000020L});
    public static final BitSet FOLLOW_refinementTerm_in_andRefinement1001 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_selector_in_refinementTerm1031 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_38_in_refinementTerm1041 = new BitSet(new long[]{0x000000483C000020L});
    public static final BitSet FOLLOW_refinementExpression_in_refinementTerm1042 = new BitSet(new long[]{0x0000008000000000L});
    public static final BitSet FOLLOW_39_in_refinementTerm1043 = new BitSet(new long[]{0x0000000000000002L});

}