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

import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;

/**
 * A content handler for PDF based {@link de.catma.document.source.SourceDocument}s.
 *
 * @author Marco Petris
 *
 */
public class PDFContentHandler extends AbstractSourceContentHandler {

    private String content;

    public void load() throws IOException {
        PDDocument document = null;
        try {
            document = PDDocument.load(getSourceDocumentInfo().getURI().toURL(), true);

            if (document.isEncrypted()) {
                throw new IOException("can not open pdf document because it is encrypted");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            content = stripper.getText(document);
        }
        finally {
            if (document != null) {
				document.close();
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
