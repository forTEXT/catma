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
package de.catma.ui.client.ui.tagger;

import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;

import de.catma.ui.client.ui.tagger.comment.CommentPanel;
import de.catma.ui.client.ui.tagger.comment.CommentPanel.CommentPanelListener;
import de.catma.ui.client.ui.tagger.editor.Line;
import de.catma.ui.client.ui.tagger.editor.TaggerEditor;
import de.catma.ui.client.ui.tagger.editor.TaggerEditorListener;
import de.catma.ui.client.ui.tagger.shared.ClientComment;
import de.catma.ui.client.ui.tagger.shared.ClientCommentReply;
import de.catma.ui.client.ui.tagger.shared.ClientTagDefinition;
import de.catma.ui.client.ui.tagger.shared.ClientTagInstance;
import de.catma.ui.client.ui.tagger.shared.TextRange;


/**
 * @author marco.petris@web.de
 *
 */
public class VTagger extends Composite {
	
	private static Logger logger = Logger.getLogger(VTagger.class.getName());
	
	private TaggerEditor taggerEditor;
	private ClientTagInstanceJSONSerializer tagInstanceJSONSerializer;
	private ClientTagDefinitionJSONSerializer tagDefinitionJSONSerializer;
	private TextRangeJSONSerializer textRangeJSONSerializer;

	private TaggerListener taggerListener;

	private CommentPanel commentPanel;
	
	/**
	 * The constructor should first call super() to initialize the component and
	 * then handle any initialization relevant to Vaadin.
	 */
	public VTagger() {
		this.tagInstanceJSONSerializer = new ClientTagInstanceJSONSerializer();
		this.tagDefinitionJSONSerializer = new ClientTagDefinitionJSONSerializer();
		this.textRangeJSONSerializer = new TextRangeJSONSerializer();
		initComponents();
		initActions();
	}
	
	private void initActions() {
	}

	private void initComponents() {

		taggerEditor = new TaggerEditor(new TaggerEditorListener() {
			public void annotationChanged(TaggerEditorEventType type, Object... args) {
				switch(type) {
				
					case ADD : {
						ClientTagInstance tagInstance = (ClientTagInstance)args[0];
						taggerListener.tagInstanceAdded( 
								tagInstanceJSONSerializer.toJSONObject(tagInstance));
						break;
					}
				
					case REMOVE : {
						String tagInstanceID  = (String)args[0];
						boolean reportToServer = (Boolean)args[1];
						if (reportToServer) {
							taggerListener.tagInstanceRemoved(tagInstanceID);
						}
						break;
					}
					
				}
			}
			
			public void annotationSelected(String tagInstancePartID, String lineID) {
				taggerListener.tagInstanceSelected(
						tagInstanceJSONSerializer.toJSONArrayString(tagInstancePartID, lineID));
			}
			
			@Override
			public void annotationsSelected(HashSet<String> tagInstanceIDs) {
				taggerListener.tagInstancesSelected(tagInstanceJSONSerializer.toJSONArrayString(tagInstanceIDs));
			}
			
			public void logEvent(String event) {
				logToServer(event);
			}
			
			@Override
			public void contextMenuSelected(int x, int y) {
				taggerListener.contextMenuSelected(x, y);
			}
			
			@Override
			public void addComment(List<TextRange> ranges, int x, int y) {
				taggerListener.addComment(ranges, x, y);
			}
			
			@Override
			public void setAddCommentButtonVisible(boolean visible, Line line) {
				commentPanel.setAddCommentButtonVisible(visible, line);
			}
		});

		FlowPanel panel = new FlowPanel();
		panel.addStyleName("v-tagger-panel");
		panel.add(taggerEditor);
		
		this.commentPanel = new CommentPanel(new CommentPanelListener() {
			
			@Override
			public void remove(ClientComment comment) {
				taggerListener.removeComment(comment);
			}
			
			@Override
			public void edit(ClientComment comment, int x, int y) {
				taggerListener.editComment(comment, x, y);
			}
			
			@Override
			public void addComment(int x, int y) {
				List<TextRange> ranges = taggerEditor.getSelectedTextRanges();
				
				taggerListener.addComment(ranges, x, y);
			}
			
			@Override
			public void replyTo(ClientComment comment, int x, int y) {
				taggerListener.replyToComment(comment, x, y);
			}
			
			@Override
			public void loadReplies(String uuid) {
				taggerListener.loadReplies(uuid);
			}
			
			@Override
			public void showCommentHighlight(ClientComment comment) {
				taggerEditor.showCommentHighlight(comment);
			}
			
			@Override
			public void edit(ClientComment comment, ClientCommentReply reply, int x, int y) {
				taggerListener.editReply(comment, reply, x, y);
			}
			
			@Override
			public void remove(ClientComment comment, ClientCommentReply reply) {
				taggerListener.removeReply(comment, reply);
			}
		});
		
		panel.add(commentPanel);
		
		initWidget(panel);
	}
	
