package de.catma.ui.legacy.repository;

import com.github.appreciated.material.MaterialTheme;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.StreamResource;
import com.vaadin.server.StreamResource.StreamSource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

public class DownloadDialog extends Window {
	
	public DownloadDialog(StreamSource source, String filename) {
		super("Download files");
		initComponents(source, filename);
	}

	private void initComponents(StreamSource source, String filename) {
		setWidth("300px");
		setHeight("200px");
		setModal(true);
		VerticalLayout content = new VerticalLayout();
		content.setSizeFull();
		content.setMargin(true);
		content.setSpacing(true);
		
		Button btDownload = new Button("Download " + filename);
		btDownload.addStyleName(MaterialTheme.BUTTON_LINK);
		
		FileDownloader fileDownloader = new FileDownloader(new StreamResource(source, filename));
		fileDownloader.extend(btDownload);
		
		Button btClose = new Button("Close");
		btClose.addClickListener(event -> close());
		
		content.addComponent(btDownload);
		content.setComponentAlignment(btDownload, Alignment.MIDDLE_CENTER);
		content.addComponent(btClose);
		content.setComponentAlignment(btClose, Alignment.BOTTOM_CENTER);
		setContent(content);
		center();
	}

	public void show() {
		UI.getCurrent().addWindow(this);
	}

}
