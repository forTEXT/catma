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

import nu.xom.Attribute;
import nu.xom.Node;
import nu.xom.Nodes;

/**
 * Created by IntelliJ IDEA.
 * User: mp
 * Date: 08.05.2010
 * Time: 12:46:41
 * To change this template use File | Settings | File Templates.
 */
public class V2TeiDocumentConverter implements TeiDocumentConverter {

    public void convert(TeiDocument teiDocument) {

        Nodes fsNodes = teiDocument.getNodes(TeiElementName.fs);

        for( int i=0; i<fsNodes.size(); i++ ) {
            Node curNode = fsNodes.get(i);
            if( curNode instanceof TeiElement ) {
                TeiElement curElement = (TeiElement)curNode;
                if( !curElement.getID().equals( "TARGET_INFO" )
                        && (curElement.getID().startsWith("CATMA"))) {

                    // extract name info
                    Attribute attributeType = curElement.getAttribute( "type" );
                    if (attributeType != null) {
	                    String oldType = attributeType.getValue();
	
	                    // make new name "property"
	                    TeiElement fElement = new TeiElement( TeiElementName.f );
	                    fElement.addAttribute( TeiDocument.getNewXmlIDAttribute() );
	
	                    nu.xom.Attribute nameAttr =
	                        new nu.xom.Attribute(
	                        		de.catma.serialization.tei.Attribute.f_name.getLocalName(), "catma_tagname" );
	                    fElement.addAttribute( nameAttr );
	                    TeiElement stringElement = new TeiElement( TeiElementName.string );
	                    fElement.appendChild( stringElement );
	                    stringElement.appendChild( oldType );
	                    curElement.appendChild( fElement );
	
	                    // set type name
	                    attributeType.setValue("catma_tag");
                    }
                }
            }
        }
        
        Nodes fNodes =
        		teiDocument.getNodes(TeiElementName.f);
        
        for( int i=0; i<fNodes.size(); i++ ) {
            Node curNode = fNodes.get(i);
            if( curNode instanceof TeiElement ) {
                TeiElement curElement = (TeiElement)curNode;
                String id = curElement.getID();
                if( ( id != null ) && id.startsWith("CATMA") ) {
                    // extract name info
                    Attribute attributeName = curElement.getAttribute( "name" );
                    String name = attributeName.getValue();

                    if(name.equals("displaycolor")) {
                        attributeName.setValue("catma_displaycolor");
                    }
                }
            }
        }

        teiDocument.getTeiHeader().getTechnicalDescription().setVersion(TeiDocumentVersion.V2);
    }

}
