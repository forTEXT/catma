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
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import de.catma.document.repository.RepositoryPropertyKey;
import de.catma.ui.dialog.SaveCancelListener;

public class SpamProtectionDialog extends Window {
	
	private TextField input;
	private Button btOk;

	public SpamProtectionDialog(SaveCancelListener<Void> saveCancelListener) {
		super("Guest Access");
		
		initComponents();
		initActions(saveCancelListener);
	}

	private void initActions(SaveCancelListener<Void> saveCancelListener) {
		
		btOk.addClickListener(event -> {
			try {
				String value = input.getValue();
				if (value == null) {
					value = "";
				}
				
				Set<String> answers = new HashSet<>();
				
				String correctValues = RepositoryPropertyKey.SpamProtectionAnswer.getValue();
				
				for (String correctValue : correctValues.split(",")) {
					answers.add(correctValue.trim().toLowerCase());
				}
				
				if (answers.contains(value.trim().toLowerCase())) {
					saveCancelListener.savePressed(null);
				}
				else {
					Notification.show(
						"Info", 
						"Sorry, you seem to be a robot.", 
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
		
		setWidth("300px");
		setHeight("300px");
		
		setModal(true);
		setClosable(false);
		setResizable(false);
		
		center();
		
		VerticalLayout content = new VerticalLayout();
		content.setMargin(true);
		content.setSizeFull();
		content.setSpacing(true);
		
		setContent(content);
		
		Image image = new Image("", new ThemeResource("catma-logo.png"));
		content.addComponent(image);
		content.setComponentAlignment(image, Alignment.TOP_CENTER);
		input = new TextField(RepositoryPropertyKey.SpamProtectionQuestion.getValue());
		input.focus();
		content.addComponent(input);
		content.setComponentAlignment(input, Alignment.TOP_CENTER);
		
		btOk =new Button("OK");
		content.addComponent(btOk);
		content.setComponentAlignment(btOk, Alignment.MIDDLE_CENTER);
		btOk.setClickShortcut(KeyCode.ENTER);
		
	}

	public void show() {
		UI.getCurrent().addWindow(this);
	}
}
