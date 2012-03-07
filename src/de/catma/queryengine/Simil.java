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

package de.catma.queryengine;

import java.util.Stack;

/**
 * Implementation of the Ratcliff/Obershelp Pattern Matching Algorithm as described in the
 * July 1988 issue of <a href=http://www.ddj.com/184407970?pgno=5>Dr. Dobbs Journal</a>
 *
 * @author Marco Petris
 *
 */
public class Simil {

    private String upBaseInput;

    /**
     * Constructor.
     * @param baseInput the basic input to compare against
     */
    public Simil(String baseInput) {
        upBaseInput = baseInput.toUpperCase();
    }

    /**
     * This method takes two strings, one from each stack, and looks for the largest
     * substring which both have in common. The fragments of the two strings which do not
     * belong to the common substring are pushed on the stacks. The size of the common
     * susbstring is returned.
     *
     * @param baseInputStack the stack with the remaining portions of the base input string which await
     * examination
     * @param inputStack the stack with the remaining portions of the input string which await
     * examination
     * @return the size of the largets common substring of the two strings which were on the tops of
     * the incoming stacks.
     */
    private int compare(Stack<String> baseInputStack, Stack<String> inputStack) {

        String comp1 = baseInputStack.pop();
        String comp2 = inputStack.pop();

        // we start with the largest possible size
        int windowSize = Math.min(comp1.length(), comp2.length());

        // loop over the decrementing size until we find
        // a common substring
        while (windowSize > 0 ) {

            // we start to compare subtrings of the current windowsize
            // from the beginning of the base input fragment
            // and move forward with this window by one-character-steps until
            // we find a match
            int pos = 0;
            while (pos+windowSize-1 < comp1.length()) {

                // is this a common substring?
                if (comp2.contains(comp1.substring(pos,pos+windowSize))) {

                    // yes, so we take the parts that do not belong to our matching
                    // string and push them onto the stack for later examination
                    String comp2Rest[] =
                            comp2.split(comp1.substring(pos,pos+windowSize),2);
                    String comp1Rest[] =
                            comp1.split(comp1.substring(pos,pos+windowSize),2);

                    // both rest-arrays should have at least one entry to compare to next time
                    int resultLen = Math.min(comp1Rest.length, comp2Rest.length);
                    if (resultLen> 1) {
                        // we do not push empty fragments onto the stack
                        // but everything else
                        for (int idx=0; idx<resultLen; idx++) {
                            if (!"".equals(comp1Rest[idx])) {
                                baseInputStack.push(comp1Rest[idx]);
                            }
                            if (!"".equals(comp2Rest[idx])) {
                                inputStack.push(comp2Rest[idx]);
                            }
                        }
                    }
                    // this is the size of the first largest substring we could find
                    return windowSize;
                }
                // nothing so far, so we move the window one character forward
                pos++;
            }
            // nothing so far, so we make the window (substring size) smaller
            windowSize--;
        }

        return 0;
    }

    /**
     * Computes the similarity of the base input of this instance to the given
     * input and returns the computed value in percent.
     *
     * @param input the input to compare with the basic input of this instance
     * @return the percentage value of similarity
     */
    public int getSimilarityInPercentFor(String input) {
        String upInput = input.toUpperCase();
        Stack<String> inputStack = new Stack<String>();
        Stack<String> baseInputStack = new Stack<String>();

        baseInputStack.push(upBaseInput);
        inputStack.push(upInput);

        // total length of the matching substrings
        int compCount = 0;

        // we loop over the portions of the strings and try to find
        // the lengths of largest substrings
        // the stacks gets filled when common substrings are found
        while(inputStack.size() > 0 && baseInputStack.size() > 0) {

            compCount += compare(baseInputStack, inputStack);

        }

        // compute the percent value for the total length of the matching substrings
        // regarding the combined total length of the two strings we compared 
        double percentVal = (compCount*2);
        percentVal /= (upBaseInput.length()+upInput.length());
        
        return (int)(Math.round(percentVal*100.0));
    }
}

