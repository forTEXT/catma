package de.catma.util;

import java.util.logging.Logger;

import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;

import de.catma.properties.CATMAPropertyKey;

public class MailSender {
	private final static Logger LOGGER = Logger.getLogger(MailSender.class.getName());

	public static void sendMail(String toEmailAddress, String subject, String msg) throws EmailException {
		if (CATMAPropertyKey.DEV_MAIL_LOG_ONLY.getBooleanValue()) {
			LOGGER.info(String.format("Would send a mail with subject %1$s to recipient %2$s with body:\n%3$s", subject, toEmailAddress, msg));
		}
		else {
			Email email = new SimpleEmail();
			email.setHostName(CATMAPropertyKey.MAIL_SMTP_HOST.getValue());
			email.setSmtpPort(CATMAPropertyKey.MAIL_SMTP_PORT.getIntValue());
	
			if (CATMAPropertyKey.MAIL_SMTP_AUTHENTICATION_REQUIRED.getBooleanValue()) {
				email.setAuthenticator(
						new DefaultAuthenticator(
								CATMAPropertyKey.MAIL_SMTP_USER.getValue(),
								CATMAPropertyKey.MAIL_SMTP_PASS.getValue()
						)
				);
				email.setStartTLSEnabled(true);
			}
	
			email.setFrom(CATMAPropertyKey.MAIL_FROM.getValue());
			email.setSubject(subject);
			email.setMsg(msg);
			email.addTo(toEmailAddress);
			email.send();
		}
	}
}
