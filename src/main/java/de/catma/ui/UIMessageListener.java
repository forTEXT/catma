package de.catma.ui;

import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import com.vaadin.ui.UI;

public abstract class UIMessageListener<T> implements MessageListener<T> {

	public abstract void uiBlockingOnMessage(Message<T> message);
	
	@Override
	public void onMessage(Message<T> message) {
		UI.getCurrent().access( () -> {
			uiBlockingOnMessage(message);
		});
	}
}
