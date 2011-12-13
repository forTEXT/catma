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

import de.catma.core.tag.IDGenerator;



/**
 * An enumeration of attribute values used by CATMA.
 *
 * @author Marco Petris
 *
 */
public enum AttributeValue {

	/**
	 * 'catma' value of {@link Attribute#type}.
	 */
	type_catma( Attribute.type, "catma" ),
	/**
	 * 'catma_tag' value of {@link Attribute#type}.
	 */
	type_catma_tag( Attribute.type, "catma_tag" ),	
	/**
	 * 'scene' value of {@link Attribute#type}.
	 */
	type_scene( Attribute.type, "scene" ),
	/**
	 * 'act' value of {@link Attribute#type}.
	 */
	type_act( Attribute.type, "act" ),
	/**
	 * 'inclusion' value of {@link Attribute#type}.
	 */
	type_inclusion( Attribute.type, "inclusion" ), 
	/**
	 * 'catma_displaycolor' value of {@link Attribute#f_name}.
	 */
	f_name_catma_displaycolor(
            Attribute.f_name, PropertyValueFactory.CATMA_SYSTEM_PROPERTY_PREFIX + "displaycolor" ),
            
    f_name_catma_system_property(
            Attribute.f_name, PropertyValueFactory.CATMA_SYSTEM_PROPERTY_PREFIX ),
                    
	f_Decl_name_catma_system_property(
            Attribute.fDecl_name, PropertyValueFactory.CATMA_SYSTEM_PROPERTY_PREFIX ),
            
    /**
     * 'catma_tagname' value of {@link Attribute#f_name}.
     */
    name_catma_tagname(
        Attribute.f_name, PropertyValueFactory.CATMA_SYSTEM_PROPERTY_PREFIX + "tagname" ),
        
    seg_ana_catma_tag_ref(
    	Attribute.ana, "#" + IDGenerator.ID_PREFIX ),
    	
    /**
     * 'list' value of {@link Attribute#vColl_org}.
     */
    org_list( Attribute.vColl_org, "list" ),
    ;
	
	private Attribute attribute;
	private String valueName;
	
	/**
	 * Constructor.
	 * @param attribute the attribute 
	 * @param valueName the value
	 */
	private AttributeValue( Attribute attribute, String valueName) {
		this.attribute = attribute;
		this.valueName = valueName;
	}
	
	/**
	 * @return the value of the attribute
	 */
	public String getValueName() {
		return valueName;
	}
	
	/**
	 * @return the attribute
	 */
	public Attribute getAttribute() {
		return attribute;
	}

    @Override
    public String toString() {
        return attribute + "='" + valueName + "'";
    }
    
    public String getStartsWith() {
    	return "starts-with("+attribute+",'"+valueName+"')";
    }
    
    public String getNotStartsWith() {
    	return "not("+getStartsWith()+")";
    }
}
