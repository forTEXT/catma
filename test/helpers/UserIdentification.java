package helpers;

import java.util.Map;

import com.google.common.collect.Maps;

import de.catma.user.UserProperty;

public class UserIdentification {
	public static Map<String, String> userToMap(String user){
		
		Map<String, String> userIdentification = Maps.newHashMap();
		userIdentification.put(
			UserProperty.identifier.name(), user);
		userIdentification.put(
			UserProperty.provider.name(), "catma");
		
		userIdentification.put(
			UserProperty.email.name(), user + "@catma.de"); //TODO: debugging purposes only
		userIdentification.put(
			UserProperty.name.name(), user);
	
		return userIdentification;
		
	}
}
