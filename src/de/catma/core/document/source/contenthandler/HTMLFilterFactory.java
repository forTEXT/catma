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

package de.catma.core.document.source.contenthandler;

import nu.xom.Element;
import nu.xom.NodeFactory;
import nu.xom.Nodes;

/**
 * A node factory that filters certain HTML elements.
 *
 * @author Marco Petris
 *
 * @see HTMLContentHandler
 */
public class HTMLFilterFactory extends NodeFactory {

    private static final String SCRIPT = "script";

    private Nodes emptyNodes = new Nodes();
    private int inScriptMode = 0;

    @Override
    public Nodes makeComment(String data) {
        return emptyNodes;
    }

    @Override
    public Element startMakingElement(String name, String namespace) {
        if(name.toLowerCase().equals(SCRIPT)) {
            inScriptMode++;
        }
        return super.startMakingElement(name, namespace);
    }

    @Override
    public Nodes finishMakingElement(Element element) {
        if(element.getQualifiedName().toLowerCase().equals(SCRIPT)) {
            inScriptMode--;
            if (inScriptMode<0) {
                inScriptMode=0;
            }
        }
        return super.finishMakingElement(element);
    }

    @Override
    public Nodes makeText(String data) {
        if(inScriptMode>0) {
            return emptyNodes;
        }
        return super.makeText(data);
    }
}
