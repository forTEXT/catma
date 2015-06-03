package de.catma.ui.client.ui.events;

import com.vaadin.shared.communication.ServerRpc;

public interface DragEventsServerRPC extends ServerRpc {
    public void dragStart();
    public void dragEnd();
    public void dragEnter();
    public void dragLeave();
    public void drag();
    public void dragOver();
}
