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

package de.catma.document.source;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import de.catma.ExceptionHandler;


/**
 * Builds and provides a mapping of geographical regions to languages to character
 * encodings based on the resourcefile CharsetLanguageInfo.properties<br>
 * Character Encodings which are not mentioned in that file are listed under the
 * category "others".<br>
 * <br>
 * This class is a singleton.
 * 
 * @author marco.petris@web.d
 *
 */
public enum CharsetLanguageInfo {
	SINGLETON;
	
	private static final String OTHERS = "OTHERS";
	// resource which contains the mapping information
	private Properties charsetLanguageInfoProperties;
	// region->language->charsetList
	private Map<String,Map<String,List<Charset>>> regionLanguageCharsetMapping;
	
	// encodings for which have no information concerning region and language
	private Map<String,List<Charset>> simpleCategoryCharsetMapping;
	
	/**
	 * Builds the mappings.
	 */
	private CharsetLanguageInfo() {
		
		regionLanguageCharsetMapping = 
			new HashMap<String, Map<String,List<Charset>>>();
		simpleCategoryCharsetMapping = new HashMap<String, List<Charset>>();
		simpleCategoryCharsetMapping.put( OTHERS, new ArrayList<Charset>() );
		charsetLanguageInfoProperties = new Properties();
		
		// load resource file with mapping infos
		InputStream is = null;
		try {
			is = 
				Thread.currentThread().getContextClassLoader().getResourceAsStream(
					"de/catma/document/source/resources/" +
					"CharsetLanguageInfo.properties" );
			try {
				charsetLanguageInfoProperties.load( is );
			} catch( IOException e ) {
				ExceptionHandler.log( e );
			}
		}
		finally {
			if( is != null ) {
				try {
					is.close();
				} catch( Exception e ) {
					// not of importance
				}
			}
		}

		// we loop through the available charset and try to apply them to the mapping
		Map<String,Charset> availableCharsets = Charset.availableCharsets();
		for( String charsetName : availableCharsets.keySet() ) {
			String languageInfo = 
				charsetLanguageInfoProperties.getProperty( charsetName );
			
			// do we have infos about this charset?
			if( ( languageInfo != null ) 
					&& !( languageInfo.equals( "" ) ) ) {
				
				// yes, so we try to extract language and region
				String[] infos = languageInfo.trim().split( "," );
				if( infos.length >= 2 ) {
					String language = infos[0];
					String region = infos[1];
					
					// create region mapping if not present
					if( !regionLanguageCharsetMapping.containsKey( region ) ) {
						regionLanguageCharsetMapping.put( 
							region, new HashMap<String, List<Charset>>() );
					}
					
					// create language mapping if not present
					if( !regionLanguageCharsetMapping.get( region ).containsKey( language ) ) {
						regionLanguageCharsetMapping.get( region ).put( 
								language, new ArrayList<Charset>() );
					}
					
					// add this charset its region/languge
					regionLanguageCharsetMapping.get( region ).get( language ).add( 
							availableCharsets.get( charsetName ) );
				}
				else { // no region/language information
					// we use that what we have as a category
					String cat = infos[0];
					if( !simpleCategoryCharsetMapping.containsKey( cat ) ) {
						simpleCategoryCharsetMapping.put( cat, new ArrayList<Charset>() );
					}
					simpleCategoryCharsetMapping.get( cat ).add( 
							availableCharsets.get( charsetName ) );
				}
				
			}
			else { // no infos for that charset, it goes to the 'others' category
				simpleCategoryCharsetMapping.get( OTHERS ).add( 
						availableCharsets.get( charsetName ) );
			}
		}
			
		
	}

	/**
	 * @return a mapping region->language->charset
	 */
	public Map<String, Map<String, List<Charset>>> getRegionLanguageCharsetMapping() {
		return regionLanguageCharsetMapping;
	}
	
	/**
	 * @return a mapping category->charset (these charsets came without language/region infos)
	 */
	public Map<String, List<Charset>> getCategoryCharsetMapping() {
		return simpleCategoryCharsetMapping;
	}
}
