/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2012  University Of Hamburg
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
package de.catma.ui.tagger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.UI;

import de.catma.document.Range;
import de.catma.document.repository.Repository;
import de.catma.document.standoffmarkup.usermarkup.Annotation;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.tag.TagDefinition;
import de.catma.ui.CatmaApplication;
import de.catma.ui.client.ui.tagger.TaggerClientRpc;
import de.catma.ui.client.ui.tagger.TaggerServerRpc;
import de.catma.ui.client.ui.tagger.shared.ClientTagDefinition;
import de.catma.ui.client.ui.tagger.shared.ClientTagInstance;
import de.catma.ui.client.ui.tagger.shared.TaggerState;
import de.catma.ui.client.ui.tagger.shared.TextRange;
import de.catma.ui.tagger.pager.Page;
import de.catma.ui.tagger.pager.Pager;
import de.catma.util.ColorConverter;
import de.catma.util.Pair;

/**
 * @author marco.petris@web.de
 *
 */
public class Tagger extends AbstractComponent {
	
	public static interface TaggerListener {
		public void tagInstanceAdded(ClientTagInstance clientTagInstance);
		public void tagInstanceSelected(String instancePartID, String lineID);
		public void tagInstanceSelected(Set<String> tagInstanceIDs);
		public Annotation getTagInstanceInfo(String tagInstanceId);
	}
	
	private static final long serialVersionUID = 1L;
	
	private TaggerServerRpc rpc = new TaggerServerRpc() {
		
		@Override
		public void tagInstanceSelected(String instanceIDLineIDJson) {
			try {
				Pair<String,String> instancePartIDLineID =
					tagInstanceJSONSerializer.fromInstanceIDLineIDJSONArray(instanceIDLineIDJson);
				
				taggerListener.tagInstanceSelected(
						instancePartIDLineID.getFirst(), instancePartIDLineID.getSecond());
				
			} catch (IOException e) {
				((CatmaApplication)UI.getCurrent()).showAndLogError(
					Messages.getString("Tagger.errorDisplayingAnnotationInstanceInfo"), e); //$NON-NLS-1$
			}
		}
		
		@Override
		public void tagInstancesSelected(String tagInstanceIDsJson) {
			try {
				Set<String> tagInstanceIDs = tagInstanceJSONSerializer.fromInstanceIDsArray(tagInstanceIDsJson);
				taggerListener.tagInstanceSelected(tagInstanceIDs);
			
			} catch (IOException e) {
				((CatmaApplication)UI.getCurrent()).showAndLogError(
					Messages.getString("Tagger.errorDisplayingAnnotationInstancesInfo"), e); //$NON-NLS-1$
			}
			
		}
		
		@Override
		public void tagInstanceAdded(String tagInstanceJson) {
			try {
				ClientTagInstance tagInstance = 
						tagInstanceJSONSerializer.fromJSON(tagInstanceJson);
				
				pager.getCurrentPage().addRelativeTagInstance(tagInstance);
				taggerListener.tagInstanceAdded(
						pager.getCurrentPage().getAbsoluteTagInstance(tagInstance));
			} catch (IOException e) {
				((CatmaApplication)UI.getCurrent()).showAndLogError(
					Messages.getString("Tagger.errorAddingAnnotation"), e); //$NON-NLS-1$
			}
		}
		
		@Override
		public void log(String msg) {
			System.out.println(
					"Got log message from client: " +  msg); //$NON-NLS-1$
		}
	};

	private Pager pager;
	private TaggerListener taggerListener;
	private ClientTagInstanceJSONSerializer tagInstanceJSONSerializer;
	private TagInstanceInfoHTMLSerializer tagInstanceInfoHTMLSerializer;
	private String taggerID;
	private Repository project;

	public Tagger(int taggerID, Pager pager, TaggerListener taggerListener, Repository project) {
		registerRpc(rpc);
		this.pager = pager;
		this.taggerListener = taggerListener;
		this.project = project;
		this.tagInstanceJSONSerializer = new ClientTagInstanceJSONSerializer();
		this.tagInstanceInfoHTMLSerializer = new TagInstanceInfoHTMLSerializer(project);
		this.taggerID = String.valueOf(taggerID);
		getRpcProxy(TaggerClientRpc.class).setTaggerId(this.taggerID);
		getState().tagInstanceIdToTooltipInfo = new HashMap<>();
	}
	
	public void updateAnnotation(String annotationId) {
		Annotation annotation = 
				taggerListener.getTagInstanceInfo(annotationId);
		getState().tagInstanceIdToTooltipInfo.put(
			annotationId, 
			tagInstanceInfoHTMLSerializer.toHTML(annotation));
	}
	
