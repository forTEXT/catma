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
import java.util.Set;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.UI;
import com.vaadin.ui.TabSheet.CloseHandler;
import com.vaadin.ui.TabSheet.SelectedTabChangeEvent;
import com.vaadin.ui.TabSheet.SelectedTabChangeListener;
import com.vaadin.ui.TabSheet.Tab;

import de.catma.ui.CatmaApplication;
import de.catma.ui.analyzer.Messages;

import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;

public class TabbedView extends VerticalLayout implements CloseHandler {
	
	private TabSheet tabSheet;
	private Label noOpenTabsLabel;
	private TabComponent lastTab;
	public Button btnAnalyzeCurrentActiveDoc;
	private String incomingText;

	
	public TabbedView(String noOpenTabsText) {
		initComponents(noOpenTabsText);
		initActions();
		
		if(noOpenTabsText.toString()== Messages.getString("AnalyzerManagerView.intro")){
			createBtnAndFunctionality();
			incomingText=noOpenTabsText;
		}
	}
	
		private void createBtnAndFunctionality(){
			btnAnalyzeCurrentActiveDoc = new Button("Analyze current open Document");
			btnAnalyzeCurrentActiveDoc.setVisible(true);
			addComponent(btnAnalyzeCurrentActiveDoc);
	
		}
		
		
	private void initActions() {
		
		
		tabSheet.addSelectedTabChangeListener(new SelectedTabChangeListener() {
			
			public void selectedTabChange(SelectedTabChangeEvent event) {
				if (lastTab != null) {
					lastTab.removeClickshortCuts();
				}
				lastTab = (TabComponent)tabSheet.getSelectedTab();
				lastTab.addClickshortCuts();
			}
		});
		
		
		
	}

	private void initComponents(String noOpenTabsText) {
		tabSheet = new TabSheet();
		tabSheet.setSizeFull();
		noOpenTabsLabel = 
				new Label(noOpenTabsText);
						
		
		noOpenTabsLabel.setSizeFull();
		setMargin(true);
		addComponent(noOpenTabsLabel);
		setComponentAlignment(noOpenTabsLabel, Alignment.MIDDLE_CENTER);
		

		
		
		
		
		
		addComponent(tabSheet);
		tabSheet.setTabsVisible(false);
		tabSheet.setHeight("0px");	
		tabSheet.setCloseHandler(this);
		
		
	}

	protected void onTabClose(Component tabContent) {
		onTabClose(tabSheet, tabContent);
	}
	
	public void onTabClose(TabSheet tabsheet, Component tabContent) {
		tabsheet.removeComponent(tabContent);
		((ClosableTab)tabContent).close();

		if (tabsheet.getComponentCount() == 0) {
			tabSheet.setTabsVisible(false);
			tabSheet.setHeight("0px");
			addComponent(noOpenTabsLabel, 0);
			noOpenTabsLabel.setVisible(true);
			
			if(incomingText!=null){
				btnAnalyzeCurrentActiveDoc.setVisible(true);			
			}					
			setMargin(true);
			setSizeUndefined();
		}
		
	}
	
	protected Tab addTab(TabComponent component, String caption) {
		
		Tab tab = tabSheet.addTab(component, caption);
		
		tab.setClosable(false);
		tabSheet.setSelectedTab(tab.getComponent());
		
		if (tabSheet.getComponentCount() != 0) {
			noOpenTabsLabel.setVisible(false);
			
			if(incomingText!=null){
			btnAnalyzeCurrentActiveDoc.setVisible(false);
			}
			
			removeComponent(noOpenTabsLabel);
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
		}
		else {
			return null;
		}
	}
	
	public String getCaption(Component c) {
		Tab t = tabSheet.getTab(c);
		if (t != null) {
			return t.getCaption();
		}
		else {
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
}
