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
package de.catma.ui.tagmanager;

import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Table.TableDragMode;

import de.catma.tag.TagLibrary;
import de.catma.tag.TagManager;
import de.catma.tag.TagsetDefinition;
import de.catma.ui.tabbedview.ClosableTab;

public class TagLibraryView extends HorizontalLayout implements ClosableTab {
	
	private TagLibrary tagLibrary;
	private TagsetTree tagsetTree;
	private boolean init = true;
	private ClickListener reloadListener;
	
	public TagLibraryView(TagManager tagManager, TagLibrary tagLibrary) {
		super();
		this.tagLibrary = tagLibrary;
		initComponents(tagManager, tagLibrary);
	}
	
	public void setReloadListener(ClickListener reloadListener) {
		this.reloadListener = reloadListener;
	}

	private void initActions() {
		tagsetTree.addBtReloadListener(reloadListener);
	}

	private void initComponents(TagManager tagManager, TagLibrary tagLibrary) {
		setSizeFull();
		tagsetTree = new TagsetTree(tagManager, tagLibrary);
		addComponent(tagsetTree);
	}
	
	@Override
	public void attach() {
		super.attach();
		if (init) {
			tagsetTree.getTagTree().setDragMode(TableDragMode.ROW);
			tagsetTree.setTagLibrary(tagLibrary);
			
			initActions();
			init = false;
		}
	}

	TagLibrary getTagLibrary() {
		return tagLibrary;
	}

	public void close() {
		tagsetTree.close();
	}
	
	public void addClickshortCuts() { /* noop*/	}
	
	public void removeClickshortCuts() { /* noop*/ }

}