	@Override
	public void beforeClientResponse(boolean initial) {
		super.beforeClientResponse(initial);
		if (initial) {
			setPage(pager.getCurrentPageNumber());
		}
	}

	private void setPage(String pageContent, int lineCount) {
		getRpcProxy(TaggerClientRpc.class).setTaggerId(this.taggerID);
		getRpcProxy(TaggerClientRpc.class).setPage(
				pageContent, 
				lineCount);
	}
	
	public void setText(String text) {
		pager.setText(text);
	}
	
	public void setPage(int pageNumber) {
		Page page = pager.getPage(pageNumber);
		setPage(page.toHTML(), page.getLineCount());
	}
	
	void removeTagInstances(Collection<String> annotationIds) {
		for (String annotationId : annotationIds) {
			for (Page page : pager.getPagesForAnnotationId(annotationId)) {
				page.removeRelativeTagInstance(annotationId);
			}
			getState().tagInstanceIdToTooltipInfo.remove(annotationId);
		}
		if (pager.getCurrentPage().isDirty()) {
			setPage(pager.getCurrentPage().toHTML(), pager.getCurrentPage().getLineCount());
		}
	}
	
	
	void setTagInstancesVisible(
			Collection<ClientTagInstance> tagInstances, boolean visible) {
				
		for (ClientTagInstance ti : tagInstances) {
			List<Page> pages = pager.getPagesForAbsoluteTagInstance(ti);
			if (!pages.isEmpty()) {
				if (visible) {
					for (Page page : pages) {
						page.addAbsoluteTagInstance(ti);
					}
					Annotation tagInstanceInfo = 
							taggerListener.getTagInstanceInfo(ti.getInstanceID());
					getState().tagInstanceIdToTooltipInfo.put(
						ti.getInstanceID(), 
						tagInstanceInfoHTMLSerializer.toHTML(tagInstanceInfo));
				}
				else {
					for (Page page : pages) {
						page.removeRelativeTagInstance(ti.getInstanceID());
					}
					getState().tagInstanceIdToTooltipInfo.remove(ti.getInstanceID());
				}
			}	
		}
		if (pager.getCurrentPage().isDirty()) {
			setPage(pager.getCurrentPage().toHTML(), pager.getCurrentPage().getLineCount());
		}
	}

	public void addTagInstanceWith(TagDefinition tagDefinition) {
		try {
			getRpcProxy(TaggerClientRpc.class).addTagInstanceWith(
				new ClientTagDefinitionJSONSerializer().toJSON(
						new ClientTagDefinition(
							tagDefinition.getUuid(),
							ColorConverter.toHex(tagDefinition.getColor()))));
		} catch (IOException e) {
			((CatmaApplication)UI.getCurrent()).showAndLogError(
					Messages.getString("Tagger.errorAddingAnnotation"), e); //$NON-NLS-1$
		}
	}

	public void setVisible(List<TagReference> tagReferences, boolean visible) {
		Map<String, ClientTagInstance> tagInstancesByInstanceID = new HashMap<String, ClientTagInstance>();
		
		for (TagReference tagReference : tagReferences) {
			List<TextRange> textRanges = new ArrayList<TextRange>();
			textRanges.add(
					new TextRange(
							tagReference.getRange().getStartPoint(), 
							tagReference.getRange().getEndPoint()));
			ClientTagInstance tagInstance = 
					tagInstancesByInstanceID.get(tagReference.getTagInstanceID());
			if (tagInstance == null) {
				TagDefinition tagDefintion = 
					project.getTagManager().getTagLibrary().getTagDefinition(tagReference.getTagDefinitionId());
				
				tagInstancesByInstanceID.put(
						tagReference.getTagInstanceID(),
						new ClientTagInstance(
								tagReference.getTagDefinitionId(),
								tagReference.getTagInstanceID(), 
								ColorConverter.toHex(tagDefintion.getColor()), 
								textRanges));
			}
			else {
				tagInstance.addRanges(textRanges);
			}
		}
		setTagInstancesVisible(tagInstancesByInstanceID.values(), visible);
	}

	public void highlight(Range absoluteRange) {
		pager.highlight(absoluteRange);
	}

	public void setTagInstanceSelected(String annotationId) {
		getRpcProxy(TaggerClientRpc.class).setTagInstanceSelected(
				annotationId==null?"":annotationId);//$NON-NLS-1$
	}

	public void setTraceSelection(Boolean traceSelection) {
		getRpcProxy(TaggerClientRpc.class).setTraceSelection(traceSelection);
	}

	public void removeHighlights() {
		pager.removeHighlights();
		getRpcProxy(TaggerClientRpc.class).removeHighlights();
	}
	
	@Override
	protected TaggerState getState() {
		return (TaggerState)super.getState();
	}	
}
