// $ANTLR null C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g 2020-09-12 13:11:49

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
* About:	EBNF Grammar for the CATMA Query Parser with AST (tree) generation rules
*/
@SuppressWarnings("all")
public class CatmaQueryParser extends Parser {
	public static final String[] tokenNames = new String[] {
		"<invalid>", "<EOR>", "<DOWN>", "<UP>", "COMMENT", "EQUAL", "FREQ", "GROUPIDENT", 
		"INT", "LETTER", "LETTEREXTENDED", "MATCH_MODE", "ND_ADJACENCY", "ND_ANDREFINE", 
		"ND_COLLOC", "ND_COMMENT", "ND_EXCLUSION", "ND_FREQ", "ND_ORREFINE", "ND_PHRASE", 
		"ND_PROPERTY", "ND_QUERY", "ND_REFINE", "ND_REG", "ND_SIMIL", "ND_TAG", 
		"ND_TAGDIFF", "ND_TAGPROPERTY", "ND_UNION", "ND_WILD", "PROPERTY", "REG", 
		"SIMIL", "TAG", "TAGDIFF", "TXT", "UNEQUAL", "VALUE", "WHITESPACE", "WILD", 
		"'%'", "'&'", "'('", "')'", "','", "'-'", "';'", "'CI'", "'EXCL'", "'where'", 
		"'|'"
	};
	public static final int EOF=-1;
	public static final int T__40=40;
	public static final int T__41=41;
	public static final int T__42=42;
	public static final int T__43=43;
	public static final int T__44=44;
	public static final int T__45=45;
	public static final int T__46=46;
	public static final int T__47=47;
	public static final int T__48=48;
	public static final int T__49=49;
	public static final int T__50=50;
	public static final int COMMENT=4;
	public static final int EQUAL=5;
	public static final int FREQ=6;
	public static final int GROUPIDENT=7;
	public static final int INT=8;
	public static final int LETTER=9;
	public static final int LETTEREXTENDED=10;
	public static final int MATCH_MODE=11;
	public static final int ND_ADJACENCY=12;
	public static final int ND_ANDREFINE=13;
	public static final int ND_COLLOC=14;
	public static final int ND_COMMENT=15;
	public static final int ND_EXCLUSION=16;
	public static final int ND_FREQ=17;
	public static final int ND_ORREFINE=18;
	public static final int ND_PHRASE=19;
	public static final int ND_PROPERTY=20;
	public static final int ND_QUERY=21;
	public static final int ND_REFINE=22;
	public static final int ND_REG=23;
	public static final int ND_SIMIL=24;
	public static final int ND_TAG=25;
	public static final int ND_TAGDIFF=26;
	public static final int ND_TAGPROPERTY=27;
	public static final int ND_UNION=28;
	public static final int ND_WILD=29;
	public static final int PROPERTY=30;
	public static final int REG=31;
	public static final int SIMIL=32;
	public static final int TAG=33;
	public static final int TAGDIFF=34;
	public static final int TXT=35;
	public static final int UNEQUAL=36;
	public static final int VALUE=37;
	public static final int WHITESPACE=38;
	public static final int WILD=39;

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
	@Override public String[] getTokenNames() { return CatmaQueryParser.tokenNames; }
	@Override public String getGrammarFileName() { return "C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g"; }



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
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "start"
	// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:106:1: start : query EOF ;
	public final CatmaQueryParser.start_return start() throws RecognitionException {
		CatmaQueryParser.start_return retval = new CatmaQueryParser.start_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token EOF2=null;
		ParserRuleReturnScope query1 =null;

		Object EOF2_tree=null;

		try {
			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:106:7: ( query EOF )
			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:106:9: query EOF
			{
			root_0 = (Object)adaptor.nil();


			pushFollow(FOLLOW_query_in_start159);
			query1=query();
			state._fsp--;

			adaptor.addChild(root_0, query1.getTree());

			EOF2=(Token)match(input,EOF,FOLLOW_EOF_in_start161); 
			EOF2_tree = (Object)adaptor.create(EOF2);
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
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "query"
	// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:110:1: query : queryExpression ( refinement )? -> ^( ND_QUERY queryExpression ( refinement )? ) ;
	public final CatmaQueryParser.query_return query() throws RecognitionException {
		CatmaQueryParser.query_return retval = new CatmaQueryParser.query_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		ParserRuleReturnScope queryExpression3 =null;
		ParserRuleReturnScope refinement4 =null;

		RewriteRuleSubtreeStream stream_queryExpression=new RewriteRuleSubtreeStream(adaptor,"rule queryExpression");
		RewriteRuleSubtreeStream stream_refinement=new RewriteRuleSubtreeStream(adaptor,"rule refinement");

		try {
			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:110:7: ( queryExpression ( refinement )? -> ^( ND_QUERY queryExpression ( refinement )? ) )
			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:110:9: queryExpression ( refinement )?
			{
			pushFollow(FOLLOW_queryExpression_in_query177);
			queryExpression3=queryExpression();
			state._fsp--;

			stream_queryExpression.add(queryExpression3.getTree());
			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:110:25: ( refinement )?
			int alt1=2;
			int LA1_0 = input.LA(1);
			if ( (LA1_0==49) ) {
				alt1=1;
			}
			switch (alt1) {
				case 1 :
					// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:110:25: refinement
					{
					pushFollow(FOLLOW_refinement_in_query179);
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
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (Object)adaptor.nil();
			// 110:37: -> ^( ND_QUERY queryExpression ( refinement )? )
			{
				// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:110:40: ^( ND_QUERY queryExpression ( refinement )? )
				{
				Object root_1 = (Object)adaptor.nil();
				root_1 = (Object)adaptor.becomeRoot((Object)adaptor.create(ND_QUERY, "ND_QUERY"), root_1);
				adaptor.addChild(root_1, stream_queryExpression.nextTree());
				// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:110:67: ( refinement )?
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
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "queryExpression"
	// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:119:1: queryExpression : startTerm= term ( unionQuery[(CommonTree)$startTerm.tree] -> unionQuery | collocQuery[(CommonTree)$startTerm.tree] -> collocQuery | exclusionQuery[(CommonTree)$startTerm.tree] -> exclusionQuery | adjacencyQuery[(CommonTree)$startTerm.tree] -> adjacencyQuery | -> term ) ;
	public final CatmaQueryParser.queryExpression_return queryExpression() throws RecognitionException {
		CatmaQueryParser.queryExpression_return retval = new CatmaQueryParser.queryExpression_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		ParserRuleReturnScope startTerm =null;
		ParserRuleReturnScope unionQuery5 =null;
		ParserRuleReturnScope collocQuery6 =null;
		ParserRuleReturnScope exclusionQuery7 =null;
		ParserRuleReturnScope adjacencyQuery8 =null;

		RewriteRuleSubtreeStream stream_unionQuery=new RewriteRuleSubtreeStream(adaptor,"rule unionQuery");
		RewriteRuleSubtreeStream stream_exclusionQuery=new RewriteRuleSubtreeStream(adaptor,"rule exclusionQuery");
		RewriteRuleSubtreeStream stream_adjacencyQuery=new RewriteRuleSubtreeStream(adaptor,"rule adjacencyQuery");
		RewriteRuleSubtreeStream stream_term=new RewriteRuleSubtreeStream(adaptor,"rule term");
		RewriteRuleSubtreeStream stream_collocQuery=new RewriteRuleSubtreeStream(adaptor,"rule collocQuery");

		try {
			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:120:2: (startTerm= term ( unionQuery[(CommonTree)$startTerm.tree] -> unionQuery | collocQuery[(CommonTree)$startTerm.tree] -> collocQuery | exclusionQuery[(CommonTree)$startTerm.tree] -> exclusionQuery | adjacencyQuery[(CommonTree)$startTerm.tree] -> adjacencyQuery | -> term ) )
			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:120:4: startTerm= term ( unionQuery[(CommonTree)$startTerm.tree] -> unionQuery | collocQuery[(CommonTree)$startTerm.tree] -> collocQuery | exclusionQuery[(CommonTree)$startTerm.tree] -> exclusionQuery | adjacencyQuery[(CommonTree)$startTerm.tree] -> adjacencyQuery | -> term )
			{
			pushFollow(FOLLOW_term_in_queryExpression216);
			startTerm=term();
			state._fsp--;

			stream_term.add(startTerm.getTree());
			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:120:19: ( unionQuery[(CommonTree)$startTerm.tree] -> unionQuery | collocQuery[(CommonTree)$startTerm.tree] -> collocQuery | exclusionQuery[(CommonTree)$startTerm.tree] -> exclusionQuery | adjacencyQuery[(CommonTree)$startTerm.tree] -> adjacencyQuery | -> term )
			int alt2=5;
			switch ( input.LA(1) ) {
			case 44:
				{
				alt2=1;
				}
				break;
			case 41:
				{
				alt2=2;
				}
				break;
			case 45:
				{
				alt2=3;
				}
				break;
			case 46:
				{
				alt2=4;
				}
				break;
			case EOF:
			case 43:
			case 49:
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
					// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:121:4: unionQuery[(CommonTree)$startTerm.tree]
					{
					pushFollow(FOLLOW_unionQuery_in_queryExpression223);
					unionQuery5=unionQuery((CommonTree)(startTerm!=null?((Object)startTerm.getTree()):null));
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
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (Object)adaptor.nil();
					// 121:44: -> unionQuery
					{
						adaptor.addChild(root_0, stream_unionQuery.nextTree());
					}


					retval.tree = root_0;

					}
					break;
				case 2 :
					// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:122:6: collocQuery[(CommonTree)$startTerm.tree]
					{
					pushFollow(FOLLOW_collocQuery_in_queryExpression235);
					collocQuery6=collocQuery((CommonTree)(startTerm!=null?((Object)startTerm.getTree()):null));
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
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (Object)adaptor.nil();
					// 122:48: -> collocQuery
					{
						adaptor.addChild(root_0, stream_collocQuery.nextTree());
					}


					retval.tree = root_0;

					}
					break;
				case 3 :
					// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:123:6: exclusionQuery[(CommonTree)$startTerm.tree]
					{
					pushFollow(FOLLOW_exclusionQuery_in_queryExpression248);
					exclusionQuery7=exclusionQuery((CommonTree)(startTerm!=null?((Object)startTerm.getTree()):null));
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
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (Object)adaptor.nil();
					// 123:51: -> exclusionQuery
					{
						adaptor.addChild(root_0, stream_exclusionQuery.nextTree());
					}


					retval.tree = root_0;

					}
					break;
				case 4 :
					// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:124:6: adjacencyQuery[(CommonTree)$startTerm.tree]
					{
					pushFollow(FOLLOW_adjacencyQuery_in_queryExpression261);
					adjacencyQuery8=adjacencyQuery((CommonTree)(startTerm!=null?((Object)startTerm.getTree()):null));
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
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (Object)adaptor.nil();
					// 124:51: -> adjacencyQuery
					{
						adaptor.addChild(root_0, stream_adjacencyQuery.nextTree());
					}


					retval.tree = root_0;

					}
					break;
				case 5 :
					// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:125:6: 
					{
					// AST REWRITE
					// elements: term
					// token labels: 
					// rule labels: retval
					// token list labels: 
					// rule list labels: 
					// wildcard labels: 
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (Object)adaptor.nil();
					// 125:6: -> term
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
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "unionQuery"
	// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:135:1: unionQuery[CommonTree startTerm] : ',' term ( 'EXCL' )? -> ^( ND_UNION term ( 'EXCL' )? ) ;
	public final CatmaQueryParser.unionQuery_return unionQuery(CommonTree startTerm) throws RecognitionException {
		CatmaQueryParser.unionQuery_return retval = new CatmaQueryParser.unionQuery_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token char_literal9=null;
		Token string_literal11=null;
		ParserRuleReturnScope term10 =null;

		Object char_literal9_tree=null;
		Object string_literal11_tree=null;
		RewriteRuleTokenStream stream_44=new RewriteRuleTokenStream(adaptor,"token 44");
		RewriteRuleTokenStream stream_48=new RewriteRuleTokenStream(adaptor,"token 48");
		RewriteRuleSubtreeStream stream_term=new RewriteRuleSubtreeStream(adaptor,"rule term");

		try {
			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:136:2: ( ',' term ( 'EXCL' )? -> ^( ND_UNION term ( 'EXCL' )? ) )
			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:136:4: ',' term ( 'EXCL' )?
			{
			char_literal9=(Token)match(input,44,FOLLOW_44_in_unionQuery304);  
			stream_44.add(char_literal9);

			pushFollow(FOLLOW_term_in_unionQuery306);
			term10=term();
			state._fsp--;

			stream_term.add(term10.getTree());
			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:136:13: ( 'EXCL' )?
			int alt3=2;
			int LA3_0 = input.LA(1);
			if ( (LA3_0==48) ) {
				alt3=1;
			}
			switch (alt3) {
				case 1 :
					// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:136:13: 'EXCL'
					{
					string_literal11=(Token)match(input,48,FOLLOW_48_in_unionQuery308);  
					stream_48.add(string_literal11);

					}
					break;

			}

			// AST REWRITE
			// elements: 48, term
			// token labels: 
			// rule labels: retval
			// token list labels: 
			// rule list labels: 
			// wildcard labels: 
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (Object)adaptor.nil();
			// 136:21: -> ^( ND_UNION term ( 'EXCL' )? )
			{
				// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:136:24: ^( ND_UNION term ( 'EXCL' )? )
				{
				Object root_1 = (Object)adaptor.nil();
				root_1 = (Object)adaptor.becomeRoot((Object)adaptor.create(ND_UNION, "ND_UNION"), root_1);
				adaptor.addChild(root_1, startTerm);
				adaptor.addChild(root_1, stream_term.nextTree());
				// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:136:53: ( 'EXCL' )?
				if ( stream_48.hasNext() ) {
					adaptor.addChild(root_1, stream_48.nextNode());
				}
				stream_48.reset();

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
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "collocQuery"
	// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:140:1: collocQuery[CommonTree startTerm] : '&' term ( INT )? -> ^( ND_COLLOC term ( INT )? ) ;
	public final CatmaQueryParser.collocQuery_return collocQuery(CommonTree startTerm) throws RecognitionException {
		CatmaQueryParser.collocQuery_return retval = new CatmaQueryParser.collocQuery_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token char_literal12=null;
		Token INT14=null;
		ParserRuleReturnScope term13 =null;

		Object char_literal12_tree=null;
		Object INT14_tree=null;
		RewriteRuleTokenStream stream_41=new RewriteRuleTokenStream(adaptor,"token 41");
		RewriteRuleTokenStream stream_INT=new RewriteRuleTokenStream(adaptor,"token INT");
		RewriteRuleSubtreeStream stream_term=new RewriteRuleSubtreeStream(adaptor,"rule term");

		try {
			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:141:2: ( '&' term ( INT )? -> ^( ND_COLLOC term ( INT )? ) )
			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:141:4: '&' term ( INT )?
			{
			char_literal12=(Token)match(input,41,FOLLOW_41_in_collocQuery343);  
			stream_41.add(char_literal12);

			pushFollow(FOLLOW_term_in_collocQuery345);
			term13=term();
			state._fsp--;

			stream_term.add(term13.getTree());
			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:141:13: ( INT )?
			int alt4=2;
			int LA4_0 = input.LA(1);
			if ( (LA4_0==INT) ) {
				alt4=1;
			}
			switch (alt4) {
				case 1 :
					// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:141:13: INT
					{
					INT14=(Token)match(input,INT,FOLLOW_INT_in_collocQuery347);  
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
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (Object)adaptor.nil();
			// 141:18: -> ^( ND_COLLOC term ( INT )? )
			{
				// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:141:21: ^( ND_COLLOC term ( INT )? )
				{
				Object root_1 = (Object)adaptor.nil();
				root_1 = (Object)adaptor.becomeRoot((Object)adaptor.create(ND_COLLOC, "ND_COLLOC"), root_1);
				adaptor.addChild(root_1, startTerm);
				adaptor.addChild(root_1, stream_term.nextTree());
				// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:141:51: ( INT )?
				if ( stream_INT.hasNext() ) {
					adaptor.addChild(root_1, stream_INT.nextNode());
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
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "exclusionQuery"
	// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:145:1: exclusionQuery[CommonTree startTerm] : '-' term ( MATCH_MODE )? -> ^( ND_EXCLUSION term ( MATCH_MODE )? ) ;
	public final CatmaQueryParser.exclusionQuery_return exclusionQuery(CommonTree startTerm) throws RecognitionException {
		CatmaQueryParser.exclusionQuery_return retval = new CatmaQueryParser.exclusionQuery_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token char_literal15=null;
		Token MATCH_MODE17=null;
		ParserRuleReturnScope term16 =null;

		Object char_literal15_tree=null;
		Object MATCH_MODE17_tree=null;
		RewriteRuleTokenStream stream_45=new RewriteRuleTokenStream(adaptor,"token 45");
		RewriteRuleTokenStream stream_MATCH_MODE=new RewriteRuleTokenStream(adaptor,"token MATCH_MODE");
		RewriteRuleSubtreeStream stream_term=new RewriteRuleSubtreeStream(adaptor,"rule term");

		try {
			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:146:2: ( '-' term ( MATCH_MODE )? -> ^( ND_EXCLUSION term ( MATCH_MODE )? ) )
			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:146:4: '-' term ( MATCH_MODE )?
			{
			char_literal15=(Token)match(input,45,FOLLOW_45_in_exclusionQuery381);  
			stream_45.add(char_literal15);

			pushFollow(FOLLOW_term_in_exclusionQuery383);
			term16=term();
			state._fsp--;

			stream_term.add(term16.getTree());
			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:146:13: ( MATCH_MODE )?
			int alt5=2;
			int LA5_0 = input.LA(1);
			if ( (LA5_0==MATCH_MODE) ) {
				alt5=1;
			}
			switch (alt5) {
				case 1 :
					// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:146:13: MATCH_MODE
					{
					MATCH_MODE17=(Token)match(input,MATCH_MODE,FOLLOW_MATCH_MODE_in_exclusionQuery385);  
					stream_MATCH_MODE.add(MATCH_MODE17);

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
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (Object)adaptor.nil();
			// 146:24: -> ^( ND_EXCLUSION term ( MATCH_MODE )? )
			{
				// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:146:27: ^( ND_EXCLUSION term ( MATCH_MODE )? )
				{
				Object root_1 = (Object)adaptor.nil();
				root_1 = (Object)adaptor.becomeRoot((Object)adaptor.create(ND_EXCLUSION, "ND_EXCLUSION"), root_1);
				adaptor.addChild(root_1, startTerm);
				adaptor.addChild(root_1, stream_term.nextTree());
				// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:146:60: ( MATCH_MODE )?
				if ( stream_MATCH_MODE.hasNext() ) {
					adaptor.addChild(root_1, stream_MATCH_MODE.nextNode());
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
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "adjacencyQuery"
	// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:150:1: adjacencyQuery[CommonTree startTerm] : ';' term -> ^( ND_ADJACENCY term ) ;
	public final CatmaQueryParser.adjacencyQuery_return adjacencyQuery(CommonTree startTerm) throws RecognitionException {
		CatmaQueryParser.adjacencyQuery_return retval = new CatmaQueryParser.adjacencyQuery_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token char_literal18=null;
		ParserRuleReturnScope term19 =null;

		Object char_literal18_tree=null;
		RewriteRuleTokenStream stream_46=new RewriteRuleTokenStream(adaptor,"token 46");
		RewriteRuleSubtreeStream stream_term=new RewriteRuleSubtreeStream(adaptor,"rule term");

		try {
			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:151:2: ( ';' term -> ^( ND_ADJACENCY term ) )
			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:151:4: ';' term
			{
			char_literal18=(Token)match(input,46,FOLLOW_46_in_adjacencyQuery419);  
			stream_46.add(char_literal18);

			pushFollow(FOLLOW_term_in_adjacencyQuery421);
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
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (Object)adaptor.nil();
			// 151:13: -> ^( ND_ADJACENCY term )
			{
				// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:151:16: ^( ND_ADJACENCY term )
				{
				Object root_1 = (Object)adaptor.nil();
				root_1 = (Object)adaptor.becomeRoot((Object)adaptor.create(ND_ADJACENCY, "ND_ADJACENCY"), root_1);
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
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "term"
	// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:156:1: term : ( phrase -> phrase | selector -> selector | '(' query ')' -> query );
	public final CatmaQueryParser.term_return term() throws RecognitionException {
		CatmaQueryParser.term_return retval = new CatmaQueryParser.term_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token char_literal22=null;
		Token char_literal24=null;
		ParserRuleReturnScope phrase20 =null;
		ParserRuleReturnScope selector21 =null;
		ParserRuleReturnScope query23 =null;

		Object char_literal22_tree=null;
		Object char_literal24_tree=null;
		RewriteRuleTokenStream stream_42=new RewriteRuleTokenStream(adaptor,"token 42");
		RewriteRuleTokenStream stream_43=new RewriteRuleTokenStream(adaptor,"token 43");
		RewriteRuleSubtreeStream stream_phrase=new RewriteRuleSubtreeStream(adaptor,"rule phrase");
		RewriteRuleSubtreeStream stream_query=new RewriteRuleSubtreeStream(adaptor,"rule query");
		RewriteRuleSubtreeStream stream_selector=new RewriteRuleSubtreeStream(adaptor,"rule selector");

		try {
			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:156:7: ( phrase -> phrase | selector -> selector | '(' query ')' -> query )
			int alt6=3;
			switch ( input.LA(1) ) {
			case TXT:
				{
				alt6=1;
				}
				break;
			case COMMENT:
			case FREQ:
			case PROPERTY:
			case REG:
			case SIMIL:
			case TAG:
			case TAGDIFF:
			case WILD:
				{
				alt6=2;
				}
				break;
			case 42:
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
					// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:156:9: phrase
					{
					pushFollow(FOLLOW_phrase_in_term451);
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
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (Object)adaptor.nil();
					// 156:16: -> phrase
					{
						adaptor.addChild(root_0, stream_phrase.nextTree());
					}


					retval.tree = root_0;

					}
					break;
				case 2 :
					// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:157:5: selector
					{
					pushFollow(FOLLOW_selector_in_term461);
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
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (Object)adaptor.nil();
					// 157:14: -> selector
					{
						adaptor.addChild(root_0, stream_selector.nextTree());
					}


					retval.tree = root_0;

					}
					break;
				case 3 :
					// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:158:5: '(' query ')'
					{
					char_literal22=(Token)match(input,42,FOLLOW_42_in_term472);  
					stream_42.add(char_literal22);

					pushFollow(FOLLOW_query_in_term473);
					query23=query();
					state._fsp--;

					stream_query.add(query23.getTree());
					char_literal24=(Token)match(input,43,FOLLOW_43_in_term474);  
					stream_43.add(char_literal24);

					// AST REWRITE
					// elements: query
					// token labels: 
					// rule labels: retval
					// token list labels: 
					// rule list labels: 
					// wildcard labels: 
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (Object)adaptor.nil();
					// 158:17: -> query
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
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "phrase"
	// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:165:1: phrase : TXT -> ^( ND_PHRASE TXT ) ;
	public final CatmaQueryParser.phrase_return phrase() throws RecognitionException {
		CatmaQueryParser.phrase_return retval = new CatmaQueryParser.phrase_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token TXT25=null;

		Object TXT25_tree=null;
		RewriteRuleTokenStream stream_TXT=new RewriteRuleTokenStream(adaptor,"token TXT");

		try {
			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:165:8: ( TXT -> ^( ND_PHRASE TXT ) )
			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:165:10: TXT
			{
			TXT25=(Token)match(input,TXT,FOLLOW_TXT_in_phrase498);  
			stream_TXT.add(TXT25);

			// AST REWRITE
			// elements: TXT
			// token labels: 
			// rule labels: retval
			// token list labels: 
			// rule list labels: 
			// wildcard labels: 
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (Object)adaptor.nil();
			// 165:14: -> ^( ND_PHRASE TXT )
			{
				// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:165:17: ^( ND_PHRASE TXT )
				{
				Object root_1 = (Object)adaptor.nil();
				root_1 = (Object)adaptor.becomeRoot((Object)adaptor.create(ND_PHRASE, "ND_PHRASE"), root_1);
				adaptor.addChild(root_1, stream_TXT.nextNode());
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
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "selector"
	// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:175:1: selector : ( tagQuery | tagdiffQuery | propertyQuery | regQuery | freqQuery | similQuery | wildQuery | commentQuery );
	public final CatmaQueryParser.selector_return selector() throws RecognitionException {
		CatmaQueryParser.selector_return retval = new CatmaQueryParser.selector_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		ParserRuleReturnScope tagQuery26 =null;
		ParserRuleReturnScope tagdiffQuery27 =null;
		ParserRuleReturnScope propertyQuery28 =null;
		ParserRuleReturnScope regQuery29 =null;
		ParserRuleReturnScope freqQuery30 =null;
		ParserRuleReturnScope similQuery31 =null;
		ParserRuleReturnScope wildQuery32 =null;
		ParserRuleReturnScope commentQuery33 =null;


		try {
			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:176:2: ( tagQuery | tagdiffQuery | propertyQuery | regQuery | freqQuery | similQuery | wildQuery | commentQuery )
			int alt7=8;
			switch ( input.LA(1) ) {
			case TAG:
				{
				int LA7_1 = input.LA(2);
				if ( (LA7_1==EQUAL) ) {
					int LA7_9 = input.LA(3);
					if ( (LA7_9==TXT) ) {
						int LA7_10 = input.LA(4);
						if ( (LA7_10==EOF||LA7_10==INT||LA7_10==MATCH_MODE||LA7_10==41||(LA7_10 >= 43 && LA7_10 <= 46)||(LA7_10 >= 48 && LA7_10 <= 50)) ) {
							alt7=1;
						}
						else if ( (LA7_10==PROPERTY) ) {
							alt7=3;
						}

						else {
							int nvaeMark = input.mark();
							try {
								for (int nvaeConsume = 0; nvaeConsume < 4 - 1; nvaeConsume++) {
									input.consume();
								}
								NoViableAltException nvae =
									new NoViableAltException("", 7, 10, input);
								throw nvae;
							} finally {
								input.rewind(nvaeMark);
							}
						}

					}

					else {
						int nvaeMark = input.mark();
						try {
							for (int nvaeConsume = 0; nvaeConsume < 3 - 1; nvaeConsume++) {
								input.consume();
							}
							NoViableAltException nvae =
								new NoViableAltException("", 7, 9, input);
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
							new NoViableAltException("", 7, 1, input);
						throw nvae;
					} finally {
						input.rewind(nvaeMark);
					}
				}

				}
				break;
			case TAGDIFF:
				{
				alt7=2;
				}
				break;
			case PROPERTY:
				{
				alt7=3;
				}
				break;
			case REG:
				{
				alt7=4;
				}
				break;
			case FREQ:
				{
				alt7=5;
				}
				break;
			case SIMIL:
				{
				alt7=6;
				}
				break;
			case WILD:
				{
				alt7=7;
				}
				break;
			case COMMENT:
				{
				alt7=8;
				}
				break;
			default:
				NoViableAltException nvae =
					new NoViableAltException("", 7, 0, input);
				throw nvae;
			}
			switch (alt7) {
				case 1 :
					// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:176:4: tagQuery
					{
					root_0 = (Object)adaptor.nil();


					pushFollow(FOLLOW_tagQuery_in_selector531);
					tagQuery26=tagQuery();
					state._fsp--;

					adaptor.addChild(root_0, tagQuery26.getTree());

					}
					break;
				case 2 :
					// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:177:4: tagdiffQuery
					{
					root_0 = (Object)adaptor.nil();


					pushFollow(FOLLOW_tagdiffQuery_in_selector536);
					tagdiffQuery27=tagdiffQuery();
					state._fsp--;

					adaptor.addChild(root_0, tagdiffQuery27.getTree());

					}
					break;
				case 3 :
					// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:178:4: propertyQuery
					{
					root_0 = (Object)adaptor.nil();


					pushFollow(FOLLOW_propertyQuery_in_selector541);
					propertyQuery28=propertyQuery();
					state._fsp--;

					adaptor.addChild(root_0, propertyQuery28.getTree());

					}
					break;
				case 4 :
					// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:179:4: regQuery
					{
					root_0 = (Object)adaptor.nil();


					pushFollow(FOLLOW_regQuery_in_selector546);
					regQuery29=regQuery();
					state._fsp--;

					adaptor.addChild(root_0, regQuery29.getTree());

					}
					break;
				case 5 :
					// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:180:4: freqQuery
					{
					root_0 = (Object)adaptor.nil();


					pushFollow(FOLLOW_freqQuery_in_selector551);
					freqQuery30=freqQuery();
					state._fsp--;

					adaptor.addChild(root_0, freqQuery30.getTree());

					}
					break;
				case 6 :
					// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:181:4: similQuery
					{
					root_0 = (Object)adaptor.nil();


					pushFollow(FOLLOW_similQuery_in_selector556);
					similQuery31=similQuery();
					state._fsp--;

					adaptor.addChild(root_0, similQuery31.getTree());

					}
					break;
				case 7 :
					// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:182:4: wildQuery
					{
					root_0 = (Object)adaptor.nil();


					pushFollow(FOLLOW_wildQuery_in_selector561);
					wildQuery32=wildQuery();
					state._fsp--;

					adaptor.addChild(root_0, wildQuery32.getTree());

					}
					break;
				case 8 :
					// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:183:4: commentQuery
					{
					root_0 = (Object)adaptor.nil();


					pushFollow(FOLLOW_commentQuery_in_selector566);
					commentQuery33=commentQuery();
					state._fsp--;

					adaptor.addChild(root_0, commentQuery33.getTree());

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
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "tagQuery"
	// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:188:1: tagQuery : TAG EQUAL phrase -> ^( ND_TAG phrase ) ;
	public final CatmaQueryParser.tagQuery_return tagQuery() throws RecognitionException {
		CatmaQueryParser.tagQuery_return retval = new CatmaQueryParser.tagQuery_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token TAG34=null;
		Token EQUAL35=null;
		ParserRuleReturnScope phrase36 =null;

		Object TAG34_tree=null;
		Object EQUAL35_tree=null;
		RewriteRuleTokenStream stream_EQUAL=new RewriteRuleTokenStream(adaptor,"token EQUAL");
		RewriteRuleTokenStream stream_TAG=new RewriteRuleTokenStream(adaptor,"token TAG");
		RewriteRuleSubtreeStream stream_phrase=new RewriteRuleSubtreeStream(adaptor,"rule phrase");

		try {
			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:189:2: ( TAG EQUAL phrase -> ^( ND_TAG phrase ) )
			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:189:4: TAG EQUAL phrase
			{
			TAG34=(Token)match(input,TAG,FOLLOW_TAG_in_tagQuery586);  
			stream_TAG.add(TAG34);

			EQUAL35=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_tagQuery588);  
			stream_EQUAL.add(EQUAL35);

			pushFollow(FOLLOW_phrase_in_tagQuery590);
			phrase36=phrase();
			state._fsp--;

			stream_phrase.add(phrase36.getTree());
			// AST REWRITE
			// elements: phrase
			// token labels: 
			// rule labels: retval
			// token list labels: 
			// rule list labels: 
			// wildcard labels: 
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (Object)adaptor.nil();
			// 189:21: -> ^( ND_TAG phrase )
			{
				// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:189:24: ^( ND_TAG phrase )
				{
				Object root_1 = (Object)adaptor.nil();
				root_1 = (Object)adaptor.becomeRoot((Object)adaptor.create(ND_TAG, "ND_TAG"), root_1);
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


	public static class tagdiffQuery_return extends ParserRuleReturnScope {
		Object tree;
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "tagdiffQuery"
	// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:193:1: tagdiffQuery : TAGDIFF EQUAL phrase ( PROPERTY EQUAL phrase )? -> ^( ND_TAGDIFF phrase ( phrase )? ) ;
	public final CatmaQueryParser.tagdiffQuery_return tagdiffQuery() throws RecognitionException {
		CatmaQueryParser.tagdiffQuery_return retval = new CatmaQueryParser.tagdiffQuery_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token TAGDIFF37=null;
		Token EQUAL38=null;
		Token PROPERTY40=null;
		Token EQUAL41=null;
		ParserRuleReturnScope phrase39 =null;
		ParserRuleReturnScope phrase42 =null;

		Object TAGDIFF37_tree=null;
		Object EQUAL38_tree=null;
		Object PROPERTY40_tree=null;
		Object EQUAL41_tree=null;
		RewriteRuleTokenStream stream_TAGDIFF=new RewriteRuleTokenStream(adaptor,"token TAGDIFF");
		RewriteRuleTokenStream stream_EQUAL=new RewriteRuleTokenStream(adaptor,"token EQUAL");
		RewriteRuleTokenStream stream_PROPERTY=new RewriteRuleTokenStream(adaptor,"token PROPERTY");
		RewriteRuleSubtreeStream stream_phrase=new RewriteRuleSubtreeStream(adaptor,"rule phrase");

		try {
			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:194:2: ( TAGDIFF EQUAL phrase ( PROPERTY EQUAL phrase )? -> ^( ND_TAGDIFF phrase ( phrase )? ) )
			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:194:4: TAGDIFF EQUAL phrase ( PROPERTY EQUAL phrase )?
			{
			TAGDIFF37=(Token)match(input,TAGDIFF,FOLLOW_TAGDIFF_in_tagdiffQuery617);  
			stream_TAGDIFF.add(TAGDIFF37);

			EQUAL38=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_tagdiffQuery619);  
			stream_EQUAL.add(EQUAL38);

			pushFollow(FOLLOW_phrase_in_tagdiffQuery621);
			phrase39=phrase();
			state._fsp--;

			stream_phrase.add(phrase39.getTree());
			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:194:25: ( PROPERTY EQUAL phrase )?
			int alt8=2;
			int LA8_0 = input.LA(1);
			if ( (LA8_0==PROPERTY) ) {
				alt8=1;
			}
			switch (alt8) {
				case 1 :
					// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:194:26: PROPERTY EQUAL phrase
					{
					PROPERTY40=(Token)match(input,PROPERTY,FOLLOW_PROPERTY_in_tagdiffQuery624);  
					stream_PROPERTY.add(PROPERTY40);

					EQUAL41=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_tagdiffQuery626);  
					stream_EQUAL.add(EQUAL41);

					pushFollow(FOLLOW_phrase_in_tagdiffQuery628);
					phrase42=phrase();
					state._fsp--;

					stream_phrase.add(phrase42.getTree());
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
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (Object)adaptor.nil();
			// 194:50: -> ^( ND_TAGDIFF phrase ( phrase )? )
			{
				// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:194:53: ^( ND_TAGDIFF phrase ( phrase )? )
				{
				Object root_1 = (Object)adaptor.nil();
				root_1 = (Object)adaptor.becomeRoot((Object)adaptor.create(ND_TAGDIFF, "ND_TAGDIFF"), root_1);
				adaptor.addChild(root_1, stream_phrase.nextTree());
				// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:194:73: ( phrase )?
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
	// $ANTLR end "tagdiffQuery"


	public static class propertyQuery_return extends ParserRuleReturnScope {
		Object tree;
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "propertyQuery"
	// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:199:1: propertyQuery : ( PROPERTY EQUAL phrase ( VALUE EQUAL phrase )? -> ^( ND_PROPERTY phrase ( phrase )? ) | TAG EQUAL phrase PROPERTY EQUAL phrase ( VALUE EQUAL phrase )? -> ^( ND_TAGPROPERTY phrase phrase ( phrase )? ) );
	public final CatmaQueryParser.propertyQuery_return propertyQuery() throws RecognitionException {
		CatmaQueryParser.propertyQuery_return retval = new CatmaQueryParser.propertyQuery_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token PROPERTY43=null;
		Token EQUAL44=null;
		Token VALUE46=null;
		Token EQUAL47=null;
		Token TAG49=null;
		Token EQUAL50=null;
		Token PROPERTY52=null;
		Token EQUAL53=null;
		Token VALUE55=null;
		Token EQUAL56=null;
		ParserRuleReturnScope phrase45 =null;
		ParserRuleReturnScope phrase48 =null;
		ParserRuleReturnScope phrase51 =null;
		ParserRuleReturnScope phrase54 =null;
		ParserRuleReturnScope phrase57 =null;

		Object PROPERTY43_tree=null;
		Object EQUAL44_tree=null;
		Object VALUE46_tree=null;
		Object EQUAL47_tree=null;
		Object TAG49_tree=null;
		Object EQUAL50_tree=null;
		Object PROPERTY52_tree=null;
		Object EQUAL53_tree=null;
		Object VALUE55_tree=null;
		Object EQUAL56_tree=null;
		RewriteRuleTokenStream stream_PROPERTY=new RewriteRuleTokenStream(adaptor,"token PROPERTY");
		RewriteRuleTokenStream stream_EQUAL=new RewriteRuleTokenStream(adaptor,"token EQUAL");
		RewriteRuleTokenStream stream_VALUE=new RewriteRuleTokenStream(adaptor,"token VALUE");
		RewriteRuleTokenStream stream_TAG=new RewriteRuleTokenStream(adaptor,"token TAG");
		RewriteRuleSubtreeStream stream_phrase=new RewriteRuleSubtreeStream(adaptor,"rule phrase");

		try {
			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:200:2: ( PROPERTY EQUAL phrase ( VALUE EQUAL phrase )? -> ^( ND_PROPERTY phrase ( phrase )? ) | TAG EQUAL phrase PROPERTY EQUAL phrase ( VALUE EQUAL phrase )? -> ^( ND_TAGPROPERTY phrase phrase ( phrase )? ) )
			int alt11=2;
			int LA11_0 = input.LA(1);
			if ( (LA11_0==PROPERTY) ) {
				alt11=1;
			}
			else if ( (LA11_0==TAG) ) {
				alt11=2;
			}

			else {
				NoViableAltException nvae =
					new NoViableAltException("", 11, 0, input);
				throw nvae;
			}

			switch (alt11) {
				case 1 :
					// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:200:4: PROPERTY EQUAL phrase ( VALUE EQUAL phrase )?
					{
					PROPERTY43=(Token)match(input,PROPERTY,FOLLOW_PROPERTY_in_propertyQuery661);  
					stream_PROPERTY.add(PROPERTY43);

					EQUAL44=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_propertyQuery663);  
					stream_EQUAL.add(EQUAL44);

					pushFollow(FOLLOW_phrase_in_propertyQuery665);
					phrase45=phrase();
					state._fsp--;

					stream_phrase.add(phrase45.getTree());
					// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:200:26: ( VALUE EQUAL phrase )?
					int alt9=2;
					int LA9_0 = input.LA(1);
					if ( (LA9_0==VALUE) ) {
						alt9=1;
					}
					switch (alt9) {
						case 1 :
							// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:200:27: VALUE EQUAL phrase
							{
							VALUE46=(Token)match(input,VALUE,FOLLOW_VALUE_in_propertyQuery668);  
							stream_VALUE.add(VALUE46);

							EQUAL47=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_propertyQuery670);  
							stream_EQUAL.add(EQUAL47);

							pushFollow(FOLLOW_phrase_in_propertyQuery672);
							phrase48=phrase();
							state._fsp--;

							stream_phrase.add(phrase48.getTree());
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
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (Object)adaptor.nil();
					// 200:48: -> ^( ND_PROPERTY phrase ( phrase )? )
					{
						// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:200:51: ^( ND_PROPERTY phrase ( phrase )? )
						{
						Object root_1 = (Object)adaptor.nil();
						root_1 = (Object)adaptor.becomeRoot((Object)adaptor.create(ND_PROPERTY, "ND_PROPERTY"), root_1);
						adaptor.addChild(root_1, stream_phrase.nextTree());
						// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:200:72: ( phrase )?
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
					// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:201:4: TAG EQUAL phrase PROPERTY EQUAL phrase ( VALUE EQUAL phrase )?
					{
					TAG49=(Token)match(input,TAG,FOLLOW_TAG_in_propertyQuery691);  
					stream_TAG.add(TAG49);

					EQUAL50=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_propertyQuery693);  
					stream_EQUAL.add(EQUAL50);

					pushFollow(FOLLOW_phrase_in_propertyQuery695);
					phrase51=phrase();
					state._fsp--;

					stream_phrase.add(phrase51.getTree());
					PROPERTY52=(Token)match(input,PROPERTY,FOLLOW_PROPERTY_in_propertyQuery697);  
					stream_PROPERTY.add(PROPERTY52);

					EQUAL53=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_propertyQuery699);  
					stream_EQUAL.add(EQUAL53);

					pushFollow(FOLLOW_phrase_in_propertyQuery701);
					phrase54=phrase();
					state._fsp--;

					stream_phrase.add(phrase54.getTree());
					// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:201:43: ( VALUE EQUAL phrase )?
					int alt10=2;
					int LA10_0 = input.LA(1);
					if ( (LA10_0==VALUE) ) {
						alt10=1;
					}
					switch (alt10) {
						case 1 :
							// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:201:44: VALUE EQUAL phrase
							{
							VALUE55=(Token)match(input,VALUE,FOLLOW_VALUE_in_propertyQuery704);  
							stream_VALUE.add(VALUE55);

							EQUAL56=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_propertyQuery706);  
							stream_EQUAL.add(EQUAL56);

							pushFollow(FOLLOW_phrase_in_propertyQuery708);
							phrase57=phrase();
							state._fsp--;

							stream_phrase.add(phrase57.getTree());
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
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (Object)adaptor.nil();
					// 201:65: -> ^( ND_TAGPROPERTY phrase phrase ( phrase )? )
					{
						// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:201:68: ^( ND_TAGPROPERTY phrase phrase ( phrase )? )
						{
						Object root_1 = (Object)adaptor.nil();
						root_1 = (Object)adaptor.becomeRoot((Object)adaptor.create(ND_TAGPROPERTY, "ND_TAGPROPERTY"), root_1);
						adaptor.addChild(root_1, stream_phrase.nextTree());
						adaptor.addChild(root_1, stream_phrase.nextTree());
						// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:201:99: ( phrase )?
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
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "regQuery"
	// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:205:1: regQuery : REG EQUAL phrase ( 'CI' )? -> ^( ND_REG phrase ( 'CI' )? ) ;
	public final CatmaQueryParser.regQuery_return regQuery() throws RecognitionException {
		CatmaQueryParser.regQuery_return retval = new CatmaQueryParser.regQuery_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token REG58=null;
		Token EQUAL59=null;
		Token string_literal61=null;
		ParserRuleReturnScope phrase60 =null;

		Object REG58_tree=null;
		Object EQUAL59_tree=null;
		Object string_literal61_tree=null;
		RewriteRuleTokenStream stream_47=new RewriteRuleTokenStream(adaptor,"token 47");
		RewriteRuleTokenStream stream_REG=new RewriteRuleTokenStream(adaptor,"token REG");
		RewriteRuleTokenStream stream_EQUAL=new RewriteRuleTokenStream(adaptor,"token EQUAL");
		RewriteRuleSubtreeStream stream_phrase=new RewriteRuleSubtreeStream(adaptor,"rule phrase");

		try {
			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:206:2: ( REG EQUAL phrase ( 'CI' )? -> ^( ND_REG phrase ( 'CI' )? ) )
			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:206:4: REG EQUAL phrase ( 'CI' )?
			{
			REG58=(Token)match(input,REG,FOLLOW_REG_in_regQuery741);  
			stream_REG.add(REG58);

			EQUAL59=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_regQuery743);  
			stream_EQUAL.add(EQUAL59);

			pushFollow(FOLLOW_phrase_in_regQuery745);
			phrase60=phrase();
			state._fsp--;

			stream_phrase.add(phrase60.getTree());
			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:206:21: ( 'CI' )?
			int alt12=2;
			int LA12_0 = input.LA(1);
			if ( (LA12_0==47) ) {
				alt12=1;
			}
			switch (alt12) {
				case 1 :
					// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:206:21: 'CI'
					{
					string_literal61=(Token)match(input,47,FOLLOW_47_in_regQuery747);  
					stream_47.add(string_literal61);

					}
					break;

			}

			// AST REWRITE
			// elements: 47, phrase
			// token labels: 
			// rule labels: retval
			// token list labels: 
			// rule list labels: 
			// wildcard labels: 
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (Object)adaptor.nil();
			// 206:27: -> ^( ND_REG phrase ( 'CI' )? )
			{
				// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:206:30: ^( ND_REG phrase ( 'CI' )? )
				{
				Object root_1 = (Object)adaptor.nil();
				root_1 = (Object)adaptor.becomeRoot((Object)adaptor.create(ND_REG, "ND_REG"), root_1);
				adaptor.addChild(root_1, stream_phrase.nextTree());
				// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:206:46: ( 'CI' )?
				if ( stream_47.hasNext() ) {
					adaptor.addChild(root_1, stream_47.nextNode());
				}
				stream_47.reset();

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
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "freqQuery"
	// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:210:1: freqQuery : FREQ ( EQUAL INT ( '-' INT )? -> ^( ND_FREQ EQUAL INT ( INT )? ) | UNEQUAL INT -> ^( ND_FREQ UNEQUAL INT ) ) ;
	public final CatmaQueryParser.freqQuery_return freqQuery() throws RecognitionException {
		CatmaQueryParser.freqQuery_return retval = new CatmaQueryParser.freqQuery_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token FREQ62=null;
		Token EQUAL63=null;
		Token INT64=null;
		Token char_literal65=null;
		Token INT66=null;
		Token UNEQUAL67=null;
		Token INT68=null;

		Object FREQ62_tree=null;
		Object EQUAL63_tree=null;
		Object INT64_tree=null;
		Object char_literal65_tree=null;
		Object INT66_tree=null;
		Object UNEQUAL67_tree=null;
		Object INT68_tree=null;
		RewriteRuleTokenStream stream_45=new RewriteRuleTokenStream(adaptor,"token 45");
		RewriteRuleTokenStream stream_UNEQUAL=new RewriteRuleTokenStream(adaptor,"token UNEQUAL");
		RewriteRuleTokenStream stream_EQUAL=new RewriteRuleTokenStream(adaptor,"token EQUAL");
		RewriteRuleTokenStream stream_FREQ=new RewriteRuleTokenStream(adaptor,"token FREQ");
		RewriteRuleTokenStream stream_INT=new RewriteRuleTokenStream(adaptor,"token INT");

		try {
			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:211:2: ( FREQ ( EQUAL INT ( '-' INT )? -> ^( ND_FREQ EQUAL INT ( INT )? ) | UNEQUAL INT -> ^( ND_FREQ UNEQUAL INT ) ) )
			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:211:4: FREQ ( EQUAL INT ( '-' INT )? -> ^( ND_FREQ EQUAL INT ( INT )? ) | UNEQUAL INT -> ^( ND_FREQ UNEQUAL INT ) )
			{
			FREQ62=(Token)match(input,FREQ,FOLLOW_FREQ_in_freqQuery778);  
			stream_FREQ.add(FREQ62);

			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:212:3: ( EQUAL INT ( '-' INT )? -> ^( ND_FREQ EQUAL INT ( INT )? ) | UNEQUAL INT -> ^( ND_FREQ UNEQUAL INT ) )
			int alt14=2;
			int LA14_0 = input.LA(1);
			if ( (LA14_0==EQUAL) ) {
				alt14=1;
			}
			else if ( (LA14_0==UNEQUAL) ) {
				alt14=2;
			}

			else {
				NoViableAltException nvae =
					new NoViableAltException("", 14, 0, input);
				throw nvae;
			}

			switch (alt14) {
				case 1 :
					// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:212:5: EQUAL INT ( '-' INT )?
					{
					EQUAL63=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_freqQuery785);  
					stream_EQUAL.add(EQUAL63);

					INT64=(Token)match(input,INT,FOLLOW_INT_in_freqQuery787);  
					stream_INT.add(INT64);

					// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:212:15: ( '-' INT )?
					int alt13=2;
					int LA13_0 = input.LA(1);
					if ( (LA13_0==45) ) {
						int LA13_1 = input.LA(2);
						if ( (LA13_1==INT) ) {
							alt13=1;
						}
					}
					switch (alt13) {
						case 1 :
							// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:212:16: '-' INT
							{
							char_literal65=(Token)match(input,45,FOLLOW_45_in_freqQuery790);  
							stream_45.add(char_literal65);

							INT66=(Token)match(input,INT,FOLLOW_INT_in_freqQuery792);  
							stream_INT.add(INT66);

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
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (Object)adaptor.nil();
					// 212:26: -> ^( ND_FREQ EQUAL INT ( INT )? )
					{
						// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:212:29: ^( ND_FREQ EQUAL INT ( INT )? )
						{
						Object root_1 = (Object)adaptor.nil();
						root_1 = (Object)adaptor.becomeRoot((Object)adaptor.create(ND_FREQ, "ND_FREQ"), root_1);
						adaptor.addChild(root_1, stream_EQUAL.nextNode());
						adaptor.addChild(root_1, stream_INT.nextNode());
						// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:212:49: ( INT )?
						if ( stream_INT.hasNext() ) {
							adaptor.addChild(root_1, stream_INT.nextNode());
						}
						stream_INT.reset();

						adaptor.addChild(root_0, root_1);
						}

					}


					retval.tree = root_0;

					}
					break;
				case 2 :
					// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:213:5: UNEQUAL INT
					{
					UNEQUAL67=(Token)match(input,UNEQUAL,FOLLOW_UNEQUAL_in_freqQuery814);  
					stream_UNEQUAL.add(UNEQUAL67);

					INT68=(Token)match(input,INT,FOLLOW_INT_in_freqQuery816);  
					stream_INT.add(INT68);

					// AST REWRITE
					// elements: UNEQUAL, INT
					// token labels: 
					// rule labels: retval
					// token list labels: 
					// rule list labels: 
					// wildcard labels: 
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (Object)adaptor.nil();
					// 213:17: -> ^( ND_FREQ UNEQUAL INT )
					{
						// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:213:20: ^( ND_FREQ UNEQUAL INT )
						{
						Object root_1 = (Object)adaptor.nil();
						root_1 = (Object)adaptor.becomeRoot((Object)adaptor.create(ND_FREQ, "ND_FREQ"), root_1);
						adaptor.addChild(root_1, stream_UNEQUAL.nextNode());
						adaptor.addChild(root_1, stream_INT.nextNode());
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
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "similQuery"
	// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:217:1: similQuery : SIMIL EQUAL phrase INT ( '%' )? -> ^( ND_SIMIL phrase INT ) ;
	public final CatmaQueryParser.similQuery_return similQuery() throws RecognitionException {
		CatmaQueryParser.similQuery_return retval = new CatmaQueryParser.similQuery_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token SIMIL69=null;
		Token EQUAL70=null;
		Token INT72=null;
		Token char_literal73=null;
		ParserRuleReturnScope phrase71 =null;

		Object SIMIL69_tree=null;
		Object EQUAL70_tree=null;
		Object INT72_tree=null;
		Object char_literal73_tree=null;
		RewriteRuleTokenStream stream_EQUAL=new RewriteRuleTokenStream(adaptor,"token EQUAL");
		RewriteRuleTokenStream stream_SIMIL=new RewriteRuleTokenStream(adaptor,"token SIMIL");
		RewriteRuleTokenStream stream_40=new RewriteRuleTokenStream(adaptor,"token 40");
		RewriteRuleTokenStream stream_INT=new RewriteRuleTokenStream(adaptor,"token INT");
		RewriteRuleSubtreeStream stream_phrase=new RewriteRuleSubtreeStream(adaptor,"rule phrase");

		try {
			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:218:2: ( SIMIL EQUAL phrase INT ( '%' )? -> ^( ND_SIMIL phrase INT ) )
			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:218:4: SIMIL EQUAL phrase INT ( '%' )?
			{
			SIMIL69=(Token)match(input,SIMIL,FOLLOW_SIMIL_in_similQuery848);  
			stream_SIMIL.add(SIMIL69);

			EQUAL70=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_similQuery850);  
			stream_EQUAL.add(EQUAL70);

			pushFollow(FOLLOW_phrase_in_similQuery852);
			phrase71=phrase();
			state._fsp--;

			stream_phrase.add(phrase71.getTree());
			INT72=(Token)match(input,INT,FOLLOW_INT_in_similQuery854);  
			stream_INT.add(INT72);

			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:218:27: ( '%' )?
			int alt15=2;
			int LA15_0 = input.LA(1);
			if ( (LA15_0==40) ) {
				alt15=1;
			}
			switch (alt15) {
				case 1 :
					// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:218:27: '%'
					{
					char_literal73=(Token)match(input,40,FOLLOW_40_in_similQuery856);  
					stream_40.add(char_literal73);

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
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (Object)adaptor.nil();
			// 218:32: -> ^( ND_SIMIL phrase INT )
			{
				// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:218:35: ^( ND_SIMIL phrase INT )
				{
				Object root_1 = (Object)adaptor.nil();
				root_1 = (Object)adaptor.becomeRoot((Object)adaptor.create(ND_SIMIL, "ND_SIMIL"), root_1);
				adaptor.addChild(root_1, stream_phrase.nextTree());
				adaptor.addChild(root_1, stream_INT.nextNode());
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
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "wildQuery"
	// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:222:1: wildQuery : WILD EQUAL phrase -> ^( ND_WILD phrase ) ;
	public final CatmaQueryParser.wildQuery_return wildQuery() throws RecognitionException {
		CatmaQueryParser.wildQuery_return retval = new CatmaQueryParser.wildQuery_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token WILD74=null;
		Token EQUAL75=null;
		ParserRuleReturnScope phrase76 =null;

		Object WILD74_tree=null;
		Object EQUAL75_tree=null;
		RewriteRuleTokenStream stream_WILD=new RewriteRuleTokenStream(adaptor,"token WILD");
		RewriteRuleTokenStream stream_EQUAL=new RewriteRuleTokenStream(adaptor,"token EQUAL");
		RewriteRuleSubtreeStream stream_phrase=new RewriteRuleSubtreeStream(adaptor,"rule phrase");

		try {
			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:223:2: ( WILD EQUAL phrase -> ^( ND_WILD phrase ) )
			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:223:4: WILD EQUAL phrase
			{
			WILD74=(Token)match(input,WILD,FOLLOW_WILD_in_wildQuery886);  
			stream_WILD.add(WILD74);

			EQUAL75=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_wildQuery888);  
			stream_EQUAL.add(EQUAL75);

			pushFollow(FOLLOW_phrase_in_wildQuery890);
			phrase76=phrase();
			state._fsp--;

			stream_phrase.add(phrase76.getTree());
			// AST REWRITE
			// elements: phrase
			// token labels: 
			// rule labels: retval
			// token list labels: 
			// rule list labels: 
			// wildcard labels: 
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (Object)adaptor.nil();
			// 223:22: -> ^( ND_WILD phrase )
			{
				// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:223:25: ^( ND_WILD phrase )
				{
				Object root_1 = (Object)adaptor.nil();
				root_1 = (Object)adaptor.becomeRoot((Object)adaptor.create(ND_WILD, "ND_WILD"), root_1);
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


	public static class commentQuery_return extends ParserRuleReturnScope {
		Object tree;
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "commentQuery"
	// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:227:1: commentQuery : COMMENT EQUAL phrase -> ^( ND_COMMENT phrase ) ;
	public final CatmaQueryParser.commentQuery_return commentQuery() throws RecognitionException {
		CatmaQueryParser.commentQuery_return retval = new CatmaQueryParser.commentQuery_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token COMMENT77=null;
		Token EQUAL78=null;
		ParserRuleReturnScope phrase79 =null;

		Object COMMENT77_tree=null;
		Object EQUAL78_tree=null;
		RewriteRuleTokenStream stream_EQUAL=new RewriteRuleTokenStream(adaptor,"token EQUAL");
		RewriteRuleTokenStream stream_COMMENT=new RewriteRuleTokenStream(adaptor,"token COMMENT");
		RewriteRuleSubtreeStream stream_phrase=new RewriteRuleSubtreeStream(adaptor,"rule phrase");

		try {
			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:228:2: ( COMMENT EQUAL phrase -> ^( ND_COMMENT phrase ) )
			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:228:4: COMMENT EQUAL phrase
			{
			COMMENT77=(Token)match(input,COMMENT,FOLLOW_COMMENT_in_commentQuery915);  
			stream_COMMENT.add(COMMENT77);

			EQUAL78=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_commentQuery917);  
			stream_EQUAL.add(EQUAL78);

			pushFollow(FOLLOW_phrase_in_commentQuery919);
			phrase79=phrase();
			state._fsp--;

			stream_phrase.add(phrase79.getTree());
			// AST REWRITE
			// elements: phrase
			// token labels: 
			// rule labels: retval
			// token list labels: 
			// rule list labels: 
			// wildcard labels: 
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (Object)adaptor.nil();
			// 228:25: -> ^( ND_COMMENT phrase )
			{
				// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:228:28: ^( ND_COMMENT phrase )
				{
				Object root_1 = (Object)adaptor.nil();
				root_1 = (Object)adaptor.becomeRoot((Object)adaptor.create(ND_COMMENT, "ND_COMMENT"), root_1);
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
	// $ANTLR end "commentQuery"


	public static class refinement_return extends ParserRuleReturnScope {
		Object tree;
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "refinement"
	// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:236:1: refinement : 'where' refinementExpression -> refinementExpression ;
	public final CatmaQueryParser.refinement_return refinement() throws RecognitionException {
		CatmaQueryParser.refinement_return retval = new CatmaQueryParser.refinement_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token string_literal80=null;
		ParserRuleReturnScope refinementExpression81 =null;

		Object string_literal80_tree=null;
		RewriteRuleTokenStream stream_49=new RewriteRuleTokenStream(adaptor,"token 49");
		RewriteRuleSubtreeStream stream_refinementExpression=new RewriteRuleSubtreeStream(adaptor,"rule refinementExpression");

		try {
			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:237:2: ( 'where' refinementExpression -> refinementExpression )
			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:237:4: 'where' refinementExpression
			{
			string_literal80=(Token)match(input,49,FOLLOW_49_in_refinement949);  
			stream_49.add(string_literal80);

			pushFollow(FOLLOW_refinementExpression_in_refinement951);
			refinementExpression81=refinementExpression();
			state._fsp--;

			stream_refinementExpression.add(refinementExpression81.getTree());
			// AST REWRITE
			// elements: refinementExpression
			// token labels: 
			// rule labels: retval
			// token list labels: 
			// rule list labels: 
			// wildcard labels: 
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (Object)adaptor.nil();
			// 237:33: -> refinementExpression
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
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "refinementExpression"
	// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:241:1: refinementExpression : startRefinement= term ( MATCH_MODE )? ( orRefinement[(CommonTree)$startRefinement.tree] -> orRefinement | andRefinement[(CommonTree)$startRefinement.tree] -> andRefinement | -> ^( ND_REFINE term ( MATCH_MODE )? ) ) ;
	public final CatmaQueryParser.refinementExpression_return refinementExpression() throws RecognitionException {
		CatmaQueryParser.refinementExpression_return retval = new CatmaQueryParser.refinementExpression_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token MATCH_MODE82=null;
		ParserRuleReturnScope startRefinement =null;
		ParserRuleReturnScope orRefinement83 =null;
		ParserRuleReturnScope andRefinement84 =null;

		Object MATCH_MODE82_tree=null;
		RewriteRuleTokenStream stream_MATCH_MODE=new RewriteRuleTokenStream(adaptor,"token MATCH_MODE");
		RewriteRuleSubtreeStream stream_andRefinement=new RewriteRuleSubtreeStream(adaptor,"rule andRefinement");
		RewriteRuleSubtreeStream stream_term=new RewriteRuleSubtreeStream(adaptor,"rule term");
		RewriteRuleSubtreeStream stream_orRefinement=new RewriteRuleSubtreeStream(adaptor,"rule orRefinement");

		try {
			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:242:2: (startRefinement= term ( MATCH_MODE )? ( orRefinement[(CommonTree)$startRefinement.tree] -> orRefinement | andRefinement[(CommonTree)$startRefinement.tree] -> andRefinement | -> ^( ND_REFINE term ( MATCH_MODE )? ) ) )
			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:242:4: startRefinement= term ( MATCH_MODE )? ( orRefinement[(CommonTree)$startRefinement.tree] -> orRefinement | andRefinement[(CommonTree)$startRefinement.tree] -> andRefinement | -> ^( ND_REFINE term ( MATCH_MODE )? ) )
			{
			pushFollow(FOLLOW_term_in_refinementExpression975);
			startRefinement=term();
			state._fsp--;

			stream_term.add(startRefinement.getTree());
			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:242:25: ( MATCH_MODE )?
			int alt16=2;
			int LA16_0 = input.LA(1);
			if ( (LA16_0==MATCH_MODE) ) {
				alt16=1;
			}
			switch (alt16) {
				case 1 :
					// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:242:25: MATCH_MODE
					{
					MATCH_MODE82=(Token)match(input,MATCH_MODE,FOLLOW_MATCH_MODE_in_refinementExpression977);  
					stream_MATCH_MODE.add(MATCH_MODE82);

					}
					break;

			}

			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:242:37: ( orRefinement[(CommonTree)$startRefinement.tree] -> orRefinement | andRefinement[(CommonTree)$startRefinement.tree] -> andRefinement | -> ^( ND_REFINE term ( MATCH_MODE )? ) )
			int alt17=3;
			switch ( input.LA(1) ) {
			case 50:
				{
				alt17=1;
				}
				break;
			case 44:
				{
				alt17=2;
				}
				break;
			case EOF:
			case 43:
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
					// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:243:4: orRefinement[(CommonTree)$startRefinement.tree]
					{
					pushFollow(FOLLOW_orRefinement_in_refinementExpression986);
					orRefinement83=orRefinement((CommonTree)(startRefinement!=null?((Object)startRefinement.getTree()):null));
					state._fsp--;

					stream_orRefinement.add(orRefinement83.getTree());
					// AST REWRITE
					// elements: orRefinement
					// token labels: 
					// rule labels: retval
					// token list labels: 
					// rule list labels: 
					// wildcard labels: 
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (Object)adaptor.nil();
					// 243:52: -> orRefinement
					{
						adaptor.addChild(root_0, stream_orRefinement.nextTree());
					}


					retval.tree = root_0;

					}
					break;
				case 2 :
					// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:244:6: andRefinement[(CommonTree)$startRefinement.tree]
					{
					pushFollow(FOLLOW_andRefinement_in_refinementExpression998);
					andRefinement84=andRefinement((CommonTree)(startRefinement!=null?((Object)startRefinement.getTree()):null));
					state._fsp--;

					stream_andRefinement.add(andRefinement84.getTree());
					// AST REWRITE
					// elements: andRefinement
					// token labels: 
					// rule labels: retval
					// token list labels: 
					// rule list labels: 
					// wildcard labels: 
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (Object)adaptor.nil();
					// 244:55: -> andRefinement
					{
						adaptor.addChild(root_0, stream_andRefinement.nextTree());
					}


					retval.tree = root_0;

					}
					break;
				case 3 :
					// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:245:6: 
					{
					// AST REWRITE
					// elements: term, MATCH_MODE
					// token labels: 
					// rule labels: retval
					// token list labels: 
					// rule list labels: 
					// wildcard labels: 
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (Object)adaptor.nil();
					// 245:6: -> ^( ND_REFINE term ( MATCH_MODE )? )
					{
						// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:245:9: ^( ND_REFINE term ( MATCH_MODE )? )
						{
						Object root_1 = (Object)adaptor.nil();
						root_1 = (Object)adaptor.becomeRoot((Object)adaptor.create(ND_REFINE, "ND_REFINE"), root_1);
						adaptor.addChild(root_1, stream_term.nextTree());
						// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:245:26: ( MATCH_MODE )?
						if ( stream_MATCH_MODE.hasNext() ) {
							adaptor.addChild(root_1, stream_MATCH_MODE.nextNode());
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
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "orRefinement"
	// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:249:1: orRefinement[CommonTree startRefinement] : '|' term ( MATCH_MODE )? -> ^( ND_ORREFINE term ( MATCH_MODE )? ) ;
	public final CatmaQueryParser.orRefinement_return orRefinement(CommonTree startRefinement) throws RecognitionException {
		CatmaQueryParser.orRefinement_return retval = new CatmaQueryParser.orRefinement_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token char_literal85=null;
		Token MATCH_MODE87=null;
		ParserRuleReturnScope term86 =null;

		Object char_literal85_tree=null;
		Object MATCH_MODE87_tree=null;
		RewriteRuleTokenStream stream_MATCH_MODE=new RewriteRuleTokenStream(adaptor,"token MATCH_MODE");
		RewriteRuleTokenStream stream_50=new RewriteRuleTokenStream(adaptor,"token 50");
		RewriteRuleSubtreeStream stream_term=new RewriteRuleSubtreeStream(adaptor,"rule term");

		try {
			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:250:2: ( '|' term ( MATCH_MODE )? -> ^( ND_ORREFINE term ( MATCH_MODE )? ) )
			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:250:4: '|' term ( MATCH_MODE )?
			{
			char_literal85=(Token)match(input,50,FOLLOW_50_in_orRefinement1040);  
			stream_50.add(char_literal85);

			pushFollow(FOLLOW_term_in_orRefinement1042);
			term86=term();
			state._fsp--;

			stream_term.add(term86.getTree());
			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:250:13: ( MATCH_MODE )?
			int alt18=2;
			int LA18_0 = input.LA(1);
			if ( (LA18_0==MATCH_MODE) ) {
				alt18=1;
			}
			switch (alt18) {
				case 1 :
					// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:250:13: MATCH_MODE
					{
					MATCH_MODE87=(Token)match(input,MATCH_MODE,FOLLOW_MATCH_MODE_in_orRefinement1044);  
					stream_MATCH_MODE.add(MATCH_MODE87);

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
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (Object)adaptor.nil();
			// 250:25: -> ^( ND_ORREFINE term ( MATCH_MODE )? )
			{
				// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:250:28: ^( ND_ORREFINE term ( MATCH_MODE )? )
				{
				Object root_1 = (Object)adaptor.nil();
				root_1 = (Object)adaptor.becomeRoot((Object)adaptor.create(ND_ORREFINE, "ND_ORREFINE"), root_1);
				adaptor.addChild(root_1, startRefinement);
				adaptor.addChild(root_1, stream_term.nextTree());
				// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:250:66: ( MATCH_MODE )?
				if ( stream_MATCH_MODE.hasNext() ) {
					adaptor.addChild(root_1, stream_MATCH_MODE.nextNode());
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
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "andRefinement"
	// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:254:1: andRefinement[CommonTree startRefinement] : ',' term ( MATCH_MODE )? -> ^( ND_ANDREFINE term ( MATCH_MODE )? ) ;
	public final CatmaQueryParser.andRefinement_return andRefinement(CommonTree startRefinement) throws RecognitionException {
		CatmaQueryParser.andRefinement_return retval = new CatmaQueryParser.andRefinement_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token char_literal88=null;
		Token MATCH_MODE90=null;
		ParserRuleReturnScope term89 =null;

		Object char_literal88_tree=null;
		Object MATCH_MODE90_tree=null;
		RewriteRuleTokenStream stream_44=new RewriteRuleTokenStream(adaptor,"token 44");
		RewriteRuleTokenStream stream_MATCH_MODE=new RewriteRuleTokenStream(adaptor,"token MATCH_MODE");
		RewriteRuleSubtreeStream stream_term=new RewriteRuleSubtreeStream(adaptor,"rule term");

		try {
			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:255:2: ( ',' term ( MATCH_MODE )? -> ^( ND_ANDREFINE term ( MATCH_MODE )? ) )
			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:255:4: ',' term ( MATCH_MODE )?
			{
			char_literal88=(Token)match(input,44,FOLLOW_44_in_andRefinement1078);  
			stream_44.add(char_literal88);

			pushFollow(FOLLOW_term_in_andRefinement1080);
			term89=term();
			state._fsp--;

			stream_term.add(term89.getTree());
			// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:255:13: ( MATCH_MODE )?
			int alt19=2;
			int LA19_0 = input.LA(1);
			if ( (LA19_0==MATCH_MODE) ) {
				alt19=1;
			}
			switch (alt19) {
				case 1 :
					// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:255:13: MATCH_MODE
					{
					MATCH_MODE90=(Token)match(input,MATCH_MODE,FOLLOW_MATCH_MODE_in_andRefinement1082);  
					stream_MATCH_MODE.add(MATCH_MODE90);

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
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (Object)adaptor.nil();
			// 255:24: -> ^( ND_ANDREFINE term ( MATCH_MODE )? )
			{
				// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:255:27: ^( ND_ANDREFINE term ( MATCH_MODE )? )
				{
				Object root_1 = (Object)adaptor.nil();
				root_1 = (Object)adaptor.becomeRoot((Object)adaptor.create(ND_ANDREFINE, "ND_ANDREFINE"), root_1);
				adaptor.addChild(root_1, startRefinement);
				adaptor.addChild(root_1, stream_term.nextTree());
				// C:\\data\\workspace201903\\catma\\grammars\\ast\\CatmaQuery.g:255:66: ( MATCH_MODE )?
				if ( stream_MATCH_MODE.hasNext() ) {
					adaptor.addChild(root_1, stream_MATCH_MODE.nextNode());
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



	public static final BitSet FOLLOW_query_in_start159 = new BitSet(new long[]{0x0000000000000000L});
	public static final BitSet FOLLOW_EOF_in_start161 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_queryExpression_in_query177 = new BitSet(new long[]{0x0002000000000002L});
	public static final BitSet FOLLOW_refinement_in_query179 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_term_in_queryExpression216 = new BitSet(new long[]{0x0000720000000002L});
	public static final BitSet FOLLOW_unionQuery_in_queryExpression223 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_collocQuery_in_queryExpression235 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_exclusionQuery_in_queryExpression248 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_adjacencyQuery_in_queryExpression261 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_44_in_unionQuery304 = new BitSet(new long[]{0x0000048FC0000050L});
	public static final BitSet FOLLOW_term_in_unionQuery306 = new BitSet(new long[]{0x0001000000000002L});
	public static final BitSet FOLLOW_48_in_unionQuery308 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_41_in_collocQuery343 = new BitSet(new long[]{0x0000048FC0000050L});
	public static final BitSet FOLLOW_term_in_collocQuery345 = new BitSet(new long[]{0x0000000000000102L});
	public static final BitSet FOLLOW_INT_in_collocQuery347 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_45_in_exclusionQuery381 = new BitSet(new long[]{0x0000048FC0000050L});
	public static final BitSet FOLLOW_term_in_exclusionQuery383 = new BitSet(new long[]{0x0000000000000802L});
	public static final BitSet FOLLOW_MATCH_MODE_in_exclusionQuery385 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_46_in_adjacencyQuery419 = new BitSet(new long[]{0x0000048FC0000050L});
	public static final BitSet FOLLOW_term_in_adjacencyQuery421 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_phrase_in_term451 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_selector_in_term461 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_42_in_term472 = new BitSet(new long[]{0x0000048FC0000050L});
	public static final BitSet FOLLOW_query_in_term473 = new BitSet(new long[]{0x0000080000000000L});
	public static final BitSet FOLLOW_43_in_term474 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_TXT_in_phrase498 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_tagQuery_in_selector531 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_tagdiffQuery_in_selector536 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_propertyQuery_in_selector541 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_regQuery_in_selector546 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_freqQuery_in_selector551 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_similQuery_in_selector556 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_wildQuery_in_selector561 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_commentQuery_in_selector566 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_TAG_in_tagQuery586 = new BitSet(new long[]{0x0000000000000020L});
	public static final BitSet FOLLOW_EQUAL_in_tagQuery588 = new BitSet(new long[]{0x0000000800000000L});
	public static final BitSet FOLLOW_phrase_in_tagQuery590 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_TAGDIFF_in_tagdiffQuery617 = new BitSet(new long[]{0x0000000000000020L});
	public static final BitSet FOLLOW_EQUAL_in_tagdiffQuery619 = new BitSet(new long[]{0x0000000800000000L});
	public static final BitSet FOLLOW_phrase_in_tagdiffQuery621 = new BitSet(new long[]{0x0000000040000002L});
	public static final BitSet FOLLOW_PROPERTY_in_tagdiffQuery624 = new BitSet(new long[]{0x0000000000000020L});
	public static final BitSet FOLLOW_EQUAL_in_tagdiffQuery626 = new BitSet(new long[]{0x0000000800000000L});
	public static final BitSet FOLLOW_phrase_in_tagdiffQuery628 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_PROPERTY_in_propertyQuery661 = new BitSet(new long[]{0x0000000000000020L});
	public static final BitSet FOLLOW_EQUAL_in_propertyQuery663 = new BitSet(new long[]{0x0000000800000000L});
	public static final BitSet FOLLOW_phrase_in_propertyQuery665 = new BitSet(new long[]{0x0000002000000002L});
	public static final BitSet FOLLOW_VALUE_in_propertyQuery668 = new BitSet(new long[]{0x0000000000000020L});
	public static final BitSet FOLLOW_EQUAL_in_propertyQuery670 = new BitSet(new long[]{0x0000000800000000L});
	public static final BitSet FOLLOW_phrase_in_propertyQuery672 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_TAG_in_propertyQuery691 = new BitSet(new long[]{0x0000000000000020L});
	public static final BitSet FOLLOW_EQUAL_in_propertyQuery693 = new BitSet(new long[]{0x0000000800000000L});
	public static final BitSet FOLLOW_phrase_in_propertyQuery695 = new BitSet(new long[]{0x0000000040000000L});
	public static final BitSet FOLLOW_PROPERTY_in_propertyQuery697 = new BitSet(new long[]{0x0000000000000020L});
	public static final BitSet FOLLOW_EQUAL_in_propertyQuery699 = new BitSet(new long[]{0x0000000800000000L});
	public static final BitSet FOLLOW_phrase_in_propertyQuery701 = new BitSet(new long[]{0x0000002000000002L});
	public static final BitSet FOLLOW_VALUE_in_propertyQuery704 = new BitSet(new long[]{0x0000000000000020L});
	public static final BitSet FOLLOW_EQUAL_in_propertyQuery706 = new BitSet(new long[]{0x0000000800000000L});
	public static final BitSet FOLLOW_phrase_in_propertyQuery708 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_REG_in_regQuery741 = new BitSet(new long[]{0x0000000000000020L});
	public static final BitSet FOLLOW_EQUAL_in_regQuery743 = new BitSet(new long[]{0x0000000800000000L});
	public static final BitSet FOLLOW_phrase_in_regQuery745 = new BitSet(new long[]{0x0000800000000002L});
	public static final BitSet FOLLOW_47_in_regQuery747 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_FREQ_in_freqQuery778 = new BitSet(new long[]{0x0000001000000020L});
	public static final BitSet FOLLOW_EQUAL_in_freqQuery785 = new BitSet(new long[]{0x0000000000000100L});
	public static final BitSet FOLLOW_INT_in_freqQuery787 = new BitSet(new long[]{0x0000200000000002L});
	public static final BitSet FOLLOW_45_in_freqQuery790 = new BitSet(new long[]{0x0000000000000100L});
	public static final BitSet FOLLOW_INT_in_freqQuery792 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_UNEQUAL_in_freqQuery814 = new BitSet(new long[]{0x0000000000000100L});
	public static final BitSet FOLLOW_INT_in_freqQuery816 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_SIMIL_in_similQuery848 = new BitSet(new long[]{0x0000000000000020L});
	public static final BitSet FOLLOW_EQUAL_in_similQuery850 = new BitSet(new long[]{0x0000000800000000L});
	public static final BitSet FOLLOW_phrase_in_similQuery852 = new BitSet(new long[]{0x0000000000000100L});
	public static final BitSet FOLLOW_INT_in_similQuery854 = new BitSet(new long[]{0x0000010000000002L});
	public static final BitSet FOLLOW_40_in_similQuery856 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_WILD_in_wildQuery886 = new BitSet(new long[]{0x0000000000000020L});
	public static final BitSet FOLLOW_EQUAL_in_wildQuery888 = new BitSet(new long[]{0x0000000800000000L});
	public static final BitSet FOLLOW_phrase_in_wildQuery890 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_COMMENT_in_commentQuery915 = new BitSet(new long[]{0x0000000000000020L});
	public static final BitSet FOLLOW_EQUAL_in_commentQuery917 = new BitSet(new long[]{0x0000000800000000L});
	public static final BitSet FOLLOW_phrase_in_commentQuery919 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_49_in_refinement949 = new BitSet(new long[]{0x0000048FC0000050L});
	public static final BitSet FOLLOW_refinementExpression_in_refinement951 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_term_in_refinementExpression975 = new BitSet(new long[]{0x0004100000000802L});
	public static final BitSet FOLLOW_MATCH_MODE_in_refinementExpression977 = new BitSet(new long[]{0x0004100000000002L});
	public static final BitSet FOLLOW_orRefinement_in_refinementExpression986 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_andRefinement_in_refinementExpression998 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_50_in_orRefinement1040 = new BitSet(new long[]{0x0000048FC0000050L});
	public static final BitSet FOLLOW_term_in_orRefinement1042 = new BitSet(new long[]{0x0000000000000802L});
	public static final BitSet FOLLOW_MATCH_MODE_in_orRefinement1044 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_44_in_andRefinement1078 = new BitSet(new long[]{0x0000048FC0000050L});
	public static final BitSet FOLLOW_term_in_andRefinement1080 = new BitSet(new long[]{0x0000000000000802L});
	public static final BitSet FOLLOW_MATCH_MODE_in_andRefinement1082 = new BitSet(new long[]{0x0000000000000002L});
}
