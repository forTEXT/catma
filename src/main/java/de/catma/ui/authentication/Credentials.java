package de.catma.ui.authentication;

import java.time.LocalTime;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import de.catma.user.UserProperty;

/**
 * Used to store user credentials in session, an reuse them to aquire a gitlab api 
 * @author db
 *
 */
public interface Credentials {

	public String getImpersonationToken();
	public String getIdentifier();
	public LocalTime getLoginTime();
	public String getProvider();
	public String getName();
	
	public default String getEmail(){
		return getIdentifier();
	}
	
	public default String getUsername(){
		return getIdentifier();
	}
	
	public default Map<String,String> toMap() {
		return new ImmutableMap.Builder<String,String>()
				.put(UserProperty.provider.name(),getProvider())
				.put(UserProperty.identifier.name(),getIdentifier())
				.put(UserProperty.email.name(),getEmail())
				.put(UserProperty.name.name(),getName())
				.build();
	}
}
