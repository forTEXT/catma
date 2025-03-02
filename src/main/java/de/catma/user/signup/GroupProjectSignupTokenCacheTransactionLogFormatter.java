package de.catma.user.signup;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class GroupProjectSignupTokenCacheTransactionLogFormatter extends Formatter {

	@Override
	public String format(LogRecord record) {
		return record.getMessage()+"\n";
	}

}
