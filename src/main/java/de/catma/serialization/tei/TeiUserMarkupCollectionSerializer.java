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
import de.catma.document.annotation.AnnotationCollection;
import de.catma.document.annotation.TagReference;
import de.catma.document.source.FileType;
import de.catma.document.source.SourceDocument;
import de.catma.tag.Property;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;

public class TeiUserMarkupCollectionSerializer {
	
	private TeiDocument teiDocument;
	private boolean withText;
	
	public TeiUserMarkupCollectionSerializer(TeiDocument teiDocument, boolean withText) {
		super();
		this.teiDocument = teiDocument;
		this.withText = withText;
	}
	
	
	public void serialize(
			AnnotationCollection userMarkupCollection, 
			SourceDocument sourceDocument) throws IOException {
		
		String targetURI = makeTargetURI(sourceDocument);
		
		if (userMarkupCollection.isEmpty()) {
			return;
		}
		
		TeiElement textElement = 
				(TeiElement)teiDocument.getNodes(TeiElementName.text).get(0);
		
		TeiElement ptrParentElement =
				(TeiElement)teiDocument.getNodes(
						TeiElementName.ab, AttributeValue.type_catma).get(0);

		Set<String> addedTagInstances = new HashSet<String>();
		
		HashMap<Range, List<TagReference>> mergedTagReferences = 
			mergeTagReferences(
				userMarkupCollection.getTagReferences(), 
				new Range(0, sourceDocument.getLength()));
		
		TreeSet<Range> sortedRanges = new TreeSet<Range>();
		sortedRanges.addAll(mergedTagReferences.keySet());
		
		
		for (Range range : sortedRanges) {
			List<TagReference> currentReferences = mergedTagReferences.get(range);
			
			TeiElement parent = ptrParentElement; 
			if (!currentReferences.isEmpty()) {
				parent = writeSegment(
						currentReferences, ptrParentElement, 
						textElement, addedTagInstances, 
						userMarkupCollection.getTagLibrary());
			}
			writeText(
				targetURI, range, parent, sourceDocument);
		}
	}

	private HashMap<Range, List<TagReference>> mergeTagReferences(
			List<TagReference> tagReferences, Range initialRange) {
		
		HashMap<Range, List<TagReference>> mergedTagReferences = 
				new HashMap<Range, List<TagReference>>();
		
		mergedTagReferences.put(initialRange, new ArrayList<TagReference>());
		
		for (TagReference tagReference : tagReferences) {
			Range targetRange = tagReference.getRange();
			List<Range> affectedRanges = 
					getAffectedRanges(mergedTagReferences.keySet(), targetRange);
			
			for (Range affectedRange : affectedRanges) {
				
				if (affectedRange.isInBetween(targetRange)) {
					mergedTagReferences.get(affectedRange).add(tagReference);
				}
				else {
					List<TagReference> existingReferences = 
							mergedTagReferences.get(affectedRange);

					Range overlappingRange = affectedRange.getOverlappingRange(targetRange);
					
					List<Range> disjointRanges = affectedRange.getDisjointRanges(targetRange);

					// range outside of the overlapping range
					// left or right depending on the position of the overlapping range
					Range firstDisjointRange = disjointRanges.get( 0 );
					List<TagReference> firstCopy = new ArrayList<TagReference>();
					firstCopy.addAll(existingReferences);
					mergedTagReferences.put(
							firstDisjointRange, firstCopy);
					
					// the overlapping range sits in the middle
					if( disjointRanges.size() == 2 ) {
						// range right of the overlappting range
						Range secondDisjointRange = disjointRanges.get( 1 );

						List<TagReference> secondCopy = new ArrayList<TagReference>();
						secondCopy.addAll(existingReferences);
						
						mergedTagReferences.put(
								secondDisjointRange, secondCopy);
					}
					
					existingReferences.add(tagReference);
					mergedTagReferences.put(overlappingRange, existingReferences);
					
					mergedTagReferences.remove(affectedRange);
					
				}
			}
		}			
		
		return mergedTagReferences;
	}


