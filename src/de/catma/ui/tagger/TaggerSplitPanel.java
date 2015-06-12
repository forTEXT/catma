package de.catma.ui.tagger;

import java.lang.reflect.Method;

import com.vaadin.event.ConnectorEventListener;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.shared.MouseEventDetails;
import com.vaadin.shared.ui.splitpanel.AbstractSplitPanelRpc;
import com.vaadin.shared.ui.splitpanel.AbstractSplitPanelState.SplitterState;
import com.vaadin.ui.AbstractSplitPanel;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.AbstractSplitPanel.SplitterClickEvent;
import com.vaadin.ui.AbstractSplitPanel.SplitterClickListener;
import com.vaadin.util.ReflectTools;


public class TaggerSplitPanel extends HorizontalSplitPanel {
	
	private AbstractSplitPanelRpc rpc = new AbstractSplitPanelRpc() {

        @Override
        public void splitterClick(MouseEventDetails mouseDetails) {
        }

        @Override
        public void setSplitterPosition(float position) {
        	SplitterState splitterState = getState().splitterState;
        	
        	fireEvent(new SplitterPositionChangedEvent(TaggerSplitPanel.this, position, splitterState.positionUnit));
        }
    };
    
    public TaggerSplitPanel(){
    	super();
    	registerRpc(rpc);
    }
    
    public static class SplitterPositionChangedEvent extends Component.Event {
    	
    	private float position;
    	private Unit positionUnit;
    	
    	public SplitterPositionChangedEvent(Component source, float position, String positionUnit) {
            super(source);
            this.position = position;
            this.positionUnit = Unit.getUnitFromSymbol(positionUnit);
        }
    	
    	public float getPosition(){
    		return position;
    	}
    	
    	public Unit getPositionUnit(){
    		return positionUnit;
    	}
    }
    
    public interface SplitterPositionChangedListener extends ConnectorEventListener {

        public static final Method positionChangedMethod = ReflectTools.findMethod(
        		SplitterPositionChangedListener.class, "positionChanged",
        		SplitterPositionChangedEvent.class);

        /**
         * SplitPanel splitter has been moved
         * 
         * @param event
         *            SplitterPositionChangedEvent event.
         */
        public void positionChanged(SplitterPositionChangedEvent event);
    }
}