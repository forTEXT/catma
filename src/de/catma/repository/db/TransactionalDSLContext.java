package de.catma.repository.db;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.jooq.*;
import org.jooq.conf.Settings;
import org.jooq.exception.DataAccessException;
import org.jooq.exception.InvalidResultException;
import org.jooq.impl.DSL;

public class TransactionalDSLContext implements DSLContext {
	
	private DSLContext delegateContext;
	
	public TransactionalDSLContext(Connection connection, SQLDialect dialect,
			Settings settings) {
		delegateContext = DSL.using(connection, dialect, settings);
	}

	public TransactionalDSLContext(Connection connection, SQLDialect dialect) {
		delegateContext = DSL.using(connection, dialect);
	}

	public TransactionalDSLContext(ConnectionProvider connectionProvider,
			SQLDialect dialect, Settings settings) {
		delegateContext = DSL.using(connectionProvider.acquire(), dialect, settings);
	}

	public TransactionalDSLContext(ConnectionProvider connectionProvider,
			SQLDialect dialect) {
		delegateContext = DSL.using(connectionProvider.acquire(), dialect);
	}

	public TransactionalDSLContext(DataSource dataSource, SQLDialect dialect,
			Settings settings) {
		try {
			delegateContext = DSL.using(dataSource.getConnection(), dialect, settings);
		} catch (SQLException e) {
			throw new DataAccessException(e.getMessage(), e);
		}
	}

	public TransactionalDSLContext(DataSource dataSource, SQLDialect dialect) {
		try {
			delegateContext = DSL.using(dataSource.getConnection(), dialect);
		} catch (SQLException e) {
			throw new DataAccessException(e.getMessage(), e);
		}
	}

	/**
	 * Startet eine Transaktion, falls schon eine Transaktion offen ist, ist
	 * diese Operation eine NOOP.
	 * 
	 * @throws DataAccessException im Falle einer SQLException
	 */
	public void beginTransaction() throws DataAccessException {
		try {
			Connection con = configuration().connectionProvider().acquire();
			con.setAutoCommit(false);
			
		} catch (SQLException e) {
			throw new DataAccessException(e.getMessage(), e);
		}
	}

	public void commitTransaction() {
		try {
			Connection con = configuration().connectionProvider().acquire();
			con.setAutoCommit(true);
		} catch (SQLException e) {
			throw new DataAccessException(e.getMessage(), e);
		}
	}
	
	public void rollbackTransaction() {
		try {
			Connection con = configuration().connectionProvider().acquire();
			
			if ((con != null) && (!con.isClosed())) {
				con.rollback();
			}
		}
		catch(SQLException e) {
			Logger.getLogger("com.boehling").log(
					Level.SEVERE,
					"rollback fehlgeschlagen", e);
		}
	}
	
	public void close() {
		
		try {
			Connection con = configuration().connectionProvider().acquire();
			
			if ((con != null) && (!con.isClosed())) {
				con.close();
			}
		}
		catch(SQLException e) {
			Logger.getLogger("com.boehling").log(
					Level.SEVERE,
					"close fehlgeschlagen", e);
		}		
	}
	
	@Override
	protected void finalize() throws Throwable {
		try {
			close();
		}
		finally{
			super.finalize();
		}
	}

	public Configuration configuration() {
		return delegateContext.configuration();
	}

	public Schema map(Schema schema) {
		return delegateContext.map(schema);
	}

	public <R extends Record> Table<R> map(Table<R> table) {
		return delegateContext.map(table);
	}

	public Meta meta() {
		return delegateContext.meta();
	}

	public RenderContext renderContext() {
		return delegateContext.renderContext();
	}

	public String render(QueryPart part) {
		return delegateContext.render(part);
	}

	public String renderNamedParams(QueryPart part) {
		return delegateContext.renderNamedParams(part);
	}

	public String renderInlined(QueryPart part) {
		return delegateContext.renderInlined(part);
	}

	public List<Object> extractBindValues(QueryPart part) {
		return delegateContext.extractBindValues(part);
	}

	public Map<String, Param<?>> extractParams(QueryPart part) {
		return delegateContext.extractParams(part);
	}

	public Param<?> extractParam(QueryPart part, String name) {
		return delegateContext.extractParam(part, name);
	}

	public BindContext bindContext(PreparedStatement stmt) {
		return delegateContext.bindContext(stmt);
	}

	public int bind(QueryPart part, PreparedStatement stmt) {
		return delegateContext.bind(part, stmt);
	}

	public void attach(Attachable... attachables) {
		delegateContext.attach(attachables);
	}

	public void attach(Collection<? extends Attachable> attachables) {
		delegateContext.attach(attachables);
	}

	public <R extends TableRecord<R>> LoaderOptionsStep<R> loadInto(
			Table<R> table) {
		return delegateContext.loadInto(table);
	}

	public Query query(String sql) {
		return delegateContext.query(sql);
	}

	public Query query(String sql, Object... bindings) {
		return delegateContext.query(sql, bindings);
	}

	public Query query(String sql, QueryPart... parts) {
		return delegateContext.query(sql, parts);
	}

	public Result<Record> fetch(String sql) throws DataAccessException {
		return delegateContext.fetch(sql);
	}

	public Result<Record> fetch(String sql, Object... bindings) {
		return delegateContext.fetch(sql, bindings);
	}

	public Result<Record> fetch(String sql, QueryPart... parts) {
		return delegateContext.fetch(sql, parts);
	}

	public Cursor<Record> fetchLazy(String sql) throws DataAccessException {
		return delegateContext.fetchLazy(sql);
	}

	public Cursor<Record> fetchLazy(String sql, Object... bindings)
			throws DataAccessException {
		return delegateContext.fetchLazy(sql, bindings);
	}

	public Cursor<Record> fetchLazy(String sql, QueryPart... parts)
			throws DataAccessException {
		return delegateContext.fetchLazy(sql, parts);
	}

	public List<Result<Record>> fetchMany(String sql)
			throws DataAccessException {
		return delegateContext.fetchMany(sql);
	}

	public List<Result<Record>> fetchMany(String sql, Object... bindings)
			throws DataAccessException {
		return delegateContext.fetchMany(sql, bindings);
	}

	public List<Result<Record>> fetchMany(String sql, QueryPart... parts)
			throws DataAccessException {
		return delegateContext.fetchMany(sql, parts);
	}

	public Record fetchOne(String sql) throws DataAccessException,
			InvalidResultException {
		return delegateContext.fetchOne(sql);
	}

	public Record fetchOne(String sql, Object... bindings)
			throws DataAccessException, InvalidResultException {
		return delegateContext.fetchOne(sql, bindings);
	}

	public Record fetchOne(String sql, QueryPart... parts)
			throws DataAccessException, InvalidResultException {
		return delegateContext.fetchOne(sql, parts);
	}