	private List<Range> getAffectedRanges(
			Set<Range> documentRanges,
			Range targetRange) {
		Set<Range> sortedDocumentRanges = new TreeSet<Range>();
		sortedDocumentRanges.addAll(documentRanges);
		
		List<Range> affectedRanges = new ArrayList<Range>();
		
		for (Range docRange : sortedDocumentRanges) {
			if (docRange.hasOverlappingRange(targetRange)) {
				affectedRanges.add(docRange);
			}
		}
		
		return affectedRanges;
	}


	private TeiElement writeSegment(
		List<TagReference> tagReferences, TeiElement abElement, 
		TeiElement textElement, Set<String> addedTagInstances, TagLibrary tagLibrary) {
		
		List<TagInstance> tagInstances = new ArrayList<TagInstance>();
		
		for (TagReference tr : tagReferences) {
			if (!addedTagInstances.contains(tr.getTagInstanceId())) {
				writeTagInstance(tr.getTagInstance(), textElement, tagLibrary);
				addedTagInstances.add(tr.getTagInstanceId());
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
			TeiElement textElement, TagLibrary tagLibrary) {
		
		TeiElement fs = new TeiElement(TeiElementName.fs);
		textElement.appendChild(fs);
		
		fs.setID(tagInstance.getUuid());
		fs.setAttributeValue(
			Attribute.type, tagInstance.getTagDefinitionId());
		
		writeProperty(
			PropertyDefinition.SystemPropertyName.catma_markupauthor.name(),
			tagInstance.getAuthor(), fs);
		writeProperty(
			PropertyDefinition.SystemPropertyName.catma_markuptimestamp.name(),
			tagInstance.getTimestamp(), fs);
		writeProperty(
			PropertyDefinition.SystemPropertyName.catma_displaycolor.name(), 
			tagLibrary.getTagDefinition(tagInstance.getTagDefinitionId()).getColor(), fs);
		
		for (Property p : tagInstance.getUserDefinedProperties()) {
			writeProperty(p, fs, tagInstance.getTagDefinitionId(), tagLibrary);
		}
		
	}
	
	private void writeText(String uri, Range range, 
			TeiElement parentElement, SourceDocument sourceDocument) throws IOException {
		if (withText) {
			parentElement.appendChild(sourceDocument.getContent(range));
		}
		else {
			writePointer(uri, range, parentElement);
		}
	}

	private void writeProperty(String propertyName, String value, TeiElement fs) {
		TeiElement f = new TeiElement(TeiElementName.f);
		fs.appendChild(f);
		
		f.setAttributeValue(Attribute.f_name, Validator.SINGLETON.convertToXMLName(propertyName));

		TeiElement string = new TeiElement(TeiElementName.string);
		string.appendChild(value);
		f.appendChild(string);
	}
	
	private void writeProperty(Property property, TeiElement fs, String tagDefinitionId, TagLibrary tagLibrary) {
		TeiElement f = new TeiElement(TeiElementName.f);
		fs.appendChild(f);
		
		String propertyName = 
			tagLibrary.getTagDefinition(tagDefinitionId).getPropertyDefinitionByUuid(property.getPropertyDefinitionId()).getName();
		f.setAttributeValue(Attribute.f_name, Validator.SINGLETON.convertToXMLName(propertyName));

		if (property.getPropertyValueList().size() > 1) {
			TeiElement vRange = new TeiElement(TeiElementName.vRange);
			f.appendChild(vRange);
			TeiElement vColl = new TeiElement(TeiElementName.vColl);
			vRange.appendChild(vColl);
			for (String val : property.getPropertyValueList()) {
				TeiElement string = new TeiElement(TeiElementName.string);
				vColl.appendChild(string);
				string.appendChild(val);
			}
			
		}
		else if (property.getPropertyValueList().size() == 1) {
			TeiElement string = new TeiElement(TeiElementName.string);
			string.appendChild(property.getFirstValue());
			f.appendChild(string);
		}
	}

	private String makeTargetURI(SourceDocument sourceDocument) {
		if ((!sourceDocument.getSourceContentHandler().getSourceDocumentInfo().
				getTechInfoSet().getFileType().equals(FileType.TEXT)) 
			&& (sourceDocument.getUuid().startsWith("http"))) {
			return sourceDocument.getUuid().replaceFirst("http", "catma");
			
		}
		return sourceDocument.getUuid();
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
