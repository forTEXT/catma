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
package de.catma.document.source.contenthandler;

import de.catma.document.source.SourceDocumentInfo;

import java.io.IOException;

/**
 * Basic implementation that provides lazy loading.
 */
public abstract class AbstractSourceContentHandler implements SourceContentHandler {
	private SourceDocumentInfo sourceDocumentInfo;
	private String content;

	@Override
	public void setSourceDocumentInfo(SourceDocumentInfo sourceDocumentInfo) {
		this.sourceDocumentInfo = sourceDocumentInfo;
	}

	@Override
	public SourceDocumentInfo getSourceDocumentInfo() {
		return sourceDocumentInfo;
	}

	@Override
	public String getContent() throws IOException {
		if (content == null) {
			load();
		}
		return content;
	}

	/**
	 * To be used by concrete implementations to set the content.
	 *
	 * @param content the entire document text
	 */
	protected void setContent(String content) {
		this.content = content;
	}

	@Override
	public void unload() {
		content = null;
	}

	@Override
	public boolean isLoaded() {
		return content != null;
	}

	@Override
	public boolean hasIntrinsicAnnotationCollection() {
		return false;
	}
}
