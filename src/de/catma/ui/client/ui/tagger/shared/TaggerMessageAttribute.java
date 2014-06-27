/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2012  University Of Hamburg
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
package de.catma.ui.client.ui.tagger.shared;

/**
 * @author marco.petris@web.de
 *
 */
public enum TaggerMessageAttribute {
	/**
	 * Attribute signals a new page to the client side. This
	 * event is usually accompanied by one or more numbered 
	 * {@link EventAttribute#TAGINSTANCES_ADD} attributes.
	 */
	PAGE_SET,
	/**
	 * Attribute signals the selection of a TagDefinition that
	 * takes part in a tagging action. to client
	 */
	TAGDEFINITION_SELECTED,
	/**
	 * Attribute signals the selction of a Tagset that gets added
	 * to the currently available Tagsets for tagging actions.
	 */
//	TAGSETDEFINITION_ATTACH,
	/**
	 * Attributes signals to the server that a TagInstance has been added
	 * by a tagging action.
	 */
	TAGINSTANCE_ADD,
	/**
	 * Attribute signals to the client that a set of TagInstances are selected
	 * to show up in the editor.
	 */
	TAGINSTANCES_ADD,
	/**
	 * Attribute signals to the server that a TagInstance has been removed
	 * by a tagging (remove-)action.
	 * Not used by CATMA.
	 */
	TAGINSTANCE_REMOVE,
	/**
	 * Attribute signals to the server that a text point with one or more 
	 * TagInstances had been selected or in case of an empty list  a text point 
	 * with no TagInstances had been selected.
	 */
	TAGINSTANCES_SELECT,
	/**
	 * Attribute signals to the client that a set of TagInstances are selected
	 * to NO LONGER show up in the editor.
	 */
	TAGINSTANCES_REMOVE,
	/**
	 * Log messages sent from the client to the server.
	 */
	LOGMESSAGE, 
	/**
	 * Attribute signals the tagger ID to the client.
	 */
	ID,
	/**
	 * Attribute signals to the client that a range should be highlighted.
	 */
	HIGHLIGHT
	;
}