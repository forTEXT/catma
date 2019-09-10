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
package de.catma.ui.module.project.document;

import java.util.ArrayList;
import java.util.Collection;

import de.catma.document.source.TechInfoSet;

public class AddSourceDocWizardResult {
	
	private TechInfoSet inputTechInfoSet;	
	private ArrayList<SourceDocumentResult> sourceDocumentResults;

	public AddSourceDocWizardResult() {
		super();
		
		this.sourceDocumentResults = new ArrayList<SourceDocumentResult>();
	}
	
	public Collection<SourceDocumentResult> getSourceDocumentResults() {
		return this.sourceDocumentResults;
	}
	
	public void addSourceDocumentResults(ArrayList<SourceDocumentResult> sourceDocumentResults) {
		this.sourceDocumentResults.addAll(sourceDocumentResults);
	}
	
	public void clearAllSourceDocumentResults() {
		this.sourceDocumentResults.clear();
	}
	
	public TechInfoSet getInputTechInfoSet() {
		return this.inputTechInfoSet;
	}
	
	public void setInputTechInfoSet(TechInfoSet tis) {
		this.inputTechInfoSet = tis;
	}
	
}
