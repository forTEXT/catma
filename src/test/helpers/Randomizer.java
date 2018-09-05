package helpers;

import de.catma.repository.db.DBUser;
import org.apache.commons.lang3.RandomStringUtils;

public class Randomizer {
	public static DBUser getDbUser() {
		Integer userId = Integer.parseInt(RandomStringUtils.randomNumeric(3));

		return new DBUser(
			userId,
			String.format("catma-testuser-%s", userId),
			false, false, false
		);
	}

	public static String getGroupName() {
		return RandomStringUtils.randomAlphabetic(10).toLowerCase();
	}

	public static String getRepoName() {
		return RandomStringUtils.randomAlphabetic(10).toLowerCase();
	}
}
