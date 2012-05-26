package de.catma.ui.dialog;

import com.vaadin.ui.ProgressIndicator;
import com.vaadin.ui.Upload;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

public class UploadDialog extends VerticalLayout {
	private Window dialogWindow;
	private String caption;
	private SaveCancelListener saveCancelListener;
	private Upload upload;
	private ProgressIndicator pi;
	
	public UploadDialog(String caption, SaveCancelListener saveCancelListener) {
		this.caption = caption;
		this.saveCancelListener = saveCancelListener;
		initComponents();
	}
	
	private void initComponents() {
		setSizeFull();
		setSpacing(true);
		
	}
	
	
	
}
