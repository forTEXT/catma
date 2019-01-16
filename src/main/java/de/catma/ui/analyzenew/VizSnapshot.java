package de.catma.ui.analyzenew;

import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;



public class VizSnapshot extends Panel {
	String title;
	Button btEdit;
	//Image snapshot;
	
	
	public VizSnapshot(String title) {
		this.title=title;
		setHeight("200px");
		setWidth("100%");
		setContent(title);
		initListeners();
	}
	
	private void setContent(String title) {
		VerticalLayout content = new VerticalLayout();
		Label titleLabel = new Label(title);
		btEdit = new Button ("EDIT");
		content.addComponents(titleLabel,btEdit);
		
		setContent(content);
	}
	
	private void initListeners() {
		btEdit.addClickListener(new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				// TODO Auto-generated method stub
				
			}
		});
	}
	
	

}
