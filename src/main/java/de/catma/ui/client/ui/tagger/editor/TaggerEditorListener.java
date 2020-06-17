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
package de.catma.ui.client.ui.tagger.editor;

import java.util.HashSet;
import java.util.List;

import de.catma.ui.client.ui.tagger.shared.TextRange;

public interface TaggerEditorListener {
	public static enum TaggerEditorEventType {
		ADD,
		REMOVE,
		;
	}
	
	public void annotationChanged(TaggerEditorEventType type, Object... args);
	public void annotationSelected(String tagInstancePartID, String lineID);
	public void logEvent(String event);
	public void annotationsSelected(HashSet<String> tagInstanceIDs);
	public void contextMenuSelected(int x, int y);
	public void addComment(List<TextRange> ranges, int x, int y);
}
