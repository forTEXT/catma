package de.catma.ui.tagger;

import com.vaadin.terminal.gwt.server.WebApplicationContext;
import com.vaadin.terminal.gwt.server.WebBrowser;
import com.vaadin.ui.Window;

import de.catma.ui.CatmaWindow;

public class TaggerManagerWindow extends CatmaWindow {
	
	private float startHeight;
	private int startHeightUnits;
	
	public TaggerManagerWindow(final TaggerManagerView taggerManagerView) {
		super("Tagger");
		setContent(taggerManagerView);
		setHeight("85%");
		setWidth("70%");
		
		//TODO: window resize should lead to a resize of the inner components as well
//		addListener(new ResizeListener() {
//			
//			public void windowResized(ResizeEvent e) {
//				Window w = e.getWindow();
//				
//				System.out.println("H: " + w.getHeight() + " u:" + w.getHeightUnits());
//				taggerManagerView.windowHeightChanged(
//					(100/startHeight*(w.getHeight()-startHeight)));
//			}
//		});
//		setImmediate(true);
	}
	
//	@Override
//	public void attach() {
//		super.attach();
//		WebApplicationContext context = 
//				((WebApplicationContext) getApplication().getContext());
//		WebBrowser wb = context.getBrowser();
//		
//		setHeight(wb.getScreenHeight()*0.65f, UNITS_PIXELS);
//		startHeight = getHeight();
//		startHeightUnits = getHeightUnits();
//		System.out.println("start: " + startHeight + " unit: " + startHeightUnits);
//	}

}
