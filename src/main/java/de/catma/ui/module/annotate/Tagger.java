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
package de.catma.ui.module.annotate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.UI;

import de.catma.document.Range;
import de.catma.document.annotation.Annotation;
import de.catma.document.annotation.TagReference;
import de.catma.document.comment.Comment;
import de.catma.document.comment.Reply;
import de.catma.project.Project;
import de.catma.tag.TagDefinition;
import de.catma.ui.CatmaApplication;
import de.catma.ui.client.ui.tagger.TaggerClientRpc;
import de.catma.ui.client.ui.tagger.TaggerServerRpc;
import de.catma.ui.client.ui.tagger.shared.ClientComment;
import de.catma.ui.client.ui.tagger.shared.ClientTagDefinition;
import de.catma.ui.client.ui.tagger.shared.ClientTagInstance;
import de.catma.ui.client.ui.tagger.shared.TaggerState;
import de.catma.ui.client.ui.tagger.shared.TextRange;
import de.catma.ui.module.annotate.pager.Page;
import de.catma.ui.module.annotate.pager.Pager;
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
		public void contextMenuSelected(int x, int y);
		public void addComment(List<Range> ranges, int x, int y);
		public void editComment(Optional<Comment> optionalComment, int x, int y);
		public void removeComment(Optional<Comment> optionalComment);
		public void replyToComment(Optional<Comment> optionalComment, int x, int y);
		public void loadReplies(Optional<Comment> optionalComment);
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
					"Error displaying Annotation Instance information!", e); 
			}
		}
		
		@Override
		public void tagInstancesSelected(String tagInstanceIDsJson) {
			try {
				Set<String> tagInstanceIDs = tagInstanceJSONSerializer.fromInstanceIDsArray(tagInstanceIDsJson);
				taggerListener.tagInstanceSelected(tagInstanceIDs);
			
			} catch (IOException e) {
				((CatmaApplication)UI.getCurrent()).showAndLogError(
					"Error displaying Annotation Instances information!", e); 
			}
			
		}
		
		@Override
		public void tagInstanceAdded(String tagInstanceJson) {
			if (pager.hasPages()) {
				try {
					ClientTagInstance tagInstance = 
							tagInstanceJSONSerializer.fromJSON(tagInstanceJson);
					
					pager.getCurrentPage().addRelativeTagInstance(tagInstance);
					taggerListener.tagInstanceAdded(
							pager.getCurrentPage().getAbsoluteTagInstance(tagInstance));
				} catch (IOException e) {
					((CatmaApplication)UI.getCurrent()).showAndLogError(
						"Error adding Annotation!", e); 
				}
			}
		}
		
		@Override
		public void log(String msg) {
			System.out.println(
					"Got log message from client: " +  msg); //$NON-NLS-1$
		}
		
		@Override
		public void contextMenuSelected(int x, int y) {
			taggerListener.contextMenuSelected(x, y);
		}
		
		@Override
		public void addComment(String textRanges, int x, int y) {
			Page currentPage = pager.getCurrentPage();
			
			String[] textRangeSegments = textRanges.split(",");
			List<Range> ranges = new ArrayList<>();
			for (String textRangeSegment : textRangeSegments) {
				String[] positions = textRangeSegment.split(":");
				
				ranges.add(
					new Range(
						Integer.valueOf(positions[0])+currentPage.getPageStart(), 
						Integer.valueOf(positions[1])+currentPage.getPageStart()));
			}
			taggerListener.addComment(ranges, x, y);
		}
		
		@Override
		public void editComment(String uuid, int x, int y) {
			Optional<Comment> optionalComment = pager.getComment(uuid);
			
			taggerListener.editComment(optionalComment, x, y);
		}
		
		@Override
		public void removeComment(String uuid) {
			Optional<Comment> optionalComment = pager.getComment(uuid);
			
			taggerListener.removeComment(optionalComment);
		}
		
		@Override
		public void replyToComment(String uuid, int x, int y) {
			Optional<Comment> optionalComment = pager.getComment(uuid);

			taggerListener.replyToComment(optionalComment, x, y);
		}
		
		@Override
		public void loadReplies(String uuid) {
			Optional<Comment> optionalComment = pager.getComment(uuid);
			taggerListener.loadReplies(optionalComment);
		}
	};

	private Pager pager;
	private TaggerListener taggerListener;
	private ClientTagInstanceJSONSerializer tagInstanceJSONSerializer;
	private TagInstanceInfoHTMLSerializer tagInstanceInfoHTMLSerializer;
	private String taggerID;
	private Project project;

	private boolean forceClientRefresh = false;

	public Tagger(int taggerID, Pager pager, TaggerListener taggerListener, Project project) {
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
		if (annotation != null) {
			getState().tagInstanceIdToTooltipInfo.put(
				annotationId, 
				tagInstanceInfoHTMLSerializer.toHTML(annotation));
		}
		else {
			getState().tagInstanceIdToTooltipInfo.remove(annotationId);
		}
	}
	
	@Override
	public void beforeClientResponse(boolean initial) {
		super.beforeClientResponse(initial);
		if ((initial || forceClientRefresh) && pager.hasPages()) {
			forceClientRefresh = false;
			setPage(pager.getCurrentPageNumber());
		}
	}

	private void setPage(String pageContent, int lineCount, Collection<Comment> relativeComments) {
		String serializedComments = "";
		try {
			serializedComments = new ClientCommentJSONSerializer().toJSON(relativeComments);
		}
		catch (IOException e) {
			((CatmaApplication)UI.getCurrent()).showAndLogError(
					"Error serializing Comments!", e); 
		}
		
		getRpcProxy(TaggerClientRpc.class).setTaggerId(this.taggerID);
		getRpcProxy(TaggerClientRpc.class).setPage(
				pageContent, 
				lineCount,
				serializedComments);
	}
	
	public void setText(String text, Collection<Comment> comments) {
		this.forceClientRefresh  = true;
		pager.setText(text, comments);
	}
	
	public void setPage(int pageNumber) {
		Page page = pager.getPage(pageNumber);
		setPage(page.toHTML(), page.getLineCount(), page.getRelativeComments());
	}
	
	void removeTagInstances(Collection<String> annotationIds) {
		if (pager.hasPages()) {
			for (String annotationId : annotationIds) {
				for (Page page : pager.getPagesForAnnotationId(annotationId)) {
					page.removeRelativeTagInstance(annotationId);
				}
				getState().tagInstanceIdToTooltipInfo.remove(annotationId);
			}
			if (pager.getCurrentPage().isDirty()) {
				setPage(
					pager.getCurrentPage().toHTML(), 
					pager.getCurrentPage().getLineCount(), 
					pager.getCurrentPage().getRelativeComments());
			}
		}
	}
	
	
	void setTagInstancesVisible(
			Collection<ClientTagInstance> tagInstances, boolean visible) {
		if (pager.hasPages()) {
			for (ClientTagInstance ti : tagInstances) {
				List<Page> pages = pager.getPagesForAbsoluteTagInstance(ti);
				if (!pages.isEmpty()) {
					if (visible) {
						for (Page page : pages) {
							page.addAbsoluteTagInstance(ti);
						}
						Annotation tagInstanceInfo = 
								taggerListener.getTagInstanceInfo(ti.getInstanceID());
						if (tagInstanceInfo != null) {
							getState().tagInstanceIdToTooltipInfo.put(
								ti.getInstanceID(), 
								tagInstanceInfoHTMLSerializer.toHTML(tagInstanceInfo));
						}
						else {
							getState().tagInstanceIdToTooltipInfo.remove(ti.getInstanceID());
						}
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
				setPage(
					pager.getCurrentPage().toHTML(), 
					pager.getCurrentPage().getLineCount(), 
					pager.getCurrentPage().getRelativeComments());
			}
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
					"Error adding Annotation!", e); 
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
					tagInstancesByInstanceID.get(tagReference.getTagInstanceId());
			if (tagInstance == null) {
				TagDefinition tagDefinition = 
					project.getTagManager().getTagLibrary().getTagDefinition(tagReference.getTagDefinitionId());
				
				tagInstancesByInstanceID.put(
						tagReference.getTagInstanceId(),
						new ClientTagInstance(
								tagReference.getTagDefinitionId(),
								tagReference.getTagInstanceId(), 
								tagDefinition==null?"FFFFFF":ColorConverter.toHex(tagDefinition.getColor()), 
								textRanges));
			}
			else {
				tagInstance.addRanges(textRanges);
			}
		}
		setTagInstancesVisible(tagInstancesByInstanceID.values(), visible);
	}

	public void highlight(Range absoluteRange) {
		if (pager.hasPages()) {
			int firstLineId = pager.highlight(absoluteRange);
			setPage(
				pager.getCurrentPage().toHTML(), 
				pager.getCurrentPage().getLineCount(), 
				pager.getCurrentPage().getRelativeComments());
			if (firstLineId != -1) {
								
				if (pager.getCurrentPage().hasLine(pager.getCurrentPage().getLineCount()-1)) {
					getRpcProxy(TaggerClientRpc.class).scrollLineToVisible("LINE." + (pager.getCurrentPage().getLineCount()-1));
				}
				
				getRpcProxy(TaggerClientRpc.class).scrollLineToVisible("LINE." + firstLineId);
			}
		}
		
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

	public void addComment(Comment comment) throws IOException {
		ClientComment clientComment = pager.addComment(comment);
		
		if (clientComment != null) {
			getRpcProxy(TaggerClientRpc.class).addComment(
				new ClientCommentJSONSerializer().toJSON(clientComment));
		}
	}

	public void updateComment(Comment comment) {
		getRpcProxy(TaggerClientRpc.class).updateComment(comment.getUuid(), comment.getBody(), comment.getStartPos());
	}

	public void removeComment(Comment comment) {
		pager.removeComment(comment);
		getRpcProxy(TaggerClientRpc.class).removeComment(comment.getUuid(), comment.getStartPos());
	}

	public void setReplies(List<Reply> replies, Comment comment) throws IOException {
		getRpcProxy(TaggerClientRpc.class).setReplies(comment.getUuid(), comment.getStartPos(), 
				new CommentReplyJSONSerializer().toJSONArrayString(replies));
	}
}
