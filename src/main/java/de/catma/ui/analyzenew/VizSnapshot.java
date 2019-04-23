package de.catma.ui.analyzenew;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import de.catma.ui.layout.VerticalLayout;

import de.catma.ui.layout.HorizontalLayout;

import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.themes.ValoTheme;



public class VizSnapshot extends Panel {
	private String title;
	private Button btEdit;
	private Button btDelete;
	private HorizontalLayout buttonBar;
	private KwicVizPanelNew kwicVizPanel;
	private EditVizSnapshotListener editVizSnapshotListener;
	private DeleteVizSnapshotListener deleteVizSnapshotListener;
	
	
	public EditVizSnapshotListener getEditVizSnapshotListener() {
		return editVizSnapshotListener;
	}

	public void setEditVizSnapshotListener(EditVizSnapshotListener editVizSnapshotListener) {
		this.editVizSnapshotListener = editVizSnapshotListener;
	}
	
	

	public DeleteVizSnapshotListener getDeleteVizSnapshotListener() {
		return deleteVizSnapshotListener;
	}

	public void setDeleteVizSnapshotListener(DeleteVizSnapshotListener deleteVizSnapshotListener) {
		this.deleteVizSnapshotListener = deleteVizSnapshotListener;
	}

	public KwicVizPanelNew getKwicVizPanel() {
		return kwicVizPanel;
	}

	public void setKwicVizPanel(KwicVizPanelNew kwicVizPanel) {
		this.kwicVizPanel = kwicVizPanel;
	}

	public VizSnapshot(String title) {
		this.title=title;
		//setHeight("160px");
		//setWidth("100%");
		setContent(title);
		initListeners();
	}
	
	private void setContent(String title) {

		this.addStyleName("analyze_queryresultpanel__card_frame");
		VerticalLayout content = new VerticalLayout();
		content.addStyleName("analyze_queryresultpanel__card");
		
		Label titleLabel = new Label(title);
		titleLabel.addStyleName("analyze_queryresultpanel_infobar");
		
		HorizontalLayout buttonBar = new HorizontalLayout();
		buttonBar.addStyleName("analyze_queryresultpanel_buttonbar");

		btDelete = new Button ("DELETE",VaadinIcons.TRASH);
		btDelete.addStyleName(ValoTheme.BUTTON_BORDERLESS);
		
		btEdit = new Button ("EDIT",VaadinIcons.PENCIL);
		btEdit.addStyleName(ValoTheme.BUTTON_BORDERLESS);
		
		buttonBar.addComponents(btDelete,btEdit);
		content.addComponents(titleLabel,buttonBar);
		
		setContent(content);
	}
	
	private void initListeners() {
		btEdit.addClickListener(new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				editVizSnapshotListener.reopenKwicView();
				
			}
		});
		
		
		
		btDelete.addClickListener(new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
			deleteVizSnapshotListener.deleteSnapshot();
				
			}
		});
	}
	
	
	
	

}
