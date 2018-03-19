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
package de.catma.ui.tagger.pager;

import java.text.MessageFormat;

import com.vaadin.v7.data.Property.ValueChangeEvent;
import com.vaadin.v7.data.Property.ValueChangeListener;
import com.vaadin.v7.data.Validator;
import com.vaadin.server.ClassResource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.v7.ui.HorizontalLayout;
import com.vaadin.v7.ui.Label;
import com.vaadin.v7.ui.TextField;

import de.catma.ui.tagger.pager.Pager.PagerListener;

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
	private boolean init = true;
	
	public PagerComponent(final Pager pager, PageChangeListener pageChangeListener) {
		this.pageChangeListener = pageChangeListener;
		initComponents();
		initActions();
		pager.setPagerListener(new PagerListener() {
			
			public void textChanged() {
				setLastPageNumber(pager.getLastPageNumber());
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
		pageInput.addValueChangeListener(new ValueChangeListener() {
			
			public void valueChange(ValueChangeEvent event) {
				currentPageNumber = pageInput.getNumber();
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
		
		pageInput.addValidator(new Validator() {
			public boolean isValid(Object value) {
				if (value != null) {
					try {
						int i = Integer.valueOf(value.toString());
						if (i>=1 && i<=lastPageNumber) {
							return true;
						}
					}
					catch (NumberFormatException nfe) {
						return false;
					}
				}
				return false;
			}
			
			public void validate(Object value) throws InvalidValueException {
				if (!isValid(value)) {
					throw new InvalidValueException(MessageFormat.format(Messages.getString("PagerComponent.numberRequirements"), lastPageNumber));  //$NON-NLS-1$
				}
			}
		});
		pageInput.setInvalidAllowed(false);
		pageInput.setInvalidCommitted(false);
	}

	private void initComponents() {
		setSpacing(true);
		firstPageButton = new Button();
		addComponent(firstPageButton);
		previousPageButton = new Button();
		addComponent(previousPageButton);
		pageInput = new NumberField(1);
		pageInput.setImmediate(true);
		pageInput.setStyleName("pager-pageinput"); //$NON-NLS-1$
		addComponent(pageInput);
		lastPageNumberLabel = new Label(Messages.getString("PagerComponent.notAvailableAbbr")); //$NON-NLS-1$
		addComponent(lastPageNumberLabel);
		this.setComponentAlignment(lastPageNumberLabel, Alignment.MIDDLE_LEFT);
		nextPageButton = new Button();
		addComponent(nextPageButton);
		lastPageButton = new Button();
		addComponent(lastPageButton);
	}
	
	@Override
	public void attach() {
		super.attach();
		
		if (init) {
			ClassResource firstPageIcon = 
					new ClassResource(
							"tagger/pager/resources/page-first.gif"); //$NON-NLS-1$
			firstPageButton.setIcon(firstPageIcon);
			ClassResource previousPageIcon = 
					new ClassResource(
							"tagger/pager/resources/page-prev.gif"); //$NON-NLS-1$
			previousPageButton.setIcon(previousPageIcon);
			ClassResource nextPageIcon = 
					new ClassResource(
							"tagger/pager/resources/page-next.gif"); //$NON-NLS-1$
			nextPageButton.setIcon(nextPageIcon);
			ClassResource lastPageIcon = new ClassResource(
					"tagger/pager/resources/page-last.gif"); //$NON-NLS-1$
			lastPageButton.setIcon(lastPageIcon);
			init = false;
		}
	}

	public void setPage(int pageNumber) {
		currentPageNumber = pageNumber;
		pageInput.setNumber(currentPageNumber);
	}
}
