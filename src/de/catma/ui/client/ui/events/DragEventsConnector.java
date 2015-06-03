package de.catma.ui.client.ui.events;

import com.google.gwt.event.dom.client.DragStartEvent;
import com.google.gwt.event.dom.client.DragStartHandler;
import com.google.gwt.event.dom.client.DragEndEvent;
import com.google.gwt.event.dom.client.DragEndHandler;

import com.google.gwt.event.dom.client.DragEnterEvent;
import com.google.gwt.event.dom.client.DragEnterHandler;
import com.google.gwt.event.dom.client.DragLeaveEvent;
import com.google.gwt.event.dom.client.DragLeaveHandler;
import com.google.gwt.event.dom.client.DragEvent;
import com.google.gwt.event.dom.client.DragHandler;
import com.google.gwt.event.dom.client.DragOverEvent;
import com.google.gwt.event.dom.client.DragOverHandler;

import com.google.gwt.user.client.ui.Widget;
import com.vaadin.client.ComponentConnector;
import com.vaadin.client.ServerConnector;
import com.vaadin.client.extensions.AbstractExtensionConnector;
import com.vaadin.shared.ui.Connect;

import de.catma.ui.events.DragEvents;

@Connect(DragEvents.class)
public class DragEventsConnector extends AbstractExtensionConnector implements DragStartHandler, DragEndHandler, 
DragEnterHandler, DragLeaveHandler, DragHandler, DragOverHandler {

    @Override
    protected void extend(ServerConnector serverConnector) {
        Widget target = ((ComponentConnector) serverConnector).getWidget();

        target.addDomHandler(this, DragStartEvent.getType());
        target.addDomHandler(this, DragEndEvent.getType());
        target.addDomHandler(this, DragEnterEvent.getType());
        target.addDomHandler(this, DragLeaveEvent.getType());
        target.addDomHandler(this, DragEvent.getType());
        target.addDomHandler(this, DragOverEvent.getType());
    }

    @Override
    public void onDragStart(DragStartEvent dragStartEvent) {
    	// if you do want to pass something through the RPC call, it must be serializable
        getRpcProxy(DragEventsServerRPC.class).dragStart();
    }

    @Override
    public void onDragEnd(DragEndEvent dragEndEvent) {
        getRpcProxy(DragEventsServerRPC.class).dragEnd();
    }
    
    @Override
    public void onDragEnter(DragEnterEvent dragEnterEvent) {
        getRpcProxy(DragEventsServerRPC.class).dragEnter();
    }
    
    @Override
    public void onDragLeave(DragLeaveEvent dragLeaveEvent) {
        getRpcProxy(DragEventsServerRPC.class).dragLeave();
    }
    
    @Override
    public void onDrag(DragEvent dragEvent) {
        getRpcProxy(DragEventsServerRPC.class).drag();
    }
    
    @Override
    public void onDragOver(DragOverEvent dragOverEvent) {
        getRpcProxy(DragEventsServerRPC.class).dragOver();
    }
}
