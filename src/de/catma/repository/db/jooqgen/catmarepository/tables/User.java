/**
 * This class is generated by jOOQ
 */
package de.catma.repository.db.jooqgen.catmarepository.tables;


import de.catma.repository.db.jooqgen.catmarepository.Catmarepository;
import de.catma.repository.db.jooqgen.catmarepository.Keys;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Generated;

import org.jooq.Field;
import org.jooq.Identity;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.TableImpl;


/**
 * This class is generated by jOOQ.
 */
@Generated(
	value = {
		"http://www.jooq.org",
		"jOOQ version:3.7.2"
	},
	comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class User extends TableImpl<Record> {

	private static final long serialVersionUID = 714496817;

	/**
	 * The reference instance of <code>catmarepository.user</code>
	 */
	public static final User USER = new User();

	/**
	 * The class holding records for this type
	 */
	@Override
	public Class<Record> getRecordType() {
		return Record.class;
	}

	/**
	 * The column <code>catmarepository.user.userID</code>.
	 */
	public final TableField<Record, Integer> USERID = createField("userID", org.jooq.impl.SQLDataType.INTEGER.nullable(false), this, "");

	/**
	 * The column <code>catmarepository.user.identifier</code>.
	 */
	public final TableField<Record, String> IDENTIFIER = createField("identifier", org.jooq.impl.SQLDataType.VARCHAR.length(300).nullable(false), this, "");

	/**
	 * The column <code>catmarepository.user.locked</code>.
	 */
	public final TableField<Record, Byte> LOCKED = createField("locked", org.jooq.impl.SQLDataType.TINYINT.nullable(false).defaulted(true), this, "");

	/**
	 * The column <code>catmarepository.user.email</code>.
	 */
	public final TableField<Record, String> EMAIL = createField("email", org.jooq.impl.SQLDataType.VARCHAR.length(300), this, "");

	/**
	 * The column <code>catmarepository.user.lastlogin</code>.
	 */
	public final TableField<Record, Timestamp> LASTLOGIN = createField("lastlogin", org.jooq.impl.SQLDataType.TIMESTAMP, this, "");

	/**
	 * The column <code>catmarepository.user.firstlogin</code>.
	 */
	public final TableField<Record, Timestamp> FIRSTLOGIN = createField("firstlogin", org.jooq.impl.SQLDataType.TIMESTAMP.defaulted(true), this, "");

	/**
	 * The column <code>catmarepository.user.guest</code>.
	 */
	public final TableField<Record, Byte> GUEST = createField("guest", org.jooq.impl.SQLDataType.TINYINT.nullable(false).defaulted(true), this, "");

	/**
	 * The column <code>catmarepository.user.spawnable</code>.
	 */
	public final TableField<Record, Byte> SPAWNABLE = createField("spawnable", org.jooq.impl.SQLDataType.TINYINT.nullable(false).defaulted(true), this, "");

	/**
	 * The column <code>catmarepository.user.termsOfUseConsent</code>.
	 */
	public final TableField<Record, Byte> TERMSOFUSECONSENT = createField("termsOfUseConsent", org.jooq.impl.SQLDataType.TINYINT.nullable(false).defaulted(true), this, "");

	/**
	 * Create a <code>catmarepository.user</code> table reference
	 */
	public User() {
		this("user", null);
	}

	/**
	 * Create an aliased <code>catmarepository.user</code> table reference
	 */
	public User(String alias) {
		this(alias, USER);
	}

	private User(String alias, Table<Record> aliased) {
		this(alias, aliased, null);
	}

	private User(String alias, Table<Record> aliased, Field<?>[] parameters) {
		super(alias, Catmarepository.CATMAREPOSITORY, aliased, parameters, "");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Identity<Record, Integer> getIdentity() {
		return Keys.IDENTITY_USER;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public UniqueKey<Record> getPrimaryKey() {
		return Keys.KEY_USER_PRIMARY;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<UniqueKey<Record>> getKeys() {
		return Arrays.<UniqueKey<Record>>asList(Keys.KEY_USER_PRIMARY);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public User as(String alias) {
		return new User(alias, this);
	}

	/**
	 * Rename this table
	 */
	public User rename(String name) {
		return new User(name, null);
	}
}
