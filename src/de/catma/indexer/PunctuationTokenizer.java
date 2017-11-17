/*
 *    CATMA Computer Aided Text Markup and Analysis
 * 
 *    Copyright (C) 2009  University Of Hamburg
 * 
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 * 
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 * 
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.catma.indexer;

import java.io.IOException;
import java.text.BreakIterator;
import java.util.ArrayDeque;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

import de.catma.indexer.unseparablecharactersequence.CharTree;
import de.catma.indexer.unseparablecharactersequence.UnseparableCharacterSequenceAttribute;

/**
 * A punctuation aware tokenizer.
 *
 * @author Marco Petris <marco.petris@web.de>
 */
public final class PunctuationTokenizer extends TokenFilter {

    private CharTermAttribute termAtt;
    private OffsetAttribute offsetAtt;
    private UnseparableCharacterSequenceAttribute ucAtt;
    private ArrayDeque<TermInfo> termInfoBuffer;
    private CharTree treeRoot;
    private Pattern userDefSeparatingPunctuationPattern;
    private Locale locale;

    /**
     * Constructor.
     *
     * @param input the input stream
     * @param unseparableCharacterSequences the list of unseparable character sequences
     * @param userDefSeparatingPunctuationPattern a pattern of OR-ed user defined
     *  separating punctuation characters
     * @param locale the locale of the main language of the content
     */
    public PunctuationTokenizer(
            TokenStream input,
            CharTree unseparableCharacterSequences,
            Pattern userDefSeparatingPunctuationPattern,
            Locale locale) {
        super(input);

        termInfoBuffer = new ArrayDeque<TermInfo>();
        offsetAtt = (OffsetAttribute) input.getAttribute(OffsetAttribute.class);
        termAtt = (CharTermAttribute) input.getAttribute(CharTermAttribute.class);
        ucAtt = (UnseparableCharacterSequenceAttribute)
                        input.getAttribute(UnseparableCharacterSequenceAttribute.class);

        treeRoot =  unseparableCharacterSequences;
        if (treeRoot == null) {
            treeRoot = CharTree.EMPTY_TREE;
        }
        this.userDefSeparatingPunctuationPattern = userDefSeparatingPunctuationPattern;
        this.locale = locale;
    }

    @Override
    public boolean incrementToken() throws IOException {

        // do we have entries from the previos tokenize operation?
        if (termInfoBuffer.size() == 0) {
            // no, ok then ask the stream

            boolean rc = input.incrementToken();

            if (!rc) {
                return false; // no more tokens in the stream
            }

            String origTerm = termAtt.toString();
            int origStartOffset = offsetAtt.startOffset();
            int origEndOffset = offsetAtt.endOffset();

            boolean isUC = ucAtt.isUnseparable();

            // is this entry already known as unseparable?
            if (!isUC) {
                // no, ok we look for sequences that include punctuation, that
                // might have been separated but should't be
                tokenize(origTerm, origStartOffset);
            }
            else {
                // not separable, so just leave it for later handling
                termInfoBuffer.addLast(
                    new TermInfo(
                        origTerm,
                        origStartOffset,
                        origEndOffset));
            }
        }

        // ok, now we handle everything we left for later, while tokenizing
        if (termInfoBuffer.size() > 0) {
            TermInfo ti = termInfoBuffer.pop();
            termAtt.setEmpty();
            termAtt.append(ti.getTerm());
            offsetAtt.setOffset(
                    (int)ti.getRange().getStartPoint(),
                    (int)ti.getRange().getEndPoint());
        }

        return true;
    }

//    public static boolean isPunctuation(char c) {
//        return !Character.isDigit(c) && !Character.isLetter(c);
//    }

