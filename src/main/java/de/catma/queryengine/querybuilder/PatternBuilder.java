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

package de.catma.queryengine.querybuilder;

import java.util.regex.Pattern;

/**
 * A builder for regex patterns.
 *
 * @author Marco Petris
 *
 */
public class PatternBuilder {

    /**
     * Constructor.
     */
    public PatternBuilder() {
    }


    /**
     * Builds a pattern that matches the given input exactly.
     * @param input the string to match
     * @return the pattern
     */
    public String getExactPatternFor(String input) {
        String pattern = "";

        if ( (input != null) && !input.isEmpty() ) {
            pattern = "\\b"+ Pattern.quote(input)+"\\b";
        }
        return pattern;
    }

    /**
     * Builds a pattern that matches character sequences that end with the given input sequence.
     * @param input the end sequence to match
     * @param withPrefix <code>true</code> -> the pattern is prefixed by a word boundary pattern. Use this if you will not
     * specify how the words you are trying to match with this pattern look like at the beginning or in the middle of the
     * word, else use <code>false</code>.
     * @return the pattern
     */
    public String getEndsWithPatternFor(String input, boolean withPrefix) {
        String pattern = "";

        if ( (input != null) && !input.isEmpty() ) {
            if (withPrefix) {
                pattern =  "\\b\\S*";
            }
            pattern += Pattern.quote(input) +"(?=\\W)";
        }

        return pattern;
    }

    /**
     * Builds a pattern that matches character sequences that start with the given input sequence.
     * @param input the start sequence to match
     * @return the pattern
     */
    public String getStartsWithPatternFor(String input) {
        String pattern = "";

        if ( (input != null) && !input.isEmpty() ) {
            pattern =  "\\b"+ Pattern.quote(input)+"\\S*";
        }

        return pattern;
    }

    /**
     * Builds a pattern that matches character sequences that contain the given input sequence.
     * @param input the contained sequence to match
     * @param withPrefix <code>true</code> -> the pattern is prefixed by a word boundary pattern. Use this if you will
     * not specify how the words you are trying to match with this pattern look like at the beginning of the word,
     * else use <code>false</code>.
     * @return the pattern
     */
    public String getContainsPattern(String input, boolean withPrefix) {
        String pattern = "";

        if ( (input != null) && !input.isEmpty() ) {
            if (withPrefix) {
                pattern =  "\\b\\S*";
            }
            pattern += Pattern.quote(input)+"\\S*";
        }

        return pattern;
    }

    /**
     * Builds a position pattern, that wildcards words that are at the positions before the given position. The word we
     * care about is at the given position, the words we don't care about are at the preceding positions and will be wild
     * carded with this pattern.
     * @param position the position of the word we care about
     * @return the pattern
     */
    public String getPositionPattern(int position) {
        if (position < 0) {
            throw new IllegalArgumentException(
                "position must not be a negative value, current value: " +
                        position);
        }
        if (position == 0) {
            return "\\W+.*?";

        }
        else if (position == 1) {
            return "\\W*";
        }
        else {
            StringBuilder builder = new StringBuilder();
            for( int i=0; i<position-1; i++ ) {
                builder.append("\\W+");
                builder.append( "\\b\\S+\\W*" );
            }

            return builder.toString();
        }
    }

}