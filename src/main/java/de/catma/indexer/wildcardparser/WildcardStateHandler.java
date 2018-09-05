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

import java.util.List;

import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

import de.catma.indexer.TermInfo;

class WildcardStateHandler extends AbstractStateHandler {

	private TermInfo wildcardTermInfo;

	public WildcardStateHandler(List<TermInfo> orderedTermInfos,
			CharTermAttribute termAttr, OffsetAttribute offsetAttr) {
		super(orderedTermInfos);
		this.wildcardTermInfo = new TermInfo(termAttr.toString(),
				offsetAttr.startOffset(), offsetAttr.endOffset());
		checkLastListEntry();
	}

	public WildcardStateHandler(List<TermInfo> orderedTermInfos, TermInfo ti) {
		super(orderedTermInfos);
		this.wildcardTermInfo = ti;
		checkLastListEntry();
	}
	
	private void checkLastListEntry() {
		
		if (!orderedTermInfos.isEmpty()) {
			TermInfo lastTermInfo = orderedTermInfos.get(orderedTermInfos.size()-1);
			if (lastTermInfo.getRange().getEndPoint() 
					== this.wildcardTermInfo.getRange().getStartPoint()) {
				this.wildcardTermInfo = 
						new TermInfo(lastTermInfo.getTerm() + wildcardTermInfo.getTerm(),
								lastTermInfo.getRange().getStartPoint(),
								wildcardTermInfo.getRange().getEndPoint());
				orderedTermInfos.remove(orderedTermInfos.size()-1);
			}
		}
	}

	public StateHandler handle(CharTermAttribute termAttr,
			OffsetAttribute offsetAttr) {
		if (offsetAttr.startOffset() == wildcardTermInfo.getRange().getEndPoint()) {
			
			if (termAttr.toString().equals(WildcardParser.ESC)) {
				return new EscapeStateHandler(orderedTermInfos, termAttr, offsetAttr);
			}
			else if (WildcardParser.WILDCARDS.contains(termAttr.toString())) {
				TermInfo ti = new
						TermInfo(wildcardTermInfo.getTerm()+termAttr.toString(),
								wildcardTermInfo.getRange().getStartPoint(),
								offsetAttr.endOffset());
				
				return new WildcardStateHandler(orderedTermInfos, ti);
			}
			else {
				this.orderedTermInfos.add(new
						TermInfo(wildcardTermInfo.getTerm()+termAttr.toString(),
								wildcardTermInfo.getRange().getStartPoint(),
								offsetAttr.endOffset()));

				return new TermStateHandler(orderedTermInfos);
			}
		}
		else {
			this.orderedTermInfos.add(wildcardTermInfo);
			
			return new TermStateHandler(orderedTermInfos).handle(
					termAttr, offsetAttr);
		}
	}
	
	public void close() {
		if (wildcardTermInfo != null) {
			orderedTermInfos.add(wildcardTermInfo);
		}
	}
}