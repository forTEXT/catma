/*
 * CATMA Computer Aided Text Markup and Analysis
 *
 *    Copyright (C) 2008-2010  University Of Hamburg
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

import java.io.Reader;

import org.apache.lucene.analysis.CharTokenizer;

import de.catma.indexer.unseparablecharactersequence.CharTree;
import de.catma.indexer.unseparablecharactersequence.UnseparableCharacterSequenceAttribute;


/**
 * A tokenizer similar to the classic {@link org.apache.lucene.analysis.WhitespaceTokenizer} but
 * this respectes unseparable character sequences defined by the user.
 *
 * @author Marco Petris
 *
 * @see org.catma.indexer.unseparablecharactersequence.CharTree
 */
class CatmaWhitespaceTokenizer extends CharTokenizer {

    private UnseparableCharacterSequenceAttribute ucAtt;

    private CharTree treeRoot;
    private CharTree currentEntry;

    /**
     * Constructor
     *
     * @param reader input
     * @param unseparableCharacterSequences the root of the unseperable character sequence tree,
     * can be <code>null</code>
     */
    CatmaWhitespaceTokenizer(
            Reader reader, CharTree unseparableCharacterSequences) {
        super(reader);
        ucAtt = (UnseparableCharacterSequenceAttribute)
                addAttribute(UnseparableCharacterSequenceAttribute.class);

        treeRoot =  unseparableCharacterSequences;
        if (treeRoot == null) {
            treeRoot = CharTree.EMPTY_TREE;
        }

        currentEntry = treeRoot;
    }

    @Override
    protected boolean isTokenChar(char c) {

        boolean isWS = Character.isWhitespace(c);


        if (treeRoot.size() == 0 ) {// no unseparable character sequences defined
            return !isWS;
        }

        if (isWS) { // white space char

            // did we expect a whitspace?
            if (currentEntry.isWhitespaceSequence()) {
                return true; // yes, so this belongs to an unseparable sequence
            }
            else {
                // no, ok unseparable if we have reached the end, separable else
                ucAtt.setUnseparable(currentEntry.isEndEntry());
                // restart next time
                currentEntry = treeRoot;

                return false; // this whitespace is not a token
            }
        }
        else {

            // did we reach the end last time,  then restart
            if (currentEntry.isEndEntry()) {
                currentEntry = treeRoot;
            }

            // look for successors of the current character
            CharTree next = currentEntry.get(c);

            if (next == null) { // no successors
                // ok, sequence is separable
                ucAtt.setUnseparable(false);
                // restart
                currentEntry = treeRoot;
            }
            else {
                // sequence seems unseparable so far
                currentEntry = next;
                ucAtt.setUnseparable(true);
            }
            // not a whitspace in any case
            return true;
        }
    }
}
