package de.catma.ui.dialog;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import com.github.appreciated.material.MaterialTheme;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

public abstract class AbstractOkCancelDialog<T> extends Window {
	
	private Button btOk;
	private Button btCancel;
	protected SaveCancelListener<T> saveCancelListener;
	private boolean init;
	private HorizontalLayout buttonPanel;
	private CloseShortcut closeShortcut;

	public AbstractOkCancelDialog(
			String dialogCaption, SaveCancelListener<T> saveCancelListener) {
		super(dialogCaption);
		this.saveCancelListener = saveCancelListener;
		
		removeAllCloseShortcuts();
		
		this.closeShortcut = new CloseShortcut(this, KeyCode.ESCAPE) {
			public void handleAction(Object sender, Object target) {
				AbstractOkCancelDialog.this.close();
				if (saveCancelListener != null) {
					saveCancelListener.cancelPressed();
				}
			};
		};
		
		addAction(closeShortcut);
		setShowMaximizeRestoreIcon(false);
	}
	
	@Override
	public Collection<CloseShortcut> getCloseShortcuts() {
		if (closeShortcut != null) {
			Collection<CloseShortcut> col = 
					new HashSet<CloseShortcut>(super.getCloseShortcuts());
			col.add(closeShortcut);
			return Collections.unmodifiableCollection(col);
		}
		else {
			return super.getCloseShortcuts();
		}
	}
	
	@Override
	public void removeCloseShortcut(int keyCode, int... modifiers) {
		super.removeCloseShortcut(keyCode, modifiers);
		if (keyCode == KeyCode.ESCAPE) {
			if (closeShortcut != null) {
				removeAction(closeShortcut);
			}
			closeShortcut = null;
		}
	}
	
	@Override
	public void removeAllCloseShortcuts() {
		super.removeAllCloseShortcuts();
		removeCloseShortcut(KeyCode.ESCAPE);
	}
	
	private void initActions() {
		btCancel.addClickListener(new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				handleCancelPressed();
			}
		});
		
		btOk.addClickListener(new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				handleOkPressed();
			}
		});
	}
	
	protected void handleOkPressed() {
		T result = getResult();
		if (isAttached()) {
			UI.getCurrent().removeWindow(AbstractOkCancelDialog.this);
		}
		this.saveCancelListener.savePressed(result);
	}
	
	protected void handleCancelPressed() {
		if (isAttached()) {
			UI.getCurrent().removeWindow(AbstractOkCancelDialog.this);
		}
		this.saveCancelListener.cancelPressed();
	}

	@Override
	public void attach() {
		super.attach();
		if (!init) {
			initComponents();
			initActions();
			init = true;
		}
		this.bringToFront();
	}
	
	protected boolean isEnterClickShortcut() {
		return true;
	}

	protected void layoutWindow(){
		setWidth("50%");
		setHeight("50%");
	}

	protected void layoutContent(VerticalLayout layout) {
		layout.setSizeFull();
		layout.setMargin(true);
		layout.setSpacing(true);
	}
	
	private void initComponents() {
		setModal(true);
		setClosable(false);
		layoutWindow();
	
		VerticalLayout content = new VerticalLayout();
		layoutContent(content);

		buttonPanel = new HorizontalLayout();
		
		btOk = new Button(getOkCaption());
		btOk.addStyleName(MaterialTheme.BUTTON_FLAT);
		btOk.addStyleName(MaterialTheme.BUTTON_PRIMARY);
		
		if (isEnterClickShortcut()) {
			btOk.setClickShortcut(KeyCode.ENTER);
		}
		
		btCancel = new Button(getCancelCaption());
		btCancel.addStyleName(MaterialTheme.BUTTON_FLAT);
		buttonPanel.setSpacing(true);
		buttonPanel.addComponent(btOk);
		buttonPanel.addComponent(btCancel);
		
		addContent(content);
		
		layoutButtonPanel(content);
		
		setContent(content);
	}
	
	protected void layoutButtonPanel(ComponentContainer content) { 
		content.addComponent(buttonPanel);
		if (content instanceof AbstractOrderedLayout) {
			((AbstractOrderedLayout) content).setComponentAlignment(
					buttonPanel, Alignment.BOTTOM_RIGHT);
		}
	}
	
	protected String getCancelCaption() {
		return "Cancel";
	}

	protected String getOkCaption() {
		return "Ok";
	}
	
	protected abstract void addContent(ComponentContainer content);

	public void show() {
		if (!this.isAttached()) {
			UI.getCurrent().addWindow(this);
		}
		else {
			this.bringToFront();
		}
	}
	
	protected abstract T getResult();
	
	protected Button getBtOk() {
		return btOk;
	}
	
	protected Button getBtCancel() {
		return btCancel;
	}

	protected HorizontalLayout getButtonPanel() {
		return buttonPanel;
	}

	public void setShowMaximizeRestoreIcon(boolean show) {
		if (show) {
			removeStyleName("abstract-ok-cancel-dialog-hide-maximize-restore-icon");
		}
		else {
			addStyleName("abstract-ok-cancel-dialog-hide-maximize-restore-icon");			
		}
	}
}
