package de.catma.ui.module.main.auth;

import com.vaadin.data.ValidationResult;
import com.vaadin.data.Validator;
import com.vaadin.data.ValueContext;

import java.util.*;

/**
 * This validator attempts to replicate GitLab's own implementation of password validation and thus includes checks based on the email address and username.
 * <p>
 * A validation failure that occurs when interacting with the GitLab API (e.g., via {@link de.catma.repository.git.managers.GitlabManagerPrivileged#createUser}
 * and {@link org.gitlab4j.api.UserApi#createUser}) results in a rather generic error message like "The following fields have validation errors: password", so
 * we try to catch problems early and provide more helpful details to users with this validator.
 * <p>
 * References:
 *  - <a href="https://docs.gitlab.com/user/profile/user_passwords/#block-weak-passwords">GitLab Docs - Password requirements</a>
 *  - <a href="https://gitlab.com/gitlab-org/gitlab/-/blob/master/lib/security/weak_passwords.rb">GitLab source code</a>
 */
public class PasswordValidator implements Validator<String> {
	// GitLab defaults + "catma"
	private static final List<String> FORBIDDEN_WORDS = List.of("gitlab", "devops", "catma");
	// GitLab defaults
	private static final int MIN_LENGTH = 8; // can be changed in Gitlab's settings
	private static final int MAX_LENGTH = 128;
	private static final int MINIMUM_SUBSTRING_SIZE = 4;
	private static final int PASSWORD_SUBSTRING_CHECK_MAX_LENGTH = 64;

	private final int minLength;
	private final int maxLength;
	private final List<String> forbiddenWords;

	private final String emailAddress;
	private String username;

	public PasswordValidator(String emailAddress) {
		this(MIN_LENGTH, MAX_LENGTH, FORBIDDEN_WORDS, emailAddress);
	}

	public PasswordValidator(int minLength, int maxLength, List<String> forbiddenWords, String emailAddress) {
		this.minLength = minLength;
		this.maxLength = Math.min(maxLength, MAX_LENGTH);
		this.forbiddenWords = forbiddenWords;
		this.emailAddress = emailAddress == null ? "" : emailAddress;
		this.username = "";
	}

	public void setUsername(String username) {
		this.username = username == null ? "" : username;
	}

	private String forbiddenWordAppearsInPassword(String password) {
		return containsPredictableSubstring(password, forbiddenWords);
	}

	private String usernameAppearsInPassword(String password) {
		if (username.isBlank()) {
			return null;
		}

		ArrayList<String> substrings = new ArrayList<>();
		substrings.add(username); // full username
		substrings.addAll(List.of(username.split("\\W"))); // any parts delimited by non-word characters

		return containsPredictableSubstring(password, substrings);
	}

	private String emailAppearsInPassword(String password) {
		if (emailAddress.isBlank()) {
			return null;
		}

		ArrayList<String> substrings = new ArrayList<>();
		substrings.add(emailAddress); // full email
		substrings.addAll(List.of(emailAddress.split("@"))); // full part before '@' and full domain name
		substrings.addAll(List.of(emailAddress.split("\\W"))); // any parts delimited by non-word characters

		return containsPredictableSubstring(password, substrings);
	}

	private String containsPredictableSubstring(String password, List<String> substrings) {
		if (password.length() >= PASSWORD_SUBSTRING_CHECK_MAX_LENGTH) {
			return null;
		}

		List<String> filteredSubstrings = substrings.stream()
				.filter(substring -> substring.length() >= MINIMUM_SUBSTRING_SIZE)
				.map(substring -> substring.toLowerCase(Locale.ROOT))
				.toList();

		return filteredSubstrings.stream()
				.filter(substring -> password.toLowerCase(Locale.ROOT).contains(substring))
				.findFirst()
				.orElse(null);
	}

	@Override
	public ValidationResult apply(String value, ValueContext context) {
		if (value == null || value.isBlank()) {
			return ValidationResult.error("Password can't be empty");
		}

		if (value.length() < minLength || value.length() > maxLength) {
			return ValidationResult.error(
					String.format("Password must be between %d and %d characters long", minLength, maxLength)
			);
		}

		String predictableWord = usernameAppearsInPassword(value);
		if (predictableWord == null) {
			predictableWord = emailAppearsInPassword(value);
		}
		if (predictableWord == null) {
			predictableWord = forbiddenWordAppearsInPassword(value);
		}

		if (predictableWord != null) {
			return ValidationResult.error("Password contains predictable word: " + predictableWord);
		}

		return ValidationResult.ok();
	}
}
