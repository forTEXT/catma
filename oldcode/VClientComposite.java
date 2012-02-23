package de.catma.ui.client.ui;

import java.util.Map;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.vaadin.terminal.gwt.client.ApplicationConnection;
import com.vaadin.terminal.gwt.client.Paintable;
import com.vaadin.terminal.gwt.client.UIDL;
import com.vaadin.terminal.gwt.client.VConsole;
import com.vaadin.terminal.gwt.client.ui.VButton;
import com.vaadin.terminal.gwt.client.ui.VTextField;
import com.vaadin.terminal.gwt.client.ui.VVerticalLayout;

public class VClientComposite extends VVerticalLayout {
	
	private static class BlockingApplicationCon extends ApplicationConnection {
		public BlockingApplicationCon() {
			// TODO Auto-generated constructor stub
		}
		@Override
		public void updateVariable(String paintableId, String variableName,
				boolean newValue, boolean immediate) {
		}
		@Override
		public void updateVariable(String paintableId, String variableName,
				double newValue, boolean immediate) {
		}
		@Override
		public void updateVariable(String paintableId, String variableName,
				float newValue, boolean immediate) {
		}
		@Override
		public void updateVariable(String paintableId, String variableName,
				int newValue, boolean immediate) {
		}
		@Override
		public void updateVariable(String paintableId, String variableName,
				Map<String, Object> map, boolean immediate) {
		}
		@Override
		public void updateVariable(String paintableId, String variableName,
				long newValue, boolean immediate) {
		}
		@Override
		public void updateVariable(String paintableId, String variableName,
				Object[] values, boolean immediate) {
		}
		@Override
		public void updateVariable(String paintableId, String variableName,
				Paintable newValue, boolean immediate) {
		}
		@Override
		public void updateVariable(String paintableId, String variableName,
				String newValue, boolean immediate) {
		}
		@Override
		public void updateVariable(String paintableId, String variableName,
				String[] values, boolean immediate) {
		}
		@Override
		public void sendPendingVariableChanges() {
		}
		
		@Override
		public boolean hasEventListeners(Paintable paintable,
				String eventIdentifier) {
			return false;
		}
		
		
	}
	
	private static class BButton extends VButton {
		public BButton() {
			this.client = new BlockingApplicationCon();
		}
		
		@Override
		public void updateFromUIDL(UIDL uidl, ApplicationConnection client) {
		}
		
	}
	
	private static class BTextField extends VTextField {
		
		public BTextField() {
			this.client = new BlockingApplicationCon();
		}
		
		@Override
		public void updateFromUIDL(UIDL uidl, ApplicationConnection client) {
		}
	}

	
	public VClientComposite() {
		super();
		initComponents();
	}

	private void initComponents() {
		final VButton btEnable = new BButton();
		
		btEnable.setText("En-/Disable");
		VConsole.log("pos1");
		add(btEnable, getElement());
		VConsole.log("pos2");
		final VTextField textInput = new BTextField();
		textInput.setText("Hello");
		textInput.setEnabled(false);
		VConsole.log("pos3");
		add(textInput, getElement());
		VConsole.log("pos4");
		btEnable.addClickHandler(new ClickHandler() {
			
			public void onClick(ClickEvent event) {
				textInput.setEnabled(!textInput.isEnabled());
				
			}
		});
		VConsole.log("pos5");
	}
	
	@Override
	public void updateFromUIDL(UIDL uidl, ApplicationConnection client) {
		VConsole.log("pos6");
//		super.updateFromUIDL(uidl, client);
		VConsole.log("pos7");
//		if (client.updateComponent(this, uidl, true)) {
//			VConsole.log("pos8");
//            return;
//        }
		VConsole.log("pos9");
	}
}
