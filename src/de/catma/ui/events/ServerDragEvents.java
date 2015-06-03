package de.catma.ui.events;

import com.vaadin.server.AbstractClientConnector;
import com.vaadin.server.AbstractExtension;

import de.catma.ui.client.ui.events.DragEventsServerRPC;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

// see for reference:
// https://vaadin.com/wiki/-/wiki/Main/Sending%20events%20from%20the%20client%20to%20the%20server%20using%20RPC
// https://vaadin.com/book/vaadin7/-/page/gwt.extension.html
// https://vaadin.com/book/vaadin7/-/page/gwt.rpc.html
// https://vaadin.com/directory#!addon/mouse-events-extension

// Simple how to use:
//
//Button button = new Button("Button with Drag", e -> layout.addComponent(new Label("Click")));
// it may be useful to also include button.setImmediate(true); but the events do make it to the server without it
//
//ServerDragEvents serverDragEvents = ServerDragEvents.enableFor(button);
//serverDragEvents.addDragStartListener(new ServerDragEvents.DragStartListener(){
//	public void dragStart(){
//		logger.info("dragStart: We started to move");
//	}
//});

public class ServerDragEvents extends AbstractExtension {

    private Set<DragStartListener> dragStartListeners = new HashSet<DragStartListener>();
    private Set<DragEndListener> dragEndListeners = new HashSet<DragEndListener>();
    private Set<DragEnterListener> dragEnterListeners = new HashSet<DragEnterListener>();
    private Set<DragLeaveListener> dragLeaveListeners = new HashSet<DragLeaveListener>();
    private Set<DragListener> dragListeners = new HashSet<DragListener>();
    private Set<DragOverListener> dragOverListeners = new HashSet<DragOverListener>();


    public interface DragStartListener extends Serializable {
        void dragStart();
    }

    public interface DragEndListener extends Serializable  {
        void dragEnd();
    }
    
    public interface DragEnterListener extends Serializable  {
        void dragEnter();
    }
    
    public interface DragLeaveListener extends Serializable  {
        void dragLeave();
    }
    
    public interface DragListener extends Serializable  {
        void drag();
    }
    
    public interface DragOverListener extends Serializable  {
        void dragOver();
    }

    DragEventsServerRPC rpc = new DragEventsServerRPC() {
        @Override
        public void dragStart() {
            fireDragStartEvents();
        }

        @Override
        public void dragEnd() {
            fireDragEndEvents();
        }
        
        @Override
        public void dragEnter() {
            fireDragEnterEvents();
        }
        
        @Override
        public void dragLeave() {
            fireDragLeaveEvents();
        }
        
        @Override
        public void drag() {
            fireDragEvents();
        }
        
        @Override
        public void dragOver() {
            fireDragOverEvents();
        }
    };

    protected ServerDragEvents(AbstractClientConnector component) {
        registerRpc(rpc);
        extend(component);
    }

    public static ServerDragEvents enableFor(AbstractClientConnector component) {
        return new ServerDragEvents(component);
    }

    private void fireDragStartEvents() {
        for (DragStartListener listener : Collections.unmodifiableCollection(dragStartListeners)) {
            listener.dragStart();
        }
    }
    
    private void fireDragEndEvents() {
        for (DragEndListener listener : Collections.unmodifiableCollection(dragEndListeners)) {
            listener.dragEnd();
        }
    }

    private void fireDragEnterEvents() {
        for (DragEnterListener listener : Collections.unmodifiableCollection(dragEnterListeners)) {
            listener.dragEnter();
        }
    }
    
    private void fireDragLeaveEvents() {
        for (DragLeaveListener listener : Collections.unmodifiableCollection(dragLeaveListeners)) {
            listener.dragLeave();
        }
    }
    
    private void fireDragEvents() {
        for (DragListener listener : Collections.unmodifiableCollection(dragListeners)) {
            listener.drag();
        }
    }
    
    private void fireDragOverEvents() {
        for (DragOverListener listener : Collections.unmodifiableCollection(dragOverListeners)) {
            listener.dragOver();
        }
    }

    public void addDragStartListener(DragStartListener listener) {
        dragStartListeners.add(listener);
    }

    public void removeDragStartListener(DragStartListener listener) {
    	dragStartListeners.remove(listener);
    }

    public void addDragEndListener(DragEndListener listener) {
        dragEndListeners.add(listener);
    }

    public void removeDragEndListener(DragEndListener listener) {
    	dragEndListeners.remove(listener);
    }
    
    public void addDragEnterListener(DragEnterListener listener) {
        dragEnterListeners.add(listener);
    }

    public void removeDragEnterListener(DragEnterListener listener) {
    	dragEnterListeners.remove(listener);
    }
    
    public void addDragLeaveListener(DragLeaveListener listener) {
        dragLeaveListeners.add(listener);
    }

    public void removeDragLeaveListener(DragLeaveListener listener) {
    	dragLeaveListeners.remove(listener);
    }
    
    public void addDragListener(DragListener listener) {
        dragListeners.add(listener);
    }

    public void removeDragListener(DragListener listener) {
    	dragListeners.remove(listener);
    }
    
    public void addDragOverListener(DragOverListener listener) {
        dragOverListeners.add(listener);
    }

    public void removeDragOverListener(DragOverListener listener) {
    	dragOverListeners.remove(listener);
    }
}
