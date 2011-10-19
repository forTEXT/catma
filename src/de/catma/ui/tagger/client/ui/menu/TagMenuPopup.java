package de.catma.ui.tagger.client.ui.menu;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.web.bindery.event.shared.HandlerRegistration;

import de.catma.ui.common.client.ui.event.EventListenerSupport;

class TagMenuPopup extends PopupPanel {
		
		private TreeItem root;
		private List<HandlerRegistration> handlerRegistrations = new ArrayList<HandlerRegistration>();
		private EventListenerSupport eventlistenerSupport = new EventListenerSupport();
		
		public TagMenuPopup(TagActionListener listener) {
			super(true);
			eventlistenerSupport.addEventListener(listener);
			root = new TreeItem("Tags");
			Tree tree = new Tree();
			tree.addItem(root);
			root.setState(true);
			root.setStyleName("tagger_menu_root");
			VerticalPanel vPanel = new VerticalPanel();
			vPanel.add(tree);
			vPanel.setStylePrimaryName("tagger_menu");
			setWidget(vPanel);
		}
		
		public void addTag(final String tag) {
//			HorizontalPanel hPanel = new HorizontalPanel();
//			hPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
//			hPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);

//			hPanel.setStylePrimaryName("tagger_menu_item");
			
			Grid grid = new Grid(1,2);
			Label l = new Label(tag);
			grid.setWidget(0, 0, l);
			
			Button tagRemoveButton = new Button("remove");
			grid.setWidget(0, 1, tagRemoveButton);
			HandlerRegistration handlerRegistration = tagRemoveButton.addClickHandler(new ClickHandler() {
				
				public void onClick(ClickEvent event) {
					
					
				}
			});
			handlerRegistrations.add(handlerRegistration);
			root.addItem(grid);
			root.setState(true);
		}
		
		@Override
		public void hide() {
			super.hide();
			for (HandlerRegistration hr : handlerRegistrations) {
				hr.removeHandler();
			}
			eventlistenerSupport.clear();
		}
		
	}