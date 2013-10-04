package de.catma.repository.db;

import java.util.Calendar;
import java.util.Date;

public class SqlTimestamp {

	public static java.sql.Timestamp from(Date date) {
		if (date == null) {
			return null;
		}

		return new java.sql.Timestamp(date.getTime());
	}
	
	public static java.sql.Timestamp from(Calendar calendar) {
		if (calendar == null) {
			return null;
		}
		return new java.sql.Timestamp(calendar.getTimeInMillis());
	}
}
