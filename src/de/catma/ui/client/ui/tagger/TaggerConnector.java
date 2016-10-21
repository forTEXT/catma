package de.catma.ui.client.ui.tagger;

import com.google.gwt.core.client.GWT;
import com.vaadin.client.communication.RpcProxy;
import com.vaadin.client.ui.AbstractComponentConnector;
import com.vaadin.shared.ui.Connect;

import de.catma.ui.tagger.Tagger;

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
			public void setPage(String page, String tagInstancesJson) {
				getWidget().setPage(page, tagInstancesJson);
			}
			
			@Override
			public void removeTagInstances(String tagInstancesJson) {
				getWidget().removeTagInstances(tagInstancesJson);
			}
			
			@Override
			public void highlight(String textRangeJson) {
				getWidget().highlight(textRangeJson);
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
			public void tagInstanceSelected(String instanceIDsJson) {
				rpc.tagInstanceSelected(instanceIDsJson);
			}
			
			@Override
			public void tagInstanceRemoved(String tagInstanceID) {
				//does not get reported back to the server since we do
				// not use client side removal within CATMA
			}
		});

		return tagger;
	}
	
	@Override
	public VTagger getWidget() {
		return (VTagger) super.getWidget();
	}
}
