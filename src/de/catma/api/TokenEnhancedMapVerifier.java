package de.catma.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.jboss.aerogear.security.otp.Totp;
import org.jboss.aerogear.security.otp.api.Clock;
import org.restlet.security.MapVerifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import de.catma.document.repository.RepositoryPropertyKey;
import de.catma.util.CloseSafe;

public class TokenEnhancedMapVerifier extends MapVerifier {
	private Properties properties;
	private String apiUsersFilePath;

	public TokenEnhancedMapVerifier(Properties properties, String apiUsersFilePath) {
		super();
		this.properties = properties;
		this.apiUsersFilePath = apiUsersFilePath;
	}
	
	@Override
	public int verify(String identifier, char[] secret) {
		try {
			loadValidApiUsers();
		} catch (IOException e1) {
			Logger.getLogger(TokenEnhancedMapVerifier.class.getName()).log(Level.WARNING, "unable to update api users");
		}
		
		int result = super.verify(identifier, secret);
		if (result != RESULT_VALID) {
			try {
				Totp totp = new Totp(
						properties.getProperty(RepositoryPropertyKey.otpSecret.name())+identifier, 
						new Clock(Integer.valueOf(
							properties.getProperty(
								RepositoryPropertyKey.otpDuration.name()))));
				if (totp.verify(new String(secret))) {
					return RESULT_VALID;
				}
			}
			catch (Exception e) {
				Logger.getLogger(TokenEnhancedMapVerifier.class.getName()).log(Level.INFO, 
						"invalid identifier/secret combination", e);
			}
		}
		return result;
	}
	
	private void loadValidApiUsers() 
			throws IOException {
		File apiUsersFile = new File(apiUsersFilePath);
		
		if (apiUsersFile.exists()) {
			FileInputStream fis = new FileInputStream(apiUsersFile);
			try {
				ObjectMapper mapper = new ObjectMapper();
				
				String apiUsersJson = IOUtils.toString(fis, "UTF-8");
				ArrayNode apiUsersList = mapper.readValue(apiUsersJson, ArrayNode.class);
				
				for (int i=0; i<apiUsersList.size(); i++) {
					JsonNode entry = apiUsersList.get(i);
					getLocalSecrets().put(
						entry.get("u").asText(), 
						entry.get("p").asText().toCharArray());
				}
		
			}
			finally {
				CloseSafe.close(fis);
			}
		}
	}
	

}
