package de.catma.ui.module.dashboard;

import com.vaadin.data.ValidationResult;
import com.vaadin.data.Validator;
import com.vaadin.data.ValueContext;

import java.util.regex.Pattern;

/**
 * Checks if the given group name is valid.
 */
public class GroupNameValidator implements Validator<String>{
    // Must start with letter, digit, emoji, or underscore. Can also contain periods, dashes, spaces, and parentheses.
    private final Pattern groupNamePattern = Pattern.compile("^[\\p{Alnum}_][\\p{Alnum}_\\.\\- \\(\\)]+");

    @Override
    public ValidationResult apply(String value, ValueContext context) {
        if (value == null || value.trim().isEmpty()) {
            return ValidationResult.error("Group name can't be empty");
        }

        if (value.length() < 2 || value.length() > 50) {
            return ValidationResult.error("Group name must be between 2 and 50 characters long");
        }

        if (!groupNamePattern.matcher(value).matches()) {
            return ValidationResult.error(
                    "Group name must start with letter, digit or underscore. Can also contain periods, dashes, spaces and parentheses."
            );
        }

        return ValidationResult.ok();
    }
}
