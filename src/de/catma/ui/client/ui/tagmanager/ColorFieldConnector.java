package de.catma.ui.client.ui.tagmanager;

import com.google.gwt.core.client.GWT;
import com.vaadin.client.annotations.OnStateChange;
import com.vaadin.client.communication.RpcProxy;
import com.vaadin.client.ui.AbstractFieldConnector;
import com.vaadin.shared.ui.Connect;

import de.catma.ui.client.ui.tagmanager.VColorField.VColorFieldListener;
import de.catma.ui.dialog.ColorField;

@Connect(ColorField.class)
public class ColorFieldConnector extends AbstractFieldConnector {
	
    ColorFieldServerRpc rpc = RpcProxy.create(ColorFieldServerRpc.class, this);
    
    public ColorFieldConnector() {
    	getWidget().setColorFieldListener(new VColorFieldListener() {
			
			@Override
			public void colorChanged(String hexColor) {
				rpc.colorChanged(hexColor);
			}
		});
    }
    
	@Override
	protected VColorField createWidget() {
		return GWT.create(VColorField.class);
	}
	
	@Override
	public ColorFieldState getState() {
		return (ColorFieldState) super.getState();
	}
	
	 @OnStateChange("hexcolor")
	 void updateColor() {
		 getWidget().setHexColor(getState().hexcolor);
	 }
	 
	 @Override
	public VColorField getWidget() {
		return (VColorField) super.getWidget();
	}

}