	public void logToServer(String logMsg) {
		taggerListener.log(logMsg);
	}

	public void setTaggerListener(TaggerListener taggerListener) {
		this.taggerListener = taggerListener;
	}

	public void setTaggerId(String taggerId) {
		taggerEditor.setTaggerID(taggerId);
	}

	public void setPage(String page, int lineCount, List<ClientComment> comments) {

		logger.info("setting page content");
		Timer timer = new Timer() {
			@Override
			public void run() {
				taggerEditor.setHTML(new HTML(page), lineCount);
				commentPanel.setLines(taggerEditor.getLines());
				for (ClientComment comment : comments) {
					addComment(comment);
				}
			}
		};
		
		timer.schedule(100);
	}

	public void removeTagInstances(String tagInstancesJson) {
		List<ClientTagInstance> tagInstances = 
				tagInstanceJSONSerializer.fromJSONArray(tagInstancesJson);
		
		for (ClientTagInstance tagInstance : tagInstances) {
			logger.info("got TagInstance from server (hide): " + tagInstance);
			taggerEditor.removeTagInstance(
					tagInstance.getInstanceID(), 
					false); //don't report to server
		}
	}

	//FIXME: reimplement with line replacement
	public void addTagInstances(String tagInstancesJson) {
//		List<ClientTagInstance> tagInstances = 
//				tagInstanceJSONSerializer.fromJSONArray(tagInstancesJson);
//		
//		for (ClientTagInstance tagInstance : tagInstances) {
//			logger.info("got tag instance from server (show): " + tagInstance);
//			taggerEditor.addTagInstance(tagInstance);
//		}
	}

	public void addTagInstanceWith(String tagDefinitionJson) {
		ClientTagDefinition tagDefinition = 
				tagDefinitionJSONSerializer.fromJSON(tagDefinitionJson);
			taggerEditor.createAndAddTagIntance(tagDefinition);
	}

	public void setTagInstanceSelected(String tagInstanceId) {
		 taggerEditor.setTagInstanceSelected(tagInstanceId);
	}

	public void setTraceSelection(boolean traceSelection) {
		taggerEditor.setTraceSelection(traceSelection);
		
	}

	public void removeHighlights() {
		taggerEditor.removeHighlights();
		
	}

	public void scrollLineToVisible(String lineId) {
		taggerEditor.scrollLineToVisible(lineId);
	}

	public void addComment(ClientComment comment) {
		Line line = taggerEditor.addComment(comment);
		if (line != null) {
			commentPanel.addComment(comment, line);
		}
	}

	public void updateComment(String uuid, String body, int startPos) {
		if (startPos >= 0) {
			Line line = taggerEditor.updateComment(uuid, body, startPos);
			if (line != null) {
				commentPanel.refreshComment(uuid, line);
			}
		}		
	}

	public void removeComment(String uuid, int startPos) {
		if (startPos >= 0) {
			Line line = taggerEditor.removeComment(uuid, startPos);
			if (line != null) {
				commentPanel.removeCommment(uuid, line);
			}
		}
		
	}

	public void setReplies(String uuid, int startPos, List<ClientCommentReply> replies) {
		if (startPos >= 0) {
			Line line = taggerEditor.updateComment(uuid, replies, startPos);
			if (line != null) {
				commentPanel.refreshComment(uuid, line);
			}
		}		
	}
}
