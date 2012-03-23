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

import java.util.List;

/**
 * A factory that creates a {@link org.catma.indexer.unseparablecharactersequence.CharTree}
 * from some input.
 *
 * @author Marco Petris
 *
 */
public class CharTreeFactory {

    /**
     * The given list of unseparable character sequences (USC) is converted to a
     * {@link org.catma.indexer.unseparablecharactersequence.CharTree}-structure.
     * @param inputList the list of unseparable character sequences
     * @return the resulting CharTree structure
     */
    public CharTree createCharMap(List<String> inputList) {
        // create the root of the tree
        CharTree head = new CharTree();

        // loop over the USCs
        for (String input : inputList ) {
            CharTree cur = head; // always restart from the head

            char[] inputBuf = input.toCharArray();

            // loop over the characters of the curren USC
            for (int i=0; i<inputBuf.length; i++) {

                Character c = inputBuf[i];

                // whitespaces are handled in a peek mode and can be skipped here
                if (!(Character.isWhitespace(c) && cur.isWhitespaceSequence())) {

                    // if there is no entry for the current character we create one
                    if (!cur.containsKey(c)) {

                        // peek for the next character, as this has to be taken into account as well

                        // we are finished with this character, so current char points to the end entry
                        if (i==inputBuf.length-1) {
                            cur.put(c, CharTree.END_ENTRY);
                        }
                        // next character will be a whitespace, so current char points to a whitespace
                        else if (Character.isWhitespace(inputBuf[i+1])) {
                            cur.put(c, new WhitespaceSequenceEntry());
                        }
                        // just create a new entry
                        else {
                            cur.put(c, new CharTree());
                        }
                    }

                    // descend down the tree one level in the direction of the current char
                    cur = cur.get(c);
                }
            }
        }


        return head;
    }




}
