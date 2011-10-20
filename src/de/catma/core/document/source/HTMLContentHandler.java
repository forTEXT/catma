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

package de.catma.core.document.source;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Text;

import org.apache.commons.io.FileUtils;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * A content handler HTML based {@link org.catma.document.source.SourceDocument}s.
 *
 * @author Marco Petris
 *
 */
public class HTMLContentHandler implements SourceContentHandler {

    private String content;

    public long load(
    		SourceDocumentInfo sourceDocumentInfo,
            URI uri, ProgressListener progressListener) throws IOException {

        File file = new File(uri);

        if( progressListener != null ) {
            progressListener.setIndeterminate(true,
                "FileManager.loadingFile", file.getName() );
        }

        long checksum = FileUtils.checksumCRC32(file);

        try {
            XMLReader reader =
                XMLReaderFactory.createXMLReader("org.ccil.cowan.tagsoup.Parser");
            Builder builder = new Builder(reader, false, new HTMLFilterFactory());
            Document document = builder.build(file);
            StringBuilder contentBuilder = new StringBuilder();
            processTextNodes(contentBuilder, document.getRootElement());
            content = contentBuilder.toString();
        }
        catch (Exception e) {
            throw new IOException(e);
        }

        return checksum;
    }

    /**
     * Appends text elements to the given builder otherwise descents deeper into the
     * document tree.
     * @param contentBuilder the builder is filled with text elements
     * @param element the current element to process
     */
    private void processTextNodes(StringBuilder contentBuilder, Element element) {
        for( int idx=0; idx<element.getChildCount(); idx++) {
            Node curChild = element.getChild(idx);
            if (curChild instanceof Text) {
                contentBuilder.append(curChild.getValue());
            }
            else if (curChild instanceof Element) { //descent
                processTextNodes(contentBuilder, (Element)curChild);
            }
        }
    }

    public String getContent(long startPoint, long endPoint) {
        return content.substring((int)startPoint, (int)endPoint);
    }

    public String getContent(long startPoint) {
        return content.substring((int)startPoint);
    }

    public long getSize() {
        return content.length();
    }
}
