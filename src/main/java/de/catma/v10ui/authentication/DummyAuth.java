package de.catma.v10ui.authentication;

import com.google.common.collect.ImmutableMap;
import de.catma.user.UserProperty;

import java.util.Map;

/**
 * For now just a dummy auth.
 *
 * TODO should be removed and done properly using DI with guice
 * @author db
 */
public class DummyAuth {

    public static String user = System.getProperty("user.name");

    public static Map<String,String> DUMMYIDENT =
            ImmutableMap.<String,String>builder()
                    .put(UserProperty.identifier.name(), user)
                    .put(UserProperty.provider.name(), "catma")
                    .put(UserProperty.email.name(), user + "@catma.de")
                    .put(UserProperty.name.name(), user)
                    .build();

}
