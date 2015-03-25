// $ANTLR 3.5.1 /home/mp/workspace/catma/grammars/ast/CatmaQuery.g 2015-03-24 18:14:58

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
		"<invalid>", "<EOR>", "<DOWN>", "<UP>", "EQUAL", "FREQ", "GROUPIDENT", 
		"INT", "LETTER", "LETTEREXTENDED", "MATCH_MODE", "ND_ADJACENCY", "ND_ANDREFINE", 
		"ND_COLLOC", "ND_EXCLUSION", "ND_FREQ", "ND_ORREFINE", "ND_PHRASE", "ND_PROPERTY", 
		"ND_QUERY", "ND_REFINE", "ND_REG", "ND_SIMIL", "ND_TAG", "ND_TAGDIFF", 
		"ND_TAGPROPERTY", "ND_UNION", "ND_WILD", "PROPERTY", "REG", "SIMIL", "TAG", 
		"TAGDIFF", "TXT", "UNEQUAL", "VALUE", "WHITESPACE", "WILD", "'%'", "'&'", 
		"'('", "')'", "','", "'-'", "';'", "'CI'", "'EXCL'", "'where'", "'|'"
	};
	public static final int EOF=-1;
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
	public static final int T__48=48;
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
	public static final int ND_TAGDIFF=24;
	public static final int ND_TAGPROPERTY=25;
	public static final int ND_UNION=26;
	public static final int ND_WILD=27;
	public static final int PROPERTY=28;
	public static final int REG=29;
	public static final int SIMIL=30;
	public static final int TAG=31;
	public static final int TAGDIFF=32;
	public static final int TXT=33;
	public static final int UNEQUAL=34;
	public static final int VALUE=35;
	public static final int WHITESPACE=36;
	public static final int WILD=37;

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
	@Override public String getGrammarFileName() { return "/home/mp/workspace/catma/grammars/ast/CatmaQuery.g"; }



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
	// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:105:1: start : query EOF ;
	public final CatmaQueryParser.start_return start() throws RecognitionException {
		CatmaQueryParser.start_return retval = new CatmaQueryParser.start_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token EOF2=null;
		ParserRuleReturnScope query1 =null;

		Object EOF2_tree=null;

		try {
			// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:105:7: ( query EOF )
			// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:105:9: query EOF
			{
			root_0 = (Object)adaptor.nil();


			pushFollow(FOLLOW_query_in_start155);
			query1=query();
			state._fsp--;

			adaptor.addChild(root_0, query1.getTree());

			EOF2=(Token)match(input,EOF,FOLLOW_EOF_in_start157); 
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
	// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:109:1: query : queryExpression ( refinement )? -> ^( ND_QUERY queryExpression ( refinement )? ) ;
	public final CatmaQueryParser.query_return query() throws RecognitionException {
		CatmaQueryParser.query_return retval = new CatmaQueryParser.query_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		ParserRuleReturnScope queryExpression3 =null;
		ParserRuleReturnScope refinement4 =null;

		RewriteRuleSubtreeStream stream_refinement=new RewriteRuleSubtreeStream(adaptor,"rule refinement");
		RewriteRuleSubtreeStream stream_queryExpression=new RewriteRuleSubtreeStream(adaptor,"rule queryExpression");

		try {
			// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:109:7: ( queryExpression ( refinement )? -> ^( ND_QUERY queryExpression ( refinement )? ) )
			// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:109:9: queryExpression ( refinement )?
			{
			pushFollow(FOLLOW_queryExpression_in_query173);
			queryExpression3=queryExpression();
			state._fsp--;

			stream_queryExpression.add(queryExpression3.getTree());
			// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:109:25: ( refinement )?
			int alt1=2;
			int LA1_0 = input.LA(1);
			if ( (LA1_0==47) ) {
				alt1=1;
			}
			switch (alt1) {
				case 1 :
					// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:109:25: refinement
					{
					pushFollow(FOLLOW_refinement_in_query175);
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
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (Object)adaptor.nil();
			// 109:37: -> ^( ND_QUERY queryExpression ( refinement )? )
			{
				// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:109:40: ^( ND_QUERY queryExpression ( refinement )? )
				{
				Object root_1 = (Object)adaptor.nil();
				root_1 = (Object)adaptor.becomeRoot((Object)adaptor.create(ND_QUERY, "ND_QUERY"), root_1);
				adaptor.addChild(root_1, stream_queryExpression.nextTree());
				// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:109:67: ( refinement )?
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
	// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:118:1: queryExpression : startTerm= term ( unionQuery[(CommonTree)$startTerm.tree] -> unionQuery | collocQuery[(CommonTree)$startTerm.tree] -> collocQuery | exclusionQuery[(CommonTree)$startTerm.tree] -> exclusionQuery | adjacencyQuery[(CommonTree)$startTerm.tree] -> adjacencyQuery | -> term ) ;
	public final CatmaQueryParser.queryExpression_return queryExpression() throws RecognitionException {
		CatmaQueryParser.queryExpression_return retval = new CatmaQueryParser.queryExpression_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		ParserRuleReturnScope startTerm =null;
		ParserRuleReturnScope unionQuery5 =null;
		ParserRuleReturnScope collocQuery6 =null;
		ParserRuleReturnScope exclusionQuery7 =null;
		ParserRuleReturnScope adjacencyQuery8 =null;

		RewriteRuleSubtreeStream stream_adjacencyQuery=new RewriteRuleSubtreeStream(adaptor,"rule adjacencyQuery");
		RewriteRuleSubtreeStream stream_unionQuery=new RewriteRuleSubtreeStream(adaptor,"rule unionQuery");
		RewriteRuleSubtreeStream stream_term=new RewriteRuleSubtreeStream(adaptor,"rule term");
		RewriteRuleSubtreeStream stream_exclusionQuery=new RewriteRuleSubtreeStream(adaptor,"rule exclusionQuery");
		RewriteRuleSubtreeStream stream_collocQuery=new RewriteRuleSubtreeStream(adaptor,"rule collocQuery");

		try {
			// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:119:2: (startTerm= term ( unionQuery[(CommonTree)$startTerm.tree] -> unionQuery | collocQuery[(CommonTree)$startTerm.tree] -> collocQuery | exclusionQuery[(CommonTree)$startTerm.tree] -> exclusionQuery | adjacencyQuery[(CommonTree)$startTerm.tree] -> adjacencyQuery | -> term ) )
			// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:119:4: startTerm= term ( unionQuery[(CommonTree)$startTerm.tree] -> unionQuery | collocQuery[(CommonTree)$startTerm.tree] -> collocQuery | exclusionQuery[(CommonTree)$startTerm.tree] -> exclusionQuery | adjacencyQuery[(CommonTree)$startTerm.tree] -> adjacencyQuery | -> term )
			{
			pushFollow(FOLLOW_term_in_queryExpression212);
			startTerm=term();
			state._fsp--;

			stream_term.add(startTerm.getTree());
			// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:119:19: ( unionQuery[(CommonTree)$startTerm.tree] -> unionQuery | collocQuery[(CommonTree)$startTerm.tree] -> collocQuery | exclusionQuery[(CommonTree)$startTerm.tree] -> exclusionQuery | adjacencyQuery[(CommonTree)$startTerm.tree] -> adjacencyQuery | -> term )
			int alt2=5;
			switch ( input.LA(1) ) {
			case 42:
				{
				alt2=1;
				}
				break;
			case 39:
				{
				alt2=2;
				}
				break;
			case 43:
				{
				alt2=3;
				}
				break;
			case 44:
				{
				alt2=4;
				}
				break;
			case EOF:
			case 41:
			case 47:
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
					// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:120:4: unionQuery[(CommonTree)$startTerm.tree]
					{
					pushFollow(FOLLOW_unionQuery_in_queryExpression219);
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
					// 120:44: -> unionQuery
					{
						adaptor.addChild(root_0, stream_unionQuery.nextTree());
					}


					retval.tree = root_0;

					}
					break;
				case 2 :
					// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:121:6: collocQuery[(CommonTree)$startTerm.tree]
					{
					pushFollow(FOLLOW_collocQuery_in_queryExpression231);
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
					// 121:48: -> collocQuery
					{
						adaptor.addChild(root_0, stream_collocQuery.nextTree());
					}


					retval.tree = root_0;

					}
					break;
				case 3 :
					// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:122:6: exclusionQuery[(CommonTree)$startTerm.tree]
					{
					pushFollow(FOLLOW_exclusionQuery_in_queryExpression244);
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
					// 122:51: -> exclusionQuery
					{
						adaptor.addChild(root_0, stream_exclusionQuery.nextTree());
					}


					retval.tree = root_0;

					}
					break;
				case 4 :
					// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:123:6: adjacencyQuery[(CommonTree)$startTerm.tree]
					{
					pushFollow(FOLLOW_adjacencyQuery_in_queryExpression257);
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
					// 123:51: -> adjacencyQuery
					{
						adaptor.addChild(root_0, stream_adjacencyQuery.nextTree());
					}


					retval.tree = root_0;

					}
					break;
				case 5 :
					// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:124:6: 
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
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "unionQuery"
	// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:134:1: unionQuery[CommonTree startTerm] : ',' term ( 'EXCL' )? -> ^( ND_UNION term ( 'EXCL' )? ) ;
	public final CatmaQueryParser.unionQuery_return unionQuery(CommonTree startTerm) throws RecognitionException {
		CatmaQueryParser.unionQuery_return retval = new CatmaQueryParser.unionQuery_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token char_literal9=null;
		Token string_literal11=null;
		ParserRuleReturnScope term10 =null;

		Object char_literal9_tree=null;
		Object string_literal11_tree=null;
		RewriteRuleTokenStream stream_42=new RewriteRuleTokenStream(adaptor,"token 42");
		RewriteRuleTokenStream stream_46=new RewriteRuleTokenStream(adaptor,"token 46");
		RewriteRuleSubtreeStream stream_term=new RewriteRuleSubtreeStream(adaptor,"rule term");

		try {
			// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:135:2: ( ',' term ( 'EXCL' )? -> ^( ND_UNION term ( 'EXCL' )? ) )
			// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:135:4: ',' term ( 'EXCL' )?
			{
			char_literal9=(Token)match(input,42,FOLLOW_42_in_unionQuery300);  
			stream_42.add(char_literal9);

			pushFollow(FOLLOW_term_in_unionQuery302);
			term10=term();
			state._fsp--;

			stream_term.add(term10.getTree());
			// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:135:13: ( 'EXCL' )?
			int alt3=2;
			int LA3_0 = input.LA(1);
			if ( (LA3_0==46) ) {
				alt3=1;
			}
			switch (alt3) {
				case 1 :
					// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:135:13: 'EXCL'
					{
					string_literal11=(Token)match(input,46,FOLLOW_46_in_unionQuery304);  
					stream_46.add(string_literal11);

					}
					break;

			}

			// AST REWRITE
			// elements: term, 46
			// token labels: 
			// rule labels: retval
			// token list labels: 
			// rule list labels: 
			// wildcard labels: 
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (Object)adaptor.nil();
			// 135:21: -> ^( ND_UNION term ( 'EXCL' )? )
			{
				// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:135:24: ^( ND_UNION term ( 'EXCL' )? )
				{
				Object root_1 = (Object)adaptor.nil();
				root_1 = (Object)adaptor.becomeRoot((Object)adaptor.create(ND_UNION, "ND_UNION"), root_1);
				adaptor.addChild(root_1, startTerm);
				adaptor.addChild(root_1, stream_term.nextTree());
				// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:135:53: ( 'EXCL' )?
				if ( stream_46.hasNext() ) {
					adaptor.addChild(root_1, stream_46.nextNode());
				}
				stream_46.reset();

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
	// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:139:1: collocQuery[CommonTree startTerm] : '&' term ( INT )? -> ^( ND_COLLOC term ( INT )? ) ;
	public final CatmaQueryParser.collocQuery_return collocQuery(CommonTree startTerm) throws RecognitionException {
		CatmaQueryParser.collocQuery_return retval = new CatmaQueryParser.collocQuery_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token char_literal12=null;
		Token INT14=null;
		ParserRuleReturnScope term13 =null;

		Object char_literal12_tree=null;
		Object INT14_tree=null;
		RewriteRuleTokenStream stream_INT=new RewriteRuleTokenStream(adaptor,"token INT");
		RewriteRuleTokenStream stream_39=new RewriteRuleTokenStream(adaptor,"token 39");
		RewriteRuleSubtreeStream stream_term=new RewriteRuleSubtreeStream(adaptor,"rule term");

		try {
			// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:140:2: ( '&' term ( INT )? -> ^( ND_COLLOC term ( INT )? ) )
			// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:140:4: '&' term ( INT )?
			{
			char_literal12=(Token)match(input,39,FOLLOW_39_in_collocQuery339);  
			stream_39.add(char_literal12);

			pushFollow(FOLLOW_term_in_collocQuery341);
			term13=term();
			state._fsp--;

			stream_term.add(term13.getTree());
			// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:140:13: ( INT )?
			int alt4=2;
			int LA4_0 = input.LA(1);
			if ( (LA4_0==INT) ) {
				alt4=1;
			}
			switch (alt4) {
				case 1 :
					// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:140:13: INT
					{
					INT14=(Token)match(input,INT,FOLLOW_INT_in_collocQuery343);  
					stream_INT.add(INT14);

					}
					break;

			}

			// AST REWRITE
			// elements: term, INT
			// token labels: 
			// rule labels: retval
			// token list labels: 
			// rule list labels: 
			// wildcard labels: 
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (Object)adaptor.nil();
			// 140:18: -> ^( ND_COLLOC term ( INT )? )
			{
				// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:140:21: ^( ND_COLLOC term ( INT )? )
				{
				Object root_1 = (Object)adaptor.nil();
				root_1 = (Object)adaptor.becomeRoot((Object)adaptor.create(ND_COLLOC, "ND_COLLOC"), root_1);
				adaptor.addChild(root_1, startTerm);
				adaptor.addChild(root_1, stream_term.nextTree());
				// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:140:51: ( INT )?
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
	// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:144:1: exclusionQuery[CommonTree startTerm] : '-' term ( MATCH_MODE )? -> ^( ND_EXCLUSION term ( MATCH_MODE )? ) ;
	public final CatmaQueryParser.exclusionQuery_return exclusionQuery(CommonTree startTerm) throws RecognitionException {
		CatmaQueryParser.exclusionQuery_return retval = new CatmaQueryParser.exclusionQuery_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token char_literal15=null;
		Token MATCH_MODE17=null;
		ParserRuleReturnScope term16 =null;

		Object char_literal15_tree=null;
		Object MATCH_MODE17_tree=null;
		RewriteRuleTokenStream stream_43=new RewriteRuleTokenStream(adaptor,"token 43");
		RewriteRuleTokenStream stream_MATCH_MODE=new RewriteRuleTokenStream(adaptor,"token MATCH_MODE");
		RewriteRuleSubtreeStream stream_term=new RewriteRuleSubtreeStream(adaptor,"rule term");

		try {
			// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:145:2: ( '-' term ( MATCH_MODE )? -> ^( ND_EXCLUSION term ( MATCH_MODE )? ) )
			// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:145:4: '-' term ( MATCH_MODE )?
			{
			char_literal15=(Token)match(input,43,FOLLOW_43_in_exclusionQuery377);  
			stream_43.add(char_literal15);

			pushFollow(FOLLOW_term_in_exclusionQuery379);
			term16=term();
			state._fsp--;

			stream_term.add(term16.getTree());
			// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:145:13: ( MATCH_MODE )?
			int alt5=2;
			int LA5_0 = input.LA(1);
			if ( (LA5_0==MATCH_MODE) ) {
				alt5=1;
			}
			switch (alt5) {
				case 1 :
					// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:145:13: MATCH_MODE
					{
					MATCH_MODE17=(Token)match(input,MATCH_MODE,FOLLOW_MATCH_MODE_in_exclusionQuery381);  
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
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (Object)adaptor.nil();
			// 145:24: -> ^( ND_EXCLUSION term ( MATCH_MODE )? )
			{
				// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:145:27: ^( ND_EXCLUSION term ( MATCH_MODE )? )
				{
				Object root_1 = (Object)adaptor.nil();
				root_1 = (Object)adaptor.becomeRoot((Object)adaptor.create(ND_EXCLUSION, "ND_EXCLUSION"), root_1);
				adaptor.addChild(root_1, startTerm);
				adaptor.addChild(root_1, stream_term.nextTree());
				// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:145:60: ( MATCH_MODE )?
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
	// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:149:1: adjacencyQuery[CommonTree startTerm] : ';' term -> ^( ND_ADJACENCY term ) ;
	public final CatmaQueryParser.adjacencyQuery_return adjacencyQuery(CommonTree startTerm) throws RecognitionException {
		CatmaQueryParser.adjacencyQuery_return retval = new CatmaQueryParser.adjacencyQuery_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token char_literal18=null;
		ParserRuleReturnScope term19 =null;

		Object char_literal18_tree=null;
		RewriteRuleTokenStream stream_44=new RewriteRuleTokenStream(adaptor,"token 44");
		RewriteRuleSubtreeStream stream_term=new RewriteRuleSubtreeStream(adaptor,"rule term");

		try {
			// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:150:2: ( ';' term -> ^( ND_ADJACENCY term ) )
			// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:150:4: ';' term
			{
			char_literal18=(Token)match(input,44,FOLLOW_44_in_adjacencyQuery415);  
			stream_44.add(char_literal18);

			pushFollow(FOLLOW_term_in_adjacencyQuery417);
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
			// 150:13: -> ^( ND_ADJACENCY term )
			{
				// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:150:16: ^( ND_ADJACENCY term )
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
	// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:155:1: term : ( phrase -> phrase | selector -> selector | '(' query ')' -> query );
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
		RewriteRuleTokenStream stream_41=new RewriteRuleTokenStream(adaptor,"token 41");
		RewriteRuleTokenStream stream_40=new RewriteRuleTokenStream(adaptor,"token 40");
		RewriteRuleSubtreeStream stream_selector=new RewriteRuleSubtreeStream(adaptor,"rule selector");
		RewriteRuleSubtreeStream stream_query=new RewriteRuleSubtreeStream(adaptor,"rule query");
		RewriteRuleSubtreeStream stream_phrase=new RewriteRuleSubtreeStream(adaptor,"rule phrase");

		try {
			// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:155:7: ( phrase -> phrase | selector -> selector | '(' query ')' -> query )
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
			case TAGDIFF:
			case WILD:
				{
				alt6=2;
				}
				break;
			case 40:
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
					// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:155:9: phrase
					{
					pushFollow(FOLLOW_phrase_in_term447);
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
					// 155:16: -> phrase
					{
						adaptor.addChild(root_0, stream_phrase.nextTree());
					}


					retval.tree = root_0;

					}
					break;
				case 2 :
					// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:156:5: selector
					{
					pushFollow(FOLLOW_selector_in_term457);
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
					// 156:14: -> selector
					{
						adaptor.addChild(root_0, stream_selector.nextTree());
					}


					retval.tree = root_0;

					}
					break;
				case 3 :
					// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:157:5: '(' query ')'
					{
					char_literal22=(Token)match(input,40,FOLLOW_40_in_term468);  
					stream_40.add(char_literal22);

					pushFollow(FOLLOW_query_in_term469);
					query23=query();
					state._fsp--;

					stream_query.add(query23.getTree());
					char_literal24=(Token)match(input,41,FOLLOW_41_in_term470);  
					stream_41.add(char_literal24);

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
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "phrase"
	// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:164:1: phrase : TXT -> ^( ND_PHRASE TXT ) ;
	public final CatmaQueryParser.phrase_return phrase() throws RecognitionException {
		CatmaQueryParser.phrase_return retval = new CatmaQueryParser.phrase_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token TXT25=null;

		Object TXT25_tree=null;
		RewriteRuleTokenStream stream_TXT=new RewriteRuleTokenStream(adaptor,"token TXT");

		try {
			// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:164:8: ( TXT -> ^( ND_PHRASE TXT ) )
			// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:164:10: TXT
			{
			TXT25=(Token)match(input,TXT,FOLLOW_TXT_in_phrase494);  
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
			// 164:14: -> ^( ND_PHRASE TXT )
			{
				// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:164:17: ^( ND_PHRASE TXT )
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
	// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:174:1: selector : ( tagQuery | tagdiffQuery | propertyQuery | regQuery | freqQuery | similQuery | wildQuery );
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


		try {
			// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:175:2: ( tagQuery | tagdiffQuery | propertyQuery | regQuery | freqQuery | similQuery | wildQuery )
			int alt7=7;
			switch ( input.LA(1) ) {
			case TAG:
				{
				int LA7_1 = input.LA(2);
				if ( (LA7_1==EQUAL) ) {
					int LA7_8 = input.LA(3);
					if ( (LA7_8==TXT) ) {
						int LA7_9 = input.LA(4);
						if ( (LA7_9==EOF||LA7_9==INT||LA7_9==MATCH_MODE||LA7_9==39||(LA7_9 >= 41 && LA7_9 <= 44)||(LA7_9 >= 46 && LA7_9 <= 48)) ) {
							alt7=1;
						}
						else if ( (LA7_9==PROPERTY) ) {
							alt7=3;
						}

						else {
							int nvaeMark = input.mark();
							try {
								for (int nvaeConsume = 0; nvaeConsume < 4 - 1; nvaeConsume++) {
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
							for (int nvaeConsume = 0; nvaeConsume < 3 - 1; nvaeConsume++) {
								input.consume();
							}
							NoViableAltException nvae =
								new NoViableAltException("", 7, 8, input);
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
			default:
				NoViableAltException nvae =
					new NoViableAltException("", 7, 0, input);
				throw nvae;
			}
			switch (alt7) {
				case 1 :
					// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:175:4: tagQuery
					{
					root_0 = (Object)adaptor.nil();


					pushFollow(FOLLOW_tagQuery_in_selector527);
					tagQuery26=tagQuery();
					state._fsp--;

					adaptor.addChild(root_0, tagQuery26.getTree());

					}
					break;
				case 2 :
					// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:176:4: tagdiffQuery
					{
					root_0 = (Object)adaptor.nil();


					pushFollow(FOLLOW_tagdiffQuery_in_selector532);
					tagdiffQuery27=tagdiffQuery();
					state._fsp--;

					adaptor.addChild(root_0, tagdiffQuery27.getTree());

					}
					break;
				case 3 :
					// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:177:4: propertyQuery
					{
					root_0 = (Object)adaptor.nil();


					pushFollow(FOLLOW_propertyQuery_in_selector537);
					propertyQuery28=propertyQuery();
					state._fsp--;

					adaptor.addChild(root_0, propertyQuery28.getTree());

					}
					break;
				case 4 :
					// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:178:4: regQuery
					{
					root_0 = (Object)adaptor.nil();


					pushFollow(FOLLOW_regQuery_in_selector542);
					regQuery29=regQuery();
					state._fsp--;

					adaptor.addChild(root_0, regQuery29.getTree());

					}
					break;
				case 5 :
					// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:179:4: freqQuery
					{
					root_0 = (Object)adaptor.nil();


					pushFollow(FOLLOW_freqQuery_in_selector547);
					freqQuery30=freqQuery();
					state._fsp--;

					adaptor.addChild(root_0, freqQuery30.getTree());

					}
					break;
				case 6 :
					// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:180:4: similQuery
					{
					root_0 = (Object)adaptor.nil();


					pushFollow(FOLLOW_similQuery_in_selector552);
					similQuery31=similQuery();
					state._fsp--;

					adaptor.addChild(root_0, similQuery31.getTree());

					}
					break;
				case 7 :
					// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:181:4: wildQuery
					{
					root_0 = (Object)adaptor.nil();


					pushFollow(FOLLOW_wildQuery_in_selector557);
					wildQuery32=wildQuery();
					state._fsp--;

					adaptor.addChild(root_0, wildQuery32.getTree());

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
	// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:186:1: tagQuery : TAG EQUAL phrase -> ^( ND_TAG phrase ) ;
	public final CatmaQueryParser.tagQuery_return tagQuery() throws RecognitionException {
		CatmaQueryParser.tagQuery_return retval = new CatmaQueryParser.tagQuery_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token TAG33=null;
		Token EQUAL34=null;
		ParserRuleReturnScope phrase35 =null;

		Object TAG33_tree=null;
		Object EQUAL34_tree=null;
		RewriteRuleTokenStream stream_EQUAL=new RewriteRuleTokenStream(adaptor,"token EQUAL");
		RewriteRuleTokenStream stream_TAG=new RewriteRuleTokenStream(adaptor,"token TAG");
		RewriteRuleSubtreeStream stream_phrase=new RewriteRuleSubtreeStream(adaptor,"rule phrase");

		try {
			// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:187:2: ( TAG EQUAL phrase -> ^( ND_TAG phrase ) )
			// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:187:4: TAG EQUAL phrase
			{
			TAG33=(Token)match(input,TAG,FOLLOW_TAG_in_tagQuery577);  
			stream_TAG.add(TAG33);

			EQUAL34=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_tagQuery579);  
			stream_EQUAL.add(EQUAL34);

			pushFollow(FOLLOW_phrase_in_tagQuery581);
			phrase35=phrase();
			state._fsp--;

			stream_phrase.add(phrase35.getTree());
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
			// 187:21: -> ^( ND_TAG phrase )
			{
				// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:187:24: ^( ND_TAG phrase )
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
	// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:191:1: tagdiffQuery : TAGDIFF EQUAL phrase ( PROPERTY EQUAL phrase )? -> ^( ND_TAGDIFF phrase ( phrase )? ) ;
	public final CatmaQueryParser.tagdiffQuery_return tagdiffQuery() throws RecognitionException {
		CatmaQueryParser.tagdiffQuery_return retval = new CatmaQueryParser.tagdiffQuery_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token TAGDIFF36=null;
		Token EQUAL37=null;
		Token PROPERTY39=null;
		Token EQUAL40=null;
		ParserRuleReturnScope phrase38 =null;
		ParserRuleReturnScope phrase41 =null;

		Object TAGDIFF36_tree=null;
		Object EQUAL37_tree=null;
		Object PROPERTY39_tree=null;
		Object EQUAL40_tree=null;
		RewriteRuleTokenStream stream_TAGDIFF=new RewriteRuleTokenStream(adaptor,"token TAGDIFF");
		RewriteRuleTokenStream stream_PROPERTY=new RewriteRuleTokenStream(adaptor,"token PROPERTY");
		RewriteRuleTokenStream stream_EQUAL=new RewriteRuleTokenStream(adaptor,"token EQUAL");
		RewriteRuleSubtreeStream stream_phrase=new RewriteRuleSubtreeStream(adaptor,"rule phrase");

		try {
			// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:192:2: ( TAGDIFF EQUAL phrase ( PROPERTY EQUAL phrase )? -> ^( ND_TAGDIFF phrase ( phrase )? ) )
			// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:192:4: TAGDIFF EQUAL phrase ( PROPERTY EQUAL phrase )?
			{
			TAGDIFF36=(Token)match(input,TAGDIFF,FOLLOW_TAGDIFF_in_tagdiffQuery608);  
			stream_TAGDIFF.add(TAGDIFF36);

			EQUAL37=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_tagdiffQuery610);  
			stream_EQUAL.add(EQUAL37);

			pushFollow(FOLLOW_phrase_in_tagdiffQuery612);
			phrase38=phrase();
			state._fsp--;

			stream_phrase.add(phrase38.getTree());
			// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:192:25: ( PROPERTY EQUAL phrase )?
			int alt8=2;
			int LA8_0 = input.LA(1);
			if ( (LA8_0==PROPERTY) ) {
				alt8=1;
			}
			switch (alt8) {
				case 1 :
					// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:192:26: PROPERTY EQUAL phrase
					{
					PROPERTY39=(Token)match(input,PROPERTY,FOLLOW_PROPERTY_in_tagdiffQuery615);  
					stream_PROPERTY.add(PROPERTY39);

					EQUAL40=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_tagdiffQuery617);  
					stream_EQUAL.add(EQUAL40);

					pushFollow(FOLLOW_phrase_in_tagdiffQuery619);
					phrase41=phrase();
					state._fsp--;

					stream_phrase.add(phrase41.getTree());
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
			// 192:50: -> ^( ND_TAGDIFF phrase ( phrase )? )
			{
				// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:192:53: ^( ND_TAGDIFF phrase ( phrase )? )
				{
				Object root_1 = (Object)adaptor.nil();
				root_1 = (Object)adaptor.becomeRoot((Object)adaptor.create(ND_TAGDIFF, "ND_TAGDIFF"), root_1);
				adaptor.addChild(root_1, stream_phrase.nextTree());
				// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:192:73: ( phrase )?
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
	// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:197:1: propertyQuery : ( PROPERTY EQUAL phrase ( VALUE EQUAL phrase )? -> ^( ND_PROPERTY phrase ( phrase )? ) | TAG EQUAL phrase PROPERTY EQUAL phrase ( VALUE EQUAL phrase )? -> ^( ND_TAGPROPERTY phrase phrase ( phrase )? ) );
	public final CatmaQueryParser.propertyQuery_return propertyQuery() throws RecognitionException {
		CatmaQueryParser.propertyQuery_return retval = new CatmaQueryParser.propertyQuery_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token PROPERTY42=null;
		Token EQUAL43=null;
		Token VALUE45=null;
		Token EQUAL46=null;
		Token TAG48=null;
		Token EQUAL49=null;
		Token PROPERTY51=null;
		Token EQUAL52=null;
		Token VALUE54=null;
		Token EQUAL55=null;
		ParserRuleReturnScope phrase44 =null;
		ParserRuleReturnScope phrase47 =null;
		ParserRuleReturnScope phrase50 =null;
		ParserRuleReturnScope phrase53 =null;
		ParserRuleReturnScope phrase56 =null;

		Object PROPERTY42_tree=null;
		Object EQUAL43_tree=null;
		Object VALUE45_tree=null;
		Object EQUAL46_tree=null;
		Object TAG48_tree=null;
		Object EQUAL49_tree=null;
		Object PROPERTY51_tree=null;
		Object EQUAL52_tree=null;
		Object VALUE54_tree=null;
		Object EQUAL55_tree=null;
		RewriteRuleTokenStream stream_VALUE=new RewriteRuleTokenStream(adaptor,"token VALUE");
		RewriteRuleTokenStream stream_PROPERTY=new RewriteRuleTokenStream(adaptor,"token PROPERTY");
		RewriteRuleTokenStream stream_EQUAL=new RewriteRuleTokenStream(adaptor,"token EQUAL");
		RewriteRuleTokenStream stream_TAG=new RewriteRuleTokenStream(adaptor,"token TAG");
		RewriteRuleSubtreeStream stream_phrase=new RewriteRuleSubtreeStream(adaptor,"rule phrase");

		try {
			// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:198:2: ( PROPERTY EQUAL phrase ( VALUE EQUAL phrase )? -> ^( ND_PROPERTY phrase ( phrase )? ) | TAG EQUAL phrase PROPERTY EQUAL phrase ( VALUE EQUAL phrase )? -> ^( ND_TAGPROPERTY phrase phrase ( phrase )? ) )
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
					// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:198:4: PROPERTY EQUAL phrase ( VALUE EQUAL phrase )?
					{
					PROPERTY42=(Token)match(input,PROPERTY,FOLLOW_PROPERTY_in_propertyQuery652);  
					stream_PROPERTY.add(PROPERTY42);

					EQUAL43=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_propertyQuery654);  
					stream_EQUAL.add(EQUAL43);

					pushFollow(FOLLOW_phrase_in_propertyQuery656);
					phrase44=phrase();
					state._fsp--;

					stream_phrase.add(phrase44.getTree());
					// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:198:26: ( VALUE EQUAL phrase )?
					int alt9=2;
					int LA9_0 = input.LA(1);
					if ( (LA9_0==VALUE) ) {
						alt9=1;
					}
					switch (alt9) {
						case 1 :
							// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:198:27: VALUE EQUAL phrase
							{
							VALUE45=(Token)match(input,VALUE,FOLLOW_VALUE_in_propertyQuery659);  
							stream_VALUE.add(VALUE45);

							EQUAL46=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_propertyQuery661);  
							stream_EQUAL.add(EQUAL46);

							pushFollow(FOLLOW_phrase_in_propertyQuery663);
							phrase47=phrase();
							state._fsp--;

							stream_phrase.add(phrase47.getTree());
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
					// 198:48: -> ^( ND_PROPERTY phrase ( phrase )? )
					{
						// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:198:51: ^( ND_PROPERTY phrase ( phrase )? )
						{
						Object root_1 = (Object)adaptor.nil();
						root_1 = (Object)adaptor.becomeRoot((Object)adaptor.create(ND_PROPERTY, "ND_PROPERTY"), root_1);
						adaptor.addChild(root_1, stream_phrase.nextTree());
						// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:198:72: ( phrase )?
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
					// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:199:4: TAG EQUAL phrase PROPERTY EQUAL phrase ( VALUE EQUAL phrase )?
					{
					TAG48=(Token)match(input,TAG,FOLLOW_TAG_in_propertyQuery682);  
					stream_TAG.add(TAG48);

					EQUAL49=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_propertyQuery684);  
					stream_EQUAL.add(EQUAL49);

					pushFollow(FOLLOW_phrase_in_propertyQuery686);
					phrase50=phrase();
					state._fsp--;

					stream_phrase.add(phrase50.getTree());
					PROPERTY51=(Token)match(input,PROPERTY,FOLLOW_PROPERTY_in_propertyQuery688);  
					stream_PROPERTY.add(PROPERTY51);

					EQUAL52=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_propertyQuery690);  
					stream_EQUAL.add(EQUAL52);

					pushFollow(FOLLOW_phrase_in_propertyQuery692);
					phrase53=phrase();
					state._fsp--;

					stream_phrase.add(phrase53.getTree());
					// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:199:43: ( VALUE EQUAL phrase )?
					int alt10=2;
					int LA10_0 = input.LA(1);
					if ( (LA10_0==VALUE) ) {
						alt10=1;
					}
					switch (alt10) {
						case 1 :
							// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:199:44: VALUE EQUAL phrase
							{
							VALUE54=(Token)match(input,VALUE,FOLLOW_VALUE_in_propertyQuery695);  
							stream_VALUE.add(VALUE54);

							EQUAL55=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_propertyQuery697);  
							stream_EQUAL.add(EQUAL55);

							pushFollow(FOLLOW_phrase_in_propertyQuery699);
							phrase56=phrase();
							state._fsp--;

							stream_phrase.add(phrase56.getTree());
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
					// 199:65: -> ^( ND_TAGPROPERTY phrase phrase ( phrase )? )
					{
						// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:199:68: ^( ND_TAGPROPERTY phrase phrase ( phrase )? )
						{
						Object root_1 = (Object)adaptor.nil();
						root_1 = (Object)adaptor.becomeRoot((Object)adaptor.create(ND_TAGPROPERTY, "ND_TAGPROPERTY"), root_1);
						adaptor.addChild(root_1, stream_phrase.nextTree());
						adaptor.addChild(root_1, stream_phrase.nextTree());
						// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:199:99: ( phrase )?
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
	// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:203:1: regQuery : REG EQUAL phrase ( 'CI' )? -> ^( ND_REG phrase ( 'CI' )? ) ;
	public final CatmaQueryParser.regQuery_return regQuery() throws RecognitionException {
		CatmaQueryParser.regQuery_return retval = new CatmaQueryParser.regQuery_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token REG57=null;
		Token EQUAL58=null;
		Token string_literal60=null;
		ParserRuleReturnScope phrase59 =null;

		Object REG57_tree=null;
		Object EQUAL58_tree=null;
		Object string_literal60_tree=null;
		RewriteRuleTokenStream stream_45=new RewriteRuleTokenStream(adaptor,"token 45");
		RewriteRuleTokenStream stream_REG=new RewriteRuleTokenStream(adaptor,"token REG");
		RewriteRuleTokenStream stream_EQUAL=new RewriteRuleTokenStream(adaptor,"token EQUAL");
		RewriteRuleSubtreeStream stream_phrase=new RewriteRuleSubtreeStream(adaptor,"rule phrase");

		try {
			// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:204:2: ( REG EQUAL phrase ( 'CI' )? -> ^( ND_REG phrase ( 'CI' )? ) )
			// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:204:4: REG EQUAL phrase ( 'CI' )?
			{
			REG57=(Token)match(input,REG,FOLLOW_REG_in_regQuery732);  
			stream_REG.add(REG57);

			EQUAL58=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_regQuery734);  
			stream_EQUAL.add(EQUAL58);

			pushFollow(FOLLOW_phrase_in_regQuery736);
			phrase59=phrase();
			state._fsp--;

			stream_phrase.add(phrase59.getTree());
			// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:204:21: ( 'CI' )?
			int alt12=2;
			int LA12_0 = input.LA(1);
			if ( (LA12_0==45) ) {
				alt12=1;
			}
			switch (alt12) {
				case 1 :
					// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:204:21: 'CI'
					{
					string_literal60=(Token)match(input,45,FOLLOW_45_in_regQuery738);  
					stream_45.add(string_literal60);

					}
					break;

			}

			// AST REWRITE
			// elements: 45, phrase
			// token labels: 
			// rule labels: retval
			// token list labels: 
			// rule list labels: 
			// wildcard labels: 
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (Object)adaptor.nil();
			// 204:27: -> ^( ND_REG phrase ( 'CI' )? )
			{
				// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:204:30: ^( ND_REG phrase ( 'CI' )? )
				{
				Object root_1 = (Object)adaptor.nil();
				root_1 = (Object)adaptor.becomeRoot((Object)adaptor.create(ND_REG, "ND_REG"), root_1);
				adaptor.addChild(root_1, stream_phrase.nextTree());
				// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:204:46: ( 'CI' )?
				if ( stream_45.hasNext() ) {
					adaptor.addChild(root_1, stream_45.nextNode());
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
	// $ANTLR end "regQuery"


	public static class freqQuery_return extends ParserRuleReturnScope {
		Object tree;
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "freqQuery"
	// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:208:1: freqQuery : FREQ ( EQUAL INT ( '-' INT )? -> ^( ND_FREQ EQUAL INT ( INT )? ) | UNEQUAL INT -> ^( ND_FREQ UNEQUAL INT ) ) ;
	public final CatmaQueryParser.freqQuery_return freqQuery() throws RecognitionException {
		CatmaQueryParser.freqQuery_return retval = new CatmaQueryParser.freqQuery_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token FREQ61=null;
		Token EQUAL62=null;
		Token INT63=null;
		Token char_literal64=null;
		Token INT65=null;
		Token UNEQUAL66=null;
		Token INT67=null;

		Object FREQ61_tree=null;
		Object EQUAL62_tree=null;
		Object INT63_tree=null;
		Object char_literal64_tree=null;
		Object INT65_tree=null;
		Object UNEQUAL66_tree=null;
		Object INT67_tree=null;
		RewriteRuleTokenStream stream_43=new RewriteRuleTokenStream(adaptor,"token 43");
		RewriteRuleTokenStream stream_FREQ=new RewriteRuleTokenStream(adaptor,"token FREQ");
		RewriteRuleTokenStream stream_INT=new RewriteRuleTokenStream(adaptor,"token INT");
		RewriteRuleTokenStream stream_EQUAL=new RewriteRuleTokenStream(adaptor,"token EQUAL");
		RewriteRuleTokenStream stream_UNEQUAL=new RewriteRuleTokenStream(adaptor,"token UNEQUAL");

		try {
			// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:209:2: ( FREQ ( EQUAL INT ( '-' INT )? -> ^( ND_FREQ EQUAL INT ( INT )? ) | UNEQUAL INT -> ^( ND_FREQ UNEQUAL INT ) ) )
			// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:209:4: FREQ ( EQUAL INT ( '-' INT )? -> ^( ND_FREQ EQUAL INT ( INT )? ) | UNEQUAL INT -> ^( ND_FREQ UNEQUAL INT ) )
			{
			FREQ61=(Token)match(input,FREQ,FOLLOW_FREQ_in_freqQuery769);  
			stream_FREQ.add(FREQ61);

			// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:210:3: ( EQUAL INT ( '-' INT )? -> ^( ND_FREQ EQUAL INT ( INT )? ) | UNEQUAL INT -> ^( ND_FREQ UNEQUAL INT ) )
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
					// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:210:5: EQUAL INT ( '-' INT )?
					{
					EQUAL62=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_freqQuery776);  
					stream_EQUAL.add(EQUAL62);

					INT63=(Token)match(input,INT,FOLLOW_INT_in_freqQuery778);  
					stream_INT.add(INT63);

					// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:210:15: ( '-' INT )?
					int alt13=2;
					int LA13_0 = input.LA(1);
					if ( (LA13_0==43) ) {
						int LA13_1 = input.LA(2);
						if ( (LA13_1==INT) ) {
							alt13=1;
						}
					}
					switch (alt13) {
						case 1 :
							// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:210:16: '-' INT
							{
							char_literal64=(Token)match(input,43,FOLLOW_43_in_freqQuery781);  
							stream_43.add(char_literal64);

							INT65=(Token)match(input,INT,FOLLOW_INT_in_freqQuery783);  
							stream_INT.add(INT65);

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
					// 210:26: -> ^( ND_FREQ EQUAL INT ( INT )? )
					{
						// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:210:29: ^( ND_FREQ EQUAL INT ( INT )? )
						{
						Object root_1 = (Object)adaptor.nil();
						root_1 = (Object)adaptor.becomeRoot((Object)adaptor.create(ND_FREQ, "ND_FREQ"), root_1);
						adaptor.addChild(root_1, stream_EQUAL.nextNode());
						adaptor.addChild(root_1, stream_INT.nextNode());
						// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:210:49: ( INT )?
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
					// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:211:5: UNEQUAL INT
					{
					UNEQUAL66=(Token)match(input,UNEQUAL,FOLLOW_UNEQUAL_in_freqQuery805);  
					stream_UNEQUAL.add(UNEQUAL66);

					INT67=(Token)match(input,INT,FOLLOW_INT_in_freqQuery807);  
					stream_INT.add(INT67);

					// AST REWRITE
					// elements: INT, UNEQUAL
					// token labels: 
					// rule labels: retval
					// token list labels: 
					// rule list labels: 
					// wildcard labels: 
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (Object)adaptor.nil();
					// 211:17: -> ^( ND_FREQ UNEQUAL INT )
					{
						// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:211:20: ^( ND_FREQ UNEQUAL INT )
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
	// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:215:1: similQuery : SIMIL EQUAL phrase INT ( '%' )? -> ^( ND_SIMIL phrase INT ) ;
	public final CatmaQueryParser.similQuery_return similQuery() throws RecognitionException {
		CatmaQueryParser.similQuery_return retval = new CatmaQueryParser.similQuery_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token SIMIL68=null;
		Token EQUAL69=null;
		Token INT71=null;
		Token char_literal72=null;
		ParserRuleReturnScope phrase70 =null;

		Object SIMIL68_tree=null;
		Object EQUAL69_tree=null;
		Object INT71_tree=null;
		Object char_literal72_tree=null;
		RewriteRuleTokenStream stream_INT=new RewriteRuleTokenStream(adaptor,"token INT");
		RewriteRuleTokenStream stream_EQUAL=new RewriteRuleTokenStream(adaptor,"token EQUAL");
		RewriteRuleTokenStream stream_SIMIL=new RewriteRuleTokenStream(adaptor,"token SIMIL");
		RewriteRuleTokenStream stream_38=new RewriteRuleTokenStream(adaptor,"token 38");
		RewriteRuleSubtreeStream stream_phrase=new RewriteRuleSubtreeStream(adaptor,"rule phrase");

		try {
			// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:216:2: ( SIMIL EQUAL phrase INT ( '%' )? -> ^( ND_SIMIL phrase INT ) )
			// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:216:4: SIMIL EQUAL phrase INT ( '%' )?
			{
			SIMIL68=(Token)match(input,SIMIL,FOLLOW_SIMIL_in_similQuery839);  
			stream_SIMIL.add(SIMIL68);

			EQUAL69=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_similQuery841);  
			stream_EQUAL.add(EQUAL69);

			pushFollow(FOLLOW_phrase_in_similQuery843);
			phrase70=phrase();
			state._fsp--;

			stream_phrase.add(phrase70.getTree());
			INT71=(Token)match(input,INT,FOLLOW_INT_in_similQuery845);  
			stream_INT.add(INT71);

			// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:216:27: ( '%' )?
			int alt15=2;
			int LA15_0 = input.LA(1);
			if ( (LA15_0==38) ) {
				alt15=1;
			}
			switch (alt15) {
				case 1 :
					// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:216:27: '%'
					{
					char_literal72=(Token)match(input,38,FOLLOW_38_in_similQuery847);  
					stream_38.add(char_literal72);

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
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (Object)adaptor.nil();
			// 216:32: -> ^( ND_SIMIL phrase INT )
			{
				// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:216:35: ^( ND_SIMIL phrase INT )
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
	// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:220:1: wildQuery : WILD EQUAL phrase -> ^( ND_WILD phrase ) ;
	public final CatmaQueryParser.wildQuery_return wildQuery() throws RecognitionException {
		CatmaQueryParser.wildQuery_return retval = new CatmaQueryParser.wildQuery_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token WILD73=null;
		Token EQUAL74=null;
		ParserRuleReturnScope phrase75 =null;

		Object WILD73_tree=null;
		Object EQUAL74_tree=null;
		RewriteRuleTokenStream stream_WILD=new RewriteRuleTokenStream(adaptor,"token WILD");
		RewriteRuleTokenStream stream_EQUAL=new RewriteRuleTokenStream(adaptor,"token EQUAL");
		RewriteRuleSubtreeStream stream_phrase=new RewriteRuleSubtreeStream(adaptor,"rule phrase");

		try {
			// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:221:2: ( WILD EQUAL phrase -> ^( ND_WILD phrase ) )
			// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:221:4: WILD EQUAL phrase
			{
			WILD73=(Token)match(input,WILD,FOLLOW_WILD_in_wildQuery877);  
			stream_WILD.add(WILD73);

			EQUAL74=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_wildQuery879);  
			stream_EQUAL.add(EQUAL74);

			pushFollow(FOLLOW_phrase_in_wildQuery881);
			phrase75=phrase();
			state._fsp--;

			stream_phrase.add(phrase75.getTree());
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
			// 221:22: -> ^( ND_WILD phrase )
			{
				// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:221:25: ^( ND_WILD phrase )
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


	public static class refinement_return extends ParserRuleReturnScope {
		Object tree;
		@Override
		public Object getTree() { return tree; }
	};


	// $ANTLR start "refinement"
	// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:231:1: refinement : 'where' refinementExpression -> refinementExpression ;
	public final CatmaQueryParser.refinement_return refinement() throws RecognitionException {
		CatmaQueryParser.refinement_return retval = new CatmaQueryParser.refinement_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token string_literal76=null;
		ParserRuleReturnScope refinementExpression77 =null;

		Object string_literal76_tree=null;
		RewriteRuleTokenStream stream_47=new RewriteRuleTokenStream(adaptor,"token 47");
		RewriteRuleSubtreeStream stream_refinementExpression=new RewriteRuleSubtreeStream(adaptor,"rule refinementExpression");

		try {
			// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:232:2: ( 'where' refinementExpression -> refinementExpression )
			// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:232:4: 'where' refinementExpression
			{
			string_literal76=(Token)match(input,47,FOLLOW_47_in_refinement914);  
			stream_47.add(string_literal76);

			pushFollow(FOLLOW_refinementExpression_in_refinement916);
			refinementExpression77=refinementExpression();
			state._fsp--;

			stream_refinementExpression.add(refinementExpression77.getTree());
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
			// 232:33: -> refinementExpression
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
	// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:236:1: refinementExpression : startRefinement= term ( MATCH_MODE )? ( orRefinement[(CommonTree)$startRefinement.tree] -> orRefinement | andRefinement[(CommonTree)$startRefinement.tree] -> andRefinement | -> ^( ND_REFINE term ( MATCH_MODE )? ) ) ;
	public final CatmaQueryParser.refinementExpression_return refinementExpression() throws RecognitionException {
		CatmaQueryParser.refinementExpression_return retval = new CatmaQueryParser.refinementExpression_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token MATCH_MODE78=null;
		ParserRuleReturnScope startRefinement =null;
		ParserRuleReturnScope orRefinement79 =null;
		ParserRuleReturnScope andRefinement80 =null;

		Object MATCH_MODE78_tree=null;
		RewriteRuleTokenStream stream_MATCH_MODE=new RewriteRuleTokenStream(adaptor,"token MATCH_MODE");
		RewriteRuleSubtreeStream stream_orRefinement=new RewriteRuleSubtreeStream(adaptor,"rule orRefinement");
		RewriteRuleSubtreeStream stream_andRefinement=new RewriteRuleSubtreeStream(adaptor,"rule andRefinement");
		RewriteRuleSubtreeStream stream_term=new RewriteRuleSubtreeStream(adaptor,"rule term");

		try {
			// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:237:2: (startRefinement= term ( MATCH_MODE )? ( orRefinement[(CommonTree)$startRefinement.tree] -> orRefinement | andRefinement[(CommonTree)$startRefinement.tree] -> andRefinement | -> ^( ND_REFINE term ( MATCH_MODE )? ) ) )
			// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:237:4: startRefinement= term ( MATCH_MODE )? ( orRefinement[(CommonTree)$startRefinement.tree] -> orRefinement | andRefinement[(CommonTree)$startRefinement.tree] -> andRefinement | -> ^( ND_REFINE term ( MATCH_MODE )? ) )
			{
			pushFollow(FOLLOW_term_in_refinementExpression940);
			startRefinement=term();
			state._fsp--;

			stream_term.add(startRefinement.getTree());
			// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:237:25: ( MATCH_MODE )?
			int alt16=2;
			int LA16_0 = input.LA(1);
			if ( (LA16_0==MATCH_MODE) ) {
				alt16=1;
			}
			switch (alt16) {
				case 1 :
					// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:237:25: MATCH_MODE
					{
					MATCH_MODE78=(Token)match(input,MATCH_MODE,FOLLOW_MATCH_MODE_in_refinementExpression942);  
					stream_MATCH_MODE.add(MATCH_MODE78);

					}
					break;

			}

			// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:237:37: ( orRefinement[(CommonTree)$startRefinement.tree] -> orRefinement | andRefinement[(CommonTree)$startRefinement.tree] -> andRefinement | -> ^( ND_REFINE term ( MATCH_MODE )? ) )
			int alt17=3;
			switch ( input.LA(1) ) {
			case 48:
				{
				alt17=1;
				}
				break;
			case 42:
				{
				alt17=2;
				}
				break;
			case EOF:
			case 41:
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
					// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:238:4: orRefinement[(CommonTree)$startRefinement.tree]
					{
					pushFollow(FOLLOW_orRefinement_in_refinementExpression951);
					orRefinement79=orRefinement((CommonTree)(startRefinement!=null?((Object)startRefinement.getTree()):null));
					state._fsp--;

					stream_orRefinement.add(orRefinement79.getTree());
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
					// 238:52: -> orRefinement
					{
						adaptor.addChild(root_0, stream_orRefinement.nextTree());
					}


					retval.tree = root_0;

					}
					break;
				case 2 :
					// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:239:6: andRefinement[(CommonTree)$startRefinement.tree]
					{
					pushFollow(FOLLOW_andRefinement_in_refinementExpression963);
					andRefinement80=andRefinement((CommonTree)(startRefinement!=null?((Object)startRefinement.getTree()):null));
					state._fsp--;

					stream_andRefinement.add(andRefinement80.getTree());
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
					// 239:55: -> andRefinement
					{
						adaptor.addChild(root_0, stream_andRefinement.nextTree());
					}


					retval.tree = root_0;

					}
					break;
				case 3 :
					// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:240:6: 
					{
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
					// 240:6: -> ^( ND_REFINE term ( MATCH_MODE )? )
					{
						// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:240:9: ^( ND_REFINE term ( MATCH_MODE )? )
						{
						Object root_1 = (Object)adaptor.nil();
						root_1 = (Object)adaptor.becomeRoot((Object)adaptor.create(ND_REFINE, "ND_REFINE"), root_1);
						adaptor.addChild(root_1, stream_term.nextTree());
						// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:240:26: ( MATCH_MODE )?
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
	// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:244:1: orRefinement[CommonTree startRefinement] : '|' term ( MATCH_MODE )? -> ^( ND_ORREFINE term ( MATCH_MODE )? ) ;
	public final CatmaQueryParser.orRefinement_return orRefinement(CommonTree startRefinement) throws RecognitionException {
		CatmaQueryParser.orRefinement_return retval = new CatmaQueryParser.orRefinement_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token char_literal81=null;
		Token MATCH_MODE83=null;
		ParserRuleReturnScope term82 =null;

		Object char_literal81_tree=null;
		Object MATCH_MODE83_tree=null;
		RewriteRuleTokenStream stream_48=new RewriteRuleTokenStream(adaptor,"token 48");
		RewriteRuleTokenStream stream_MATCH_MODE=new RewriteRuleTokenStream(adaptor,"token MATCH_MODE");
		RewriteRuleSubtreeStream stream_term=new RewriteRuleSubtreeStream(adaptor,"rule term");

		try {
			// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:245:2: ( '|' term ( MATCH_MODE )? -> ^( ND_ORREFINE term ( MATCH_MODE )? ) )
			// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:245:4: '|' term ( MATCH_MODE )?
			{
			char_literal81=(Token)match(input,48,FOLLOW_48_in_orRefinement1005);  
			stream_48.add(char_literal81);

			pushFollow(FOLLOW_term_in_orRefinement1007);
			term82=term();
			state._fsp--;

			stream_term.add(term82.getTree());
			// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:245:13: ( MATCH_MODE )?
			int alt18=2;
			int LA18_0 = input.LA(1);
			if ( (LA18_0==MATCH_MODE) ) {
				alt18=1;
			}
			switch (alt18) {
				case 1 :
					// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:245:13: MATCH_MODE
					{
					MATCH_MODE83=(Token)match(input,MATCH_MODE,FOLLOW_MATCH_MODE_in_orRefinement1009);  
					stream_MATCH_MODE.add(MATCH_MODE83);

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
			// 245:25: -> ^( ND_ORREFINE term ( MATCH_MODE )? )
			{
				// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:245:28: ^( ND_ORREFINE term ( MATCH_MODE )? )
				{
				Object root_1 = (Object)adaptor.nil();
				root_1 = (Object)adaptor.becomeRoot((Object)adaptor.create(ND_ORREFINE, "ND_ORREFINE"), root_1);
				adaptor.addChild(root_1, startRefinement);
				adaptor.addChild(root_1, stream_term.nextTree());
				// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:245:66: ( MATCH_MODE )?
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
	// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:249:1: andRefinement[CommonTree startRefinement] : ',' term ( MATCH_MODE )? -> ^( ND_ANDREFINE term ( MATCH_MODE )? ) ;
	public final CatmaQueryParser.andRefinement_return andRefinement(CommonTree startRefinement) throws RecognitionException {
		CatmaQueryParser.andRefinement_return retval = new CatmaQueryParser.andRefinement_return();
		retval.start = input.LT(1);

		Object root_0 = null;

		Token char_literal84=null;
		Token MATCH_MODE86=null;
		ParserRuleReturnScope term85 =null;

		Object char_literal84_tree=null;
		Object MATCH_MODE86_tree=null;
		RewriteRuleTokenStream stream_42=new RewriteRuleTokenStream(adaptor,"token 42");
		RewriteRuleTokenStream stream_MATCH_MODE=new RewriteRuleTokenStream(adaptor,"token MATCH_MODE");
		RewriteRuleSubtreeStream stream_term=new RewriteRuleSubtreeStream(adaptor,"rule term");

		try {
			// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:250:2: ( ',' term ( MATCH_MODE )? -> ^( ND_ANDREFINE term ( MATCH_MODE )? ) )
			// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:250:4: ',' term ( MATCH_MODE )?
			{
			char_literal84=(Token)match(input,42,FOLLOW_42_in_andRefinement1043);  
			stream_42.add(char_literal84);

			pushFollow(FOLLOW_term_in_andRefinement1045);
			term85=term();
			state._fsp--;

			stream_term.add(term85.getTree());
			// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:250:13: ( MATCH_MODE )?
			int alt19=2;
			int LA19_0 = input.LA(1);
			if ( (LA19_0==MATCH_MODE) ) {
				alt19=1;
			}
			switch (alt19) {
				case 1 :
					// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:250:13: MATCH_MODE
					{
					MATCH_MODE86=(Token)match(input,MATCH_MODE,FOLLOW_MATCH_MODE_in_andRefinement1047);  
					stream_MATCH_MODE.add(MATCH_MODE86);

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
			// 250:24: -> ^( ND_ANDREFINE term ( MATCH_MODE )? )
			{
				// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:250:27: ^( ND_ANDREFINE term ( MATCH_MODE )? )
				{
				Object root_1 = (Object)adaptor.nil();
				root_1 = (Object)adaptor.becomeRoot((Object)adaptor.create(ND_ANDREFINE, "ND_ANDREFINE"), root_1);
				adaptor.addChild(root_1, startRefinement);
				adaptor.addChild(root_1, stream_term.nextTree());
				// /home/mp/workspace/catma/grammars/ast/CatmaQuery.g:250:66: ( MATCH_MODE )?
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



	public static final BitSet FOLLOW_query_in_start155 = new BitSet(new long[]{0x0000000000000000L});
	public static final BitSet FOLLOW_EOF_in_start157 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_queryExpression_in_query173 = new BitSet(new long[]{0x0000800000000002L});
	public static final BitSet FOLLOW_refinement_in_query175 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_term_in_queryExpression212 = new BitSet(new long[]{0x00001C8000000002L});
	public static final BitSet FOLLOW_unionQuery_in_queryExpression219 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_collocQuery_in_queryExpression231 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_exclusionQuery_in_queryExpression244 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_adjacencyQuery_in_queryExpression257 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_42_in_unionQuery300 = new BitSet(new long[]{0x00000123F0000020L});
	public static final BitSet FOLLOW_term_in_unionQuery302 = new BitSet(new long[]{0x0000400000000002L});
	public static final BitSet FOLLOW_46_in_unionQuery304 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_39_in_collocQuery339 = new BitSet(new long[]{0x00000123F0000020L});
	public static final BitSet FOLLOW_term_in_collocQuery341 = new BitSet(new long[]{0x0000000000000082L});
	public static final BitSet FOLLOW_INT_in_collocQuery343 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_43_in_exclusionQuery377 = new BitSet(new long[]{0x00000123F0000020L});
	public static final BitSet FOLLOW_term_in_exclusionQuery379 = new BitSet(new long[]{0x0000000000000402L});
	public static final BitSet FOLLOW_MATCH_MODE_in_exclusionQuery381 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_44_in_adjacencyQuery415 = new BitSet(new long[]{0x00000123F0000020L});
	public static final BitSet FOLLOW_term_in_adjacencyQuery417 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_phrase_in_term447 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_selector_in_term457 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_40_in_term468 = new BitSet(new long[]{0x00000123F0000020L});
	public static final BitSet FOLLOW_query_in_term469 = new BitSet(new long[]{0x0000020000000000L});
	public static final BitSet FOLLOW_41_in_term470 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_TXT_in_phrase494 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_tagQuery_in_selector527 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_tagdiffQuery_in_selector532 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_propertyQuery_in_selector537 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_regQuery_in_selector542 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_freqQuery_in_selector547 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_similQuery_in_selector552 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_wildQuery_in_selector557 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_TAG_in_tagQuery577 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_EQUAL_in_tagQuery579 = new BitSet(new long[]{0x0000000200000000L});
	public static final BitSet FOLLOW_phrase_in_tagQuery581 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_TAGDIFF_in_tagdiffQuery608 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_EQUAL_in_tagdiffQuery610 = new BitSet(new long[]{0x0000000200000000L});
	public static final BitSet FOLLOW_phrase_in_tagdiffQuery612 = new BitSet(new long[]{0x0000000010000002L});
	public static final BitSet FOLLOW_PROPERTY_in_tagdiffQuery615 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_EQUAL_in_tagdiffQuery617 = new BitSet(new long[]{0x0000000200000000L});
	public static final BitSet FOLLOW_phrase_in_tagdiffQuery619 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_PROPERTY_in_propertyQuery652 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_EQUAL_in_propertyQuery654 = new BitSet(new long[]{0x0000000200000000L});
	public static final BitSet FOLLOW_phrase_in_propertyQuery656 = new BitSet(new long[]{0x0000000800000002L});
	public static final BitSet FOLLOW_VALUE_in_propertyQuery659 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_EQUAL_in_propertyQuery661 = new BitSet(new long[]{0x0000000200000000L});
	public static final BitSet FOLLOW_phrase_in_propertyQuery663 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_TAG_in_propertyQuery682 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_EQUAL_in_propertyQuery684 = new BitSet(new long[]{0x0000000200000000L});
	public static final BitSet FOLLOW_phrase_in_propertyQuery686 = new BitSet(new long[]{0x0000000010000000L});
	public static final BitSet FOLLOW_PROPERTY_in_propertyQuery688 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_EQUAL_in_propertyQuery690 = new BitSet(new long[]{0x0000000200000000L});
	public static final BitSet FOLLOW_phrase_in_propertyQuery692 = new BitSet(new long[]{0x0000000800000002L});
	public static final BitSet FOLLOW_VALUE_in_propertyQuery695 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_EQUAL_in_propertyQuery697 = new BitSet(new long[]{0x0000000200000000L});
	public static final BitSet FOLLOW_phrase_in_propertyQuery699 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_REG_in_regQuery732 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_EQUAL_in_regQuery734 = new BitSet(new long[]{0x0000000200000000L});
	public static final BitSet FOLLOW_phrase_in_regQuery736 = new BitSet(new long[]{0x0000200000000002L});
	public static final BitSet FOLLOW_45_in_regQuery738 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_FREQ_in_freqQuery769 = new BitSet(new long[]{0x0000000400000010L});
	public static final BitSet FOLLOW_EQUAL_in_freqQuery776 = new BitSet(new long[]{0x0000000000000080L});
	public static final BitSet FOLLOW_INT_in_freqQuery778 = new BitSet(new long[]{0x0000080000000002L});
	public static final BitSet FOLLOW_43_in_freqQuery781 = new BitSet(new long[]{0x0000000000000080L});
	public static final BitSet FOLLOW_INT_in_freqQuery783 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_UNEQUAL_in_freqQuery805 = new BitSet(new long[]{0x0000000000000080L});
	public static final BitSet FOLLOW_INT_in_freqQuery807 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_SIMIL_in_similQuery839 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_EQUAL_in_similQuery841 = new BitSet(new long[]{0x0000000200000000L});
	public static final BitSet FOLLOW_phrase_in_similQuery843 = new BitSet(new long[]{0x0000000000000080L});
	public static final BitSet FOLLOW_INT_in_similQuery845 = new BitSet(new long[]{0x0000004000000002L});
	public static final BitSet FOLLOW_38_in_similQuery847 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_WILD_in_wildQuery877 = new BitSet(new long[]{0x0000000000000010L});
	public static final BitSet FOLLOW_EQUAL_in_wildQuery879 = new BitSet(new long[]{0x0000000200000000L});
	public static final BitSet FOLLOW_phrase_in_wildQuery881 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_47_in_refinement914 = new BitSet(new long[]{0x00000123F0000020L});
	public static final BitSet FOLLOW_refinementExpression_in_refinement916 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_term_in_refinementExpression940 = new BitSet(new long[]{0x0001040000000402L});
	public static final BitSet FOLLOW_MATCH_MODE_in_refinementExpression942 = new BitSet(new long[]{0x0001040000000002L});
	public static final BitSet FOLLOW_orRefinement_in_refinementExpression951 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_andRefinement_in_refinementExpression963 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_48_in_orRefinement1005 = new BitSet(new long[]{0x00000123F0000020L});
	public static final BitSet FOLLOW_term_in_orRefinement1007 = new BitSet(new long[]{0x0000000000000402L});
	public static final BitSet FOLLOW_MATCH_MODE_in_orRefinement1009 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_42_in_andRefinement1043 = new BitSet(new long[]{0x00000123F0000020L});
	public static final BitSet FOLLOW_term_in_andRefinement1045 = new BitSet(new long[]{0x0000000000000402L});
	public static final BitSet FOLLOW_MATCH_MODE_in_andRefinement1047 = new BitSet(new long[]{0x0000000000000002L});
}
