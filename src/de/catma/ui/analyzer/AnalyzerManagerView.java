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
package de.catma.ui.analyzer;

import java.util.HashSet;

import com.vaadin.ui.Component;

import de.catma.document.Corpus;
import de.catma.indexer.IndexedRepository;
import de.catma.ui.analyzer.AnalyzerView.CloseListener;
import de.catma.ui.tabbedview.TabbedView;

public class AnalyzerManagerView extends TabbedView {
	
	public AnalyzerManagerView() {
		super("To analyze documents and collections please do the following: " +
				"Open a Repository with the Repository Manager and use the " +
				"analyze menu items in the corpora or source documents " +
				"sections or the analyze button of the Tagger.");
	}

	public void analyzeDocuments(Corpus corpus, IndexedRepository repository) {
		AnalyzerView analyzerView = 
				new AnalyzerView(corpus, repository, new CloseListener() {
					
					public void closeRequest(AnalyzerView analyzerView) {
						onTabClose(analyzerView);
					}
				});
		
		HashSet<String> captions = new HashSet<String>();
		
		for (Component c : this.getTabSheet()) {
			captions.add(getCaption(c));
		}
		
		String base = (corpus == null)? "All documents" : corpus.toString();
		String caption = base;
		
		int captionIndex = 1;
		while (captions.contains(caption)) {
			caption = base + "("+captionIndex+")";
			captionIndex++;
		}

		addClosableTab(analyzerView, caption);
	}
}
