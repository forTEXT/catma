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


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import de.catma.document.Range;

/**
 * A span context.
 *
 * @author Marco Petris
 *
 */
public class SpanContext {

    private List<TermInfo> backwardTokens;
    private List<TermInfo> forwardTokens;
    private String backward;
    private String forward;
    private Range backwardRange;
    private Range forwardRange;
    private String sourceDocumentId;

    /**
     * Contructor. Creates an empty span context.
     */
    public SpanContext(String sourceDocumentId) {
    	this.sourceDocumentId = sourceDocumentId;
        backwardTokens = new ArrayList<TermInfo>();
        forwardTokens = new ArrayList<TermInfo>();
    }

    /**
     * @return the context part, which had been found in front of the keyword
     */
    public String getBackward() {
        return backward;
    }

    /**
     * @param backward the context part, which had been found in front of the keyword
     */
    public void setBackward(String backward) {
        this.backward = backward;
    }

    /**
     * @return the context part, which had been found after the keyword
     */
    public String getForward() {
        return forward;
    }

    /**
     *
     * @param forward the context part, which had been found after the keyword
     */
    public void setForward(String forward) {
        this.forward = forward;
    }

    /**
     * @return the range of the backward span
     */
    public Range getBackwardRange() {
        return backwardRange;
    }

    /**
     * @param backwardRange the range of the backward span
     */
    public void setBackwardRange(Range backwardRange) {
        this.backwardRange = backwardRange;
    }

    /**
     * @return the range of the forward span
     */
    public Range getForwardRange() {
        return forwardRange;
    }

    /**
     * @param forwardRange the range of the forward span
     */
    public void setForwardRange(Range forwardRange) {
        this.forwardRange = forwardRange;
    }

    /**
     * @return true if the forward span has been set, else false
     */
    public boolean hasForwardSpan() {
        return forward != null;
    }

    /**
     * @return true if the backward span has been set, else false
     */
    public boolean hasBackwardSpan() {
        return backward != null;
    }

    /**
     * @param token the token to add to the backward tokens
     */
    public void addBackwardToken(TermInfo token) {
        backwardTokens.add(0,token);
    }

    /**
     * @param token token the token to add to the forward tokens
     */
    public void addForwardToken(TermInfo token) {
        forwardTokens.add(token);
    }

    /**
     * @return a list of the tokens contained in the backward span
     */
    public List<TermInfo> getBackwardTokens() {
        return backwardTokens;
    }

    /**
     * @return a list of the tokens contained in the forward span
     */
    public List<TermInfo> getForwardTokens() {
        return forwardTokens;
    }
    
    public String getSourceDocumentId() {
		return sourceDocumentId;
	}
    
    @Override
    public String toString() {
    	return "left["+Arrays.toString(backwardTokens.toArray())+
    	"] right["+Arrays.toString(forwardTokens.toArray()) + 
    	"]@" + sourceDocumentId;
    }

	public boolean contains(Collection<TermInfo> termInfos) {
		return (forwardTokens.containsAll(termInfos) 
				|| backwardTokens.containsAll(termInfos));
	}

	public void addForwardTokens(Set<TermInfo> termInfos) {
		for (TermInfo ti : termInfos) {
			addForwardToken(ti);
		}
	}
	
	public void addBackwardTokens(Set<TermInfo> termInfos) {
		for (TermInfo ti : termInfos) {
			addBackwardToken(ti);
		}
	}
	
}
