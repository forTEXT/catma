package de.catma.ui.module.main.auth;

import de.catma.properties.CATMAPropertyKey;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJsonProvider;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import java.util.logging.Logger;

public class GoogleRecaptchaVerifier {
	private final Logger logger = Logger.getLogger(GoogleRecaptchaVerifier.class.getName());

	private final WebTarget webTarget;

	public GoogleRecaptchaVerifier(){
		Client client = ClientBuilder.newClient().register(JacksonJsonProvider.class);
		webTarget = client.target("https://www.google.com/recaptcha/api/siteverify");
	}

	/**
	 * Verifies a given reCAPTCHA v2/3 token by using a REST client. Blocks during the request.
	 *
	 * @param token the token to verify
	 * @return a {@link GoogleVerificationResult}
	 */
	public GoogleVerificationResult verify(String token) {
		Form form = new Form();
		form.param("secret", CATMAPropertyKey.GOOGLE_RECAPTCHA_SECRET_KEY.getValue());
		form.param("response", token);

		logger.info("Verifying reCAPTCHA token: " + token);

		return webTarget
				.request(MediaType.APPLICATION_JSON)
				.post(
					Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE),
					GoogleVerificationResult.class
				);
	}
}
