package de.catma.ui.client.ui.resources;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;

public interface ResourceBundle extends ClientBundle {
	
	@Source("de/catma/ui/client/ui/resources/grndiamd.gif")
	public ImageResource tagsetDefinitionIcon();

	@Source("de/catma/ui/client/ui/resources/reddiamd.gif")
	public ImageResource tagDefinitionIcon();

	@Source("de/catma/ui/client/ui/resources/ylwdiamd.gif")
	public ImageResource propertyDefinitionIcon();
}
