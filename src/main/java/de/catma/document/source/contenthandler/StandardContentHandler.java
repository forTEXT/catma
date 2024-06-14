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
package de.catma.document.source.contenthandler;

import java.io.*;
import java.nio.charset.Charset;

/**
 * The standard content handler that handles plain text files.
 *
 * @see de.catma.document.source.TechInfoSet
 */
public class StandardContentHandler extends AbstractSourceContentHandler {
	private static final int KB64 = 65536;

	private void load(BufferedInputStream bufferedInputStream) throws IOException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		byte[] inputByteBuffer = new byte[KB64];
		int bytesRead;
		while ((bytesRead = bufferedInputStream.read(inputByteBuffer)) != -1) {
			byteArrayOutputStream.write(inputByteBuffer, 0, bytesRead);
		}

		byte[] allBytes = byteArrayOutputStream.toByteArray();
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(allBytes);

		Charset charset = getSourceDocumentInfo().getTechInfoSet().getCharset();

		InputStream conditionallyFilteredInputStream;
		if (BOMFilterInputStream.hasBOM(allBytes)) {
			conditionallyFilteredInputStream = new BOMFilterInputStream(byteArrayInputStream, charset);
		}
		else {
			conditionallyFilteredInputStream = byteArrayInputStream;
		}

		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(conditionallyFilteredInputStream, charset));
		char[] charBuffer = new char[KB64];
		int charsRead;
		StringBuilder contentBuilder = new StringBuilder();
		while ((charsRead = bufferedReader.read(charBuffer)) != -1) {
			contentBuilder.append(charBuffer, 0, charsRead);
		}

		setContent(
				// some texts seem to include invalid unicode characters and this causes problems when converting text to HTML for GUI delivery and during
				// indexing
				contentBuilder.toString().replaceAll("[^\\x09\\x0A\\x0D\\x20-\\uD7FF\\uE000-\\uFFFD\\u10000-\\u10FFFF]", "?")
		);
	}

	@Override
	public void load() throws IOException {
		try (BufferedInputStream bufferedInputStream = new BufferedInputStream(getSourceDocumentInfo().getTechInfoSet().getURI().toURL().openStream())) {
			load(bufferedInputStream);
		}
	}

	@SuppressWarnings("unused")
	private void showBytes(File file, int byteCount) {
		try (FileInputStream fileInputStream = new FileInputStream(file)) {
			for (int i=0; i<byteCount; i++) {
				System.out.printf("%1$x\n", fileInputStream.read());
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
