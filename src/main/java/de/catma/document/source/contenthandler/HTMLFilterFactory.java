/*
 * CATMA Computer Aided Text Markup and Analysis
 *
 *    Copyright (C) 2008-2010  University Of Hamburg
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.catma.document.source.contenthandler;

import nu.xom.Element;
import nu.xom.NodeFactory;
import nu.xom.Nodes;

/**
 * A node factory that filters certain HTML elements.
 *
 * @author marco.petris@web.de
 *
 * @see HTMLContentHandler
 */
public class HTMLFilterFactory extends NodeFactory {

    private static final String SCRIPT = "script";
    private static final String STYLE = "style";
    
    private Nodes emptyNodes = new Nodes();
    private int inScriptMode = 0;
	private int inStyleMode = 0;

    /* (non-Javadoc)
     * @see nu.xom.NodeFactory#makeComment(java.lang.String)
     */
    @Override
    public Nodes makeComment(String data) {
        return emptyNodes;
    }

    /* (non-Javadoc)
     * @see nu.xom.NodeFactory#startMakingElement(java.lang.String, java.lang.String)
     */
    @Override
    public Element startMakingElement(String name, String namespace) {
        if(name.toLowerCase().equals(SCRIPT)) {
            inScriptMode++;
        }
        else if (name.toLowerCase().equals(STYLE)) {
        	inStyleMode ++;
        }
        return super.startMakingElement(name, namespace);
    }

    /* (non-Javadoc)
     * @see nu.xom.NodeFactory#finishMakingElement(nu.xom.Element)
     */
    @Override
    public Nodes finishMakingElement(Element element) {
        if (element.getQualifiedName().toLowerCase().equals(SCRIPT)) {
            inScriptMode--;
            if (inScriptMode<0) {
                inScriptMode=0;
            }
        }
        else if (element.getQualifiedName().toLowerCase().equals(STYLE)) {
            inStyleMode--;
            if (inStyleMode<0) {
            	inStyleMode=0;
            }
        }
        return super.finishMakingElement(element);
    }

    /* (non-Javadoc)
     * @see nu.xom.NodeFactory#makeText(java.lang.String)
     */
    @Override
    public Nodes makeText(String data) {
        if((inScriptMode>0) || (inStyleMode>0)) {
            return emptyNodes;
        }
        return super.makeText(data);
    }
}
