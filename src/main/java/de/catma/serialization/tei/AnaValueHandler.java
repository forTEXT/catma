/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009  University Of Hamburg
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

package de.catma.serialization.tei;

import java.util.ArrayList;
import java.util.List;

import de.catma.tag.TagInstance;

/**
 * A handler for values of {@link Attribute#ana}.
 *
 * @author Marco Petris
 *
 */
public class AnaValueHandler implements AttributeValueHandler {
	
	/**
	 * Creates a value string with an existent value string and 
	 * a new reference to the given tag.
	 * @param anaValue the existent value string
	 * @param tag the tag we want to reference
	 * @return the new value string
	 */
//	public String makeValueFrom( String anaValue, Tag tag ) {
//		StringBuilder builder = new StringBuilder( anaValue );
//		if( !anaValue.equals( "" ) ) {
//			builder.append( " " );
//		}
//		builder.append( "#" );
//		builder.append( tag.getID() );
//		return builder.toString();
//	}
//	
	/**
	 * Creates a value string with references to the tags in the given list.
	 * @param tags the tags we want to reference
	 * @return the new value string
	 */
//	public String makeValueFrom( List<Tag> tags ) {
//		return makeValueFrom( tags.toArray( new Tag[]{} ) );
//	}
	

	public String makeValueFrom( List<TagInstance> tagInstances ) {
		StringBuilder builder = new StringBuilder();
		String conc = "";
		for( TagInstance tagInstance : tagInstances ) {
			builder.append( conc );
			builder.append( "#" );
			builder.append( tagInstance.getUuid() );
			conc = " ";
		}
		return builder.toString();
	}

	/**
	 * Extracts the tags that are referenced by the given value string.
	 * @param anaValue the value string which contains the references
	 * @return the list of referenced tags
	 */
//	public List<Tag> makeTagListFrom( String anaValue ) {
//		List<Tag> tags = new ArrayList<Tag>();
//		if( anaValue != null ) {
//			String[] idValues = anaValue.trim().split( "#" );
//			for( String id : idValues ) {
//				Tag tag = 
//					TagManager.SINGLETON.getCurrentTagDatabaseDocument().
//						getTeiTagDatabase().getTag( 
//							id.trim() );
//				if( tag != null ) {
//					tags.addSystemPropertyDefinition(tag);
//				}
//			}
//		}
//		return tags;
//	}
//	
	
	/**
	 * Extracts the xml:ids of the tags that are referenced by the given value
	 * string.
	 * @param anaValue the value string which contains the references
	 * @return the list of referenced tag-xml:ids
	 */
	public List<String> makeTagInstanceIDListFrom( String anaValue ) {
		List<String> tagIDs = new ArrayList<String>();
		if( anaValue != null ) {
			String[] idValues = anaValue.trim().split( "#" );
			for( String id : idValues ) {
                String trimmedID = id.trim();
				if (!trimmedID.isEmpty()) {
                    tagIDs.add( trimmedID );
                }
			}
		}
		return tagIDs;
	}
	
	/**
	 * Creates a value string with an existent value string but strips off
	 * the reference to the given tag.
	 * @param anaValue the existent value string (has to contain a reference to 
	 * the given tag!)
	 * @param tag the tag for which we want to remove the reference
	 * @return the new value string
	 */
//	public String makeListWithoutTag( String anaValue, Tag tag ) {
//		int startIdx = anaValue.indexOf( tag.getID() )-1;
//		int endIdx = startIdx+tag.getID().length()+1;
//		if( endIdx != anaValue.length() ) {
//			endIdx++; // remove separating space, too
//		}
//		return anaValue.substring( 0, startIdx ) 
//			+ anaValue.substring( endIdx, anaValue.length() );
//	}
}
