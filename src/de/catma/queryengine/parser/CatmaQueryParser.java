// $ANTLR 3.4 C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g 2012-05-17 17:47:53

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
        "<invalid>", "<EOR>", "<DOWN>", "<UP>", "EQUAL", "FREQ", "GROUPIDENT", "INT", "LETTER", "LETTEREXTENDED", "ND_ADJACENCY", "ND_ANDREFINE", "ND_COLLOC", "ND_EXCLUSION", "ND_FREQ", "ND_ORREFINE", "ND_PHRASE", "ND_PROPERTY", "ND_QUERY", "ND_REFINE", "ND_REG", "ND_SIMIL", "ND_TAG", "ND_UNION", "PROPERTY", "REG", "SIMIL", "TAG", "TAG_MATCH_MODE", "TXT", "UNEQUAL", "VALUE", "WHITESPACE", "'%'", "'&'", "'('", "')'", "','", "'-'", "';'", "'CI'", "'where'", "'|'"
    };

    public static final int EOF=-1;
    public static final int T__33=33;
    public static final int T__34=34;
    public static final int T__35=35;
    public static final int T__36=36;
    public static final int T__37=37;
    public static final int T__38=38;
    public static final int T__39=39;
    public static final int T__40=40;
    public static final int T__41=41;
    public static final int T__42=42;
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
    public static final int ND_UNION=23;
    public static final int PROPERTY=24;
    public static final int REG=25;
    public static final int SIMIL=26;
    public static final int TAG=27;
    public static final int TAG_MATCH_MODE=28;
    public static final int TXT=29;
    public static final int UNEQUAL=30;
    public static final int VALUE=31;
    public static final int WHITESPACE=32;

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
    public String getGrammarFileName() { return "C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g"; }



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
    // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:103:1: start : query EOF ;
    public final CatmaQueryParser.start_return start() throws RecognitionException {
        CatmaQueryParser.start_return retval = new CatmaQueryParser.start_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token EOF2=null;
        CatmaQueryParser.query_return query1 =null;


        Object EOF2_tree=null;

        try {
            // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:103:7: ( query EOF )
            // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:103:9: query EOF
            {
            root_0 = (Object)adaptor.nil();


            pushFollow(FOLLOW_query_in_start143);
            query1=query();

            state._fsp--;

            adaptor.addChild(root_0, query1.getTree());

            EOF2=(Token)match(input,EOF,FOLLOW_EOF_in_start145); 
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
    // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:107:1: query : queryExpression ( refinement )? -> ^( ND_QUERY queryExpression ( refinement )? ) ;
    public final CatmaQueryParser.query_return query() throws RecognitionException {
        CatmaQueryParser.query_return retval = new CatmaQueryParser.query_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        CatmaQueryParser.queryExpression_return queryExpression3 =null;

        CatmaQueryParser.refinement_return refinement4 =null;


        RewriteRuleSubtreeStream stream_refinement=new RewriteRuleSubtreeStream(adaptor,"rule refinement");
        RewriteRuleSubtreeStream stream_queryExpression=new RewriteRuleSubtreeStream(adaptor,"rule queryExpression");
        try {
            // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:107:7: ( queryExpression ( refinement )? -> ^( ND_QUERY queryExpression ( refinement )? ) )
            // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:107:9: queryExpression ( refinement )?
            {
            pushFollow(FOLLOW_queryExpression_in_query161);
            queryExpression3=queryExpression();

            state._fsp--;

            stream_queryExpression.add(queryExpression3.getTree());

            // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:107:25: ( refinement )?
            int alt1=2;
            int LA1_0 = input.LA(1);

            if ( (LA1_0==41) ) {
                alt1=1;
            }
            switch (alt1) {
                case 1 :
                    // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:107:25: refinement
                    {
                    pushFollow(FOLLOW_refinement_in_query163);
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
            // 107:37: -> ^( ND_QUERY queryExpression ( refinement )? )
            {
                // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:107:40: ^( ND_QUERY queryExpression ( refinement )? )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(ND_QUERY, "ND_QUERY")
                , root_1);

                adaptor.addChild(root_1, stream_queryExpression.nextTree());

                // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:107:67: ( refinement )?
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
    // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:116:1: queryExpression : startTerm= term ( unionQuery[(CommonTree)$startTerm.tree] -> unionQuery | collocQuery[(CommonTree)$startTerm.tree] -> collocQuery | exclusionQuery[(CommonTree)$startTerm.tree] -> exclusionQuery | adjacencyQuery[(CommonTree)$startTerm.tree] -> adjacencyQuery | -> term ) ;
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
            // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:117:2: (startTerm= term ( unionQuery[(CommonTree)$startTerm.tree] -> unionQuery | collocQuery[(CommonTree)$startTerm.tree] -> collocQuery | exclusionQuery[(CommonTree)$startTerm.tree] -> exclusionQuery | adjacencyQuery[(CommonTree)$startTerm.tree] -> adjacencyQuery | -> term ) )
            // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:117:4: startTerm= term ( unionQuery[(CommonTree)$startTerm.tree] -> unionQuery | collocQuery[(CommonTree)$startTerm.tree] -> collocQuery | exclusionQuery[(CommonTree)$startTerm.tree] -> exclusionQuery | adjacencyQuery[(CommonTree)$startTerm.tree] -> adjacencyQuery | -> term )
            {
            pushFollow(FOLLOW_term_in_queryExpression200);
            startTerm=term();

            state._fsp--;

            stream_term.add(startTerm.getTree());

            // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:117:19: ( unionQuery[(CommonTree)$startTerm.tree] -> unionQuery | collocQuery[(CommonTree)$startTerm.tree] -> collocQuery | exclusionQuery[(CommonTree)$startTerm.tree] -> exclusionQuery | adjacencyQuery[(CommonTree)$startTerm.tree] -> adjacencyQuery | -> term )
            int alt2=5;
            switch ( input.LA(1) ) {
            case 37:
                {
                alt2=1;
                }
                break;
            case 34:
                {
                alt2=2;
                }
                break;
            case 38:
                {
                alt2=3;
                }
                break;
            case 39:
                {
                alt2=4;
                }
                break;
            case EOF:
            case 36:
            case 41:
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
                    // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:118:4: unionQuery[(CommonTree)$startTerm.tree]
                    {
                    pushFollow(FOLLOW_unionQuery_in_queryExpression207);
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
                    // 118:44: -> unionQuery
                    {
                        adaptor.addChild(root_0, stream_unionQuery.nextTree());

                    }


                    retval.tree = root_0;

                    }
                    break;
                case 2 :
                    // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:119:6: collocQuery[(CommonTree)$startTerm.tree]
                    {
                    pushFollow(FOLLOW_collocQuery_in_queryExpression219);
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
                    // 119:48: -> collocQuery
                    {
                        adaptor.addChild(root_0, stream_collocQuery.nextTree());

                    }


                    retval.tree = root_0;

                    }
                    break;
                case 3 :
                    // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:120:6: exclusionQuery[(CommonTree)$startTerm.tree]
                    {
                    pushFollow(FOLLOW_exclusionQuery_in_queryExpression232);
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
                    // 120:51: -> exclusionQuery
                    {
                        adaptor.addChild(root_0, stream_exclusionQuery.nextTree());

                    }


                    retval.tree = root_0;

                    }
                    break;
                case 4 :
                    // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:121:6: adjacencyQuery[(CommonTree)$startTerm.tree]
                    {
                    pushFollow(FOLLOW_adjacencyQuery_in_queryExpression245);
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
                    // 121:51: -> adjacencyQuery
                    {
                        adaptor.addChild(root_0, stream_adjacencyQuery.nextTree());

                    }


                    retval.tree = root_0;

                    }
                    break;
                case 5 :
                    // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:122:6: 
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
                    // 122:6: -> term
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
    // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:132:1: unionQuery[CommonTree startTerm] : ',' term -> ^( ND_UNION term ) ;
    public final CatmaQueryParser.unionQuery_return unionQuery(CommonTree startTerm) throws RecognitionException {
        CatmaQueryParser.unionQuery_return retval = new CatmaQueryParser.unionQuery_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token char_literal9=null;
        CatmaQueryParser.term_return term10 =null;


        Object char_literal9_tree=null;
        RewriteRuleTokenStream stream_37=new RewriteRuleTokenStream(adaptor,"token 37");
        RewriteRuleSubtreeStream stream_term=new RewriteRuleSubtreeStream(adaptor,"rule term");
        try {
            // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:133:2: ( ',' term -> ^( ND_UNION term ) )
            // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:133:4: ',' term
            {
            char_literal9=(Token)match(input,37,FOLLOW_37_in_unionQuery288);  
            stream_37.add(char_literal9);


            pushFollow(FOLLOW_term_in_unionQuery290);
            term10=term();

            state._fsp--;

            stream_term.add(term10.getTree());

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
            // 133:13: -> ^( ND_UNION term )
            {
                // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:133:16: ^( ND_UNION term )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(ND_UNION, "ND_UNION")
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
    // $ANTLR end "unionQuery"


    public static class collocQuery_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "collocQuery"
    // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:137:1: collocQuery[CommonTree startTerm] : '&' term ( INT )? -> ^( ND_COLLOC term ( INT )? ) ;
    public final CatmaQueryParser.collocQuery_return collocQuery(CommonTree startTerm) throws RecognitionException {
        CatmaQueryParser.collocQuery_return retval = new CatmaQueryParser.collocQuery_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token char_literal11=null;
        Token INT13=null;
        CatmaQueryParser.term_return term12 =null;


        Object char_literal11_tree=null;
        Object INT13_tree=null;
        RewriteRuleTokenStream stream_INT=new RewriteRuleTokenStream(adaptor,"token INT");
        RewriteRuleTokenStream stream_34=new RewriteRuleTokenStream(adaptor,"token 34");
        RewriteRuleSubtreeStream stream_term=new RewriteRuleSubtreeStream(adaptor,"rule term");
        try {
            // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:138:2: ( '&' term ( INT )? -> ^( ND_COLLOC term ( INT )? ) )
            // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:138:4: '&' term ( INT )?
            {
            char_literal11=(Token)match(input,34,FOLLOW_34_in_collocQuery321);  
            stream_34.add(char_literal11);


            pushFollow(FOLLOW_term_in_collocQuery323);
            term12=term();

            state._fsp--;

            stream_term.add(term12.getTree());

            // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:138:13: ( INT )?
            int alt3=2;
            int LA3_0 = input.LA(1);

            if ( (LA3_0==INT) ) {
                alt3=1;
            }
            switch (alt3) {
                case 1 :
                    // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:138:13: INT
                    {
                    INT13=(Token)match(input,INT,FOLLOW_INT_in_collocQuery325);  
                    stream_INT.add(INT13);


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
            // 138:18: -> ^( ND_COLLOC term ( INT )? )
            {
                // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:138:21: ^( ND_COLLOC term ( INT )? )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(ND_COLLOC, "ND_COLLOC")
                , root_1);

                adaptor.addChild(root_1, startTerm);

                adaptor.addChild(root_1, stream_term.nextTree());

                // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:138:51: ( INT )?
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
    // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:142:1: exclusionQuery[CommonTree startTerm] : '-' term -> ^( ND_EXCLUSION term ) ;
    public final CatmaQueryParser.exclusionQuery_return exclusionQuery(CommonTree startTerm) throws RecognitionException {
        CatmaQueryParser.exclusionQuery_return retval = new CatmaQueryParser.exclusionQuery_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token char_literal14=null;
        CatmaQueryParser.term_return term15 =null;


        Object char_literal14_tree=null;
        RewriteRuleTokenStream stream_38=new RewriteRuleTokenStream(adaptor,"token 38");
        RewriteRuleSubtreeStream stream_term=new RewriteRuleSubtreeStream(adaptor,"rule term");
        try {
            // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:143:2: ( '-' term -> ^( ND_EXCLUSION term ) )
            // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:143:4: '-' term
            {
            char_literal14=(Token)match(input,38,FOLLOW_38_in_exclusionQuery359);  
            stream_38.add(char_literal14);


            pushFollow(FOLLOW_term_in_exclusionQuery361);
            term15=term();

            state._fsp--;

            stream_term.add(term15.getTree());

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
            // 143:13: -> ^( ND_EXCLUSION term )
            {
                // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:143:16: ^( ND_EXCLUSION term )
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
    // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:147:1: adjacencyQuery[CommonTree startTerm] : ';' term -> ^( ND_ADJACENCY term ) ;
    public final CatmaQueryParser.adjacencyQuery_return adjacencyQuery(CommonTree startTerm) throws RecognitionException {
        CatmaQueryParser.adjacencyQuery_return retval = new CatmaQueryParser.adjacencyQuery_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token char_literal16=null;
        CatmaQueryParser.term_return term17 =null;


        Object char_literal16_tree=null;
        RewriteRuleTokenStream stream_39=new RewriteRuleTokenStream(adaptor,"token 39");
        RewriteRuleSubtreeStream stream_term=new RewriteRuleSubtreeStream(adaptor,"rule term");
        try {
            // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:148:2: ( ';' term -> ^( ND_ADJACENCY term ) )
            // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:148:4: ';' term
            {
            char_literal16=(Token)match(input,39,FOLLOW_39_in_adjacencyQuery392);  
            stream_39.add(char_literal16);


            pushFollow(FOLLOW_term_in_adjacencyQuery394);
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
            // 148:13: -> ^( ND_ADJACENCY term )
            {
                // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:148:16: ^( ND_ADJACENCY term )
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
    // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:153:1: term : ( phrase -> phrase | selector -> selector | '(' query ')' -> query );
    public final CatmaQueryParser.term_return term() throws RecognitionException {
        CatmaQueryParser.term_return retval = new CatmaQueryParser.term_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token char_literal20=null;
        Token char_literal22=null;
        CatmaQueryParser.phrase_return phrase18 =null;

        CatmaQueryParser.selector_return selector19 =null;

        CatmaQueryParser.query_return query21 =null;


        Object char_literal20_tree=null;
        Object char_literal22_tree=null;
        RewriteRuleTokenStream stream_35=new RewriteRuleTokenStream(adaptor,"token 35");
        RewriteRuleTokenStream stream_36=new RewriteRuleTokenStream(adaptor,"token 36");
        RewriteRuleSubtreeStream stream_selector=new RewriteRuleSubtreeStream(adaptor,"rule selector");
        RewriteRuleSubtreeStream stream_query=new RewriteRuleSubtreeStream(adaptor,"rule query");
        RewriteRuleSubtreeStream stream_phrase=new RewriteRuleSubtreeStream(adaptor,"rule phrase");
        try {
            // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:153:7: ( phrase -> phrase | selector -> selector | '(' query ')' -> query )
            int alt4=3;
            switch ( input.LA(1) ) {
            case TXT:
                {
                alt4=1;
                }
                break;
            case FREQ:
            case PROPERTY:
            case REG:
            case SIMIL:
            case TAG:
                {
                alt4=2;
                }
                break;
            case 35:
                {
                alt4=3;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 4, 0, input);

                throw nvae;

            }

            switch (alt4) {
                case 1 :
                    // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:153:9: phrase
                    {
                    pushFollow(FOLLOW_phrase_in_term424);
                    phrase18=phrase();

                    state._fsp--;

                    stream_phrase.add(phrase18.getTree());

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
                    // 153:16: -> phrase
                    {
                        adaptor.addChild(root_0, stream_phrase.nextTree());

                    }


                    retval.tree = root_0;

                    }
                    break;
                case 2 :
                    // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:154:5: selector
                    {
                    pushFollow(FOLLOW_selector_in_term434);
                    selector19=selector();

                    state._fsp--;

                    stream_selector.add(selector19.getTree());

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
                    // 154:14: -> selector
                    {
                        adaptor.addChild(root_0, stream_selector.nextTree());

                    }


                    retval.tree = root_0;

                    }
                    break;
                case 3 :
                    // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:155:5: '(' query ')'
                    {
                    char_literal20=(Token)match(input,35,FOLLOW_35_in_term445);  
                    stream_35.add(char_literal20);


                    pushFollow(FOLLOW_query_in_term446);
                    query21=query();

                    state._fsp--;

                    stream_query.add(query21.getTree());

                    char_literal22=(Token)match(input,36,FOLLOW_36_in_term447);  
                    stream_36.add(char_literal22);


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
                    // 155:17: -> query
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
    // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:162:1: phrase : TXT -> ^( ND_PHRASE TXT ) ;
    public final CatmaQueryParser.phrase_return phrase() throws RecognitionException {
        CatmaQueryParser.phrase_return retval = new CatmaQueryParser.phrase_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token TXT23=null;

        Object TXT23_tree=null;
        RewriteRuleTokenStream stream_TXT=new RewriteRuleTokenStream(adaptor,"token TXT");

        try {
            // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:162:8: ( TXT -> ^( ND_PHRASE TXT ) )
            // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:162:10: TXT
            {
            TXT23=(Token)match(input,TXT,FOLLOW_TXT_in_phrase471);  
            stream_TXT.add(TXT23);


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
            // 162:14: -> ^( ND_PHRASE TXT )
            {
                // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:162:17: ^( ND_PHRASE TXT )
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
    // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:172:1: selector : ( tagQuery | propertyQuery | regQuery | freqQuery | similQuery );
    public final CatmaQueryParser.selector_return selector() throws RecognitionException {
        CatmaQueryParser.selector_return retval = new CatmaQueryParser.selector_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        CatmaQueryParser.tagQuery_return tagQuery24 =null;

        CatmaQueryParser.propertyQuery_return propertyQuery25 =null;

        CatmaQueryParser.regQuery_return regQuery26 =null;

        CatmaQueryParser.freqQuery_return freqQuery27 =null;

        CatmaQueryParser.similQuery_return similQuery28 =null;



        try {
            // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:173:2: ( tagQuery | propertyQuery | regQuery | freqQuery | similQuery )
            int alt5=5;
            switch ( input.LA(1) ) {
            case TAG:
                {
                alt5=1;
                }
                break;
            case PROPERTY:
                {
                alt5=2;
                }
                break;
            case REG:
                {
                alt5=3;
                }
                break;
            case FREQ:
                {
                alt5=4;
                }
                break;
            case SIMIL:
                {
                alt5=5;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 5, 0, input);

                throw nvae;

            }

            switch (alt5) {
                case 1 :
                    // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:173:4: tagQuery
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_tagQuery_in_selector504);
                    tagQuery24=tagQuery();

                    state._fsp--;

                    adaptor.addChild(root_0, tagQuery24.getTree());

                    }
                    break;
                case 2 :
                    // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:174:4: propertyQuery
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_propertyQuery_in_selector509);
                    propertyQuery25=propertyQuery();

                    state._fsp--;

                    adaptor.addChild(root_0, propertyQuery25.getTree());

                    }
                    break;
                case 3 :
                    // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:175:4: regQuery
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_regQuery_in_selector514);
                    regQuery26=regQuery();

                    state._fsp--;

                    adaptor.addChild(root_0, regQuery26.getTree());

                    }
                    break;
                case 4 :
                    // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:176:4: freqQuery
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_freqQuery_in_selector519);
                    freqQuery27=freqQuery();

                    state._fsp--;

                    adaptor.addChild(root_0, freqQuery27.getTree());

                    }
                    break;
                case 5 :
                    // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:177:4: similQuery
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_similQuery_in_selector524);
                    similQuery28=similQuery();

                    state._fsp--;

                    adaptor.addChild(root_0, similQuery28.getTree());

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
    // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:182:1: tagQuery : TAG EQUAL phrase ( TAG_MATCH_MODE )? -> ^( ND_TAG phrase ( TAG_MATCH_MODE )? ) ;
    public final CatmaQueryParser.tagQuery_return tagQuery() throws RecognitionException {
        CatmaQueryParser.tagQuery_return retval = new CatmaQueryParser.tagQuery_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token TAG29=null;
        Token EQUAL30=null;
        Token TAG_MATCH_MODE32=null;
        CatmaQueryParser.phrase_return phrase31 =null;


        Object TAG29_tree=null;
        Object EQUAL30_tree=null;
        Object TAG_MATCH_MODE32_tree=null;
        RewriteRuleTokenStream stream_TAG_MATCH_MODE=new RewriteRuleTokenStream(adaptor,"token TAG_MATCH_MODE");
        RewriteRuleTokenStream stream_EQUAL=new RewriteRuleTokenStream(adaptor,"token EQUAL");
        RewriteRuleTokenStream stream_TAG=new RewriteRuleTokenStream(adaptor,"token TAG");
        RewriteRuleSubtreeStream stream_phrase=new RewriteRuleSubtreeStream(adaptor,"rule phrase");
        try {
            // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:183:2: ( TAG EQUAL phrase ( TAG_MATCH_MODE )? -> ^( ND_TAG phrase ( TAG_MATCH_MODE )? ) )
            // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:183:4: TAG EQUAL phrase ( TAG_MATCH_MODE )?
            {
            TAG29=(Token)match(input,TAG,FOLLOW_TAG_in_tagQuery544);  
            stream_TAG.add(TAG29);


            EQUAL30=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_tagQuery546);  
            stream_EQUAL.add(EQUAL30);


            pushFollow(FOLLOW_phrase_in_tagQuery548);
            phrase31=phrase();

            state._fsp--;

            stream_phrase.add(phrase31.getTree());

            // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:183:21: ( TAG_MATCH_MODE )?
            int alt6=2;
            int LA6_0 = input.LA(1);

            if ( (LA6_0==TAG_MATCH_MODE) ) {
                alt6=1;
            }
            switch (alt6) {
                case 1 :
                    // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:183:21: TAG_MATCH_MODE
                    {
                    TAG_MATCH_MODE32=(Token)match(input,TAG_MATCH_MODE,FOLLOW_TAG_MATCH_MODE_in_tagQuery550);  
                    stream_TAG_MATCH_MODE.add(TAG_MATCH_MODE32);


                    }
                    break;

            }


            // AST REWRITE
            // elements: phrase, TAG_MATCH_MODE
            // token labels: 
            // rule labels: retval
            // token list labels: 
            // rule list labels: 
            // wildcard labels: 
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 183:37: -> ^( ND_TAG phrase ( TAG_MATCH_MODE )? )
            {
                // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:183:40: ^( ND_TAG phrase ( TAG_MATCH_MODE )? )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(ND_TAG, "ND_TAG")
                , root_1);

                adaptor.addChild(root_1, stream_phrase.nextTree());

                // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:183:56: ( TAG_MATCH_MODE )?
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
    // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:188:1: propertyQuery : PROPERTY EQUAL phrase ( VALUE EQUAL phrase )? -> ^( ND_PROPERTY phrase ( phrase )? ) ;
    public final CatmaQueryParser.propertyQuery_return propertyQuery() throws RecognitionException {
        CatmaQueryParser.propertyQuery_return retval = new CatmaQueryParser.propertyQuery_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token PROPERTY33=null;
        Token EQUAL34=null;
        Token VALUE36=null;
        Token EQUAL37=null;
        CatmaQueryParser.phrase_return phrase35 =null;

        CatmaQueryParser.phrase_return phrase38 =null;


        Object PROPERTY33_tree=null;
        Object EQUAL34_tree=null;
        Object VALUE36_tree=null;
        Object EQUAL37_tree=null;
        RewriteRuleTokenStream stream_VALUE=new RewriteRuleTokenStream(adaptor,"token VALUE");
        RewriteRuleTokenStream stream_PROPERTY=new RewriteRuleTokenStream(adaptor,"token PROPERTY");
        RewriteRuleTokenStream stream_EQUAL=new RewriteRuleTokenStream(adaptor,"token EQUAL");
        RewriteRuleSubtreeStream stream_phrase=new RewriteRuleSubtreeStream(adaptor,"rule phrase");
        try {
            // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:189:2: ( PROPERTY EQUAL phrase ( VALUE EQUAL phrase )? -> ^( ND_PROPERTY phrase ( phrase )? ) )
            // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:189:4: PROPERTY EQUAL phrase ( VALUE EQUAL phrase )?
            {
            PROPERTY33=(Token)match(input,PROPERTY,FOLLOW_PROPERTY_in_propertyQuery583);  
            stream_PROPERTY.add(PROPERTY33);


            EQUAL34=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_propertyQuery585);  
            stream_EQUAL.add(EQUAL34);


            pushFollow(FOLLOW_phrase_in_propertyQuery587);
            phrase35=phrase();

            state._fsp--;

            stream_phrase.add(phrase35.getTree());

            // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:189:26: ( VALUE EQUAL phrase )?
            int alt7=2;
            int LA7_0 = input.LA(1);

            if ( (LA7_0==VALUE) ) {
                alt7=1;
            }
            switch (alt7) {
                case 1 :
                    // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:189:27: VALUE EQUAL phrase
                    {
                    VALUE36=(Token)match(input,VALUE,FOLLOW_VALUE_in_propertyQuery590);  
                    stream_VALUE.add(VALUE36);


                    EQUAL37=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_propertyQuery592);  
                    stream_EQUAL.add(EQUAL37);


                    pushFollow(FOLLOW_phrase_in_propertyQuery594);
                    phrase38=phrase();

                    state._fsp--;

                    stream_phrase.add(phrase38.getTree());

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
            // 189:48: -> ^( ND_PROPERTY phrase ( phrase )? )
            {
                // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:189:51: ^( ND_PROPERTY phrase ( phrase )? )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(ND_PROPERTY, "ND_PROPERTY")
                , root_1);

                adaptor.addChild(root_1, stream_phrase.nextTree());

                // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:189:72: ( phrase )?
                if ( stream_phrase.hasNext() ) {
                    adaptor.addChild(root_1, stream_phrase.nextTree());

                }
                stream_phrase.reset();

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
    // $ANTLR end "propertyQuery"


    public static class regQuery_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "regQuery"
    // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:193:1: regQuery : REG EQUAL phrase ( 'CI' )? -> ^( ND_REG phrase ( 'CI' )? ) ;
    public final CatmaQueryParser.regQuery_return regQuery() throws RecognitionException {
        CatmaQueryParser.regQuery_return retval = new CatmaQueryParser.regQuery_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token REG39=null;
        Token EQUAL40=null;
        Token string_literal42=null;
        CatmaQueryParser.phrase_return phrase41 =null;


        Object REG39_tree=null;
        Object EQUAL40_tree=null;
        Object string_literal42_tree=null;
        RewriteRuleTokenStream stream_40=new RewriteRuleTokenStream(adaptor,"token 40");
        RewriteRuleTokenStream stream_REG=new RewriteRuleTokenStream(adaptor,"token REG");
        RewriteRuleTokenStream stream_EQUAL=new RewriteRuleTokenStream(adaptor,"token EQUAL");
        RewriteRuleSubtreeStream stream_phrase=new RewriteRuleSubtreeStream(adaptor,"rule phrase");
        try {
            // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:194:2: ( REG EQUAL phrase ( 'CI' )? -> ^( ND_REG phrase ( 'CI' )? ) )
            // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:194:4: REG EQUAL phrase ( 'CI' )?
            {
            REG39=(Token)match(input,REG,FOLLOW_REG_in_regQuery625);  
            stream_REG.add(REG39);


            EQUAL40=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_regQuery627);  
            stream_EQUAL.add(EQUAL40);


            pushFollow(FOLLOW_phrase_in_regQuery629);
            phrase41=phrase();

            state._fsp--;

            stream_phrase.add(phrase41.getTree());

            // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:194:21: ( 'CI' )?
            int alt8=2;
            int LA8_0 = input.LA(1);

            if ( (LA8_0==40) ) {
                alt8=1;
            }
            switch (alt8) {
                case 1 :
                    // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:194:21: 'CI'
                    {
                    string_literal42=(Token)match(input,40,FOLLOW_40_in_regQuery631);  
                    stream_40.add(string_literal42);


                    }
                    break;

            }


            // AST REWRITE
            // elements: 40, phrase
            // token labels: 
            // rule labels: retval
            // token list labels: 
            // rule list labels: 
            // wildcard labels: 
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 194:27: -> ^( ND_REG phrase ( 'CI' )? )
            {
                // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:194:30: ^( ND_REG phrase ( 'CI' )? )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(ND_REG, "ND_REG")
                , root_1);

                adaptor.addChild(root_1, stream_phrase.nextTree());

                // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:194:46: ( 'CI' )?
                if ( stream_40.hasNext() ) {
                    adaptor.addChild(root_1, 
                    stream_40.nextNode()
                    );

                }
                stream_40.reset();

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
    // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:198:1: freqQuery : FREQ ( EQUAL INT ( '-' INT )? -> ^( ND_FREQ EQUAL INT ( INT )? ) | UNEQUAL INT -> ^( ND_FREQ UNEQUAL INT ) ) ;
    public final CatmaQueryParser.freqQuery_return freqQuery() throws RecognitionException {
        CatmaQueryParser.freqQuery_return retval = new CatmaQueryParser.freqQuery_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token FREQ43=null;
        Token EQUAL44=null;
        Token INT45=null;
        Token char_literal46=null;
        Token INT47=null;
        Token UNEQUAL48=null;
        Token INT49=null;

        Object FREQ43_tree=null;
        Object EQUAL44_tree=null;
        Object INT45_tree=null;
        Object char_literal46_tree=null;
        Object INT47_tree=null;
        Object UNEQUAL48_tree=null;
        Object INT49_tree=null;
        RewriteRuleTokenStream stream_FREQ=new RewriteRuleTokenStream(adaptor,"token FREQ");
        RewriteRuleTokenStream stream_INT=new RewriteRuleTokenStream(adaptor,"token INT");
        RewriteRuleTokenStream stream_EQUAL=new RewriteRuleTokenStream(adaptor,"token EQUAL");
        RewriteRuleTokenStream stream_UNEQUAL=new RewriteRuleTokenStream(adaptor,"token UNEQUAL");
        RewriteRuleTokenStream stream_38=new RewriteRuleTokenStream(adaptor,"token 38");

        try {
            // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:199:2: ( FREQ ( EQUAL INT ( '-' INT )? -> ^( ND_FREQ EQUAL INT ( INT )? ) | UNEQUAL INT -> ^( ND_FREQ UNEQUAL INT ) ) )
            // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:199:4: FREQ ( EQUAL INT ( '-' INT )? -> ^( ND_FREQ EQUAL INT ( INT )? ) | UNEQUAL INT -> ^( ND_FREQ UNEQUAL INT ) )
            {
            FREQ43=(Token)match(input,FREQ,FOLLOW_FREQ_in_freqQuery662);  
            stream_FREQ.add(FREQ43);


            // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:200:3: ( EQUAL INT ( '-' INT )? -> ^( ND_FREQ EQUAL INT ( INT )? ) | UNEQUAL INT -> ^( ND_FREQ UNEQUAL INT ) )
            int alt10=2;
            int LA10_0 = input.LA(1);

            if ( (LA10_0==EQUAL) ) {
                alt10=1;
            }
            else if ( (LA10_0==UNEQUAL) ) {
                alt10=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 10, 0, input);

                throw nvae;

            }
            switch (alt10) {
                case 1 :
                    // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:200:5: EQUAL INT ( '-' INT )?
                    {
                    EQUAL44=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_freqQuery669);  
                    stream_EQUAL.add(EQUAL44);


                    INT45=(Token)match(input,INT,FOLLOW_INT_in_freqQuery671);  
                    stream_INT.add(INT45);


                    // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:200:15: ( '-' INT )?
                    int alt9=2;
                    int LA9_0 = input.LA(1);

                    if ( (LA9_0==38) ) {
                        int LA9_1 = input.LA(2);

                        if ( (LA9_1==INT) ) {
                            alt9=1;
                        }
                    }
                    switch (alt9) {
                        case 1 :
                            // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:200:16: '-' INT
                            {
                            char_literal46=(Token)match(input,38,FOLLOW_38_in_freqQuery674);  
                            stream_38.add(char_literal46);


                            INT47=(Token)match(input,INT,FOLLOW_INT_in_freqQuery676);  
                            stream_INT.add(INT47);


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
                    // 200:26: -> ^( ND_FREQ EQUAL INT ( INT )? )
                    {
                        // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:200:29: ^( ND_FREQ EQUAL INT ( INT )? )
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

                        // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:200:49: ( INT )?
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
                    // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:201:5: UNEQUAL INT
                    {
                    UNEQUAL48=(Token)match(input,UNEQUAL,FOLLOW_UNEQUAL_in_freqQuery698);  
                    stream_UNEQUAL.add(UNEQUAL48);


                    INT49=(Token)match(input,INT,FOLLOW_INT_in_freqQuery700);  
                    stream_INT.add(INT49);


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
                    // 201:17: -> ^( ND_FREQ UNEQUAL INT )
                    {
                        // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:201:20: ^( ND_FREQ UNEQUAL INT )
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
    // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:205:1: similQuery : SIMIL EQUAL phrase INT ( '%' )? -> ^( ND_SIMIL phrase INT ) ;
    public final CatmaQueryParser.similQuery_return similQuery() throws RecognitionException {
        CatmaQueryParser.similQuery_return retval = new CatmaQueryParser.similQuery_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token SIMIL50=null;
        Token EQUAL51=null;
        Token INT53=null;
        Token char_literal54=null;
        CatmaQueryParser.phrase_return phrase52 =null;


        Object SIMIL50_tree=null;
        Object EQUAL51_tree=null;
        Object INT53_tree=null;
        Object char_literal54_tree=null;
        RewriteRuleTokenStream stream_INT=new RewriteRuleTokenStream(adaptor,"token INT");
        RewriteRuleTokenStream stream_33=new RewriteRuleTokenStream(adaptor,"token 33");
        RewriteRuleTokenStream stream_EQUAL=new RewriteRuleTokenStream(adaptor,"token EQUAL");
        RewriteRuleTokenStream stream_SIMIL=new RewriteRuleTokenStream(adaptor,"token SIMIL");
        RewriteRuleSubtreeStream stream_phrase=new RewriteRuleSubtreeStream(adaptor,"rule phrase");
        try {
            // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:206:2: ( SIMIL EQUAL phrase INT ( '%' )? -> ^( ND_SIMIL phrase INT ) )
            // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:206:4: SIMIL EQUAL phrase INT ( '%' )?
            {
            SIMIL50=(Token)match(input,SIMIL,FOLLOW_SIMIL_in_similQuery732);  
            stream_SIMIL.add(SIMIL50);


            EQUAL51=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_similQuery734);  
            stream_EQUAL.add(EQUAL51);


            pushFollow(FOLLOW_phrase_in_similQuery736);
            phrase52=phrase();

            state._fsp--;

            stream_phrase.add(phrase52.getTree());

            INT53=(Token)match(input,INT,FOLLOW_INT_in_similQuery738);  
            stream_INT.add(INT53);


            // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:206:27: ( '%' )?
            int alt11=2;
            int LA11_0 = input.LA(1);

            if ( (LA11_0==33) ) {
                alt11=1;
            }
            switch (alt11) {
                case 1 :
                    // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:206:27: '%'
                    {
                    char_literal54=(Token)match(input,33,FOLLOW_33_in_similQuery740);  
                    stream_33.add(char_literal54);


                    }
                    break;

            }


            // AST REWRITE
            // elements: INT, phrase
            // token labels: 
            // rule labels: retval
            // token list labels: 
            // rule list labels: 
            // wildcard labels: 
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 206:32: -> ^( ND_SIMIL phrase INT )
            {
                // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:206:35: ^( ND_SIMIL phrase INT )
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


    public static class refinement_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "refinement"
    // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:215:1: refinement : 'where' refinementExpression -> refinementExpression ;
    public final CatmaQueryParser.refinement_return refinement() throws RecognitionException {
        CatmaQueryParser.refinement_return retval = new CatmaQueryParser.refinement_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token string_literal55=null;
        CatmaQueryParser.refinementExpression_return refinementExpression56 =null;


        Object string_literal55_tree=null;
        RewriteRuleTokenStream stream_41=new RewriteRuleTokenStream(adaptor,"token 41");
        RewriteRuleSubtreeStream stream_refinementExpression=new RewriteRuleSubtreeStream(adaptor,"rule refinementExpression");
        try {
            // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:216:2: ( 'where' refinementExpression -> refinementExpression )
            // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:216:4: 'where' refinementExpression
            {
            string_literal55=(Token)match(input,41,FOLLOW_41_in_refinement776);  
            stream_41.add(string_literal55);


            pushFollow(FOLLOW_refinementExpression_in_refinement778);
            refinementExpression56=refinementExpression();

            state._fsp--;

            stream_refinementExpression.add(refinementExpression56.getTree());

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
            // 216:33: -> refinementExpression
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
    // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:220:1: refinementExpression : startRefinement= refinementTerm ( orRefinement[(CommonTree)$startRefinement.tree] -> orRefinement | andRefinement[(CommonTree)$startRefinement.tree] -> andRefinement | -> ^( ND_REFINE refinementTerm ) ) ;
    public final CatmaQueryParser.refinementExpression_return refinementExpression() throws RecognitionException {
        CatmaQueryParser.refinementExpression_return retval = new CatmaQueryParser.refinementExpression_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        CatmaQueryParser.refinementTerm_return startRefinement =null;

        CatmaQueryParser.orRefinement_return orRefinement57 =null;

        CatmaQueryParser.andRefinement_return andRefinement58 =null;


        RewriteRuleSubtreeStream stream_orRefinement=new RewriteRuleSubtreeStream(adaptor,"rule orRefinement");
        RewriteRuleSubtreeStream stream_andRefinement=new RewriteRuleSubtreeStream(adaptor,"rule andRefinement");
        RewriteRuleSubtreeStream stream_refinementTerm=new RewriteRuleSubtreeStream(adaptor,"rule refinementTerm");
        try {
            // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:221:2: (startRefinement= refinementTerm ( orRefinement[(CommonTree)$startRefinement.tree] -> orRefinement | andRefinement[(CommonTree)$startRefinement.tree] -> andRefinement | -> ^( ND_REFINE refinementTerm ) ) )
            // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:221:4: startRefinement= refinementTerm ( orRefinement[(CommonTree)$startRefinement.tree] -> orRefinement | andRefinement[(CommonTree)$startRefinement.tree] -> andRefinement | -> ^( ND_REFINE refinementTerm ) )
            {
            pushFollow(FOLLOW_refinementTerm_in_refinementExpression802);
            startRefinement=refinementTerm();

            state._fsp--;

            stream_refinementTerm.add(startRefinement.getTree());

            // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:221:35: ( orRefinement[(CommonTree)$startRefinement.tree] -> orRefinement | andRefinement[(CommonTree)$startRefinement.tree] -> andRefinement | -> ^( ND_REFINE refinementTerm ) )
            int alt12=3;
            switch ( input.LA(1) ) {
            case 42:
                {
                alt12=1;
                }
                break;
            case 37:
                {
                alt12=2;
                }
                break;
            case EOF:
            case 36:
                {
                alt12=3;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 12, 0, input);

                throw nvae;

            }

            switch (alt12) {
                case 1 :
                    // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:222:4: orRefinement[(CommonTree)$startRefinement.tree]
                    {
                    pushFollow(FOLLOW_orRefinement_in_refinementExpression810);
                    orRefinement57=orRefinement((CommonTree)(startRefinement!=null?((Object)startRefinement.tree):null));

                    state._fsp--;

                    stream_orRefinement.add(orRefinement57.getTree());

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
                    // 222:52: -> orRefinement
                    {
                        adaptor.addChild(root_0, stream_orRefinement.nextTree());

                    }


                    retval.tree = root_0;

                    }
                    break;
                case 2 :
                    // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:223:6: andRefinement[(CommonTree)$startRefinement.tree]
                    {
                    pushFollow(FOLLOW_andRefinement_in_refinementExpression822);
                    andRefinement58=andRefinement((CommonTree)(startRefinement!=null?((Object)startRefinement.tree):null));

                    state._fsp--;

                    stream_andRefinement.add(andRefinement58.getTree());

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
                    // 223:55: -> andRefinement
                    {
                        adaptor.addChild(root_0, stream_andRefinement.nextTree());

                    }


                    retval.tree = root_0;

                    }
                    break;
                case 3 :
                    // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:224:6: 
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
                    // 224:6: -> ^( ND_REFINE refinementTerm )
                    {
                        // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:224:9: ^( ND_REFINE refinementTerm )
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
    // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:228:1: orRefinement[CommonTree startRefinement] : '|' refinementTerm -> ^( ND_ORREFINE refinementTerm ) ;
    public final CatmaQueryParser.orRefinement_return orRefinement(CommonTree startRefinement) throws RecognitionException {
        CatmaQueryParser.orRefinement_return retval = new CatmaQueryParser.orRefinement_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token char_literal59=null;
        CatmaQueryParser.refinementTerm_return refinementTerm60 =null;


        Object char_literal59_tree=null;
        RewriteRuleTokenStream stream_42=new RewriteRuleTokenStream(adaptor,"token 42");
        RewriteRuleSubtreeStream stream_refinementTerm=new RewriteRuleSubtreeStream(adaptor,"rule refinementTerm");
        try {
            // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:229:2: ( '|' refinementTerm -> ^( ND_ORREFINE refinementTerm ) )
            // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:229:4: '|' refinementTerm
            {
            char_literal59=(Token)match(input,42,FOLLOW_42_in_orRefinement861);  
            stream_42.add(char_literal59);


            pushFollow(FOLLOW_refinementTerm_in_orRefinement863);
            refinementTerm60=refinementTerm();

            state._fsp--;

            stream_refinementTerm.add(refinementTerm60.getTree());

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
            // 229:23: -> ^( ND_ORREFINE refinementTerm )
            {
                // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:229:26: ^( ND_ORREFINE refinementTerm )
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
    // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:233:1: andRefinement[CommonTree startRefinement] : ',' refinementTerm -> ^( ND_ANDREFINE refinementTerm ) ;
    public final CatmaQueryParser.andRefinement_return andRefinement(CommonTree startRefinement) throws RecognitionException {
        CatmaQueryParser.andRefinement_return retval = new CatmaQueryParser.andRefinement_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token char_literal61=null;
        CatmaQueryParser.refinementTerm_return refinementTerm62 =null;


        Object char_literal61_tree=null;
        RewriteRuleTokenStream stream_37=new RewriteRuleTokenStream(adaptor,"token 37");
        RewriteRuleSubtreeStream stream_refinementTerm=new RewriteRuleSubtreeStream(adaptor,"rule refinementTerm");
        try {
            // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:234:2: ( ',' refinementTerm -> ^( ND_ANDREFINE refinementTerm ) )
            // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:234:4: ',' refinementTerm
            {
            char_literal61=(Token)match(input,37,FOLLOW_37_in_andRefinement893);  
            stream_37.add(char_literal61);


            pushFollow(FOLLOW_refinementTerm_in_andRefinement895);
            refinementTerm62=refinementTerm();

            state._fsp--;

            stream_refinementTerm.add(refinementTerm62.getTree());

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
            // 234:23: -> ^( ND_ANDREFINE refinementTerm )
            {
                // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:234:26: ^( ND_ANDREFINE refinementTerm )
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
    // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:238:1: refinementTerm : ( selector -> selector | '(' refinementExpression ')' -> refinementExpression );
    public final CatmaQueryParser.refinementTerm_return refinementTerm() throws RecognitionException {
        CatmaQueryParser.refinementTerm_return retval = new CatmaQueryParser.refinementTerm_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token char_literal64=null;
        Token char_literal66=null;
        CatmaQueryParser.selector_return selector63 =null;

        CatmaQueryParser.refinementExpression_return refinementExpression65 =null;


        Object char_literal64_tree=null;
        Object char_literal66_tree=null;
        RewriteRuleTokenStream stream_35=new RewriteRuleTokenStream(adaptor,"token 35");
        RewriteRuleTokenStream stream_36=new RewriteRuleTokenStream(adaptor,"token 36");
        RewriteRuleSubtreeStream stream_selector=new RewriteRuleSubtreeStream(adaptor,"rule selector");
        RewriteRuleSubtreeStream stream_refinementExpression=new RewriteRuleSubtreeStream(adaptor,"rule refinementExpression");
        try {
            // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:239:2: ( selector -> selector | '(' refinementExpression ')' -> refinementExpression )
            int alt13=2;
            int LA13_0 = input.LA(1);

            if ( (LA13_0==FREQ||(LA13_0 >= PROPERTY && LA13_0 <= TAG)) ) {
                alt13=1;
            }
            else if ( (LA13_0==35) ) {
                alt13=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 13, 0, input);

                throw nvae;

            }
            switch (alt13) {
                case 1 :
                    // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:239:5: selector
                    {
                    pushFollow(FOLLOW_selector_in_refinementTerm925);
                    selector63=selector();

                    state._fsp--;

                    stream_selector.add(selector63.getTree());

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
                    // 239:14: -> selector
                    {
                        adaptor.addChild(root_0, stream_selector.nextTree());

                    }


                    retval.tree = root_0;

                    }
                    break;
                case 2 :
                    // C:\\data\\eclipse_workspace\\clea\\grammars\\ast\\CatmaQuery.g:240:5: '(' refinementExpression ')'
                    {
                    char_literal64=(Token)match(input,35,FOLLOW_35_in_refinementTerm935);  
                    stream_35.add(char_literal64);


                    pushFollow(FOLLOW_refinementExpression_in_refinementTerm936);
                    refinementExpression65=refinementExpression();

                    state._fsp--;

                    stream_refinementExpression.add(refinementExpression65.getTree());

                    char_literal66=(Token)match(input,36,FOLLOW_36_in_refinementTerm937);  
                    stream_36.add(char_literal66);


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
                    // 240:32: -> refinementExpression
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


 

    public static final BitSet FOLLOW_query_in_start143 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_start145 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_queryExpression_in_query161 = new BitSet(new long[]{0x0000020000000002L});
    public static final BitSet FOLLOW_refinement_in_query163 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_term_in_queryExpression200 = new BitSet(new long[]{0x000000E400000002L});
    public static final BitSet FOLLOW_unionQuery_in_queryExpression207 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_collocQuery_in_queryExpression219 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_exclusionQuery_in_queryExpression232 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_adjacencyQuery_in_queryExpression245 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_37_in_unionQuery288 = new BitSet(new long[]{0x000000082F000020L});
    public static final BitSet FOLLOW_term_in_unionQuery290 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_34_in_collocQuery321 = new BitSet(new long[]{0x000000082F000020L});
    public static final BitSet FOLLOW_term_in_collocQuery323 = new BitSet(new long[]{0x0000000000000082L});
    public static final BitSet FOLLOW_INT_in_collocQuery325 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_38_in_exclusionQuery359 = new BitSet(new long[]{0x000000082F000020L});
    public static final BitSet FOLLOW_term_in_exclusionQuery361 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_39_in_adjacencyQuery392 = new BitSet(new long[]{0x000000082F000020L});
    public static final BitSet FOLLOW_term_in_adjacencyQuery394 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_phrase_in_term424 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_selector_in_term434 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_35_in_term445 = new BitSet(new long[]{0x000000082F000020L});
    public static final BitSet FOLLOW_query_in_term446 = new BitSet(new long[]{0x0000001000000000L});
    public static final BitSet FOLLOW_36_in_term447 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_TXT_in_phrase471 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_tagQuery_in_selector504 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_propertyQuery_in_selector509 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_regQuery_in_selector514 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_freqQuery_in_selector519 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_similQuery_in_selector524 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_TAG_in_tagQuery544 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_EQUAL_in_tagQuery546 = new BitSet(new long[]{0x0000000020000000L});
    public static final BitSet FOLLOW_phrase_in_tagQuery548 = new BitSet(new long[]{0x0000000010000002L});
    public static final BitSet FOLLOW_TAG_MATCH_MODE_in_tagQuery550 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_PROPERTY_in_propertyQuery583 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_EQUAL_in_propertyQuery585 = new BitSet(new long[]{0x0000000020000000L});
    public static final BitSet FOLLOW_phrase_in_propertyQuery587 = new BitSet(new long[]{0x0000000080000002L});
    public static final BitSet FOLLOW_VALUE_in_propertyQuery590 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_EQUAL_in_propertyQuery592 = new BitSet(new long[]{0x0000000020000000L});
    public static final BitSet FOLLOW_phrase_in_propertyQuery594 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_REG_in_regQuery625 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_EQUAL_in_regQuery627 = new BitSet(new long[]{0x0000000020000000L});
    public static final BitSet FOLLOW_phrase_in_regQuery629 = new BitSet(new long[]{0x0000010000000002L});
    public static final BitSet FOLLOW_40_in_regQuery631 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_FREQ_in_freqQuery662 = new BitSet(new long[]{0x0000000040000010L});
    public static final BitSet FOLLOW_EQUAL_in_freqQuery669 = new BitSet(new long[]{0x0000000000000080L});
    public static final BitSet FOLLOW_INT_in_freqQuery671 = new BitSet(new long[]{0x0000004000000002L});
    public static final BitSet FOLLOW_38_in_freqQuery674 = new BitSet(new long[]{0x0000000000000080L});
    public static final BitSet FOLLOW_INT_in_freqQuery676 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_UNEQUAL_in_freqQuery698 = new BitSet(new long[]{0x0000000000000080L});
    public static final BitSet FOLLOW_INT_in_freqQuery700 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_SIMIL_in_similQuery732 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_EQUAL_in_similQuery734 = new BitSet(new long[]{0x0000000020000000L});
    public static final BitSet FOLLOW_phrase_in_similQuery736 = new BitSet(new long[]{0x0000000000000080L});
    public static final BitSet FOLLOW_INT_in_similQuery738 = new BitSet(new long[]{0x0000000200000002L});
    public static final BitSet FOLLOW_33_in_similQuery740 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_41_in_refinement776 = new BitSet(new long[]{0x000000080F000020L});
    public static final BitSet FOLLOW_refinementExpression_in_refinement778 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_refinementTerm_in_refinementExpression802 = new BitSet(new long[]{0x0000042000000002L});
    public static final BitSet FOLLOW_orRefinement_in_refinementExpression810 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_andRefinement_in_refinementExpression822 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_42_in_orRefinement861 = new BitSet(new long[]{0x000000080F000020L});
    public static final BitSet FOLLOW_refinementTerm_in_orRefinement863 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_37_in_andRefinement893 = new BitSet(new long[]{0x000000080F000020L});
    public static final BitSet FOLLOW_refinementTerm_in_andRefinement895 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_selector_in_refinementTerm925 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_35_in_refinementTerm935 = new BitSet(new long[]{0x000000080F000020L});
    public static final BitSet FOLLOW_refinementExpression_in_refinementTerm936 = new BitSet(new long[]{0x0000001000000000L});
    public static final BitSet FOLLOW_36_in_refinementTerm937 = new BitSet(new long[]{0x0000000000000002L});

}