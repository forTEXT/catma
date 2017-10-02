package de.catma.repository.git.managers.gitlab4j_api_custom;

import de.catma.repository.git.managers.gitlab4j_api_custom.models.ImpersonationToken;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.User;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.*;

public class CustomUserApiTest {
	private Properties catmaProperties;
	private GitLabApi gitLabApi;

	private CustomUserApi customUserApi;

	private Integer createdUserId;

	public CustomUserApiTest() throws Exception {
		String propertiesFile = System.getProperties().containsKey("prop") ?
				System.getProperties().getProperty("prop") : "catma.properties";

		this.catmaProperties = new Properties();
		this.catmaProperties.load(new FileInputStream(propertiesFile));

		this.gitLabApi = new GitLabApi(
			this.catmaProperties.getProperty("GitLabServerUrl"),
			this.catmaProperties.getProperty("GitLabAdminPersonalAccessToken")
		);
	}

	@Before
	public void setUp() throws Exception {
		this.customUserApi = new CustomUserApi(this.gitLabApi);
	}

	@After
	public void tearDown() throws Exception {
		if (this.createdUserId != null) {
			this.gitLabApi.getUserApi().deleteUser(this.createdUserId);
			await().until(() -> {
				try {
					this.gitLabApi.getUserApi().getUser(this.createdUserId);
					return false;
				}
				catch (GitLabApiException e) {
					return true;
				}
			});
		}
	}

	@Test
	public void createImpersonationToken() throws Exception {
		// create a user
		User user = new User();
		user.setEmail("testuser@catma.de");
		user.setUsername("testuser");
		user.setName("Test User");

		user = this.gitLabApi.getUserApi().createUser(user, "password", null);
		this.createdUserId = user.getId();

		// create an impersonation token for the user
		ImpersonationToken impersonationToken = this.customUserApi.createImpersonationToken(
			this.createdUserId, "test-token", null, null
		);

		assertNotNull(impersonationToken);
		assert impersonationToken.id > 0;
		assertFalse(impersonationToken.revoked);
		assertArrayEquals(new String[] {"api"}, impersonationToken.scopes);
		assert impersonationToken.token.length() > 0;
		assert impersonationToken.active;
		assert impersonationToken.impersonation;
		assertEquals("test-token", impersonationToken.name);
		assert impersonationToken.createdAt.length() > 0;
		assertNull(impersonationToken.expiresAt);

		List<ImpersonationToken> impersonationTokens = this.customUserApi.getImpersonationTokens(
			this.createdUserId, null
		);

		assertEquals(1, impersonationTokens.size());
		assertEquals(impersonationToken.id, impersonationTokens.get(0).id);
	}

	@Test
	public void createImpersonationTokenWithExpiryAndScopes() throws Exception {
		// create a user
		User user = new User();
		user.setEmail("testuser@catma.de");
		user.setUsername("testuser");
		user.setName("Test User");

		user = this.gitLabApi.getUserApi().createUser(user, "password", null);
		this.createdUserId = user.getId();

		// create an impersonation token for the user, with expiresAt and scopes
		GregorianCalendar cal = new GregorianCalendar();
		cal.add(Calendar.DATE, 1);
		Date expiryDate = cal.getTime();

		ImpersonationToken impersonationToken = this.customUserApi.createImpersonationToken(
			this.createdUserId, "test-token", expiryDate, new String[] {"api", "read_user"}
		);

		assertNotNull(impersonationToken);
		assert impersonationToken.id > 0;
		assertFalse(impersonationToken.revoked);
		assertArrayEquals(new String[] {"api", "read_user"}, impersonationToken.scopes);
		assert impersonationToken.token.length() > 0;
		assert impersonationToken.active;
		assert impersonationToken.impersonation;
		assertEquals("test-token", impersonationToken.name);
		assert impersonationToken.createdAt.length() > 0;

		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		String expectedIsoFormattedExpiryDate = df.format(expiryDate);

		assertEquals(expectedIsoFormattedExpiryDate, impersonationToken.expiresAt);

		List<ImpersonationToken> impersonationTokens = this.customUserApi.getImpersonationTokens(
			this.createdUserId, null
		);

		assertEquals(1, impersonationTokens.size());
		assertEquals(impersonationToken.id, impersonationTokens.get(0).id);
	}
}