    /**
     * Tokenize the given term, taking unseparable character sequences and punctuation into
     * account.
     *
     * @param term the term to tokenize
     * @param startOffset compute offsets with this base offset
     */
    private void tokenize(String term, int startOffset) {

        BreakIterator breakIterator = BreakIterator.getWordInstance(locale);
        breakIterator.setText(term);

        // buffer for unseparable character sequence
        StringBuilder uscTerm = new StringBuilder();
        // start index for unseparable character sequence
        int uscStart = 0;
        CharTree curTree = treeRoot;

        int chunkStart = breakIterator.first();

        // we loop over the chunks which the standard break iterator gives us
        for( int chunkEnd = breakIterator.next();
                chunkEnd != BreakIterator.DONE;
                chunkEnd = breakIterator.next() ) {

            String curChunk = term.substring(chunkStart,chunkEnd);

            curTree = curTree.matches(curChunk);

            if (curTree != null) { // found an entry for the current chunk

                // build uscTerm

                // mark start if this is the first chunk of the usc sequence
                if (uscTerm.length() == 0) {
                    uscStart = chunkStart;
                }
                uscTerm.append(curChunk);
                
                // did we reach the end
                if (curTree.isEndEntry()) {
                    curTree = treeRoot;
                    // end -> store uscTerm
                    if (uscTerm.length() > 0) {
                        termInfoBuffer.addLast(
                            new TermInfo(
                                uscTerm.toString(),
                                startOffset+uscStart,
                                startOffset+uscStart+uscTerm.length()));
                        uscTerm.delete(0,uscTerm.length());
                        uscStart = 0;
                    }
                }
            }
            else {

                if (uscTerm.length() > 0) {
                    split(uscTerm.toString(), startOffset);
                    uscTerm.delete(0,uscTerm.length());
                    uscStart = 0;

                    // restart with the current chunk
                    curTree = treeRoot.matches(curChunk);

                    if (curTree != null) { // found an entry for the current chunk
                        // did we reach the end
                        if (curTree.isEndEntry()) {
                            curTree = treeRoot;
                            termInfoBuffer.addLast(
                                new TermInfo(
                                    curChunk,
                                    startOffset+chunkStart,
                                    startOffset+chunkEnd));
                            chunkStart = chunkEnd;
                        }
                        continue; 
                    }                        

                }

                // no match -> this is a separable sequence, so go ahead and split
                if (userDefSeparatingPunctuationPattern != null) {
                    splitAtUserDefPunctuation(startOffset, curChunk, chunkStart);
                }
                else {
                    termInfoBuffer.addLast(
                        new TermInfo(
                            curChunk,
                            startOffset+chunkStart,
                            startOffset+chunkEnd));
                }
                
                curTree = treeRoot;
            }

            chunkStart = chunkEnd;
        }
    }

    /**
     * Split the given term into tokens. We are concerned about punctuation only, the given
     * term is not an USC.
     *
     * @param term the term to split
     * @param startOffset the base offset to compute the offsets
     */
    private void split(String term, int startOffset) {
        BreakIterator breakIterator = BreakIterator.getWordInstance(locale);
        breakIterator.setText(term);

        int chunkStart = breakIterator.first();

        // we loop over the chunks the standard break iterator gives us
        for( int chunkEnd = breakIterator.next();
                chunkEnd != BreakIterator.DONE;
                chunkEnd = breakIterator.next() ) {

            String curChunk = term.substring(chunkStart,chunkEnd);

            if (curChunk.trim().isEmpty()) {
                continue;
            }

            if (userDefSeparatingPunctuationPattern != null) {
                splitAtUserDefPunctuation(startOffset, curChunk, chunkStart);
            }
            else {
                termInfoBuffer.addLast(
                    new TermInfo(
                        curChunk,
                        startOffset+chunkStart,
                        startOffset+chunkEnd));
            }
            chunkStart = chunkEnd;
        }        
    }

    /**
     * Splits the given chunk at the user defined separating characters.
     * @param startOffset the base offset to compute the offsets
     * @param curChunk the current chunk to split
     * @param chunkStart the start offset of the current chunk within the term we
     * are currently dealing with
     */
    private void splitAtUserDefPunctuation(int startOffset, String curChunk, int chunkStart) {
        if (!curChunk.trim().isEmpty()) {
            Matcher matcher =
                    userDefSeparatingPunctuationPattern.matcher(curChunk);

            int startIdx=0;
            while(matcher.find()) {

                int idx = matcher.start();

                // add the token for the text that is separated
                if (idx!=startIdx) {
                    termInfoBuffer.addLast(
                        new TermInfo(
                            curChunk.substring(startIdx, idx),
                            startOffset+chunkStart+startIdx,
                            startOffset+chunkStart+idx));
                }

                // add the token for the separating character
                termInfoBuffer.addLast(
                    new TermInfo(
                        curChunk.substring(idx, idx+1),
                        startOffset+chunkStart+idx,
                        startOffset+chunkStart+idx+1));
                startIdx=idx+1;

            }

            // add the token for the last part of the chunk 
            termInfoBuffer.addLast(
                new TermInfo(
                    curChunk.substring(startIdx),
                    startOffset+chunkStart+startIdx,
                    startOffset+chunkStart+startIdx+
                            curChunk.substring(startIdx).length()));
        }
    }
}


