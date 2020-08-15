package de.catma.ui.client.ui.tagger;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.vaadin.client.TooltipInfo;
import com.vaadin.client.communication.RpcProxy;
import com.vaadin.client.ui.AbstractComponentConnector;
import com.vaadin.shared.ui.Connect;
import com.vaadin.shared.ui.ContentMode;

import de.catma.ui.client.ui.tagger.shared.ClientComment;
import de.catma.ui.client.ui.tagger.shared.ClientCommentReply;
import de.catma.ui.client.ui.tagger.shared.ClientTagInstance;
import de.catma.ui.client.ui.tagger.shared.TaggerState;
import de.catma.ui.client.ui.tagger.shared.TextRange;
import de.catma.ui.module.annotate.Tagger;

@Connect(Tagger.class)
public class TaggerConnector extends AbstractComponentConnector {

	private TaggerServerRpc rpc = RpcProxy.create(TaggerServerRpc.class, this);
	
	public TaggerConnector() {
		registerRpc(TaggerClientRpc.class, new TaggerClientRpc() {
			
			@Override
			public void setTaggerId(String taggerId) {
				getWidget().setTaggerId(taggerId);
			}
			
			@Override
			public void setPage(String page, int lineCount, String comments) {
				getWidget().setPage(page, lineCount, new ClientCommentJSONSerializer().fromJSONArray(comments));
			}
			
			@Override
			public void removeTagInstances(String tagInstancesJson) {
				getWidget().removeTagInstances(tagInstancesJson);
			}
			
			@Override
			public void addTagInstances(String tagInstancesJson) {
				getWidget().addTagInstances(tagInstancesJson);
			}
			
			@Override
			public void addTagInstanceWith(String tagDefinitionJson) {
				getWidget().addTagInstanceWith(tagDefinitionJson);
			}
			
			@Override
			public void setTagInstanceSelected(String tagInstanceId) {
				getWidget().setTagInstanceSelected(tagInstanceId);
			}
			
			@Override
			public void setTraceSelection(boolean traceSelection) {
				getWidget().setTraceSelection(traceSelection);
			}
			
			@Override
			public void removeHighlights() {
				getWidget().removeHighlights();
			}
			
			@Override
			public void scrollLineToVisible(String lineId) {
				getWidget().scrollLineToVisible(lineId);
			}
			
			@Override
			public void addComment(String clientCommentJson) {
				getWidget().addComment(
					new ClientCommentJSONSerializer().fromJSONString(clientCommentJson));
			}
			
			@Override
			public void updateComment(String uuid, String body, int startPos) {
				getWidget().updateComment(uuid, body, startPos);	
			}
			
			@Override
			public void removeComment(String uuid, int startPos) {
				getWidget().removeComment(uuid, startPos);
			}
			
			@Override
			public void setReplies(String uuid, int startPos, String replies) {
				getWidget().setReplies(uuid, startPos, new ClientCommentReplyJSONSerializer().fromJSONArray(replies));
			}
		});
	}
	
	
	@Override
	protected VTagger createWidget() {
		VTagger tagger= GWT.create(VTagger.class);
		tagger.setTaggerListener(new TaggerListener() {
			@Override
			public void log(String msg) {
				rpc.log(msg);
			}
			@Override
			public void tagInstanceAdded(String tagIntanceJson) {
				rpc.tagInstanceAdded(tagIntanceJson);
			}
			@Override
			public void tagInstanceSelected(String instanceIDLineIDJson) {
				rpc.tagInstanceSelected(instanceIDLineIDJson);
			}
			
			@Override
			public void tagInstancesSelected(String tagInstanceIDsJson) {
				rpc.tagInstancesSelected(tagInstanceIDsJson);
			}
			
			@Override
			public void tagInstanceRemoved(String tagInstanceID) {
				//does not get reported back to the server since we do
				// not use client side removal within CATMA
			}
			
			@Override
			public void contextMenuSelected(int x, int y) {
				rpc.contextMenuSelected(x, y);
			}
			
			@Override
			public void addComment(List<TextRange> ranges, int x, int y) {
				
				StringBuilder builder = new StringBuilder();
				String conc = "";
				for (TextRange range : ranges) {
					builder.append(conc);
					builder.append(range.getStartPos());
					builder.append(":");
					builder.append(range.getEndPos());
					conc = ",";
				}
				
				rpc.addComment(builder.toString(), x, y);
				
			}
			
			@Override
			public void editComment(ClientComment comment, int x, int y) {
				rpc.editComment(comment.getUuid(), x, y);
			}
			
			@Override
			public void removeComment(ClientComment comment) {
				rpc.removeComment(comment.getUuid());
			}
			
			@Override
			public void replyToComment(ClientComment comment, int x, int y) {
				rpc.replyToComment(comment.getUuid(), x, y);
			}
			
			@Override
			public void editReply(ClientComment comment, ClientCommentReply reply, int x, int y) {
				rpc.editReply(comment.getUuid(), reply.getUuid(), x, y);
			}
			
			@Override
			public void removeReply(ClientComment comment, ClientCommentReply reply) {
				rpc.removeReply(comment.getUuid(), reply.getUuid());
			}
			
			@Override
			public void loadReplies(String uuid) {
				rpc.loadReplies(uuid);	
			}
		});

		return tagger;
	}
	
	@Override
	public VTagger getWidget() {
		return (VTagger) super.getWidget();
	}
	
    public TooltipInfo getTooltipInfo(Element element) {
    	if (element.getId().startsWith("CATMA")) {
    		String tooltipInfo = getState().tagInstanceIdToTooltipInfo.get(
    				ClientTagInstance.getTagInstanceIDFromPartId(element.getId()));
    		if (tooltipInfo == null) {
    			tooltipInfo = "N/A";
    		}
    		return new TooltipInfo(tooltipInfo, ContentMode.HTML, getState().errorMessage);
    	}
    	else {
    		return null;
    	}
    }
    
    @Override
    public boolean hasTooltip() {
    	return true;
    }

    @Override
    public TaggerState getState() {
    	return (TaggerState) super.getState();
    }
}
