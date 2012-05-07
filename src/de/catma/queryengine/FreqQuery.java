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

package de.catma.queryengine;

import de.catma.queryengine.result.QueryResult;

/**
 * A query that looks for all types that match a numeric frequency condition. 
 *
 * @author Marco Petris <marco.petris@web.de>
 */
public class FreqQuery extends Query {

    private CompareOperator operator1;
    private CompareOperator operator2;
    private int freq1;
    private int freq2;

    /**
     * Constructor.
     *
     * @param operatorValue the operator which is used to compare the freqency values
     * @param freq1 the freqency value to check against
     */
    public FreqQuery(String operatorValue, String freq1) {
        this(operatorValue, freq1, null);
    }

    /**
     * Constructor.
     *
     * @param operatorValue the operator which is used to compare the freqency values, this argument is
     * only taken into account if freq2 is set to <code>null</code>
     * @param freq1 the freqency value to check against
     * @param freq2 for range checks this is the second frequency value to check against
     * if freq2 is set to a value non-<code>null</code> the operatorValue argument is ignored!
     */
    public FreqQuery(String operatorValue, String freq1, String freq2) {

        this.operator2 = null;

        this.freq1 = Integer.parseInt(freq1);

        // do we have a frequency range?
        if (freq2 !=null) {
            this.freq2 = Integer.parseInt(freq2);
            this.operator1 = CompareOperator.GREATEROREQUALTHAN; 
            this.operator2 = CompareOperator.LESSOREQUALTHAN;
        }
        else {
            this.operator1 = CompareOperator.getOperatorFor(operatorValue);
        }

    }

    @Override
    protected QueryResult execute() throws Exception {
    	QueryOptions options = getQueryOptions();
    	//TODO: freq refinement works different
    	return getIndexer().searchFreqency(
    			options.getDocumentIds(), operator1, freq1, operator2, freq2);
    }

}
