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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import de.catma.document.Range;
import de.catma.document.source.FileType;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.tag.Property;
import de.catma.tag.TagInstance;

public class TeiUserMarkupCollectionSerializer {
	
	private TeiDocument teiDocument;
	
	public TeiUserMarkupCollectionSerializer(TeiDocument teiDocument) {
		super();
		this.teiDocument = teiDocument;
	}
	
	
	public void serialize(
			UserMarkupCollection userMarkupCollection, 
			SourceDocument sourceDocument) throws IOException {
		
		String targetURI = makeTargetURI(sourceDocument);
		
		if (userMarkupCollection.isEmpty()) {
			return;
		}
		
		TeiElement textElement = 
				(TeiElement)teiDocument.getNodes(TeiElementName.text).get(0);
		TeiElement abElement =
				(TeiElement)teiDocument.getNodes(
						TeiElementName.ab, AttributeValue.type_catma).get(0);
		
		Set<String> addedTagInstances = new HashSet<String>();
		
		HashMap<Range, List<TagReference>> groupByRange = 
				new HashMap<Range, List<TagReference>>();
		
		for (TagReference tr : userMarkupCollection.getTagReferences()) {
			if (!groupByRange.containsKey(tr.getRange())) {
				groupByRange.put(tr.getRange(), new ArrayList<TagReference>());
			}
			groupByRange.get(tr.getRange()).add(tr);
		}
		
		TreeSet<Range> sortedRanges = new TreeSet<Range>();
		sortedRanges.addAll(groupByRange.keySet());
		
		Range lastRange = null;
		for (Range range : sortedRanges) {
			if ((lastRange == null) && (range.getStartPoint() != 0)) {
				writePointer(
						targetURI,
						new Range(0, range.getStartPoint()), abElement);
			}
			else if ((lastRange != null) && (lastRange.getEndPoint() != range.getStartPoint())) {
				writePointer(
						targetURI,
						new Range(lastRange.getEndPoint(), range.getStartPoint()),
						abElement);
			}
			List<TagReference> currentReferences = groupByRange.get(range);
			TeiElement seg = writeSegment(
				currentReferences, abElement, 
				textElement, addedTagInstances);
			
			writePointer(
				targetURI, range, seg);
			lastRange = range;
		}
		

		if (lastRange.getEndPoint() != sourceDocument.getLength()) {
			writePointer(
				targetURI, 
				new Range(lastRange.getEndPoint(), sourceDocument.getLength()), 
				abElement);
		}

	}

	private TeiElement writeSegment(
		List<TagReference> tagReferences, TeiElement abElement, 
		TeiElement textElement, Set<String> addedTagInstances) {
		
		List<TagInstance> tagInstances = new ArrayList<TagInstance>();
		
		for (TagReference tr : tagReferences) {
			if (!addedTagInstances.contains(tr.getTagInstanceID())) {
				writeTagInstance(tr.getTagInstance(), textElement);
				addedTagInstances.add(tr.getTagInstanceID());
			}
			tagInstances.add(tr.getTagInstance());
		}
		
		AnaValueHandler anaValueHandler = new AnaValueHandler();
		
		TeiElement seg = new TeiElement(TeiElementName.seg);
		seg.setAttributeValue(
			Attribute.ana, anaValueHandler.makeValueFrom(tagInstances));
		abElement.appendChild(seg);
		return seg;
	}


	private void writeTagInstance(TagInstance tagInstance,
			TeiElement textElement) {
		
		TeiElement fs = new TeiElement(TeiElementName.fs);
		textElement.appendChild(fs);
		
		fs.setID(tagInstance.getUuid());
		fs.setAttributeValue(
			Attribute.type, tagInstance.getTagDefinition().getUuid());
		for (Property p : tagInstance.getSystemProperties()) {
			writeProperty(p, fs);
		}
		
		for (Property p : tagInstance.getUserDefinedProperties()) {
			writeProperty(p,fs);
		}
		
	}

	private void writeProperty(Property property, TeiElement fs) {
		TeiElement f = new TeiElement(TeiElementName.f);
		fs.appendChild(f);
		f.setAttributeValue(Attribute.f_name, property.getName());
		TeiElement string = new TeiElement(TeiElementName.string);
		string.appendChild(property.getPropertyValueList().getFirstValue()); //TODO: support list of values
		f.appendChild(string);
	}

	private String makeTargetURI(SourceDocument sourceDocument) {
		if ((!sourceDocument.getSourceContentHandler().getSourceDocumentInfo().
				getTechInfoSet().getFileType().equals(FileType.TEXT)) 
			&& (sourceDocument.getID().startsWith("http"))) {
			return sourceDocument.getID().replaceFirst("http", "catma");
			
		}
		return sourceDocument.getID();
	}

	private void writePointer(String uri, Range range, TeiElement abElement) {
		TeiElement ptr = new TeiElement(TeiElementName.ptr);
		PtrValueHandler ptrValueHandler = new PtrValueHandler();
		
		ptr.setAttributeValue(
			Attribute.ptr_target, ptrValueHandler.makeTargetFrom(range, uri));
		
		ptr.setAttributeValue(
			Attribute.type, AttributeValue.type_inclusion.getValueName());
		
		abElement.appendChild(ptr);
	}
}
