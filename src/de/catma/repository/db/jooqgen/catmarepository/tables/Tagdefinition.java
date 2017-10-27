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
import org.jooq.ForeignKey;
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
public class Tagdefinition extends TableImpl<Record> {

	private static final long serialVersionUID = -2133969230;

	/**
	 * The reference instance of <code>catmarepository.tagdefinition</code>
	 */
	public static final Tagdefinition TAGDEFINITION = new Tagdefinition();

	/**
	 * The class holding records for this type
	 */
	@Override
	public Class<Record> getRecordType() {
		return Record.class;
	}

	/**
	 * The column <code>catmarepository.tagdefinition.tagDefinitionID</code>.
	 */
	public final TableField<Record, Integer> TAGDEFINITIONID = createField("tagDefinitionID", org.jooq.impl.SQLDataType.INTEGER.nullable(false), this, "");

	/**
	 * The column <code>catmarepository.tagdefinition.uuid</code>.
	 */
	public final TableField<Record, byte[]> UUID = createField("uuid", org.jooq.impl.SQLDataType.BINARY.length(16).nullable(false), this, "");

	/**
	 * The column <code>catmarepository.tagdefinition.version</code>.
	 */
	public final TableField<Record, Timestamp> VERSION = createField("version", org.jooq.impl.SQLDataType.TIMESTAMP.nullable(false), this, "");

	/**
	 * The column <code>catmarepository.tagdefinition.name</code>.
	 */
	public final TableField<Record, String> NAME = createField("name", org.jooq.impl.SQLDataType.VARCHAR.length(255).nullable(false), this, "");

	/**
	 * The column <code>catmarepository.tagdefinition.tagsetDefinitionID</code>.
	 */
	public final TableField<Record, Integer> TAGSETDEFINITIONID = createField("tagsetDefinitionID", org.jooq.impl.SQLDataType.INTEGER.nullable(false), this, "");

	/**
	 * The column <code>catmarepository.tagdefinition.parentID</code>.
	 */
	public final TableField<Record, Integer> PARENTID = createField("parentID", org.jooq.impl.SQLDataType.INTEGER.defaulted(true), this, "");

	/**
	 * The column <code>catmarepository.tagdefinition.parentUuid</code>.
	 */
	public final TableField<Record, byte[]> PARENTUUID = createField("parentUuid", org.jooq.impl.SQLDataType.BINARY.length(16).defaulted(true), this, "");

	/**
	 * Create a <code>catmarepository.tagdefinition</code> table reference
	 */
	public Tagdefinition() {
		this("tagdefinition", null);
	}

	/**
	 * Create an aliased <code>catmarepository.tagdefinition</code> table reference
	 */
	public Tagdefinition(String alias) {
		this(alias, TAGDEFINITION);
	}

	private Tagdefinition(String alias, Table<Record> aliased) {
		this(alias, aliased, null);
	}

	private Tagdefinition(String alias, Table<Record> aliased, Field<?>[] parameters) {
		super(alias, Catmarepository.CATMAREPOSITORY, aliased, parameters, "");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Identity<Record, Integer> getIdentity() {
		return Keys.IDENTITY_TAGDEFINITION;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public UniqueKey<Record> getPrimaryKey() {
		return Keys.KEY_TAGDEFINITION_PRIMARY;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<UniqueKey<Record>> getKeys() {
		return Arrays.<UniqueKey<Record>>asList(Keys.KEY_TAGDEFINITION_PRIMARY, Keys.KEY_TAGDEFINITION_UK_TDEF_TSDEF_UUID);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<ForeignKey<Record, ?>> getReferences() {
		return Arrays.<ForeignKey<Record, ?>>asList(Keys.FK_TDEF_TAGSETDEFINITIONID, Keys.FK_TDEF_PARENTID);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Tagdefinition as(String alias) {
		return new Tagdefinition(alias, this);
	}

	/**
	 * Rename this table
	 */
	public Tagdefinition rename(String name) {
		return new Tagdefinition(name, null);
	}
}
