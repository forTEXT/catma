package de.catma.ui.dialog;

import org.vaadin.dialogs.ConfirmDialog;

import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;

public final class BeyondResponsibilityConfirmDialog {
	public static interface Action {
		public void execute();
	}
	
	public static void executeWithConfirmDialog(
			boolean needsConfirmation, boolean authorized, Action action) {
		if (needsConfirmation) {
			if (!authorized) {
				Notification.show("Info", "You do not have the necessary privileges to alter this resource!", Type.HUMANIZED_MESSAGE);
			}
			else {
				ConfirmDialog.show(UI.getCurrent(), 
					"Warning",
					"One or more selected resources are beyond your responsibility! "
					+ "Altering these resources might result in conflicts with operations of other project members!\n\n"
					+ "Do you want to proceed?",
					"Yes",
					"Cancel",
					new ConfirmDialog.Listener() {
						
						@Override
						public void onClose(ConfirmDialog dialog) {
							if (dialog.isConfirmed()) {
								action.execute();
							}
						}
					});
			}
		}
		else {
			action.execute();
		}
	}
}
