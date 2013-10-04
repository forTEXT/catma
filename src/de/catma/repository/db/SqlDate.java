package de.catma.repository.db;

import java.util.Calendar;
import java.util.Date;

public class SqlDate {

	public static java.sql.Date from(Date date) {
		if (date == null) {
			return null;
		}
		return new java.sql.Date(date.getTime());
	}
	
	public static java.sql.Date from(Calendar calendar) {
		if (calendar == null) {
			return null;
		}
		return new java.sql.Date(calendar.getTimeInMillis());
	}
}
