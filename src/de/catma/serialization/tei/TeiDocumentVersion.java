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
package de.catma.serialization.tei;



/**
 * Created by IntelliJ IDEA.
 * User: mp
 * Date: 08.05.2010
 * Time: 11:43:25
 * To change this template use File | Settings | File Templates.
 */
public enum TeiDocumentVersion {
	V4(4, V4TeiDocumentConverter.class),
	V3(3, V4, V3TeiDocumentConverter.class),
    V2(2, V3, V2TeiDocumentConverter.class),
    V1(1, V2),
    ;

	private static final String TARGET_INFO_XML_ID = "TARGET_INFO"; //legacy
    private int version;
    private TeiDocumentVersion nextVersion;
    private Class<? extends TeiDocumentConverter> converterClazz;

    private TeiDocumentVersion(int version, Class<? extends TeiDocumentConverter> converterClazz) {
        this(version, null, converterClazz);
    }

    private TeiDocumentVersion(
            int version,
            TeiDocumentVersion nextVersion ) {
        this(version, nextVersion, null);
    }
    private TeiDocumentVersion(
            int version,
            TeiDocumentVersion nextVersion,
            Class<? extends TeiDocumentConverter> converterClazz) {
        
        this.version = version;
        this.nextVersion = nextVersion;
        this.converterClazz = converterClazz;
    }

    private void convertToNextVersion(TeiDocument teiDocument) {
        if (nextVersion != null) {
			try {
			 	TeiDocumentConverter converter = nextVersion.converterClazz.newInstance();
	            converter.convert(teiDocument);
			} catch (Exception e) {
				throw new RuntimeException(e); // TODO: better use non generic exception
			}
        }
    }

    public boolean isCurrentVersion() {
        return (nextVersion == null);
    }

    public static TeiDocumentVersion getVersion(int version) {
        for (TeiDocumentVersion tdv : values() ) {
            if (tdv.version == version) {
                return tdv;
            }
        }

        throw new IllegalArgumentException( 
        		TeiDocumentVersion.class.getSimpleName() + " " + version + " unknown!");
    }

    public static void convertToLatest(TeiDocument teiDocument) {
    	
    	TeiElement oldTargetInfo = teiDocument.getElementByID(TARGET_INFO_XML_ID);
    	if ((oldTargetInfo != null) && oldTargetInfo.parentIs(TeiElementName.ab) 
    			&& (oldTargetInfo.getTeiElementParent() != null) 
    			&& (oldTargetInfo.getTeiElementParent().parentIs(TeiElementName.sourceDesc))) {
    		oldTargetInfo.getAttribute( 
    				Attribute.xmlid.getLocalName(), 
    				Attribute.xmlid.getNamespaceURI() ).setValue(TechnicalDescription.CATMA_TECH_DESC_XML_ID);
    	}
    	teiDocument.loadHeader();
    	
        while(!teiDocument.getTeiHeader().getTechnicalDescription().getVersion().isCurrentVersion()) {
        	teiDocument.getTeiHeader().getTechnicalDescription().getVersion().convertToNextVersion(
                    teiDocument);
        }
    }


    public static TeiDocumentVersion getCurrentVersion() {
        return V4;
    }

    @Override
    public String toString() {
        return String.valueOf(version);
    }
}