	public int execute(String sql) throws DataAccessException {
		return delegateContext.execute(sql);
	}

	public int execute(String sql, Object... bindings)
			throws DataAccessException {
		return delegateContext.execute(sql, bindings);
	}

	public int execute(String sql, QueryPart... parts)
			throws DataAccessException {
		return delegateContext.execute(sql, parts);
	}

	public ResultQuery<Record> resultQuery(String sql) {
		return delegateContext.resultQuery(sql);
	}

	public ResultQuery<Record> resultQuery(String sql, Object... bindings) {
		return delegateContext.resultQuery(sql, bindings);
	}

	public ResultQuery<Record> resultQuery(String sql, QueryPart... parts) {
		return delegateContext.resultQuery(sql, parts);
	}

	public Result<Record> fetch(ResultSet rs) throws DataAccessException {
		return delegateContext.fetch(rs);
	}

	public Result<Record> fetch(ResultSet rs, Field<?>... fields)
			throws DataAccessException {
		return delegateContext.fetch(rs, fields);
	}

	public Result<Record> fetch(ResultSet rs, DataType<?>... types)
			throws DataAccessException {
		return delegateContext.fetch(rs, types);
	}

	public Result<Record> fetch(ResultSet rs, Class<?>... types)
			throws DataAccessException {
		return delegateContext.fetch(rs, types);
	}

	public Record fetchOne(ResultSet rs) throws DataAccessException,
			InvalidResultException {
		return delegateContext.fetchOne(rs);
	}

	public Record fetchOne(ResultSet rs, Field<?>... fields)
			throws DataAccessException, InvalidResultException {
		return delegateContext.fetchOne(rs, fields);
	}

	public Record fetchOne(ResultSet rs, DataType<?>... types)
			throws DataAccessException, InvalidResultException {
		return delegateContext.fetchOne(rs, types);
	}

	public Record fetchOne(ResultSet rs, Class<?>... types)
			throws DataAccessException, InvalidResultException {
		return delegateContext.fetchOne(rs, types);
	}

	public Cursor<Record> fetchLazy(ResultSet rs) throws DataAccessException {
		return delegateContext.fetchLazy(rs);
	}

	public Cursor<Record> fetchLazy(ResultSet rs, Field<?>... fields)
			throws DataAccessException {
		return delegateContext.fetchLazy(rs, fields);
	}

	public Cursor<Record> fetchLazy(ResultSet rs, DataType<?>... types)
			throws DataAccessException {
		return delegateContext.fetchLazy(rs, types);
	}

	public Cursor<Record> fetchLazy(ResultSet rs, Class<?>... types)
			throws DataAccessException {
		return delegateContext.fetchLazy(rs, types);
	}

	public Result<Record> fetchFromTXT(String string)
			throws DataAccessException {
		return delegateContext.fetchFromTXT(string);
	}

	public Result<Record> fetchFromTXT(String string, String nullLiteral)
			throws DataAccessException {
		return delegateContext.fetchFromTXT(string, nullLiteral);
	}

	public Result<Record> fetchFromCSV(String string)
			throws DataAccessException {
		return delegateContext.fetchFromCSV(string);
	}

	public Result<Record> fetchFromCSV(String string, char delimiter)
			throws DataAccessException {
		return delegateContext.fetchFromCSV(string, delimiter);
	}

	public Result<Record> fetchFromStringData(String[]... data) {
		return delegateContext.fetchFromStringData(data);
	}

	public Result<Record> fetchFromStringData(List<String[]> data) {
		return delegateContext.fetchFromStringData(data);
	}

	public <R extends Record> SelectWhereStep<R> selectFrom(Table<R> table) {
		return delegateContext.selectFrom(table);
	}

	public SelectSelectStep<Record> select(Collection<? extends Field<?>> fields) {
		return delegateContext.select(fields);
	}

	public SelectSelectStep<Record> select(Field<?>... fields) {
		return delegateContext.select(fields);
	}

	public <T1> SelectSelectStep<Record1<T1>> select(Field<T1> field1) {
		return delegateContext.select(field1);
	}

	public <T1, T2> SelectSelectStep<Record2<T1, T2>> select(Field<T1> field1,
			Field<T2> field2) {
		return delegateContext.select(field1, field2);
	}

	public <T1, T2, T3> SelectSelectStep<Record3<T1, T2, T3>> select(
			Field<T1> field1, Field<T2> field2, Field<T3> field3) {
		return delegateContext.select(field1, field2, field3);
	}

	public <T1, T2, T3, T4> SelectSelectStep<Record4<T1, T2, T3, T4>> select(
			Field<T1> field1, Field<T2> field2, Field<T3> field3,
			Field<T4> field4) {
		return delegateContext.select(field1, field2, field3, field4);
	}

	public <T1, T2, T3, T4, T5> SelectSelectStep<Record5<T1, T2, T3, T4, T5>> select(
			Field<T1> field1, Field<T2> field2, Field<T3> field3,
			Field<T4> field4, Field<T5> field5) {
		return delegateContext.select(field1, field2, field3, field4, field5);
	}

	public <T1, T2, T3, T4, T5, T6> SelectSelectStep<Record6<T1, T2, T3, T4, T5, T6>> select(
			Field<T1> field1, Field<T2> field2, Field<T3> field3,
			Field<T4> field4, Field<T5> field5, Field<T6> field6) {
		return delegateContext.select(field1, field2, field3, field4, field5,
				field6);
	}

