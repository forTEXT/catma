/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2012  University Of Hamburg
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
package de.catma.ui.component;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.vaadin.data.HasValue.ValueChangeEvent;
import com.vaadin.data.HasValue.ValueChangeListener;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;

/**
 * @author marco.petris@web.de
 *
 */
public class PagerComponent extends HorizontalLayout {
	
	public static interface PageChangeListener {
		public void pageChanged(int number);
	}
	
	private static class NumberField extends TextField {
		private int startValue;
		public NumberField(int number) {
			super();
			startValue = number;
			setNumber(number);
		}

		public void setNumber(int number) {
			setValue(String.valueOf(number));
		}
		
		public int getNumber() {
			String value = getValue().toString();
			try {
				int pageNumber = Integer.valueOf(value);
				return pageNumber;
			}
			catch(NumberFormatException nfe) {
				return startValue;
			}
		}
	}

	private Button firstPageButton;
	private Button previousPageButton;
	private NumberField pageInput;
	private Button nextPageButton;
	private Button lastPageButton;
	private PageChangeListener pageChangeListener;
	private int currentPageNumber = 1;
	private int lastPageNumber;
	private Label lastPageNumberLabel;
	private boolean allowInputPastLastPageNumber = false;
	
	public PagerComponent(final Consumer<PagerListener> pagerListenerConsumer, Supplier<Integer> lastPageNumberSupplier, PageChangeListener pageChangeListener) {
		this.pageChangeListener = pageChangeListener;
		initComponents();
		initActions();
		pagerListenerConsumer.accept(new PagerListener() {
			
			public void textChanged() {
				setLastPageNumber(lastPageNumberSupplier.get());
			}
		});
		previousPageButton.setEnabled(false);
	}
	
	public void setLastPageNumber(int lastPageNumber) {
		this.lastPageNumber = lastPageNumber;
		this.lastPageNumberLabel.setValue("/"+lastPageNumber); //$NON-NLS-1$
		nextPageButton.setEnabled((currentPageNumber < lastPageNumber));
		previousPageButton.setEnabled(currentPageNumber != 1);
	}

	private void initActions() {
		firstPageButton.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				currentPageNumber = 1;
				pageInput.setValue("1"); //$NON-NLS-1$
				previousPageButton.setEnabled(false);
				nextPageButton.setEnabled(true);
			}
		});
		previousPageButton.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				currentPageNumber--;
				pageInput.setNumber(currentPageNumber);
				if (currentPageNumber == 1) {
					previousPageButton.setEnabled(false);
				}
				nextPageButton.setEnabled(true);
			}
		});
		nextPageButton.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				currentPageNumber++;
				pageInput.setNumber(currentPageNumber);
				if (currentPageNumber == lastPageNumber) {
					nextPageButton.setEnabled(false);
				}
				
				previousPageButton.setEnabled(true);
			}
		});
		lastPageButton.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				currentPageNumber = lastPageNumber;
				pageInput.setNumber(currentPageNumber);
				previousPageButton.setEnabled(true);
				nextPageButton.setEnabled(false);
			}
		});
		pageInput.addValueChangeListener(new ValueChangeListener<String>() {
			
			public void valueChange(ValueChangeEvent<String> event) {
				currentPageNumber = pageInput.getNumber();
				if ((currentPageNumber > lastPageNumber) && !allowInputPastLastPageNumber) {
					currentPageNumber = lastPageNumber;
				}
				else if (currentPageNumber < 1) {
					currentPageNumber = 1;
				}
				pageChangeListener.pageChanged(currentPageNumber);
				if (currentPageNumber == 1) {
					previousPageButton.setEnabled(false);
					nextPageButton.setEnabled(true);
				}
				else if (currentPageNumber == lastPageNumber) {
					nextPageButton.setEnabled(false);
					previousPageButton.setEnabled(true);
				}
				else {
					previousPageButton.setEnabled(true);
					nextPageButton.setEnabled(true);
				}
				pageInput.setComponentError(null);
			}
		});
		
	}

	private void initComponents() {
		setSpacing(false);
		firstPageButton = new IconButton(VaadinIcons.FAST_BACKWARD);
		addComponent(firstPageButton);
		previousPageButton = new IconButton(VaadinIcons.BACKWARDS);
		addComponent(previousPageButton);
		pageInput = new NumberField(1);
		pageInput.setStyleName("pager-pageinput"); //$NON-NLS-1$
		addComponent(pageInput);
		lastPageNumberLabel = new Label("N/A");
		addComponent(lastPageNumberLabel);
		this.setComponentAlignment(lastPageNumberLabel, Alignment.MIDDLE_LEFT);
		nextPageButton = new IconButton(VaadinIcons.FORWARD);
		addComponent(nextPageButton);
		lastPageButton = new IconButton(VaadinIcons.FAST_FORWARD);
		addComponent(lastPageButton);
	}
	

	public void setPage(int pageNumber) {
		currentPageNumber = pageNumber;
		pageInput.setNumber(currentPageNumber);
	}
	
	public void setLastPageButtonVisible(boolean visible) {		
		this.lastPageButton.setVisible(visible);
	}
	
	public void setAllowInputPastLastPageNumber(boolean allowInputPastLastPageNumber) {
		this.allowInputPastLastPageNumber = allowInputPastLastPageNumber;
	}
}
