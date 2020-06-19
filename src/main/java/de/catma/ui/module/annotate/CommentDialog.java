package de.catma.ui.module.annotate;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.UI;

import de.catma.backgroundservice.BackgroundServiceProvider;
import de.catma.ui.dialog.AbstractOkCancelDialog;
import de.catma.ui.dialog.SaveCancelListener;

public class CommentDialog extends AbstractOkCancelDialog<String> {

	private TextArea textInput;
	private boolean abortClose = false;
	private ScheduledFuture<?> scheduledClosing;

	public CommentDialog(SaveCancelListener<String> saveCancelListener) {
		this(null, saveCancelListener);
	}

	public CommentDialog(String comment, SaveCancelListener<String> saveCancelListener) {
		super(comment==null?"Add Comment":"Edit Comment", saveCancelListener);
	}

	@Override
	protected void addContent(ComponentContainer content) {
		this.textInput = new TextArea();
		this.textInput.setSizeFull();
		this.textInput.focus();
		content.addComponent(textInput);
		if (content instanceof AbstractOrderedLayout) {
			((AbstractOrderedLayout) content).setExpandRatio(this.textInput, 1.0f);
		}
		
		this.textInput.addFocusListener(event -> abortClosing());
		this.textInput.addBlurListener(event -> startClosing());
		
		getBtOk().addFocusListener(event -> abortClosing());
		getBtOk().addBlurListener(event -> startClosing());
		
		getBtCancel().addFocusListener(event -> abortClosing());
		getBtCancel().addBlurListener(event -> startClosing());
		
		addFocusListener(event -> abortClosing());
		addBlurListener(event -> startClosing());

		addCloseListener(event -> abortClosing());
	}
	
	private void abortClosing() {
		abortClose = true;
		if (this.scheduledClosing != null) {
			this.scheduledClosing.cancel(false);
		}
	}

	private void startClosing() {
		if (this.scheduledClosing != null && !this.scheduledClosing.isCancelled()) {
			return;
		}
		abortClose = false;
		final UI ui = UI.getCurrent();
		final BackgroundServiceProvider provider = (BackgroundServiceProvider)ui;
		this.scheduledClosing = provider.accuireBackgroundService().schedule(() -> {
			ui.accessSynchronously(() -> {
				if (!abortClose) {
					if (this.textInput.getValue() != null && !this.textInput.getValue().isEmpty()) {
						setModal(true);
					}
					else {
						handleCancelPressed();
					}
					ui.push();
				}
			});			
		}, 2, TimeUnit.SECONDS);
	}

	@Override
	protected void layoutWindow() {
		setWidth("20%");
		setHeight("30%");
		setModal(false);
	}
	
	public void show(int x, int y) {
		show();
		int height = UI.getCurrent().getPage().getBrowserWindowHeight();
		int width = UI.getCurrent().getPage().getBrowserWindowWidth();
		
		height = height-(30*height/100);
		width = width-(20*width/100);
		
		if (x > width) {
			x = width;
		}
		if (y > height) {
			y = height;
		}
		
		setPosition(x, y);
		
	}

	@Override
	protected String getResult() {
		return textInput.getValue();
	}

}
