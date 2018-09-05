// $ANTLR 3.4 C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g 2011-12-09 14:11:23

package de.catma.serialization.tei.pointer.parser;


import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked"})
public class URIFragmentIdentifierPlainTextLexer extends Lexer {
    public static final int EOF=-1;
    public static final int T__12=12;
    public static final int T__13=13;
    public static final int T__14=14;
    public static final int T__15=15;
    public static final int T__16=16;
    public static final int ALPHA=4;
    public static final int CHAR_S=5;
    public static final int DIGIT=6;
    public static final int HEXDIGIT=7;
    public static final int INT=8;
    public static final int LINE_S=9;
    public static final int MD5VALUE=10;
    public static final int MIMECHARS=11;


    /**
    * overrides the default error handling. enables immediate failure
    */
    public void reportError(RecognitionException e) {
    	throw new RuntimeException(e);
    }



    // delegates
    // delegators
    public Lexer[] getDelegates() {
        return new Lexer[] {};
    }

    public URIFragmentIdentifierPlainTextLexer() {} 
    public URIFragmentIdentifierPlainTextLexer(CharStream input) {
        this(input, new RecognizerSharedState());
    }
    public URIFragmentIdentifierPlainTextLexer(CharStream input, RecognizerSharedState state) {
        super(input,state);
    }
    public String getGrammarFileName() { return "C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g"; }

