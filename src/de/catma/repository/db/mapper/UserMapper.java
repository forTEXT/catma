package de.catma.repository.db.mapper;

import static de.catma.repository.db.jooqgen.catmarepository.Tables.USER;

import org.jooq.Record;
import org.jooq.RecordMapper;

import de.catma.repository.db.DBUser;

public class UserMapper implements RecordMapper<Record, DBUser> {

	public DBUser map(Record record) {
		if (record == null) {
			return null;
		}
		return new DBUser(
			record.getValue(USER.USERID),
			record.getValue(USER.IDENTIFIER),
			record.getValue(USER.LOCKED, Boolean.class),
			record.getValue(USER.GUEST, Boolean.class),
			record.getValue(USER.SPAWNABLE, Boolean.class));
	}

}
