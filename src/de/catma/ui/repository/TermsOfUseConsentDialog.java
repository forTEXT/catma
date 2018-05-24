package de.catma.ui.repository;

import com.vaadin.server.ExternalResource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import de.catma.ui.dialog.SaveCancelListener;

public class TermsOfUseConsentDialog extends Window {

	private CheckBox cbAccept;
	private Button btOk;
	private Button btCancel;
	private UI catmaApplication;

	public TermsOfUseConsentDialog(UI catmaApplication, SaveCancelListener<Boolean> saveCancelListener) {
		this.catmaApplication = catmaApplication;
		initComponents();
		initActions(saveCancelListener);
	}

	private void initActions(final SaveCancelListener<Boolean> saveCancelListener) {
		cbAccept.addValueChangeListener(event -> btOk.setEnabled(cbAccept.getValue()));
	
		btOk.addClickListener(event -> {
			saveCancelListener.savePressed(cbAccept.getValue());
			catmaApplication.removeWindow(TermsOfUseConsentDialog.this);
		});
		btCancel.addClickListener(event -> {
			saveCancelListener.cancelPressed();
			catmaApplication.removeWindow(TermsOfUseConsentDialog.this);	
		});
	}

	private void initComponents() {
		setModal(true);
		setWidth("50%");
		setHeight("50%");
		setClosable(false);
		setCaption(Messages.getString("TermsOfUseConsentDialog.title"));  //$NON-NLS-1$
		
		VerticalLayout content = new VerticalLayout();
		content.setMargin(true);
		content.setSpacing(true);
		content.setSizeFull();
		
		Label termsOfUseInfo = new Label(Messages.getString("TermsOfUseConsentDialog.termsOfUseIntro"));  //$NON-NLS-1$
		
		content.addComponent(termsOfUseInfo);
		
		Link termsOfUseLink = new Link(Messages.getString("TermsOfUseConsentDialog.termsOfUse"), //$NON-NLS-1$
				new ExternalResource("http://www.catma.de/documentation/terms-of-use-privacy-policy/")); //$NON-NLS-1$
		termsOfUseLink.setTargetName("_blank"); //$NON-NLS-1$
		content.addComponent(termsOfUseLink);
		content.setComponentAlignment(termsOfUseLink, Alignment.MIDDLE_CENTER);
		
		Link privacyLink = new Link(Messages.getString("TermsOfUseConsentDialog.privacy"), //$NON-NLS-1$
				new ExternalResource("http://www.catma.de/documentation/privacy/")); //$NON-NLS-1$
		privacyLink.setTargetName("_blank"); //$NON-NLS-1$
		content.addComponent(privacyLink);
		content.setComponentAlignment(privacyLink, Alignment.MIDDLE_CENTER);

		cbAccept =new CheckBox(Messages.getString("TermsOfUseConsentDialog.acceptTermsOfUse"), false); //$NON-NLS-1$
		cbAccept.setCaptionAsHtml(true);
		content.addComponent(cbAccept);
		
		HorizontalLayout buttonPanel = new HorizontalLayout();
		buttonPanel.setSpacing(true);
		buttonPanel.setWidth("100%");
		btOk = new Button(Messages.getString("TermsOfUseConsentDialog.ok")); //$NON-NLS-1$
		buttonPanel.addComponent(btOk);
		buttonPanel.setComponentAlignment(btOk, Alignment.BOTTOM_RIGHT);
		buttonPanel.setExpandRatio(btOk, 1.0f);
		btOk.setEnabled(false);
		
		btCancel = new Button(Messages.getString("TermsOfUseConsentDialog.cancel")); //$NON-NLS-1$
		buttonPanel.addComponent(btCancel);
		buttonPanel.setComponentAlignment(btCancel, Alignment.BOTTOM_RIGHT);
		
		content.addComponent(buttonPanel);
		
		content.setComponentAlignment(buttonPanel, Alignment.BOTTOM_RIGHT);
		content.setExpandRatio(buttonPanel, 1.0f);
		setContent(content);
	}
	
	public void show() {
		catmaApplication.addWindow(this);
	}
}
