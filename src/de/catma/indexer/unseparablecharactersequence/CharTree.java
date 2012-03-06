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

package de.catma.indexer.unseparablecharactersequence;

import java.util.HashMap;


/**
 * This tree structure holds unseperable character sequences.<br>
 * <br>
 * The head of the tree is a map that has all first characters of the sequences as its keys. The values are
 * again trees which has the second characters as keys and so on until the last characters of
 * the sequences are keys that point to a special {@link #END_ENTRY} instance as their value.<br>
 * <br>
 * This structure provides fast checking if a a given input matches a unseperable character sequence.
 *
 * @author Marco Petris
 *
 * @see org.catma.indexer.unseparablecharactersequence.CharTreeFactory
 *
 */
public class CharTree extends HashMap<Character, CharTree> {

    /**
     * The end of an unseparable character sequence within the tree structure.
     */
    public static final CharTree END_ENTRY = new CharTree();
    /**
     * An empty tree structure.
     */
    public static final CharTree EMPTY_TREE = new CharTree();

    /**
     * Constructor.
     */
    CharTree() {
        
    }

    /**
     * @return always <code>false</code>.
     */
    public boolean isWhitespaceSequence() {
        return false;
    }

    /**
     * @return true if this tree equals the {@link #END_ENTRY}.
     */
    public boolean isEndEntry() {
        return this.equals(END_ENTRY);
    }

    /**
     * Starting from this tree we try to match the input with one of the unseparable
     * character sequences contained in this tree.
     * @param input the input to test
     * @return <code>null</code> if there has been no match <b>or</b>
     * {@link #END_ENTRY} if the input matches completely an unseparable character sequence of this tree <b>or</b>
     * the pointer to the remaining parts of unseparable character sequences that matches the input
     * so far, i. e. all the characters of the input sequence match so far but the matching character sequences
     * contain more characters, more input is needed to decide if there will be a complete successfull
     * match and to do this more input can be checked against the tree returned.  
     *
     */
    public CharTree matches(String input) {

        CharTree currentTree = this;

        // loop over characters and try to match them with one of the char sequences of this tree

        for (int i=0; i<input.length(); i++) {

            Character c = input.charAt(i);

            boolean isWS = Character.isWhitespace(c);

            if (isWS) {
                // did we expect a whitespace?
                if (!currentTree.isWhitespaceSequence()) {
                    return null; // no match
                }
            }
            else {
                
                if (currentTree.isEndEntry()) { // we reached an endpoint
                    if (i==input.length()-1) {
                        return currentTree; // input matches
                    }
                    else {
                        return null;
                    }
                }

                CharTree next = currentTree.get(c);

                if (next == null) { // no match for the current character, we exit
                    return null;
                }
                else { // match, we descend one level deeper
                    currentTree = next;
                }
            }
        }

        // matches so far, but there are more characters
        // in the matching sequence of this tree        
        return currentTree;
    }
}

