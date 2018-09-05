package de.catma.ui.repository;

import java.util.HashSet;
import java.util.Set;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.server.ThemeResource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Image;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;
import com.vaadin.v7.ui.TextField;
import com.vaadin.v7.ui.VerticalLayout;

import de.catma.document.repository.RepositoryPropertyKey;
import de.catma.ui.dialog.SaveCancelListener;

public class SpamProtectionDialog extends Window {
	
	private TextField input;
	private Button btOk;

	public SpamProtectionDialog(SaveCancelListener<Void> saveCancelListener) {
		super(Messages.getString("SpamProtectionDialog.guestAccess")); //$NON-NLS-1$
		
		initComponents();
		initActions(saveCancelListener);
	}

	private void initActions(SaveCancelListener<Void> saveCancelListener) {
		
		btOk.addClickListener(event -> {
			try {
				String value = input.getValue();
				if (value == null) {
					value = ""; //$NON-NLS-1$
				}
				
				Set<String> answers = new HashSet<>();
				
				String correctValues = RepositoryPropertyKey.SpamProtectionAnswer.getValue();
				
				for (String correctValue : correctValues.split(",")) { //$NON-NLS-1$
					answers.add(correctValue.trim().toLowerCase());
				}
				
				if (answers.contains(value.trim().toLowerCase())) {
					saveCancelListener.savePressed(null);
				}
				else {
					Notification.show(
						Messages.getString("SpamProtectionDialog.infoTitle"),  //$NON-NLS-1$
						Messages.getString("SpamProtectionDialog.robotFeedback"),  //$NON-NLS-1$
						Type.HUMANIZED_MESSAGE);
					saveCancelListener.cancelPressed();
				}
			}
			finally {
				UI.getCurrent().removeWindow(SpamProtectionDialog.this);
			}
			
		});
		
	}

	private void initComponents() {
		
		setWidth("300px"); //$NON-NLS-1$
		setHeight("300px"); //$NON-NLS-1$
		
		setModal(true);
		setClosable(false);
		setResizable(false);
		
		center();
		
		VerticalLayout content = new VerticalLayout();
		content.setMargin(true);
		content.setSizeFull();
		content.setSpacing(true);
		
		setContent(content);
		
		Image image = new Image("", new ThemeResource("catma-logo.png")); //$NON-NLS-1$ //$NON-NLS-2$
		content.addComponent(image);
		content.setComponentAlignment(image, Alignment.TOP_CENTER);
		input = new TextField(RepositoryPropertyKey.SpamProtectionQuestion.getValue());
		input.focus();
		content.addComponent(input);
		content.setComponentAlignment(input, Alignment.TOP_CENTER);
		
		btOk =new Button(Messages.getString("SpamProtectionDialog.ok")); //$NON-NLS-1$
		content.addComponent(btOk);
		content.setComponentAlignment(btOk, Alignment.MIDDLE_CENTER);
		btOk.setClickShortcut(KeyCode.ENTER);
		
	}

	public void show() {
		UI.getCurrent().addWindow(this);
	}
}
