package de.catma.ui.module.main.signup;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.client.ClientConfig;

import de.catma.properties.CATMAPropertyKey;

/**
 * verifies reCaptcha v2 / v3 by using restclient and wraps the result in a
 * result Bean {@link GoogleVerificationResult}
 * @author db
 *
 */
public class GoogleRecaptchaVerifier {

	private final ClientConfig config;	
	private final Client client;
	private final WebTarget webtarget;
	private final Logger logger = Logger.getLogger(this.getClass().getName());

	
	public GoogleRecaptchaVerifier(){
	    config = new ClientConfig();
	    client = ClientBuilder.newClient(config);
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
		logger.log(Level.INFO,"verifying token: " + token );
		return webtarget
	            .request(MediaType.APPLICATION_JSON)
	            .post(
	            	Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE), 
	            	GoogleVerificationResult.class);
	}

}