    // $ANTLR start "T__12"
    public final void mT__12() throws RecognitionException {
        try {
            int _type = T__12;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:16:7: ( ',' )
            // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:16:9: ','
            {
            match(','); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "T__12"

    // $ANTLR start "T__13"
    public final void mT__13() throws RecognitionException {
        try {
            int _type = T__13;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:17:7: ( ';' )
            // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:17:9: ';'
            {
            match(';'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "T__13"

    // $ANTLR start "T__14"
    public final void mT__14() throws RecognitionException {
        try {
            int _type = T__14;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:18:7: ( '=' )
            // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:18:9: '='
            {
            match('='); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "T__14"

    // $ANTLR start "T__15"
    public final void mT__15() throws RecognitionException {
        try {
            int _type = T__15;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:19:7: ( 'length=' )
            // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:19:9: 'length='
            {
            match("length="); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "T__15"

    // $ANTLR start "T__16"
    public final void mT__16() throws RecognitionException {
        try {
            int _type = T__16;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:20:7: ( 'md5=' )
            // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:20:9: 'md5='
            {
            match("md5="); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "T__16"

    // $ANTLR start "INT"
    public final void mINT() throws RecognitionException {
        try {
            int _type = INT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:206:5: ( ( DIGIT )+ )
            // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:206:7: ( DIGIT )+
            {
            // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:206:7: ( DIGIT )+
            int cnt1=0;
            loop1:
            do {
                int alt1=2;
                int LA1_0 = input.LA(1);

                if ( ((LA1_0 >= '0' && LA1_0 <= '9')) ) {
                    alt1=1;
                }


                switch (alt1) {
            	case 1 :
            	    // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:
            	    {
            	    if ( (input.LA(1) >= '0' && input.LA(1) <= '9') ) {
            	        input.consume();
            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;
            	    }


            	    }
            	    break;

            	default :
            	    if ( cnt1 >= 1 ) break loop1;
                        EarlyExitException eee =
                            new EarlyExitException(1, input);
                        throw eee;
                }
                cnt1++;
            } while (true);


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "INT"

    // $ANTLR start "MIMECHARS"
    public final void mMIMECHARS() throws RecognitionException {
        try {
            int _type = MIMECHARS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:210:2: ( ( ALPHA | INT | '!' | '#' | '$' | '%' | '&' | '\\'' | '+' | '-' | '^' | '_' | '`' | '{' | '}' | '~' )+ )
            // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:210:4: ( ALPHA | INT | '!' | '#' | '$' | '%' | '&' | '\\'' | '+' | '-' | '^' | '_' | '`' | '{' | '}' | '~' )+
            {
            // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:210:4: ( ALPHA | INT | '!' | '#' | '$' | '%' | '&' | '\\'' | '+' | '-' | '^' | '_' | '`' | '{' | '}' | '~' )+
            int cnt2=0;
            loop2:
            do {
                int alt2=17;
                switch ( input.LA(1) ) {
                case 'A':
                case 'B':
                case 'C':
                case 'D':
                case 'E':
                case 'F':
                case 'G':
                case 'H':
                case 'I':
                case 'J':
                case 'K':
                case 'L':
                case 'M':
                case 'N':
                case 'O':
                case 'P':
                case 'Q':
                case 'R':
                case 'S':
                case 'T':
                case 'U':
                case 'V':
                case 'W':
                case 'X':
                case 'Y':
                case 'Z':
                    {
                    alt2=1;
                    }
                    break;
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    {
                    alt2=2;
                    }
                    break;
                case '!':
                    {
                    alt2=3;
                    }
                    break;
                case '#':
                    {
                    alt2=4;
                    }
                    break;
                case '$':
                    {
                    alt2=5;
                    }
                    break;
                case '%':
                    {
                    alt2=6;
                    }
                    break;
                case '&':
                    {
                    alt2=7;
                    }
                    break;
                case '\'':
                    {
                    alt2=8;
                    }
                    break;
                case '+':
                    {
                    alt2=9;
                    }
                    break;
                case '-':
                    {
                    alt2=10;
                    }
                    break;
                case '^':
                    {
                    alt2=11;
                    }
                    break;
                case '_':
                    {
                    alt2=12;
                    }
                    break;
                case '`':
                    {
                    alt2=13;
                    }
                    break;
                case '{':
                    {
                    alt2=14;
                    }
                    break;
                case '}':
                    {
                    alt2=15;
                    }
                    break;
                case '~':
                    {
                    alt2=16;
                    }
                    break;

                }

                switch (alt2) {
            	case 1 :
            	    // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:210:5: ALPHA
            	    {
            	    mALPHA(); 


            	    }
            	    break;
            	case 2 :
            	    // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:210:13: INT
            	    {
            	    mINT(); 


            	    }
            	    break;
            	case 3 :
            	    // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:210:19: '!'
            	    {
            	    match('!'); 

            	    }
            	    break;
            	case 4 :
            	    // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:210:25: '#'
            	    {
            	    match('#'); 

            	    }
            	    break;
            	case 5 :
            	    // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:210:31: '$'
            	    {
            	    match('$'); 

            	    }
            	    break;
            	case 6 :
            	    // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:210:37: '%'
            	    {
            	    match('%'); 

            	    }
            	    break;
            	case 7 :
            	    // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:210:43: '&'
            	    {
            	    match('&'); 

            	    }
            	    break;
            	case 8 :
            	    // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:210:49: '\\''
            	    {
            	    match('\''); 

            	    }
            	    break;
            	case 9 :
            	    // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:210:56: '+'
            	    {
            	    match('+'); 

            	    }
            	    break;
            	case 10 :
            	    // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:210:62: '-'
            	    {
            	    match('-'); 

            	    }
            	    break;
            	case 11 :
            	    // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:210:68: '^'
            	    {
            	    match('^'); 

            	    }
            	    break;
            	case 12 :
            	    // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:210:74: '_'
            	    {
            	    match('_'); 

            	    }
            	    break;
            	case 13 :
            	    // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:210:80: '`'
            	    {
            	    match('`'); 

            	    }
            	    break;
            	case 14 :
            	    // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:210:86: '{'
            	    {
            	    match('{'); 

            	    }
            	    break;
            	case 15 :
            	    // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:210:92: '}'
            	    {
            	    match('}'); 

            	    }
            	    break;
            	case 16 :
            	    // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:210:98: '~'
            	    {
            	    match('~'); 

            	    }
            	    break;

            	default :
            	    if ( cnt2 >= 1 ) break loop2;
                        EarlyExitException eee =
                            new EarlyExitException(2, input);
                        throw eee;
                }
                cnt2++;
            } while (true);


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "MIMECHARS"

    // $ANTLR start "MD5VALUE"
    public final void mMD5VALUE() throws RecognitionException {
        try {
            int _type = MD5VALUE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:214:2: ( ( HEXDIGIT )+ )
            // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:214:4: ( HEXDIGIT )+
            {
            // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:214:4: ( HEXDIGIT )+
            int cnt3=0;
            loop3:
            do {
                int alt3=2;
                int LA3_0 = input.LA(1);

                if ( ((LA3_0 >= '0' && LA3_0 <= '9')||(LA3_0 >= 'A' && LA3_0 <= 'F')||(LA3_0 >= 'a' && LA3_0 <= 'f')) ) {
                    alt3=1;
                }


                switch (alt3) {
            	case 1 :
            	    // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:
            	    {
            	    if ( (input.LA(1) >= '0' && input.LA(1) <= '9')||(input.LA(1) >= 'A' && input.LA(1) <= 'F')||(input.LA(1) >= 'a' && input.LA(1) <= 'f') ) {
            	        input.consume();
            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;
            	    }


            	    }
            	    break;

            	default :
            	    if ( cnt3 >= 1 ) break loop3;
                        EarlyExitException eee =
                            new EarlyExitException(3, input);
                        throw eee;
                }
                cnt3++;
            } while (true);


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "MD5VALUE"

    // $ANTLR start "HEXDIGIT"
    public final void mHEXDIGIT() throws RecognitionException {
        try {
            // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:218:2: ( DIGIT | 'a' .. 'f' | 'A' .. 'F' )
            // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:
            {
            if ( (input.LA(1) >= '0' && input.LA(1) <= '9')||(input.LA(1) >= 'A' && input.LA(1) <= 'F')||(input.LA(1) >= 'a' && input.LA(1) <= 'f') ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            }


        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "HEXDIGIT"

    // $ANTLR start "ALPHA"
    public final void mALPHA() throws RecognitionException {
        try {
            // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:221:17: ( 'A' .. 'Z' )
            // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:
            {
            if ( (input.LA(1) >= 'A' && input.LA(1) <= 'Z') ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            }


        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "ALPHA"

    // $ANTLR start "DIGIT"
    public final void mDIGIT() throws RecognitionException {
        try {
            // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:224:16: ( '0' .. '9' )
            // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:
            {
            if ( (input.LA(1) >= '0' && input.LA(1) <= '9') ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            }


        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "DIGIT"

    // $ANTLR start "CHAR_S"
    public final void mCHAR_S() throws RecognitionException {
        try {
            int _type = CHAR_S;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:227:9: ( 'char' )
            // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:227:11: 'char'
            {
            match("char"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "CHAR_S"

    // $ANTLR start "LINE_S"
    public final void mLINE_S() throws RecognitionException {
        try {
            int _type = LINE_S;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:230:9: ( 'line' )
            // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:230:11: 'line'
            {
            match("line"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "LINE_S"

    public void mTokens() throws RecognitionException {
        // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:1:8: ( T__12 | T__13 | T__14 | T__15 | T__16 | INT | MIMECHARS | MD5VALUE | CHAR_S | LINE_S )
        int alt4=10;
        alt4 = dfa4.predict(input);
        switch (alt4) {
            case 1 :
                // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:1:10: T__12
                {
                mT__12(); 


                }
                break;
            case 2 :
                // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:1:16: T__13
                {
                mT__13(); 


                }
                break;
            case 3 :
                // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:1:22: T__14
                {
                mT__14(); 


                }
                break;
            case 4 :
                // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:1:28: T__15
                {
                mT__15(); 


                }
                break;
            case 5 :
                // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:1:34: T__16
                {
                mT__16(); 


                }
                break;
            case 6 :
                // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:1:40: INT
                {
                mINT(); 


                }
                break;
            case 7 :
                // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:1:44: MIMECHARS
                {
                mMIMECHARS(); 


                }
                break;
            case 8 :
                // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:1:54: MD5VALUE
                {
                mMD5VALUE(); 


                }
                break;
            case 9 :
                // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:1:63: CHAR_S
                {
                mCHAR_S(); 


                }
                break;
            case 10 :
                // C:\\data\\eclipse_workspace\\clea\\grammars\\pointertarget\\URIFragmentIdentifierPlainText.g:1:70: LINE_S
                {
                mLINE_S(); 


                }
                break;

        }

    }


    protected DFA4 dfa4 = new DFA4(this);
    static final String DFA4_eotS =
        "\6\uffff\1\15\1\10\1\uffff\1\12\4\uffff\1\10\1\uffff";
    static final String DFA4_eofS =
        "\20\uffff";
    static final String DFA4_minS =
        "\1\41\3\uffff\1\145\1\uffff\1\41\1\60\1\uffff\1\150\4\uffff\1\60"+
        "\1\uffff";
    static final String DFA4_maxS =
        "\1\176\3\uffff\1\151\1\uffff\1\176\1\146\1\uffff\1\150\4\uffff\1"+
        "\146\1\uffff";
    static final String DFA4_acceptS =
        "\1\uffff\1\1\1\2\1\3\1\uffff\1\5\2\uffff\1\7\1\uffff\1\10\1\4\1"+
        "\12\1\6\1\uffff\1\11";
    static final String DFA4_specialS =
        "\20\uffff}>";
    static final String[] DFA4_transitionS = {
            "\1\10\1\uffff\5\10\3\uffff\1\10\1\1\1\10\2\uffff\12\6\1\uffff"+
            "\1\2\1\uffff\1\3\3\uffff\6\7\24\10\3\uffff\3\10\2\12\1\11\3"+
            "\12\5\uffff\1\4\1\5\15\uffff\1\10\1\uffff\2\10",
            "",
            "",
            "",
            "\1\13\3\uffff\1\14",
            "",
            "\1\10\1\uffff\5\10\3\uffff\1\10\1\uffff\1\10\2\uffff\12\6\7"+
            "\uffff\6\7\24\10\3\uffff\3\10\6\12\24\uffff\1\10\1\uffff\2\10",
            "\12\16\7\uffff\6\7\32\uffff\6\12",
            "",
            "\1\17",
            "",
            "",
            "",
            "",
            "\12\16\7\uffff\6\7\32\uffff\6\12",
            ""
    };

    static final short[] DFA4_eot = DFA.unpackEncodedString(DFA4_eotS);
    static final short[] DFA4_eof = DFA.unpackEncodedString(DFA4_eofS);
    static final char[] DFA4_min = DFA.unpackEncodedStringToUnsignedChars(DFA4_minS);
    static final char[] DFA4_max = DFA.unpackEncodedStringToUnsignedChars(DFA4_maxS);
    static final short[] DFA4_accept = DFA.unpackEncodedString(DFA4_acceptS);
    static final short[] DFA4_special = DFA.unpackEncodedString(DFA4_specialS);
    static final short[][] DFA4_transition;

    static {
        int numStates = DFA4_transitionS.length;
        DFA4_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA4_transition[i] = DFA.unpackEncodedString(DFA4_transitionS[i]);
        }
    }

    class DFA4 extends DFA {

        public DFA4(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 4;
            this.eot = DFA4_eot;
            this.eof = DFA4_eof;
            this.min = DFA4_min;
            this.max = DFA4_max;
            this.accept = DFA4_accept;
            this.special = DFA4_special;
            this.transition = DFA4_transition;
        }
        public String getDescription() {
            return "1:1: Tokens : ( T__12 | T__13 | T__14 | T__15 | T__16 | INT | MIMECHARS | MD5VALUE | CHAR_S | LINE_S );";
        }
    }
 

}