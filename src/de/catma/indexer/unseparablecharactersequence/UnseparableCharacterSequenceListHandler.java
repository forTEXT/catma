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

package de.catma.indexer.unseparablecharactersequence;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Nodes;
import nu.xom.ParsingException;
import nu.xom.Serializer;
import nu.xom.XPathContext;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles file based lists of unseparable character sequences (USC).
 *
 * @author Marco Petris
 *
 */
public class UnseparableCharacterSequenceListHandler {
    /**
     * xml fields
     */
    public static enum Field {
        /**
         * container for the sequences
         */
        UnseparableCharacterSequences,
        /**
         * a single sequence
         */
        string,
        ;
    }

    /**
     * Load a list of USCs from the file with the given name.
     * @param fullFilePath the file name
     * @return the list of USCs
     * @throws IOException file access problems
     * @throws ParsingException file format problems
     */
    public List<String> load(String fullFilePath) throws IOException, ParsingException {
        List<String> result = new ArrayList<String>();

        Document doc = loadDoc(fullFilePath);

        Nodes nodes = getNodeList(doc, Field.string);

        for (int idx=0; idx<nodes.size(); idx++) {
            result.add(nodes.get(idx).getValue());
        }

        return result;
    }

    /**
     * Saves the given list of USCs in the file with the given name.
     * @param uscList the list of USCs
     * @param fullFilePath the file to save in
     * @throws IOException file access problems
     */
    public void write(List<String> uscList, String fullFilePath) throws IOException {
        Document document = loadDefaultDoc();

        Node uscNode = getMandatorySingleNode(document, Field.UnseparableCharacterSequences);
        TeiElement vCollElement = new TeiElement(TeiElementName.vColl);
        nu.xom.Attribute orgAttr =
                new nu.xom.Attribute(
                        Attribute.vColl_org.getLocalName(),
                        AttributeValue.org_list.getValueName());
        vCollElement.addAttribute(orgAttr);
        ((Element)uscNode).appendChild(vCollElement);
        for (String value : uscList) {
            TeiElement valueElement = new TeiElement( TeiElementName.string );
            vCollElement.appendChild( valueElement );
            valueElement.appendChild( value );
        }

        File uscListFile = new File(fullFilePath);
        if (!uscListFile.exists()) {
            if (!uscListFile.createNewFile()) {
                throw new IOException("could not create file " + fullFilePath);
            }
        }

        // TODO: the writing of xml files via the serialize should be externalized to an extra class
        // because it appears already in various places

        FileOutputStream fos = null;
        try  {
            fos = new FileOutputStream( uscListFile );
            fos.write( FileManager.UTF_8_BOM ); // some jdks do not write it on their own
            Serializer serializer = new Serializer( fos );
            serializer.setIndent( 4 );
            serializer.write(document);
        }
        finally {
            if( fos != null ) {
                fos.close();
            }
        }
    }

    // TODO: xml navigation methods here and in GroupManager should be combined!!!

    private Node getMandatorySingleNode(Document rawXmlDoc, Enum<?> field) {
        Node node = getSingleNode(rawXmlDoc, field);

        if (node == null) {
            throw new IllegalStateException("Node '" + field + "' is mandatory!");
        }

        return node;
    }

    private Nodes getNodeList(Document rawXmlDoc, Enum<?> field) {
        return rawXmlDoc.query(
                "//" + TeiElement.TEINAMESPACEPREFIX + ":" + field.name(),
                new XPathContext(
                    TeiElement.TEINAMESPACEPREFIX, TeiElement.TEINAMESPACE) );
    }


    private Node getSingleNode(Document rawXmlDoc, Enum<?> field) {
        Nodes nodes = rawXmlDoc.query("//" + field.name() );

        if(nodes.size() > 0) {
            return nodes.get(0);
        }

        return null;
    }

    private Document loadDoc(String fullFilePath) throws ParsingException, IOException {
        FileInputStream fr = null;

        try {
            fr =
                new FileInputStream( fullFilePath );

            Builder parser = new Builder();

            return parser.build( fr );
        }
        finally {
            if( fr != null ) {
                fr.close();
            }
        }
    }

    private Document loadDefaultDoc() throws IOException {
        InputStream stream = null;
        try {
            try {
                stream =
                    this.getClass().getClassLoader().getResourceAsStream(
                        "org/catma/indexer/unseparablecharactersequence/DefaultUSCListV1.xml");
                Builder parser = new Builder();
                return parser.build( stream );
            }
            catch(ParsingException pe) {
                throw new IOException(pe);
            }
        }
        finally {
            if (stream != null) {
                stream.close();
            }
        }

    }
}
