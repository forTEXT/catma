/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009-2013  University Of Hamburg
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.catma.indexer.wildcardparser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

import de.catma.indexer.TermInfo;




public class WildcardParser {

	static final String ESC= "\\";
	public final static HashSet<String> WILDCARDS = new HashSet<String>();
	static {
		WILDCARDS.add("%");
		WILDCARDS.add("_");
	}
	
	private StateHandler curState;
	private List<TermInfo> orderedTermInfos;
	
	public WildcardParser() {
		orderedTermInfos = new ArrayList<TermInfo>();
		curState = new TermStateHandler(orderedTermInfos);
	}
	
	public void handle(CharTermAttribute termAttr, OffsetAttribute offsetAttr) {
		curState = curState.handle(termAttr, offsetAttr);
	}
	
	public void finish() {
		curState.close();
	}
	
	public List<TermInfo> getOrderedTermInfos() {
		return orderedTermInfos;
	}
}
