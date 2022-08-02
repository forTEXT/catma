package de.catma.ui.module.main.signup;

import de.catma.properties.CATMAPropertyKey;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJsonProvider;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * verifies reCaptcha v2 / v3 by using restclient and wraps the result in a
 * result Bean {@link GoogleVerificationResult}
 * @author db
 *
 */
public class GoogleRecaptchaVerifier {

	private final Client client;
	private final WebTarget webtarget;
	private final Logger logger = Logger.getLogger(this.getClass().getName());

	
	public GoogleRecaptchaVerifier(){
	    client = ClientBuilder.newClient().register(JacksonJsonProvider.class);
	    webtarget = client.target("https://www.google.com/recaptcha/api/siteverify");
	}
	
	/**
	 * Verifies a given token by using a restclient. It blocks during request.
	 *   
	 * @param token
	 * @return verification result
	 */
	public GoogleVerificationResult verify(String token){
		Form form = new Form();
		form.param("secret", CATMAPropertyKey.Google_recaptchaSecretKey.getValue());
		form.param("response", token);

		logger.info("Verifying reCAPTCHA token: " + token);

		return webtarget
	            .request(MediaType.APPLICATION_JSON)
	            .post(
	            	Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE), 
	            	GoogleVerificationResult.class);
	}

}
