/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009-2013  University Of Hamburg
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.catma.ui.tabbedview;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.vaadin.elements.Element;
import org.vaadin.elements.ElementIntegration;
import org.vaadin.elements.Elements;
import org.vaadin.elements.Root;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TabSheet.CloseHandler;
import com.vaadin.ui.TabSheet.SelectedTabChangeEvent;
import com.vaadin.ui.TabSheet.SelectedTabChangeListener;
import com.vaadin.ui.TabSheet.Tab;
import com.vaadin.ui.VerticalLayout;

public class TabbedView extends VerticalLayout implements CloseHandler {

	private TabSheet tabSheet;
	private Label noOpenTabsLabel;
	private VerticalLayout introPanel;
	private TabComponent lastTab;

	public TabbedView(String noOpenTabsText) {
	
		initComponents(noOpenTabsText);
		initActions();
	}
	
	
	
	private void initActions() {

		tabSheet.addSelectedTabChangeListener(new SelectedTabChangeListener() {

			public void selectedTabChange(SelectedTabChangeEvent event) {
				if (lastTab != null) {
					lastTab.removeClickshortCuts();
				}
				lastTab = (TabComponent) tabSheet.getSelectedTab();
				lastTab.addClickshortCuts();
			}
		});

	}

	private void initComponents(String noOpenTabsText) {
		addStyleName("hugecard-tabbed-view");	
		
		tabSheet = new TabSheet();
		tabSheet.setSizeFull();
		
		introPanel = new VerticalLayout();
		introPanel.setSpacing(true);
		addComponent(introPanel);

		noOpenTabsLabel = new Label(noOpenTabsText);

		noOpenTabsLabel.setSizeFull();
		setMargin(false);

		introPanel.addComponent(noOpenTabsLabel);
		introPanel.setComponentAlignment(noOpenTabsLabel, Alignment.MIDDLE_CENTER);

		addComponent(tabSheet);
		setExpandRatio(tabSheet, 1.0f);
		tabSheet.setTabsVisible(false);
		tabSheet.setHeight("0px");
		tabSheet.setCloseHandler(this);
	}
	
	@Override
	public void attach() {
		super.attach();
		Root tabSheetRoot = ElementIntegration.getRoot(tabSheet);
		
		tabSheetRoot.fetchDom(() -> {
			if (!tabSheetRoot.getChildren().isEmpty()) {
				Element tabContainer = (Element)tabSheetRoot.getChildren().get(0);
				Element addButtonElement = Elements.create("div");
				addButtonElement.setAttribute(
					"class", 
					"c-tabbed-view-plus v-button v-widget icon-only "
					+ "v-button-icon-only button__icon v-button-button__icon "
					+ "flat v-button-flat borderless v-button-borderless");
				
				tabContainer.appendChild(addButtonElement);
				addButtonElement.setInnerHtml(VaadinIcons.PLUS.getHtml());

				addButtonElement.addEventListener("click", args -> {
		            Notification.show("Clicked");
		           //TODO: add new tab 
				});
			}
		}, tabSheet);

		
	}
	public void setHtmlLabel(){
	    noOpenTabsLabel.setContentMode(ContentMode.HTML);
		}

	protected void onTabClose(Component tabContent) {
		onTabClose(tabSheet, tabContent);
	}

	public void onTabClose(TabSheet tabsheet, Component tabContent) {
		tabsheet.removeComponent(tabContent);
		((ClosableTab) tabContent).close();

		if (tabsheet.getComponentCount() == 0) {
			tabSheet.setTabsVisible(false);
			tabSheet.setHeight("0px");
			introPanel.setVisible(true);
			setMargin(true);
			setSizeUndefined();
		}

	}

	protected Tab addTab(TabComponent component, String caption) {

		Tab tab = tabSheet.addTab(component, caption);

		tab.setClosable(false);
		tabSheet.setSelectedTab(tab.getComponent());

		if (tabSheet.getComponentCount() != 0) {
			introPanel.setVisible(false);
			setMargin(false);
			tabSheet.setTabsVisible(true);
			tabSheet.setSizeFull();
			setSizeFull();
		}

		return tab;
	}

	protected Tab addClosableTab(ClosableTab closableTab, String caption) {
		Tab tab = addTab(closableTab, caption);
		tab.setClosable(true);
		return tab;
	}

	public Iterable<Component> getTabSheet() {
		return tabSheet;
	}

	protected void setSelectedTab(Component tabContent) {
		tabSheet.setSelectedTab(tabContent);
	}

	public Component getSelectedTab() {
		return tabSheet.getSelectedTab();
	}

	protected int getTabPosition(Tab tab) {
		return tabSheet.getTabPosition(tab);
	}

	public Component getComponent(int position) {
		if (tabSheet.getComponentCount() > position) {
			return tabSheet.getTab(position).getComponent();
		} else {
			return null;
		}
	}
	
	protected void setCaption(Component tabContent, String caption) {
		Optional.ofNullable(tabSheet.getTab(tabContent))
		.ifPresent(tab ->tab.setCaption(caption));
	}

	public String getCaption(Component c) {
		Tab t = tabSheet.getTab(c);
		if (t != null) {
			return t.getCaption();
		} else {
			return "";
		}
	}

	public void closeClosables() {
		Set<Component> componentBuffer = new HashSet<Component>();
		for (Component comp : this) {
			componentBuffer.add(comp);
		}
		for (Component comp : componentBuffer) {
			if (comp instanceof ClosableTab) {
				onTabClose(tabSheet, comp);
			}
		}
	}

	protected VerticalLayout getIntroPanel() {
		return introPanel;
	}
}
