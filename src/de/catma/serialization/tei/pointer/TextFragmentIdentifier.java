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
package de.catma.serialization.tei.pointer;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import de.catma.document.Range;


public abstract class TextFragmentIdentifier {
	
	private Integer length;
	private String md5HexValue;
	private String mimeCharset;
	private Range range;
	
	protected TextFragmentIdentifier() {
		super();
	}

	public Integer getLength() {
		return length;
	}

	public void setLength(Integer length) {
		if (length!=null) {
			this.length = length;
		}
	}

	public String getMd5HexValue() {
		return md5HexValue;
	}

	public byte[] getMd5HexValueAsBytes() throws DecoderException {
		return Hex.decodeHex(getMd5HexValue().toCharArray());
	}
	
	public void setMd5HexValue(String md5HexValue) {
		if (md5HexValue!=null) {
			this.md5HexValue = md5HexValue;
		}
	}

	public String getMimeCharset() {
		return mimeCharset;
	}

	public void setMimeCharset(String mimeCharset) {
		if (mimeCharset!=null) {
			this.mimeCharset = mimeCharset;
		}
	}
	
	public Range getRange() {
		return range;
	}
	
	public void setRange(Range range) {
		this.range = range;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getTextScheme());
		
		if (getLength() != null) {
			builder.append(";length=");
			builder.append(getLength());
		}
		else if (getMd5HexValue() != null) {
			builder.append(";md5=");
			builder.append(getMd5HexValue());
		}
		
		if (getMimeCharset() != null) {
			builder.append(",");
			builder.append(getMimeCharset());
		}

		return builder.toString();
	}

	public abstract String getTextSchemeName();
	
	public String getTextScheme() {
		StringBuilder builder = new StringBuilder();
		builder.append(getTextSchemeName());
	
		if (range.getStartPoint() == range.getEndPoint()) {
			builder.append(range.getStartPoint());
		}
		else {
			builder.append(range.getStartPoint());
			builder.append(",");
			builder.append(range.getEndPoint());
		}
		return builder.toString();
	}

}