	public <T1, T2, T3, T4, T5, T6, T7> SelectSelectStep<Record7<T1, T2, T3, T4, T5, T6, T7>> select(
			Field<T1> field1, Field<T2> field2, Field<T3> field3,
			Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7) {
		return delegateContext.select(field1, field2, field3, field4, field5,
				field6, field7);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8> SelectSelectStep<Record8<T1, T2, T3, T4, T5, T6, T7, T8>> select(
			Field<T1> field1, Field<T2> field2, Field<T3> field3,
			Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8) {
		return delegateContext.select(field1, field2, field3, field4, field5,
				field6, field7, field8);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9> SelectSelectStep<Record9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> select(
			Field<T1> field1, Field<T2> field2, Field<T3> field3,
			Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9) {
		return delegateContext.select(field1, field2, field3, field4, field5,
				field6, field7, field8, field9);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> SelectSelectStep<Record10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>> select(
			Field<T1> field1, Field<T2> field2, Field<T3> field3,
			Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9,
			Field<T10> field10) {
		return delegateContext.select(field1, field2, field3, field4, field5,
				field6, field7, field8, field9, field10);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> SelectSelectStep<Record11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>> select(
			Field<T1> field1, Field<T2> field2, Field<T3> field3,
			Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9,
			Field<T10> field10, Field<T11> field11) {
		return delegateContext.select(field1, field2, field3, field4, field5,
				field6, field7, field8, field9, field10, field11);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> SelectSelectStep<Record12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>> select(
			Field<T1> field1, Field<T2> field2, Field<T3> field3,
			Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9,
			Field<T10> field10, Field<T11> field11, Field<T12> field12) {
		return delegateContext.select(field1, field2, field3, field4, field5,
				field6, field7, field8, field9, field10, field11, field12);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> SelectSelectStep<Record13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>> select(
			Field<T1> field1, Field<T2> field2, Field<T3> field3,
			Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9,
			Field<T10> field10, Field<T11> field11, Field<T12> field12,
			Field<T13> field13) {
		return delegateContext.select(field1, field2, field3, field4, field5,
				field6, field7, field8, field9, field10, field11, field12,
				field13);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> SelectSelectStep<Record14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>> select(
			Field<T1> field1, Field<T2> field2, Field<T3> field3,
			Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9,
			Field<T10> field10, Field<T11> field11, Field<T12> field12,
			Field<T13> field13, Field<T14> field14) {
		return delegateContext.select(field1, field2, field3, field4, field5,
				field6, field7, field8, field9, field10, field11, field12,
				field13, field14);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> SelectSelectStep<Record15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>> select(
			Field<T1> field1, Field<T2> field2, Field<T3> field3,
			Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9,
			Field<T10> field10, Field<T11> field11, Field<T12> field12,
			Field<T13> field13, Field<T14> field14, Field<T15> field15) {
		return delegateContext.select(field1, field2, field3, field4, field5,
				field6, field7, field8, field9, field10, field11, field12,
				field13, field14, field15);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> SelectSelectStep<Record16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>> select(
			Field<T1> field1, Field<T2> field2, Field<T3> field3,
			Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9,
			Field<T10> field10, Field<T11> field11, Field<T12> field12,
			Field<T13> field13, Field<T14> field14, Field<T15> field15,
			Field<T16> field16) {
		return delegateContext.select(field1, field2, field3, field4, field5,
				field6, field7, field8, field9, field10, field11, field12,
				field13, field14, field15, field16);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> SelectSelectStep<Record17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>> select(
			Field<T1> field1, Field<T2> field2, Field<T3> field3,
			Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9,
			Field<T10> field10, Field<T11> field11, Field<T12> field12,
			Field<T13> field13, Field<T14> field14, Field<T15> field15,
			Field<T16> field16, Field<T17> field17) {
		return delegateContext.select(field1, field2, field3, field4, field5,
				field6, field7, field8, field9, field10, field11, field12,
				field13, field14, field15, field16, field17);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18> SelectSelectStep<Record18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>> select(
			Field<T1> field1, Field<T2> field2, Field<T3> field3,
			Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9,
			Field<T10> field10, Field<T11> field11, Field<T12> field12,
			Field<T13> field13, Field<T14> field14, Field<T15> field15,
			Field<T16> field16, Field<T17> field17, Field<T18> field18) {
		return delegateContext.select(field1, field2, field3, field4, field5,
				field6, field7, field8, field9, field10, field11, field12,
				field13, field14, field15, field16, field17, field18);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19> SelectSelectStep<Record19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>> select(
			Field<T1> field1, Field<T2> field2, Field<T3> field3,
			Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9,
			Field<T10> field10, Field<T11> field11, Field<T12> field12,
			Field<T13> field13, Field<T14> field14, Field<T15> field15,
			Field<T16> field16, Field<T17> field17, Field<T18> field18,
			Field<T19> field19) {
		return delegateContext.select(field1, field2, field3, field4, field5,
				field6, field7, field8, field9, field10, field11, field12,
				field13, field14, field15, field16, field17, field18, field19);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20> SelectSelectStep<Record20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>> select(
			Field<T1> field1, Field<T2> field2, Field<T3> field3,
			Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9,
			Field<T10> field10, Field<T11> field11, Field<T12> field12,
			Field<T13> field13, Field<T14> field14, Field<T15> field15,
			Field<T16> field16, Field<T17> field17, Field<T18> field18,
			Field<T19> field19, Field<T20> field20) {
		return delegateContext.select(field1, field2, field3, field4, field5,
				field6, field7, field8, field9, field10, field11, field12,
				field13, field14, field15, field16, field17, field18, field19,
				field20);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21> SelectSelectStep<Record21<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21>> select(
			Field<T1> field1, Field<T2> field2, Field<T3> field3,
			Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9,
			Field<T10> field10, Field<T11> field11, Field<T12> field12,
			Field<T13> field13, Field<T14> field14, Field<T15> field15,
			Field<T16> field16, Field<T17> field17, Field<T18> field18,
			Field<T19> field19, Field<T20> field20, Field<T21> field21) {
		return delegateContext.select(field1, field2, field3, field4, field5,
				field6, field7, field8, field9, field10, field11, field12,
				field13, field14, field15, field16, field17, field18, field19,
				field20, field21);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22> SelectSelectStep<Record22<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22>> select(
			Field<T1> field1, Field<T2> field2, Field<T3> field3,
			Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9,
			Field<T10> field10, Field<T11> field11, Field<T12> field12,
			Field<T13> field13, Field<T14> field14, Field<T15> field15,
			Field<T16> field16, Field<T17> field17, Field<T18> field18,
			Field<T19> field19, Field<T20> field20, Field<T21> field21,
			Field<T22> field22) {
		return delegateContext.select(field1, field2, field3, field4, field5,
				field6, field7, field8, field9, field10, field11, field12,
				field13, field14, field15, field16, field17, field18, field19,
				field20, field21, field22);
	}

	public SelectSelectStep<Record> selectDistinct(
			Collection<? extends Field<?>> fields) {
		return delegateContext.selectDistinct(fields);
	}

	public SelectSelectStep<Record> selectDistinct(Field<?>... fields) {
		return delegateContext.selectDistinct(fields);
	}

	public <T1> SelectSelectStep<Record1<T1>> selectDistinct(Field<T1> field1) {
		return delegateContext.selectDistinct(field1);
	}

	public <T1, T2> SelectSelectStep<Record2<T1, T2>> selectDistinct(
			Field<T1> field1, Field<T2> field2) {
		return delegateContext.selectDistinct(field1, field2);
	}

	public <T1, T2, T3> SelectSelectStep<Record3<T1, T2, T3>> selectDistinct(
			Field<T1> field1, Field<T2> field2, Field<T3> field3) {
		return delegateContext.selectDistinct(field1, field2, field3);
	}

	public <T1, T2, T3, T4> SelectSelectStep<Record4<T1, T2, T3, T4>> selectDistinct(
			Field<T1> field1, Field<T2> field2, Field<T3> field3,
			Field<T4> field4) {
		return delegateContext.selectDistinct(field1, field2, field3, field4);
	}

	public <T1, T2, T3, T4, T5> SelectSelectStep<Record5<T1, T2, T3, T4, T5>> selectDistinct(
			Field<T1> field1, Field<T2> field2, Field<T3> field3,
			Field<T4> field4, Field<T5> field5) {
		return delegateContext.selectDistinct(field1, field2, field3, field4,
				field5);
	}

	public <T1, T2, T3, T4, T5, T6> SelectSelectStep<Record6<T1, T2, T3, T4, T5, T6>> selectDistinct(
			Field<T1> field1, Field<T2> field2, Field<T3> field3,
			Field<T4> field4, Field<T5> field5, Field<T6> field6) {
		return delegateContext.selectDistinct(field1, field2, field3, field4,
				field5, field6);
	}

	public <T1, T2, T3, T4, T5, T6, T7> SelectSelectStep<Record7<T1, T2, T3, T4, T5, T6, T7>> selectDistinct(
			Field<T1> field1, Field<T2> field2, Field<T3> field3,
			Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7) {
		return delegateContext.selectDistinct(field1, field2, field3, field4,
				field5, field6, field7);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8> SelectSelectStep<Record8<T1, T2, T3, T4, T5, T6, T7, T8>> selectDistinct(
			Field<T1> field1, Field<T2> field2, Field<T3> field3,
			Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8) {
		return delegateContext.selectDistinct(field1, field2, field3, field4,
				field5, field6, field7, field8);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9> SelectSelectStep<Record9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> selectDistinct(
			Field<T1> field1, Field<T2> field2, Field<T3> field3,
			Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9) {
		return delegateContext.selectDistinct(field1, field2, field3, field4,
				field5, field6, field7, field8, field9);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> SelectSelectStep<Record10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>> selectDistinct(
			Field<T1> field1, Field<T2> field2, Field<T3> field3,
			Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9,
			Field<T10> field10) {
		return delegateContext.selectDistinct(field1, field2, field3, field4,
				field5, field6, field7, field8, field9, field10);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> SelectSelectStep<Record11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>> selectDistinct(
			Field<T1> field1, Field<T2> field2, Field<T3> field3,
			Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9,
			Field<T10> field10, Field<T11> field11) {
		return delegateContext.selectDistinct(field1, field2, field3, field4,
				field5, field6, field7, field8, field9, field10, field11);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> SelectSelectStep<Record12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>> selectDistinct(
			Field<T1> field1, Field<T2> field2, Field<T3> field3,
			Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9,
			Field<T10> field10, Field<T11> field11, Field<T12> field12) {
		return delegateContext.selectDistinct(field1, field2, field3, field4,
				field5, field6, field7, field8, field9, field10, field11,
				field12);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> SelectSelectStep<Record13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>> selectDistinct(
			Field<T1> field1, Field<T2> field2, Field<T3> field3,
			Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9,
			Field<T10> field10, Field<T11> field11, Field<T12> field12,
			Field<T13> field13) {
		return delegateContext.selectDistinct(field1, field2, field3, field4,
				field5, field6, field7, field8, field9, field10, field11,
				field12, field13);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> SelectSelectStep<Record14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>> selectDistinct(
			Field<T1> field1, Field<T2> field2, Field<T3> field3,
			Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9,
			Field<T10> field10, Field<T11> field11, Field<T12> field12,
			Field<T13> field13, Field<T14> field14) {
		return delegateContext.selectDistinct(field1, field2, field3, field4,
				field5, field6, field7, field8, field9, field10, field11,
				field12, field13, field14);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> SelectSelectStep<Record15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>> selectDistinct(
			Field<T1> field1, Field<T2> field2, Field<T3> field3,
			Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9,
			Field<T10> field10, Field<T11> field11, Field<T12> field12,
			Field<T13> field13, Field<T14> field14, Field<T15> field15) {
		return delegateContext.selectDistinct(field1, field2, field3, field4,
				field5, field6, field7, field8, field9, field10, field11,
				field12, field13, field14, field15);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> SelectSelectStep<Record16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>> selectDistinct(
			Field<T1> field1, Field<T2> field2, Field<T3> field3,
			Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9,
			Field<T10> field10, Field<T11> field11, Field<T12> field12,
			Field<T13> field13, Field<T14> field14, Field<T15> field15,
			Field<T16> field16) {
		return delegateContext.selectDistinct(field1, field2, field3, field4,
				field5, field6, field7, field8, field9, field10, field11,
				field12, field13, field14, field15, field16);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> SelectSelectStep<Record17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>> selectDistinct(
			Field<T1> field1, Field<T2> field2, Field<T3> field3,
			Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9,
			Field<T10> field10, Field<T11> field11, Field<T12> field12,
			Field<T13> field13, Field<T14> field14, Field<T15> field15,
			Field<T16> field16, Field<T17> field17) {
		return delegateContext.selectDistinct(field1, field2, field3, field4,
				field5, field6, field7, field8, field9, field10, field11,
				field12, field13, field14, field15, field16, field17);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18> SelectSelectStep<Record18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>> selectDistinct(
			Field<T1> field1, Field<T2> field2, Field<T3> field3,
			Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9,
			Field<T10> field10, Field<T11> field11, Field<T12> field12,
			Field<T13> field13, Field<T14> field14, Field<T15> field15,
			Field<T16> field16, Field<T17> field17, Field<T18> field18) {
		return delegateContext.selectDistinct(field1, field2, field3, field4,
				field5, field6, field7, field8, field9, field10, field11,
				field12, field13, field14, field15, field16, field17, field18);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19> SelectSelectStep<Record19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>> selectDistinct(
			Field<T1> field1, Field<T2> field2, Field<T3> field3,
			Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9,
			Field<T10> field10, Field<T11> field11, Field<T12> field12,
			Field<T13> field13, Field<T14> field14, Field<T15> field15,
			Field<T16> field16, Field<T17> field17, Field<T18> field18,
			Field<T19> field19) {
		return delegateContext.selectDistinct(field1, field2, field3, field4,
				field5, field6, field7, field8, field9, field10, field11,
				field12, field13, field14, field15, field16, field17, field18,
				field19);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20> SelectSelectStep<Record20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>> selectDistinct(
			Field<T1> field1, Field<T2> field2, Field<T3> field3,
			Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9,
			Field<T10> field10, Field<T11> field11, Field<T12> field12,
			Field<T13> field13, Field<T14> field14, Field<T15> field15,
			Field<T16> field16, Field<T17> field17, Field<T18> field18,
			Field<T19> field19, Field<T20> field20) {
		return delegateContext.selectDistinct(field1, field2, field3, field4,
				field5, field6, field7, field8, field9, field10, field11,
				field12, field13, field14, field15, field16, field17, field18,
				field19, field20);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21> SelectSelectStep<Record21<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21>> selectDistinct(
			Field<T1> field1, Field<T2> field2, Field<T3> field3,
			Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9,
			Field<T10> field10, Field<T11> field11, Field<T12> field12,
			Field<T13> field13, Field<T14> field14, Field<T15> field15,
			Field<T16> field16, Field<T17> field17, Field<T18> field18,
			Field<T19> field19, Field<T20> field20, Field<T21> field21) {
		return delegateContext.selectDistinct(field1, field2, field3, field4,
				field5, field6, field7, field8, field9, field10, field11,
				field12, field13, field14, field15, field16, field17, field18,
				field19, field20, field21);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22> SelectSelectStep<Record22<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22>> selectDistinct(
			Field<T1> field1, Field<T2> field2, Field<T3> field3,
			Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9,
			Field<T10> field10, Field<T11> field11, Field<T12> field12,
			Field<T13> field13, Field<T14> field14, Field<T15> field15,
			Field<T16> field16, Field<T17> field17, Field<T18> field18,
			Field<T19> field19, Field<T20> field20, Field<T21> field21,
			Field<T22> field22) {
		return delegateContext.selectDistinct(field1, field2, field3, field4,
				field5, field6, field7, field8, field9, field10, field11,
				field12, field13, field14, field15, field16, field17, field18,
				field19, field20, field21, field22);
	}

	public SelectSelectStep<Record1<Integer>> selectZero() {
		return delegateContext.selectZero();
	}

	public SelectSelectStep<Record1<Integer>> selectOne() {
		return delegateContext.selectOne();
	}

	public SelectSelectStep<Record1<Integer>> selectCount() {
		return delegateContext.selectCount();
	}

	public SelectQuery<Record> selectQuery() {
		return delegateContext.selectQuery();
	}

	public <R extends Record> SelectQuery<R> selectQuery(TableLike<R> table) {
		return delegateContext.selectQuery(table);
	}

	public <R extends Record> InsertQuery<R> insertQuery(Table<R> into) {
		return delegateContext.insertQuery(into);
	}

	public <R extends Record> InsertSetStep<R> insertInto(Table<R> into) {
		return delegateContext.insertInto(into);
	}

	public <R extends Record, T1> InsertValuesStep1<R, T1> insertInto(
			Table<R> into, Field<T1> field1) {
		return delegateContext.insertInto(into, field1);
	}

	public <R extends Record, T1, T2> InsertValuesStep2<R, T1, T2> insertInto(
			Table<R> into, Field<T1> field1, Field<T2> field2) {
		return delegateContext.insertInto(into, field1, field2);
	}

	public <R extends Record, T1, T2, T3> InsertValuesStep3<R, T1, T2, T3> insertInto(
			Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3) {
		return delegateContext.insertInto(into, field1, field2, field3);
	}

	public <R extends Record, T1, T2, T3, T4> InsertValuesStep4<R, T1, T2, T3, T4> insertInto(
			Table<R> into, Field<T1> field1, Field<T2> field2,
			Field<T3> field3, Field<T4> field4) {
		return delegateContext.insertInto(into, field1, field2, field3, field4);
	}

	public <R extends Record, T1, T2, T3, T4, T5> InsertValuesStep5<R, T1, T2, T3, T4, T5> insertInto(
			Table<R> into, Field<T1> field1, Field<T2> field2,
			Field<T3> field3, Field<T4> field4, Field<T5> field5) {
		return delegateContext.insertInto(into, field1, field2, field3, field4,
				field5);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6> InsertValuesStep6<R, T1, T2, T3, T4, T5, T6> insertInto(
			Table<R> into, Field<T1> field1, Field<T2> field2,
			Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6) {
		return delegateContext.insertInto(into, field1, field2, field3, field4,
				field5, field6);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7> InsertValuesStep7<R, T1, T2, T3, T4, T5, T6, T7> insertInto(
			Table<R> into, Field<T1> field1, Field<T2> field2,
			Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7) {
		return delegateContext.insertInto(into, field1, field2, field3, field4,
				field5, field6, field7);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8> InsertValuesStep8<R, T1, T2, T3, T4, T5, T6, T7, T8> insertInto(
			Table<R> into, Field<T1> field1, Field<T2> field2,
			Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8) {
		return delegateContext.insertInto(into, field1, field2, field3, field4,
				field5, field6, field7, field8);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9> InsertValuesStep9<R, T1, T2, T3, T4, T5, T6, T7, T8, T9> insertInto(
			Table<R> into, Field<T1> field1, Field<T2> field2,
			Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8,
			Field<T9> field9) {
		return delegateContext.insertInto(into, field1, field2, field3, field4,
				field5, field6, field7, field8, field9);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> InsertValuesStep10<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> insertInto(
			Table<R> into, Field<T1> field1, Field<T2> field2,
			Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8,
			Field<T9> field9, Field<T10> field10) {
		return delegateContext.insertInto(into, field1, field2, field3, field4,
				field5, field6, field7, field8, field9, field10);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> InsertValuesStep11<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> insertInto(
			Table<R> into, Field<T1> field1, Field<T2> field2,
			Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8,
			Field<T9> field9, Field<T10> field10, Field<T11> field11) {
		return delegateContext.insertInto(into, field1, field2, field3, field4,
				field5, field6, field7, field8, field9, field10, field11);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> InsertValuesStep12<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> insertInto(
			Table<R> into, Field<T1> field1, Field<T2> field2,
			Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8,
			Field<T9> field9, Field<T10> field10, Field<T11> field11,
			Field<T12> field12) {
		return delegateContext.insertInto(into, field1, field2, field3, field4,
				field5, field6, field7, field8, field9, field10, field11,
				field12);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> InsertValuesStep13<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> insertInto(
			Table<R> into, Field<T1> field1, Field<T2> field2,
			Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8,
			Field<T9> field9, Field<T10> field10, Field<T11> field11,
			Field<T12> field12, Field<T13> field13) {
		return delegateContext.insertInto(into, field1, field2, field3, field4,
				field5, field6, field7, field8, field9, field10, field11,
				field12, field13);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> InsertValuesStep14<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> insertInto(
			Table<R> into, Field<T1> field1, Field<T2> field2,
			Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8,
			Field<T9> field9, Field<T10> field10, Field<T11> field11,
			Field<T12> field12, Field<T13> field13, Field<T14> field14) {
		return delegateContext.insertInto(into, field1, field2, field3, field4,
				field5, field6, field7, field8, field9, field10, field11,
				field12, field13, field14);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> InsertValuesStep15<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> insertInto(
			Table<R> into, Field<T1> field1, Field<T2> field2,
			Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8,
			Field<T9> field9, Field<T10> field10, Field<T11> field11,
			Field<T12> field12, Field<T13> field13, Field<T14> field14,
			Field<T15> field15) {
		return delegateContext.insertInto(into, field1, field2, field3, field4,
				field5, field6, field7, field8, field9, field10, field11,
				field12, field13, field14, field15);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> InsertValuesStep16<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> insertInto(
			Table<R> into, Field<T1> field1, Field<T2> field2,
			Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8,
			Field<T9> field9, Field<T10> field10, Field<T11> field11,
			Field<T12> field12, Field<T13> field13, Field<T14> field14,
			Field<T15> field15, Field<T16> field16) {
		return delegateContext.insertInto(into, field1, field2, field3, field4,
				field5, field6, field7, field8, field9, field10, field11,
				field12, field13, field14, field15, field16);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> InsertValuesStep17<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> insertInto(
			Table<R> into, Field<T1> field1, Field<T2> field2,
			Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8,
			Field<T9> field9, Field<T10> field10, Field<T11> field11,
			Field<T12> field12, Field<T13> field13, Field<T14> field14,
			Field<T15> field15, Field<T16> field16, Field<T17> field17) {
		return delegateContext.insertInto(into, field1, field2, field3, field4,
				field5, field6, field7, field8, field9, field10, field11,
				field12, field13, field14, field15, field16, field17);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18> InsertValuesStep18<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18> insertInto(
			Table<R> into, Field<T1> field1, Field<T2> field2,
			Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8,
			Field<T9> field9, Field<T10> field10, Field<T11> field11,
			Field<T12> field12, Field<T13> field13, Field<T14> field14,
			Field<T15> field15, Field<T16> field16, Field<T17> field17,
			Field<T18> field18) {
		return delegateContext.insertInto(into, field1, field2, field3, field4,
				field5, field6, field7, field8, field9, field10, field11,
				field12, field13, field14, field15, field16, field17, field18);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19> InsertValuesStep19<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19> insertInto(
			Table<R> into, Field<T1> field1, Field<T2> field2,
			Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8,
			Field<T9> field9, Field<T10> field10, Field<T11> field11,
			Field<T12> field12, Field<T13> field13, Field<T14> field14,
			Field<T15> field15, Field<T16> field16, Field<T17> field17,
			Field<T18> field18, Field<T19> field19) {
		return delegateContext.insertInto(into, field1, field2, field3, field4,
				field5, field6, field7, field8, field9, field10, field11,
				field12, field13, field14, field15, field16, field17, field18,
				field19);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20> InsertValuesStep20<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20> insertInto(
			Table<R> into, Field<T1> field1, Field<T2> field2,
			Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8,
			Field<T9> field9, Field<T10> field10, Field<T11> field11,
			Field<T12> field12, Field<T13> field13, Field<T14> field14,
			Field<T15> field15, Field<T16> field16, Field<T17> field17,
			Field<T18> field18, Field<T19> field19, Field<T20> field20) {
		return delegateContext.insertInto(into, field1, field2, field3, field4,
				field5, field6, field7, field8, field9, field10, field11,
				field12, field13, field14, field15, field16, field17, field18,
				field19, field20);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21> InsertValuesStep21<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21> insertInto(
			Table<R> into, Field<T1> field1, Field<T2> field2,
			Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8,
			Field<T9> field9, Field<T10> field10, Field<T11> field11,
			Field<T12> field12, Field<T13> field13, Field<T14> field14,
			Field<T15> field15, Field<T16> field16, Field<T17> field17,
			Field<T18> field18, Field<T19> field19, Field<T20> field20,
			Field<T21> field21) {
		return delegateContext.insertInto(into, field1, field2, field3, field4,
				field5, field6, field7, field8, field9, field10, field11,
				field12, field13, field14, field15, field16, field17, field18,
				field19, field20, field21);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22> InsertValuesStep22<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22> insertInto(
			Table<R> into, Field<T1> field1, Field<T2> field2,
			Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8,
			Field<T9> field9, Field<T10> field10, Field<T11> field11,
			Field<T12> field12, Field<T13> field13, Field<T14> field14,
			Field<T15> field15, Field<T16> field16, Field<T17> field17,
			Field<T18> field18, Field<T19> field19, Field<T20> field20,
			Field<T21> field21, Field<T22> field22) {
		return delegateContext.insertInto(into, field1, field2, field3, field4,
				field5, field6, field7, field8, field9, field10, field11,
				field12, field13, field14, field15, field16, field17, field18,
				field19, field20, field21, field22);
	}

	public <R extends Record> InsertValuesStepN<R> insertInto(Table<R> into,
			Field<?>... fields) {
		return delegateContext.insertInto(into, fields);
	}

	public <R extends Record> InsertValuesStepN<R> insertInto(Table<R> into,
			Collection<? extends Field<?>> fields) {
		return delegateContext.insertInto(into, fields);
	}

	public <R extends Record> UpdateQuery<R> updateQuery(Table<R> table) {
		return delegateContext.updateQuery(table);
	}

	public <R extends Record> UpdateSetFirstStep<R> update(Table<R> table) {
		return delegateContext.update(table);
	}

	public <R extends Record> MergeUsingStep<R> mergeInto(Table<R> table) {
		return delegateContext.mergeInto(table);
	}

	public <R extends Record, T1> MergeKeyStep1<R, T1> mergeInto(
			Table<R> table, Field<T1> field1) {
		return delegateContext.mergeInto(table, field1);
	}

	public <R extends Record, T1, T2> MergeKeyStep2<R, T1, T2> mergeInto(
			Table<R> table, Field<T1> field1, Field<T2> field2) {
		return delegateContext.mergeInto(table, field1, field2);
	}

	public <R extends Record, T1, T2, T3> MergeKeyStep3<R, T1, T2, T3> mergeInto(
			Table<R> table, Field<T1> field1, Field<T2> field2, Field<T3> field3) {
		return delegateContext.mergeInto(table, field1, field2, field3);
	}

	public <R extends Record, T1, T2, T3, T4> MergeKeyStep4<R, T1, T2, T3, T4> mergeInto(
			Table<R> table, Field<T1> field1, Field<T2> field2,
			Field<T3> field3, Field<T4> field4) {
		return delegateContext.mergeInto(table, field1, field2, field3, field4);
	}

	public <R extends Record, T1, T2, T3, T4, T5> MergeKeyStep5<R, T1, T2, T3, T4, T5> mergeInto(
			Table<R> table, Field<T1> field1, Field<T2> field2,
			Field<T3> field3, Field<T4> field4, Field<T5> field5) {
		return delegateContext.mergeInto(table, field1, field2, field3, field4,
				field5);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6> MergeKeyStep6<R, T1, T2, T3, T4, T5, T6> mergeInto(
			Table<R> table, Field<T1> field1, Field<T2> field2,
			Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6) {
		return delegateContext.mergeInto(table, field1, field2, field3, field4,
				field5, field6);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7> MergeKeyStep7<R, T1, T2, T3, T4, T5, T6, T7> mergeInto(
			Table<R> table, Field<T1> field1, Field<T2> field2,
			Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7) {
		return delegateContext.mergeInto(table, field1, field2, field3, field4,
				field5, field6, field7);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8> MergeKeyStep8<R, T1, T2, T3, T4, T5, T6, T7, T8> mergeInto(
			Table<R> table, Field<T1> field1, Field<T2> field2,
			Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8) {
		return delegateContext.mergeInto(table, field1, field2, field3, field4,
				field5, field6, field7, field8);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9> MergeKeyStep9<R, T1, T2, T3, T4, T5, T6, T7, T8, T9> mergeInto(
			Table<R> table, Field<T1> field1, Field<T2> field2,
			Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8,
			Field<T9> field9) {
		return delegateContext.mergeInto(table, field1, field2, field3, field4,
				field5, field6, field7, field8, field9);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> MergeKeyStep10<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> mergeInto(
			Table<R> table, Field<T1> field1, Field<T2> field2,
			Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8,
			Field<T9> field9, Field<T10> field10) {
		return delegateContext.mergeInto(table, field1, field2, field3, field4,
				field5, field6, field7, field8, field9, field10);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> MergeKeyStep11<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> mergeInto(
			Table<R> table, Field<T1> field1, Field<T2> field2,
			Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8,
			Field<T9> field9, Field<T10> field10, Field<T11> field11) {
		return delegateContext.mergeInto(table, field1, field2, field3, field4,
				field5, field6, field7, field8, field9, field10, field11);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> MergeKeyStep12<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> mergeInto(
			Table<R> table, Field<T1> field1, Field<T2> field2,
			Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8,
			Field<T9> field9, Field<T10> field10, Field<T11> field11,
			Field<T12> field12) {
		return delegateContext.mergeInto(table, field1, field2, field3, field4,
				field5, field6, field7, field8, field9, field10, field11,
				field12);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> MergeKeyStep13<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> mergeInto(
			Table<R> table, Field<T1> field1, Field<T2> field2,
			Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8,
			Field<T9> field9, Field<T10> field10, Field<T11> field11,
			Field<T12> field12, Field<T13> field13) {
		return delegateContext.mergeInto(table, field1, field2, field3, field4,
				field5, field6, field7, field8, field9, field10, field11,
				field12, field13);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> MergeKeyStep14<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> mergeInto(
			Table<R> table, Field<T1> field1, Field<T2> field2,
			Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8,
			Field<T9> field9, Field<T10> field10, Field<T11> field11,
			Field<T12> field12, Field<T13> field13, Field<T14> field14) {
		return delegateContext.mergeInto(table, field1, field2, field3, field4,
				field5, field6, field7, field8, field9, field10, field11,
				field12, field13, field14);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> MergeKeyStep15<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> mergeInto(
			Table<R> table, Field<T1> field1, Field<T2> field2,
			Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8,
			Field<T9> field9, Field<T10> field10, Field<T11> field11,
			Field<T12> field12, Field<T13> field13, Field<T14> field14,
			Field<T15> field15) {
		return delegateContext.mergeInto(table, field1, field2, field3, field4,
				field5, field6, field7, field8, field9, field10, field11,
				field12, field13, field14, field15);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> MergeKeyStep16<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> mergeInto(
			Table<R> table, Field<T1> field1, Field<T2> field2,
			Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8,
			Field<T9> field9, Field<T10> field10, Field<T11> field11,
			Field<T12> field12, Field<T13> field13, Field<T14> field14,
			Field<T15> field15, Field<T16> field16) {
		return delegateContext.mergeInto(table, field1, field2, field3, field4,
				field5, field6, field7, field8, field9, field10, field11,
				field12, field13, field14, field15, field16);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> MergeKeyStep17<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> mergeInto(
			Table<R> table, Field<T1> field1, Field<T2> field2,
			Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8,
			Field<T9> field9, Field<T10> field10, Field<T11> field11,
			Field<T12> field12, Field<T13> field13, Field<T14> field14,
			Field<T15> field15, Field<T16> field16, Field<T17> field17) {
		return delegateContext.mergeInto(table, field1, field2, field3, field4,
				field5, field6, field7, field8, field9, field10, field11,
				field12, field13, field14, field15, field16, field17);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18> MergeKeyStep18<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18> mergeInto(
			Table<R> table, Field<T1> field1, Field<T2> field2,
			Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8,
			Field<T9> field9, Field<T10> field10, Field<T11> field11,
			Field<T12> field12, Field<T13> field13, Field<T14> field14,
			Field<T15> field15, Field<T16> field16, Field<T17> field17,
			Field<T18> field18) {
		return delegateContext.mergeInto(table, field1, field2, field3, field4,
				field5, field6, field7, field8, field9, field10, field11,
				field12, field13, field14, field15, field16, field17, field18);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19> MergeKeyStep19<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19> mergeInto(
			Table<R> table, Field<T1> field1, Field<T2> field2,
			Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8,
			Field<T9> field9, Field<T10> field10, Field<T11> field11,
			Field<T12> field12, Field<T13> field13, Field<T14> field14,
			Field<T15> field15, Field<T16> field16, Field<T17> field17,
			Field<T18> field18, Field<T19> field19) {
		return delegateContext.mergeInto(table, field1, field2, field3, field4,
				field5, field6, field7, field8, field9, field10, field11,
				field12, field13, field14, field15, field16, field17, field18,
				field19);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20> MergeKeyStep20<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20> mergeInto(
			Table<R> table, Field<T1> field1, Field<T2> field2,
			Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8,
			Field<T9> field9, Field<T10> field10, Field<T11> field11,
			Field<T12> field12, Field<T13> field13, Field<T14> field14,
			Field<T15> field15, Field<T16> field16, Field<T17> field17,
			Field<T18> field18, Field<T19> field19, Field<T20> field20) {
		return delegateContext.mergeInto(table, field1, field2, field3, field4,
				field5, field6, field7, field8, field9, field10, field11,
				field12, field13, field14, field15, field16, field17, field18,
				field19, field20);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21> MergeKeyStep21<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21> mergeInto(
			Table<R> table, Field<T1> field1, Field<T2> field2,
			Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8,
			Field<T9> field9, Field<T10> field10, Field<T11> field11,
			Field<T12> field12, Field<T13> field13, Field<T14> field14,
			Field<T15> field15, Field<T16> field16, Field<T17> field17,
			Field<T18> field18, Field<T19> field19, Field<T20> field20,
			Field<T21> field21) {
		return delegateContext.mergeInto(table, field1, field2, field3, field4,
				field5, field6, field7, field8, field9, field10, field11,
				field12, field13, field14, field15, field16, field17, field18,
				field19, field20, field21);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22> MergeKeyStep22<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22> mergeInto(
			Table<R> table, Field<T1> field1, Field<T2> field2,
			Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8,
			Field<T9> field9, Field<T10> field10, Field<T11> field11,
			Field<T12> field12, Field<T13> field13, Field<T14> field14,
			Field<T15> field15, Field<T16> field16, Field<T17> field17,
			Field<T18> field18, Field<T19> field19, Field<T20> field20,
			Field<T21> field21, Field<T22> field22) {
		return delegateContext.mergeInto(table, field1, field2, field3, field4,
				field5, field6, field7, field8, field9, field10, field11,
				field12, field13, field14, field15, field16, field17, field18,
				field19, field20, field21, field22);
	}

	public <R extends Record> MergeKeyStepN<R> mergeInto(Table<R> table,
			Field<?>... fields) {
		return delegateContext.mergeInto(table, fields);
	}

	public <R extends Record> MergeKeyStepN<R> mergeInto(Table<R> table,
			Collection<? extends Field<?>> fields) {
		return delegateContext.mergeInto(table, fields);
	}

	public <R extends Record> DeleteQuery<R> deleteQuery(Table<R> table) {
		return delegateContext.deleteQuery(table);
	}

	public <R extends Record> DeleteWhereStep<R> delete(Table<R> table) {
		return delegateContext.delete(table);
	}

	public Batch batch(Query... queries) {
		return delegateContext.batch(queries);
	}

	public Batch batch(Collection<? extends Query> queries) {
		return delegateContext.batch(queries);
	}

	public BatchBindStep batch(Query query) {
		return delegateContext.batch(query);
	}

	public Batch batchStore(UpdatableRecord<?>... records) {
		return delegateContext.batchStore(records);
	}

	public Batch batchStore(Collection<? extends UpdatableRecord<?>> records) {
		return delegateContext.batchStore(records);
	}

	public Batch batchInsert(UpdatableRecord<?>... records) {
		return delegateContext.batchInsert(records);
	}

	public Batch batchInsert(Collection<? extends UpdatableRecord<?>> records) {
		return delegateContext.batchInsert(records);
	}

	public Batch batchUpdate(UpdatableRecord<?>... records) {
		return delegateContext.batchUpdate(records);
	}

	public Batch batchUpdate(Collection<? extends UpdatableRecord<?>> records) {
		return delegateContext.batchUpdate(records);
	}

	public Batch batchDelete(UpdatableRecord<?>... records) {
		return delegateContext.batchDelete(records);
	}

	public Batch batchDelete(Collection<? extends UpdatableRecord<?>> records) {
		return delegateContext.batchDelete(records);
	}

	public <R extends Record> TruncateIdentityStep<R> truncate(Table<R> table) {
		return delegateContext.truncate(table);
	}

	public BigInteger lastID() throws DataAccessException {
		return delegateContext.lastID();
	}

	public <T extends Number> T nextval(Sequence<T> sequence)
			throws DataAccessException {
		return delegateContext.nextval(sequence);
	}

	public <T extends Number> T currval(Sequence<T> sequence)
			throws DataAccessException {
		return delegateContext.currval(sequence);
	}

	public <R extends UDTRecord<R>> R newRecord(UDT<R> type) {
		return delegateContext.newRecord(type);
	}

	public <R extends Record> R newRecord(Table<R> table) {
		return delegateContext.newRecord(table);
	}

	public <R extends Record> R newRecord(Table<R> table, Object source) {
		return delegateContext.newRecord(table, source);
	}

	public <R extends Record> Result<R> newResult(Table<R> table) {
		return delegateContext.newResult(table);
	}

	public <R extends Record> Result<R> fetch(ResultQuery<R> query)
			throws DataAccessException {
		return delegateContext.fetch(query);
	}

	public <R extends Record> Cursor<R> fetchLazy(ResultQuery<R> query)
			throws DataAccessException {
		return delegateContext.fetchLazy(query);
	}

	public <R extends Record> List<Result<Record>> fetchMany(
			ResultQuery<R> query) throws DataAccessException {
		return delegateContext.fetchMany(query);
	}

	public <R extends Record> R fetchOne(ResultQuery<R> query)
			throws DataAccessException, InvalidResultException {
		return delegateContext.fetchOne(query);
	}

	public int fetchCount(Select<?> query) throws DataAccessException {
		return delegateContext.fetchCount(query);
	}

	public int execute(Query query) throws DataAccessException {
		return delegateContext.execute(query);
	}

	public <R extends Record> Result<R> fetch(Table<R> table)
			throws DataAccessException {
		return delegateContext.fetch(table);
	}

	public <R extends Record> Result<R> fetch(Table<R> table,
			Condition condition) throws DataAccessException {
		return delegateContext.fetch(table, condition);
	}

	public <R extends Record> R fetchOne(Table<R> table)
			throws DataAccessException, InvalidResultException {
		return delegateContext.fetchOne(table);
	}

	public <R extends Record> R fetchOne(Table<R> table, Condition condition)
			throws DataAccessException, InvalidResultException {
		return delegateContext.fetchOne(table, condition);
	}

	public <R extends Record> R fetchAny(Table<R> table)
			throws DataAccessException {
		return delegateContext.fetchAny(table);
	}

	public <R extends Record> Cursor<R> fetchLazy(Table<R> table)
			throws DataAccessException {
		return delegateContext.fetchLazy(table);
	}

	public <R extends Record> Cursor<R> fetchLazy(Table<R> table,
			Condition condition) throws DataAccessException {
		return delegateContext.fetchLazy(table, condition);
	}

	public <R extends TableRecord<R>> int executeInsert(R record)
			throws DataAccessException {
		return delegateContext.executeInsert(record);
	}

	public <R extends UpdatableRecord<R>> int executeUpdate(R record)
			throws DataAccessException {
		return delegateContext.executeUpdate(record);
	}

	public <R extends TableRecord<R>, T> int executeUpdate(R record,
			Condition condition) throws DataAccessException {
		return delegateContext.executeUpdate(record, condition);
	}

	public <R extends UpdatableRecord<R>> int executeDelete(R record)
			throws DataAccessException {
		return delegateContext.executeDelete(record);
	}

	public <R extends TableRecord<R>, T> int executeDelete(R record,
			Condition condition) throws DataAccessException {
		return delegateContext.executeDelete(record, condition);
	}

	public Batch batch(String... queries) {
		return delegateContext.batch(queries);
	}

	public BatchBindStep batch(String sql) {
		return delegateContext.batch(sql);
	}

	public Batch batch(Query query, Object[]... bindings) {
		return delegateContext.batch(query, bindings);
	}

	public Batch batch(String sql, Object[]... bindings) {
		return delegateContext.batch(sql, bindings);
	}

	public <R extends Record> R fetchAny(Table<R> table, Condition condition)
			throws DataAccessException {
		return delegateContext.fetchAny(table, condition);
	}
}
