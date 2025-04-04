package de.catma.ui;

import com.hazelcast.topic.Message;
import com.hazelcast.topic.MessageListener;
import com.vaadin.ui.UI;

public abstract class UIMessageListener<T> implements MessageListener<T> {

	private final UI ui;

	public abstract void uiOnMessage(Message<T> message);
	
	public UIMessageListener(UI ui) {
		this.ui = ui;
	}
	
	@Override
	public final void onMessage(Message<T> message) {
		ui.access( () -> {
			uiOnMessage(message);
		});
	}
	
	protected UI getUi() {
		return ui;
	}
}
