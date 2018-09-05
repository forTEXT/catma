package de.catma.repository.db.jooq;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.jooq.*;
import org.jooq.conf.Settings;
import org.jooq.exception.DataAccessException;
import org.jooq.exception.InvalidResultException;
import org.jooq.exception.TooManyRowsException;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockCallable;
import org.jooq.tools.jdbc.MockDataProvider;
import org.jooq.tools.jdbc.MockRunnable;

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
			
			if ((con != null) && !con.isClosed() && !con.getAutoCommit()) {
				con.rollback();
			}
		}
		catch(SQLException e) {
			Logger.getLogger("de.catma").log(
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
			Logger.getLogger("de.catma").log(
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

	public Settings settings() {
		return delegateContext.settings();
	}

	public SQLDialect dialect() {
		return delegateContext.dialect();
	}

	public SQLDialect family() {
		return delegateContext.family();
	}

	public Map<Object, Object> data() {
		return delegateContext.data();
	}

	public Object data(Object key) {
		return delegateContext.data(key);
	}

	public Object data(Object key, Object value) {
		return delegateContext.data(key, value);
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

	public <T> T transactionResult(TransactionalCallable<T> transactional) {
		return delegateContext.transactionResult(transactional);
	}

	public void transaction(TransactionalRunnable transactional) {
		delegateContext.transaction(transactional);
	}

	public <T> T connectionResult(ConnectionCallable<T> callable) {
		return delegateContext.connectionResult(callable);
	}

	public void connection(ConnectionRunnable runnable) {
		delegateContext.connection(runnable);
	}

	public <T> T mockResult(MockDataProvider provider, MockCallable<T> mockable) {
		return delegateContext.mockResult(provider, mockable);
	}

	public void mock(MockDataProvider provider, MockRunnable mockable) {
		delegateContext.mock(provider, mockable);
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

	public String renderNamedOrInlinedParams(QueryPart part) {
		return delegateContext.renderNamedOrInlinedParams(part);
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

	@Deprecated
	public int bind(QueryPart part, PreparedStatement stmt) {
		return delegateContext.bind(part, stmt);
	}

	public void attach(Attachable... attachables) {
		delegateContext.attach(attachables);
	}

	public void attach(Collection<? extends Attachable> attachables) {
		delegateContext.attach(attachables);
	}

	public <R extends TableRecord<R>> LoaderOptionsStep<R> loadInto(Table<R> table) {
		return delegateContext.loadInto(table);
	}

	public Query query(SQL sql) {
		return delegateContext.query(sql);
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

	public Result<Record> fetch(SQL sql) throws DataAccessException {
		return delegateContext.fetch(sql);
	}

	public Result<Record> fetch(String sql) throws DataAccessException {
		return delegateContext.fetch(sql);
	}

	public Result<Record> fetch(String sql, Object... bindings) throws DataAccessException {
		return delegateContext.fetch(sql, bindings);
	}

	public Result<Record> fetch(String sql, QueryPart... parts) throws DataAccessException {
		return delegateContext.fetch(sql, parts);
	}

	public Cursor<Record> fetchLazy(SQL sql) throws DataAccessException {
		return delegateContext.fetchLazy(sql);
	}

	public Cursor<Record> fetchLazy(String sql) throws DataAccessException {
		return delegateContext.fetchLazy(sql);
	}

	public Cursor<Record> fetchLazy(String sql, Object... bindings) throws DataAccessException {
		return delegateContext.fetchLazy(sql, bindings);
	}

	public Cursor<Record> fetchLazy(String sql, QueryPart... parts) throws DataAccessException {
		return delegateContext.fetchLazy(sql, parts);
	}

	public Stream<Record> fetchStream(SQL sql) throws DataAccessException {
		return delegateContext.fetchStream(sql);
	}

	public Stream<Record> fetchStream(String sql) throws DataAccessException {
		return delegateContext.fetchStream(sql);
	}

	public Stream<Record> fetchStream(String sql, Object... bindings) throws DataAccessException {
		return delegateContext.fetchStream(sql, bindings);
	}

	public Stream<Record> fetchStream(String sql, QueryPart... parts) throws DataAccessException {
		return delegateContext.fetchStream(sql, parts);
	}

	public Results fetchMany(SQL sql) throws DataAccessException {
		return delegateContext.fetchMany(sql);
	}

	public Results fetchMany(String sql) throws DataAccessException {
		return delegateContext.fetchMany(sql);
	}

	public Results fetchMany(String sql, Object... bindings) throws DataAccessException {
		return delegateContext.fetchMany(sql, bindings);
	}

	public Results fetchMany(String sql, QueryPart... parts) throws DataAccessException {
		return delegateContext.fetchMany(sql, parts);
	}

	public Record fetchOne(SQL sql) throws DataAccessException, TooManyRowsException {
		return delegateContext.fetchOne(sql);
	}

	public Record fetchOne(String sql) throws DataAccessException, TooManyRowsException {
		return delegateContext.fetchOne(sql);
	}

	public Record fetchOne(String sql, Object... bindings) throws DataAccessException, TooManyRowsException {
		return delegateContext.fetchOne(sql, bindings);
	}

	public Record fetchOne(String sql, QueryPart... parts) throws DataAccessException, TooManyRowsException {
		return delegateContext.fetchOne(sql, parts);
	}

	public Optional<Record> fetchOptional(SQL sql) throws DataAccessException, TooManyRowsException {
		return delegateContext.fetchOptional(sql);
	}

	public Optional<Record> fetchOptional(String sql) throws DataAccessException, TooManyRowsException {
		return delegateContext.fetchOptional(sql);
	}

	public Optional<Record> fetchOptional(String sql, Object... bindings)
			throws DataAccessException, TooManyRowsException {
		return delegateContext.fetchOptional(sql, bindings);
	}

	public Optional<Record> fetchOptional(String sql, QueryPart... parts)
			throws DataAccessException, TooManyRowsException {
		return delegateContext.fetchOptional(sql, parts);
	}

	public Object fetchValue(SQL sql) throws DataAccessException, TooManyRowsException, InvalidResultException {
		return delegateContext.fetchValue(sql);
	}

	public Object fetchValue(String sql) throws DataAccessException, TooManyRowsException, InvalidResultException {
		return delegateContext.fetchValue(sql);
	}

	public Object fetchValue(String sql, Object... bindings)
			throws DataAccessException, TooManyRowsException, InvalidResultException {
		return delegateContext.fetchValue(sql, bindings);
	}

	public Object fetchValue(String sql, QueryPart... parts)
			throws DataAccessException, TooManyRowsException, InvalidResultException {
		return delegateContext.fetchValue(sql, parts);
	}

	public Optional<?> fetchOptionalValue(SQL sql)
			throws DataAccessException, TooManyRowsException, InvalidResultException {
		return delegateContext.fetchOptionalValue(sql);
	}

	public Optional<?> fetchOptionalValue(String sql)
			throws DataAccessException, TooManyRowsException, InvalidResultException {
		return delegateContext.fetchOptionalValue(sql);
	}

	public Optional<?> fetchOptionalValue(String sql, Object... bindings)
			throws DataAccessException, TooManyRowsException, InvalidResultException {
		return delegateContext.fetchOptionalValue(sql, bindings);
	}

	public Optional<?> fetchOptionalValue(String sql, QueryPart... parts)
			throws DataAccessException, TooManyRowsException, InvalidResultException {
		return delegateContext.fetchOptionalValue(sql, parts);
	}

	public List<?> fetchValues(SQL sql) throws DataAccessException {
		return delegateContext.fetchValues(sql);
	}

	public List<?> fetchValues(String sql) throws DataAccessException {
		return delegateContext.fetchValues(sql);
	}

	public List<?> fetchValues(String sql, Object... bindings) throws DataAccessException {
		return delegateContext.fetchValues(sql, bindings);
	}

	public List<?> fetchValues(String sql, QueryPart... parts) throws DataAccessException {
		return delegateContext.fetchValues(sql, parts);
	}

	public int execute(SQL sql) throws DataAccessException {
		return delegateContext.execute(sql);
	}

	public int execute(String sql) throws DataAccessException {
		return delegateContext.execute(sql);
	}

	public int execute(String sql, Object... bindings) throws DataAccessException {
		return delegateContext.execute(sql, bindings);
	}

	public int execute(String sql, QueryPart... parts) throws DataAccessException {
		return delegateContext.execute(sql, parts);
	}

	public ResultQuery<Record> resultQuery(SQL sql) {
		return delegateContext.resultQuery(sql);
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

	public Result<Record> fetch(ResultSet rs, Field<?>... fields) throws DataAccessException {
		return delegateContext.fetch(rs, fields);
	}

	public Result<Record> fetch(ResultSet rs, DataType<?>... types) throws DataAccessException {
		return delegateContext.fetch(rs, types);
	}

	public Result<Record> fetch(ResultSet rs, Class<?>... types) throws DataAccessException {
		return delegateContext.fetch(rs, types);
	}

	public Record fetchOne(ResultSet rs) throws DataAccessException, TooManyRowsException {
		return delegateContext.fetchOne(rs);
	}

	public Record fetchOne(ResultSet rs, Field<?>... fields) throws DataAccessException, TooManyRowsException {
		return delegateContext.fetchOne(rs, fields);
	}

	public Record fetchOne(ResultSet rs, DataType<?>... types) throws DataAccessException, TooManyRowsException {
		return delegateContext.fetchOne(rs, types);
	}

	public Record fetchOne(ResultSet rs, Class<?>... types) throws DataAccessException, TooManyRowsException {
		return delegateContext.fetchOne(rs, types);
	}

	public Optional<Record> fetchOptional(ResultSet rs) throws DataAccessException, TooManyRowsException {
		return delegateContext.fetchOptional(rs);
	}

	public Optional<Record> fetchOptional(ResultSet rs, Field<?>... fields)
			throws DataAccessException, TooManyRowsException {
		return delegateContext.fetchOptional(rs, fields);
	}

	public Optional<Record> fetchOptional(ResultSet rs, DataType<?>... types)
			throws DataAccessException, TooManyRowsException {
		return delegateContext.fetchOptional(rs, types);
	}

	public Optional<Record> fetchOptional(ResultSet rs, Class<?>... types)
			throws DataAccessException, TooManyRowsException {
		return delegateContext.fetchOptional(rs, types);
	}

	public Object fetchValue(ResultSet rs) throws DataAccessException, TooManyRowsException, InvalidResultException {
		return delegateContext.fetchValue(rs);
	}

	public <T> T fetchValue(ResultSet rs, Field<T> field)
			throws DataAccessException, TooManyRowsException, InvalidResultException {
		return delegateContext.fetchValue(rs, field);
	}

	public <T> T fetchValue(ResultSet rs, DataType<T> type)
			throws DataAccessException, TooManyRowsException, InvalidResultException {
		return delegateContext.fetchValue(rs, type);
	}

	public <T> T fetchValue(ResultSet rs, Class<T> type)
			throws DataAccessException, TooManyRowsException, InvalidResultException {
		return delegateContext.fetchValue(rs, type);
	}

	public Optional<?> fetchOptionalValue(ResultSet rs)
			throws DataAccessException, TooManyRowsException, InvalidResultException {
		return delegateContext.fetchOptionalValue(rs);
	}

	public <T> Optional<T> fetchOptionalValue(ResultSet rs, Field<T> field)
			throws DataAccessException, TooManyRowsException, InvalidResultException {
		return delegateContext.fetchOptionalValue(rs, field);
	}

	public <T> Optional<T> fetchOptionalValue(ResultSet rs, DataType<T> type)
			throws DataAccessException, TooManyRowsException, InvalidResultException {
		return delegateContext.fetchOptionalValue(rs, type);
	}

	public <T> Optional<T> fetchOptionalValue(ResultSet rs, Class<T> type)
			throws DataAccessException, TooManyRowsException, InvalidResultException {
		return delegateContext.fetchOptionalValue(rs, type);
	}

	public List<?> fetchValues(ResultSet rs) throws DataAccessException {
		return delegateContext.fetchValues(rs);
	}

	public <T> List<T> fetchValues(ResultSet rs, Field<T> field) throws DataAccessException {
		return delegateContext.fetchValues(rs, field);
	}

	public <T> List<T> fetchValues(ResultSet rs, DataType<T> type) throws DataAccessException {
		return delegateContext.fetchValues(rs, type);
	}

	public <T> List<T> fetchValues(ResultSet rs, Class<T> type) throws DataAccessException {
		return delegateContext.fetchValues(rs, type);
	}

	public Cursor<Record> fetchLazy(ResultSet rs) throws DataAccessException {
		return delegateContext.fetchLazy(rs);
	}

	public Cursor<Record> fetchLazy(ResultSet rs, Field<?>... fields) throws DataAccessException {
		return delegateContext.fetchLazy(rs, fields);
	}

	public Cursor<Record> fetchLazy(ResultSet rs, DataType<?>... types) throws DataAccessException {
		return delegateContext.fetchLazy(rs, types);
	}

	public Cursor<Record> fetchLazy(ResultSet rs, Class<?>... types) throws DataAccessException {
		return delegateContext.fetchLazy(rs, types);
	}

	public Stream<Record> fetchStream(ResultSet rs) throws DataAccessException {
		return delegateContext.fetchStream(rs);
	}

	public Stream<Record> fetchStream(ResultSet rs, Field<?>... fields) throws DataAccessException {
		return delegateContext.fetchStream(rs, fields);
	}

	public Stream<Record> fetchStream(ResultSet rs, DataType<?>... types) throws DataAccessException {
		return delegateContext.fetchStream(rs, types);
	}

	public Stream<Record> fetchStream(ResultSet rs, Class<?>... types) throws DataAccessException {
		return delegateContext.fetchStream(rs, types);
	}

	public Result<Record> fetchFromTXT(String string) throws DataAccessException {
		return delegateContext.fetchFromTXT(string);
	}

	public Result<Record> fetchFromTXT(String string, String nullLiteral) throws DataAccessException {
		return delegateContext.fetchFromTXT(string, nullLiteral);
	}

	public Result<Record> fetchFromCSV(String string) throws DataAccessException {
		return delegateContext.fetchFromCSV(string);
	}

	public Result<Record> fetchFromCSV(String string, char delimiter) throws DataAccessException {
		return delegateContext.fetchFromCSV(string, delimiter);
	}

	public Result<Record> fetchFromCSV(String string, boolean header) throws DataAccessException {
		return delegateContext.fetchFromCSV(string, header);
	}

	public Result<Record> fetchFromCSV(String string, boolean header, char delimiter) throws DataAccessException {
		return delegateContext.fetchFromCSV(string, header, delimiter);
	}

	public Result<Record> fetchFromJSON(String string) {
		return delegateContext.fetchFromJSON(string);
	}

	public Result<Record> fetchFromStringData(String[]... data) {
		return delegateContext.fetchFromStringData(data);
	}

	public Result<Record> fetchFromStringData(List<String[]> data) {
		return delegateContext.fetchFromStringData(data);
	}

	public Result<Record> fetchFromStringData(List<String[]> data, boolean header) {
		return delegateContext.fetchFromStringData(data, header);
	}

	public WithAsStep with(String alias) {
		return delegateContext.with(alias);
	}

	public WithAsStep with(String alias, String... fieldAliases) {
		return delegateContext.with(alias, fieldAliases);
	}

	public WithStep with(CommonTableExpression<?>... tables) {
		return delegateContext.with(tables);
	}

	public WithAsStep withRecursive(String alias) {
		return delegateContext.withRecursive(alias);
	}

	public WithAsStep withRecursive(String alias, String... fieldAliases) {
		return delegateContext.withRecursive(alias, fieldAliases);
	}

	public WithStep withRecursive(CommonTableExpression<?>... tables) {
		return delegateContext.withRecursive(tables);
	}

	public <R extends Record> SelectWhereStep<R> selectFrom(Table<R> table) {
		return delegateContext.selectFrom(table);
	}

	public SelectSelectStep<Record> select(Collection<? extends SelectField<?>> fields) {
		return delegateContext.select(fields);
	}

	public SelectSelectStep<Record> select(SelectField<?>... fields) {
		return delegateContext.select(fields);
	}

	public <T1> SelectSelectStep<Record1<T1>> select(SelectField<T1> field1) {
		return delegateContext.select(field1);
	}

	public <T1, T2> SelectSelectStep<Record2<T1, T2>> select(SelectField<T1> field1, SelectField<T2> field2) {
		return delegateContext.select(field1, field2);
	}

	public <T1, T2, T3> SelectSelectStep<Record3<T1, T2, T3>> select(SelectField<T1> field1, SelectField<T2> field2,
			SelectField<T3> field3) {
		return delegateContext.select(field1, field2, field3);
	}

	public <T1, T2, T3, T4> SelectSelectStep<Record4<T1, T2, T3, T4>> select(SelectField<T1> field1,
			SelectField<T2> field2, SelectField<T3> field3, SelectField<T4> field4) {
		return delegateContext.select(field1, field2, field3, field4);
	}

	public <T1, T2, T3, T4, T5> SelectSelectStep<Record5<T1, T2, T3, T4, T5>> select(SelectField<T1> field1,
			SelectField<T2> field2, SelectField<T3> field3, SelectField<T4> field4, SelectField<T5> field5) {
		return delegateContext.select(field1, field2, field3, field4, field5);
	}

	public <T1, T2, T3, T4, T5, T6> SelectSelectStep<Record6<T1, T2, T3, T4, T5, T6>> select(SelectField<T1> field1,
			SelectField<T2> field2, SelectField<T3> field3, SelectField<T4> field4, SelectField<T5> field5,
			SelectField<T6> field6) {
		return delegateContext.select(field1, field2, field3, field4, field5, field6);
	}

	public <T1, T2, T3, T4, T5, T6, T7> SelectSelectStep<Record7<T1, T2, T3, T4, T5, T6, T7>> select(
			SelectField<T1> field1, SelectField<T2> field2, SelectField<T3> field3, SelectField<T4> field4,
			SelectField<T5> field5, SelectField<T6> field6, SelectField<T7> field7) {
		return delegateContext.select(field1, field2, field3, field4, field5, field6, field7);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8> SelectSelectStep<Record8<T1, T2, T3, T4, T5, T6, T7, T8>> select(
			SelectField<T1> field1, SelectField<T2> field2, SelectField<T3> field3, SelectField<T4> field4,
			SelectField<T5> field5, SelectField<T6> field6, SelectField<T7> field7, SelectField<T8> field8) {
		return delegateContext.select(field1, field2, field3, field4, field5, field6, field7, field8);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9> SelectSelectStep<Record9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> select(
			SelectField<T1> field1, SelectField<T2> field2, SelectField<T3> field3, SelectField<T4> field4,
			SelectField<T5> field5, SelectField<T6> field6, SelectField<T7> field7, SelectField<T8> field8,
			SelectField<T9> field9) {
		return delegateContext.select(field1, field2, field3, field4, field5, field6, field7, field8, field9);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> SelectSelectStep<Record10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>> select(
			SelectField<T1> field1, SelectField<T2> field2, SelectField<T3> field3, SelectField<T4> field4,
			SelectField<T5> field5, SelectField<T6> field6, SelectField<T7> field7, SelectField<T8> field8,
			SelectField<T9> field9, SelectField<T10> field10) {
		return delegateContext.select(field1, field2, field3, field4, field5, field6, field7, field8, field9, field10);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> SelectSelectStep<Record11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>> select(
			SelectField<T1> field1, SelectField<T2> field2, SelectField<T3> field3, SelectField<T4> field4,
			SelectField<T5> field5, SelectField<T6> field6, SelectField<T7> field7, SelectField<T8> field8,
			SelectField<T9> field9, SelectField<T10> field10, SelectField<T11> field11) {
		return delegateContext.select(field1, field2, field3, field4, field5, field6, field7, field8, field9, field10,
				field11);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> SelectSelectStep<Record12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>> select(
			SelectField<T1> field1, SelectField<T2> field2, SelectField<T3> field3, SelectField<T4> field4,
			SelectField<T5> field5, SelectField<T6> field6, SelectField<T7> field7, SelectField<T8> field8,
			SelectField<T9> field9, SelectField<T10> field10, SelectField<T11> field11, SelectField<T12> field12) {
		return delegateContext.select(field1, field2, field3, field4, field5, field6, field7, field8, field9, field10,
				field11, field12);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> SelectSelectStep<Record13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>> select(
			SelectField<T1> field1, SelectField<T2> field2, SelectField<T3> field3, SelectField<T4> field4,
			SelectField<T5> field5, SelectField<T6> field6, SelectField<T7> field7, SelectField<T8> field8,
			SelectField<T9> field9, SelectField<T10> field10, SelectField<T11> field11, SelectField<T12> field12,
			SelectField<T13> field13) {
		return delegateContext.select(field1, field2, field3, field4, field5, field6, field7, field8, field9, field10,
				field11, field12, field13);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> SelectSelectStep<Record14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>> select(
			SelectField<T1> field1, SelectField<T2> field2, SelectField<T3> field3, SelectField<T4> field4,
			SelectField<T5> field5, SelectField<T6> field6, SelectField<T7> field7, SelectField<T8> field8,
			SelectField<T9> field9, SelectField<T10> field10, SelectField<T11> field11, SelectField<T12> field12,
			SelectField<T13> field13, SelectField<T14> field14) {
		return delegateContext.select(field1, field2, field3, field4, field5, field6, field7, field8, field9, field10,
				field11, field12, field13, field14);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> SelectSelectStep<Record15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>> select(
			SelectField<T1> field1, SelectField<T2> field2, SelectField<T3> field3, SelectField<T4> field4,
			SelectField<T5> field5, SelectField<T6> field6, SelectField<T7> field7, SelectField<T8> field8,
			SelectField<T9> field9, SelectField<T10> field10, SelectField<T11> field11, SelectField<T12> field12,
			SelectField<T13> field13, SelectField<T14> field14, SelectField<T15> field15) {
		return delegateContext.select(field1, field2, field3, field4, field5, field6, field7, field8, field9, field10,
				field11, field12, field13, field14, field15);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> SelectSelectStep<Record16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>> select(
			SelectField<T1> field1, SelectField<T2> field2, SelectField<T3> field3, SelectField<T4> field4,
			SelectField<T5> field5, SelectField<T6> field6, SelectField<T7> field7, SelectField<T8> field8,
			SelectField<T9> field9, SelectField<T10> field10, SelectField<T11> field11, SelectField<T12> field12,
			SelectField<T13> field13, SelectField<T14> field14, SelectField<T15> field15, SelectField<T16> field16) {
		return delegateContext.select(field1, field2, field3, field4, field5, field6, field7, field8, field9, field10,
				field11, field12, field13, field14, field15, field16);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> SelectSelectStep<Record17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>> select(
			SelectField<T1> field1, SelectField<T2> field2, SelectField<T3> field3, SelectField<T4> field4,
			SelectField<T5> field5, SelectField<T6> field6, SelectField<T7> field7, SelectField<T8> field8,
			SelectField<T9> field9, SelectField<T10> field10, SelectField<T11> field11, SelectField<T12> field12,
			SelectField<T13> field13, SelectField<T14> field14, SelectField<T15> field15, SelectField<T16> field16,
			SelectField<T17> field17) {
		return delegateContext.select(field1, field2, field3, field4, field5, field6, field7, field8, field9, field10,
				field11, field12, field13, field14, field15, field16, field17);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18> SelectSelectStep<Record18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>> select(
			SelectField<T1> field1, SelectField<T2> field2, SelectField<T3> field3, SelectField<T4> field4,
			SelectField<T5> field5, SelectField<T6> field6, SelectField<T7> field7, SelectField<T8> field8,
			SelectField<T9> field9, SelectField<T10> field10, SelectField<T11> field11, SelectField<T12> field12,
			SelectField<T13> field13, SelectField<T14> field14, SelectField<T15> field15, SelectField<T16> field16,
			SelectField<T17> field17, SelectField<T18> field18) {
		return delegateContext.select(field1, field2, field3, field4, field5, field6, field7, field8, field9, field10,
				field11, field12, field13, field14, field15, field16, field17, field18);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19> SelectSelectStep<Record19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>> select(
			SelectField<T1> field1, SelectField<T2> field2, SelectField<T3> field3, SelectField<T4> field4,
			SelectField<T5> field5, SelectField<T6> field6, SelectField<T7> field7, SelectField<T8> field8,
			SelectField<T9> field9, SelectField<T10> field10, SelectField<T11> field11, SelectField<T12> field12,
			SelectField<T13> field13, SelectField<T14> field14, SelectField<T15> field15, SelectField<T16> field16,
			SelectField<T17> field17, SelectField<T18> field18, SelectField<T19> field19) {
		return delegateContext.select(field1, field2, field3, field4, field5, field6, field7, field8, field9, field10,
				field11, field12, field13, field14, field15, field16, field17, field18, field19);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20> SelectSelectStep<Record20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>> select(
			SelectField<T1> field1, SelectField<T2> field2, SelectField<T3> field3, SelectField<T4> field4,
			SelectField<T5> field5, SelectField<T6> field6, SelectField<T7> field7, SelectField<T8> field8,
			SelectField<T9> field9, SelectField<T10> field10, SelectField<T11> field11, SelectField<T12> field12,
			SelectField<T13> field13, SelectField<T14> field14, SelectField<T15> field15, SelectField<T16> field16,
			SelectField<T17> field17, SelectField<T18> field18, SelectField<T19> field19, SelectField<T20> field20) {
		return delegateContext.select(field1, field2, field3, field4, field5, field6, field7, field8, field9, field10,
				field11, field12, field13, field14, field15, field16, field17, field18, field19, field20);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21> SelectSelectStep<Record21<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21>> select(
			SelectField<T1> field1, SelectField<T2> field2, SelectField<T3> field3, SelectField<T4> field4,
			SelectField<T5> field5, SelectField<T6> field6, SelectField<T7> field7, SelectField<T8> field8,
			SelectField<T9> field9, SelectField<T10> field10, SelectField<T11> field11, SelectField<T12> field12,
			SelectField<T13> field13, SelectField<T14> field14, SelectField<T15> field15, SelectField<T16> field16,
			SelectField<T17> field17, SelectField<T18> field18, SelectField<T19> field19, SelectField<T20> field20,
			SelectField<T21> field21) {
		return delegateContext.select(field1, field2, field3, field4, field5, field6, field7, field8, field9, field10,
				field11, field12, field13, field14, field15, field16, field17, field18, field19, field20, field21);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22> SelectSelectStep<Record22<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22>> select(
			SelectField<T1> field1, SelectField<T2> field2, SelectField<T3> field3, SelectField<T4> field4,
			SelectField<T5> field5, SelectField<T6> field6, SelectField<T7> field7, SelectField<T8> field8,
			SelectField<T9> field9, SelectField<T10> field10, SelectField<T11> field11, SelectField<T12> field12,
			SelectField<T13> field13, SelectField<T14> field14, SelectField<T15> field15, SelectField<T16> field16,
			SelectField<T17> field17, SelectField<T18> field18, SelectField<T19> field19, SelectField<T20> field20,
			SelectField<T21> field21, SelectField<T22> field22) {
		return delegateContext.select(field1, field2, field3, field4, field5, field6, field7, field8, field9, field10,
				field11, field12, field13, field14, field15, field16, field17, field18, field19, field20, field21,
				field22);
	}

	public SelectSelectStep<Record> selectDistinct(Collection<? extends SelectField<?>> fields) {
		return delegateContext.selectDistinct(fields);
	}

	public SelectSelectStep<Record> selectDistinct(SelectField<?>... fields) {
		return delegateContext.selectDistinct(fields);
	}

	public <T1> SelectSelectStep<Record1<T1>> selectDistinct(SelectField<T1> field1) {
		return delegateContext.selectDistinct(field1);
	}

	public <T1, T2> SelectSelectStep<Record2<T1, T2>> selectDistinct(SelectField<T1> field1, SelectField<T2> field2) {
		return delegateContext.selectDistinct(field1, field2);
	}

	public <T1, T2, T3> SelectSelectStep<Record3<T1, T2, T3>> selectDistinct(SelectField<T1> field1,
			SelectField<T2> field2, SelectField<T3> field3) {
		return delegateContext.selectDistinct(field1, field2, field3);
	}

	public <T1, T2, T3, T4> SelectSelectStep<Record4<T1, T2, T3, T4>> selectDistinct(SelectField<T1> field1,
			SelectField<T2> field2, SelectField<T3> field3, SelectField<T4> field4) {
		return delegateContext.selectDistinct(field1, field2, field3, field4);
	}

	public <T1, T2, T3, T4, T5> SelectSelectStep<Record5<T1, T2, T3, T4, T5>> selectDistinct(SelectField<T1> field1,
			SelectField<T2> field2, SelectField<T3> field3, SelectField<T4> field4, SelectField<T5> field5) {
		return delegateContext.selectDistinct(field1, field2, field3, field4, field5);
	}

	public <T1, T2, T3, T4, T5, T6> SelectSelectStep<Record6<T1, T2, T3, T4, T5, T6>> selectDistinct(
			SelectField<T1> field1, SelectField<T2> field2, SelectField<T3> field3, SelectField<T4> field4,
			SelectField<T5> field5, SelectField<T6> field6) {
		return delegateContext.selectDistinct(field1, field2, field3, field4, field5, field6);
	}

	public <T1, T2, T3, T4, T5, T6, T7> SelectSelectStep<Record7<T1, T2, T3, T4, T5, T6, T7>> selectDistinct(
			SelectField<T1> field1, SelectField<T2> field2, SelectField<T3> field3, SelectField<T4> field4,
			SelectField<T5> field5, SelectField<T6> field6, SelectField<T7> field7) {
		return delegateContext.selectDistinct(field1, field2, field3, field4, field5, field6, field7);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8> SelectSelectStep<Record8<T1, T2, T3, T4, T5, T6, T7, T8>> selectDistinct(
			SelectField<T1> field1, SelectField<T2> field2, SelectField<T3> field3, SelectField<T4> field4,
			SelectField<T5> field5, SelectField<T6> field6, SelectField<T7> field7, SelectField<T8> field8) {
		return delegateContext.selectDistinct(field1, field2, field3, field4, field5, field6, field7, field8);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9> SelectSelectStep<Record9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> selectDistinct(
			SelectField<T1> field1, SelectField<T2> field2, SelectField<T3> field3, SelectField<T4> field4,
			SelectField<T5> field5, SelectField<T6> field6, SelectField<T7> field7, SelectField<T8> field8,
			SelectField<T9> field9) {
		return delegateContext.selectDistinct(field1, field2, field3, field4, field5, field6, field7, field8, field9);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> SelectSelectStep<Record10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>> selectDistinct(
			SelectField<T1> field1, SelectField<T2> field2, SelectField<T3> field3, SelectField<T4> field4,
			SelectField<T5> field5, SelectField<T6> field6, SelectField<T7> field7, SelectField<T8> field8,
			SelectField<T9> field9, SelectField<T10> field10) {
		return delegateContext.selectDistinct(field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> SelectSelectStep<Record11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>> selectDistinct(
			SelectField<T1> field1, SelectField<T2> field2, SelectField<T3> field3, SelectField<T4> field4,
			SelectField<T5> field5, SelectField<T6> field6, SelectField<T7> field7, SelectField<T8> field8,
			SelectField<T9> field9, SelectField<T10> field10, SelectField<T11> field11) {
		return delegateContext.selectDistinct(field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> SelectSelectStep<Record12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>> selectDistinct(
			SelectField<T1> field1, SelectField<T2> field2, SelectField<T3> field3, SelectField<T4> field4,
			SelectField<T5> field5, SelectField<T6> field6, SelectField<T7> field7, SelectField<T8> field8,
			SelectField<T9> field9, SelectField<T10> field10, SelectField<T11> field11, SelectField<T12> field12) {
		return delegateContext.selectDistinct(field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> SelectSelectStep<Record13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>> selectDistinct(
			SelectField<T1> field1, SelectField<T2> field2, SelectField<T3> field3, SelectField<T4> field4,
			SelectField<T5> field5, SelectField<T6> field6, SelectField<T7> field7, SelectField<T8> field8,
			SelectField<T9> field9, SelectField<T10> field10, SelectField<T11> field11, SelectField<T12> field12,
			SelectField<T13> field13) {
		return delegateContext.selectDistinct(field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12, field13);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> SelectSelectStep<Record14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>> selectDistinct(
			SelectField<T1> field1, SelectField<T2> field2, SelectField<T3> field3, SelectField<T4> field4,
			SelectField<T5> field5, SelectField<T6> field6, SelectField<T7> field7, SelectField<T8> field8,
			SelectField<T9> field9, SelectField<T10> field10, SelectField<T11> field11, SelectField<T12> field12,
			SelectField<T13> field13, SelectField<T14> field14) {
		return delegateContext.selectDistinct(field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12, field13, field14);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> SelectSelectStep<Record15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>> selectDistinct(
			SelectField<T1> field1, SelectField<T2> field2, SelectField<T3> field3, SelectField<T4> field4,
			SelectField<T5> field5, SelectField<T6> field6, SelectField<T7> field7, SelectField<T8> field8,
			SelectField<T9> field9, SelectField<T10> field10, SelectField<T11> field11, SelectField<T12> field12,
			SelectField<T13> field13, SelectField<T14> field14, SelectField<T15> field15) {
		return delegateContext.selectDistinct(field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12, field13, field14, field15);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> SelectSelectStep<Record16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>> selectDistinct(
			SelectField<T1> field1, SelectField<T2> field2, SelectField<T3> field3, SelectField<T4> field4,
			SelectField<T5> field5, SelectField<T6> field6, SelectField<T7> field7, SelectField<T8> field8,
			SelectField<T9> field9, SelectField<T10> field10, SelectField<T11> field11, SelectField<T12> field12,
			SelectField<T13> field13, SelectField<T14> field14, SelectField<T15> field15, SelectField<T16> field16) {
		return delegateContext.selectDistinct(field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12, field13, field14, field15, field16);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> SelectSelectStep<Record17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>> selectDistinct(
			SelectField<T1> field1, SelectField<T2> field2, SelectField<T3> field3, SelectField<T4> field4,
			SelectField<T5> field5, SelectField<T6> field6, SelectField<T7> field7, SelectField<T8> field8,
			SelectField<T9> field9, SelectField<T10> field10, SelectField<T11> field11, SelectField<T12> field12,
			SelectField<T13> field13, SelectField<T14> field14, SelectField<T15> field15, SelectField<T16> field16,
			SelectField<T17> field17) {
		return delegateContext.selectDistinct(field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12, field13, field14, field15, field16, field17);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18> SelectSelectStep<Record18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>> selectDistinct(
			SelectField<T1> field1, SelectField<T2> field2, SelectField<T3> field3, SelectField<T4> field4,
			SelectField<T5> field5, SelectField<T6> field6, SelectField<T7> field7, SelectField<T8> field8,
			SelectField<T9> field9, SelectField<T10> field10, SelectField<T11> field11, SelectField<T12> field12,
			SelectField<T13> field13, SelectField<T14> field14, SelectField<T15> field15, SelectField<T16> field16,
			SelectField<T17> field17, SelectField<T18> field18) {
		return delegateContext.selectDistinct(field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12, field13, field14, field15, field16, field17, field18);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19> SelectSelectStep<Record19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>> selectDistinct(
			SelectField<T1> field1, SelectField<T2> field2, SelectField<T3> field3, SelectField<T4> field4,
			SelectField<T5> field5, SelectField<T6> field6, SelectField<T7> field7, SelectField<T8> field8,
			SelectField<T9> field9, SelectField<T10> field10, SelectField<T11> field11, SelectField<T12> field12,
			SelectField<T13> field13, SelectField<T14> field14, SelectField<T15> field15, SelectField<T16> field16,
			SelectField<T17> field17, SelectField<T18> field18, SelectField<T19> field19) {
		return delegateContext.selectDistinct(field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12, field13, field14, field15, field16, field17, field18, field19);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20> SelectSelectStep<Record20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>> selectDistinct(
			SelectField<T1> field1, SelectField<T2> field2, SelectField<T3> field3, SelectField<T4> field4,
			SelectField<T5> field5, SelectField<T6> field6, SelectField<T7> field7, SelectField<T8> field8,
			SelectField<T9> field9, SelectField<T10> field10, SelectField<T11> field11, SelectField<T12> field12,
			SelectField<T13> field13, SelectField<T14> field14, SelectField<T15> field15, SelectField<T16> field16,
			SelectField<T17> field17, SelectField<T18> field18, SelectField<T19> field19, SelectField<T20> field20) {
		return delegateContext.selectDistinct(field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12, field13, field14, field15, field16, field17, field18, field19, field20);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21> SelectSelectStep<Record21<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21>> selectDistinct(
			SelectField<T1> field1, SelectField<T2> field2, SelectField<T3> field3, SelectField<T4> field4,
			SelectField<T5> field5, SelectField<T6> field6, SelectField<T7> field7, SelectField<T8> field8,
			SelectField<T9> field9, SelectField<T10> field10, SelectField<T11> field11, SelectField<T12> field12,
			SelectField<T13> field13, SelectField<T14> field14, SelectField<T15> field15, SelectField<T16> field16,
			SelectField<T17> field17, SelectField<T18> field18, SelectField<T19> field19, SelectField<T20> field20,
			SelectField<T21> field21) {
		return delegateContext.selectDistinct(field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12, field13, field14, field15, field16, field17, field18, field19, field20,
				field21);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22> SelectSelectStep<Record22<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22>> selectDistinct(
			SelectField<T1> field1, SelectField<T2> field2, SelectField<T3> field3, SelectField<T4> field4,
			SelectField<T5> field5, SelectField<T6> field6, SelectField<T7> field7, SelectField<T8> field8,
			SelectField<T9> field9, SelectField<T10> field10, SelectField<T11> field11, SelectField<T12> field12,
			SelectField<T13> field13, SelectField<T14> field14, SelectField<T15> field15, SelectField<T16> field16,
			SelectField<T17> field17, SelectField<T18> field18, SelectField<T19> field19, SelectField<T20> field20,
			SelectField<T21> field21, SelectField<T22> field22) {
		return delegateContext.selectDistinct(field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12, field13, field14, field15, field16, field17, field18, field19, field20,
				field21, field22);
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

	public <R extends Record, T1> InsertValuesStep1<R, T1> insertInto(Table<R> into, Field<T1> field1) {
		return delegateContext.insertInto(into, field1);
	}

	public <R extends Record, T1, T2> InsertValuesStep2<R, T1, T2> insertInto(Table<R> into, Field<T1> field1,
			Field<T2> field2) {
		return delegateContext.insertInto(into, field1, field2);
	}

	public <R extends Record, T1, T2, T3> InsertValuesStep3<R, T1, T2, T3> insertInto(Table<R> into, Field<T1> field1,
			Field<T2> field2, Field<T3> field3) {
		return delegateContext.insertInto(into, field1, field2, field3);
	}

	public <R extends Record, T1, T2, T3, T4> InsertValuesStep4<R, T1, T2, T3, T4> insertInto(Table<R> into,
			Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4) {
		return delegateContext.insertInto(into, field1, field2, field3, field4);
	}

	public <R extends Record, T1, T2, T3, T4, T5> InsertValuesStep5<R, T1, T2, T3, T4, T5> insertInto(Table<R> into,
			Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5) {
		return delegateContext.insertInto(into, field1, field2, field3, field4, field5);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6> InsertValuesStep6<R, T1, T2, T3, T4, T5, T6> insertInto(
			Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6) {
		return delegateContext.insertInto(into, field1, field2, field3, field4, field5, field6);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7> InsertValuesStep7<R, T1, T2, T3, T4, T5, T6, T7> insertInto(
			Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7) {
		return delegateContext.insertInto(into, field1, field2, field3, field4, field5, field6, field7);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8> InsertValuesStep8<R, T1, T2, T3, T4, T5, T6, T7, T8> insertInto(
			Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8) {
		return delegateContext.insertInto(into, field1, field2, field3, field4, field5, field6, field7, field8);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9> InsertValuesStep9<R, T1, T2, T3, T4, T5, T6, T7, T8, T9> insertInto(
			Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9) {
		return delegateContext.insertInto(into, field1, field2, field3, field4, field5, field6, field7, field8, field9);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> InsertValuesStep10<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> insertInto(
			Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10) {
		return delegateContext.insertInto(into, field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> InsertValuesStep11<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> insertInto(
			Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10,
			Field<T11> field11) {
		return delegateContext.insertInto(into, field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> InsertValuesStep12<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> insertInto(
			Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10,
			Field<T11> field11, Field<T12> field12) {
		return delegateContext.insertInto(into, field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> InsertValuesStep13<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> insertInto(
			Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10,
			Field<T11> field11, Field<T12> field12, Field<T13> field13) {
		return delegateContext.insertInto(into, field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12, field13);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> InsertValuesStep14<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> insertInto(
			Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10,
			Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14) {
		return delegateContext.insertInto(into, field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12, field13, field14);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> InsertValuesStep15<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> insertInto(
			Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10,
			Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15) {
		return delegateContext.insertInto(into, field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12, field13, field14, field15);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> InsertValuesStep16<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> insertInto(
			Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10,
			Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15,
			Field<T16> field16) {
		return delegateContext.insertInto(into, field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12, field13, field14, field15, field16);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> InsertValuesStep17<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> insertInto(
			Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10,
			Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15,
			Field<T16> field16, Field<T17> field17) {
		return delegateContext.insertInto(into, field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12, field13, field14, field15, field16, field17);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18> InsertValuesStep18<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18> insertInto(
			Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10,
			Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15,
			Field<T16> field16, Field<T17> field17, Field<T18> field18) {
		return delegateContext.insertInto(into, field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12, field13, field14, field15, field16, field17, field18);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19> InsertValuesStep19<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19> insertInto(
			Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10,
			Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15,
			Field<T16> field16, Field<T17> field17, Field<T18> field18, Field<T19> field19) {
		return delegateContext.insertInto(into, field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12, field13, field14, field15, field16, field17, field18, field19);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20> InsertValuesStep20<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20> insertInto(
			Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10,
			Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15,
			Field<T16> field16, Field<T17> field17, Field<T18> field18, Field<T19> field19, Field<T20> field20) {
		return delegateContext.insertInto(into, field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12, field13, field14, field15, field16, field17, field18, field19, field20);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21> InsertValuesStep21<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21> insertInto(
			Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10,
			Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15,
			Field<T16> field16, Field<T17> field17, Field<T18> field18, Field<T19> field19, Field<T20> field20,
			Field<T21> field21) {
		return delegateContext.insertInto(into, field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12, field13, field14, field15, field16, field17, field18, field19, field20,
				field21);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22> InsertValuesStep22<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22> insertInto(
			Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10,
			Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15,
			Field<T16> field16, Field<T17> field17, Field<T18> field18, Field<T19> field19, Field<T20> field20,
			Field<T21> field21, Field<T22> field22) {
		return delegateContext.insertInto(into, field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12, field13, field14, field15, field16, field17, field18, field19, field20,
				field21, field22);
	}

	public <R extends Record> InsertValuesStepN<R> insertInto(Table<R> into, Field<?>... fields) {
		return delegateContext.insertInto(into, fields);
	}

	public <R extends Record> InsertValuesStepN<R> insertInto(Table<R> into, Collection<? extends Field<?>> fields) {
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

	public <R extends Record, T1> MergeKeyStep1<R, T1> mergeInto(Table<R> table, Field<T1> field1) {
		return delegateContext.mergeInto(table, field1);
	}

	public <R extends Record, T1, T2> MergeKeyStep2<R, T1, T2> mergeInto(Table<R> table, Field<T1> field1,
			Field<T2> field2) {
		return delegateContext.mergeInto(table, field1, field2);
	}

	public <R extends Record, T1, T2, T3> MergeKeyStep3<R, T1, T2, T3> mergeInto(Table<R> table, Field<T1> field1,
			Field<T2> field2, Field<T3> field3) {
		return delegateContext.mergeInto(table, field1, field2, field3);
	}

	public <R extends Record, T1, T2, T3, T4> MergeKeyStep4<R, T1, T2, T3, T4> mergeInto(Table<R> table,
			Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4) {
		return delegateContext.mergeInto(table, field1, field2, field3, field4);
	}

	public <R extends Record, T1, T2, T3, T4, T5> MergeKeyStep5<R, T1, T2, T3, T4, T5> mergeInto(Table<R> table,
			Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5) {
		return delegateContext.mergeInto(table, field1, field2, field3, field4, field5);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6> MergeKeyStep6<R, T1, T2, T3, T4, T5, T6> mergeInto(Table<R> table,
			Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6) {
		return delegateContext.mergeInto(table, field1, field2, field3, field4, field5, field6);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7> MergeKeyStep7<R, T1, T2, T3, T4, T5, T6, T7> mergeInto(
			Table<R> table, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7) {
		return delegateContext.mergeInto(table, field1, field2, field3, field4, field5, field6, field7);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8> MergeKeyStep8<R, T1, T2, T3, T4, T5, T6, T7, T8> mergeInto(
			Table<R> table, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8) {
		return delegateContext.mergeInto(table, field1, field2, field3, field4, field5, field6, field7, field8);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9> MergeKeyStep9<R, T1, T2, T3, T4, T5, T6, T7, T8, T9> mergeInto(
			Table<R> table, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9) {
		return delegateContext.mergeInto(table, field1, field2, field3, field4, field5, field6, field7, field8, field9);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> MergeKeyStep10<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> mergeInto(
			Table<R> table, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10) {
		return delegateContext.mergeInto(table, field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> MergeKeyStep11<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> mergeInto(
			Table<R> table, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10,
			Field<T11> field11) {
		return delegateContext.mergeInto(table, field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> MergeKeyStep12<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> mergeInto(
			Table<R> table, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10,
			Field<T11> field11, Field<T12> field12) {
		return delegateContext.mergeInto(table, field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> MergeKeyStep13<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> mergeInto(
			Table<R> table, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10,
			Field<T11> field11, Field<T12> field12, Field<T13> field13) {
		return delegateContext.mergeInto(table, field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12, field13);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> MergeKeyStep14<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> mergeInto(
			Table<R> table, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10,
			Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14) {
		return delegateContext.mergeInto(table, field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12, field13, field14);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> MergeKeyStep15<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> mergeInto(
			Table<R> table, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10,
			Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15) {
		return delegateContext.mergeInto(table, field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12, field13, field14, field15);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> MergeKeyStep16<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> mergeInto(
			Table<R> table, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10,
			Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15,
			Field<T16> field16) {
		return delegateContext.mergeInto(table, field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12, field13, field14, field15, field16);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> MergeKeyStep17<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> mergeInto(
			Table<R> table, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10,
			Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15,
			Field<T16> field16, Field<T17> field17) {
		return delegateContext.mergeInto(table, field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12, field13, field14, field15, field16, field17);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18> MergeKeyStep18<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18> mergeInto(
			Table<R> table, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10,
			Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15,
			Field<T16> field16, Field<T17> field17, Field<T18> field18) {
		return delegateContext.mergeInto(table, field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12, field13, field14, field15, field16, field17, field18);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19> MergeKeyStep19<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19> mergeInto(
			Table<R> table, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10,
			Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15,
			Field<T16> field16, Field<T17> field17, Field<T18> field18, Field<T19> field19) {
		return delegateContext.mergeInto(table, field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12, field13, field14, field15, field16, field17, field18, field19);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20> MergeKeyStep20<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20> mergeInto(
			Table<R> table, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10,
			Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15,
			Field<T16> field16, Field<T17> field17, Field<T18> field18, Field<T19> field19, Field<T20> field20) {
		return delegateContext.mergeInto(table, field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12, field13, field14, field15, field16, field17, field18, field19, field20);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21> MergeKeyStep21<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21> mergeInto(
			Table<R> table, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10,
			Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15,
			Field<T16> field16, Field<T17> field17, Field<T18> field18, Field<T19> field19, Field<T20> field20,
			Field<T21> field21) {
		return delegateContext.mergeInto(table, field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12, field13, field14, field15, field16, field17, field18, field19, field20,
				field21);
	}

	public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22> MergeKeyStep22<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22> mergeInto(
			Table<R> table, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5,
			Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10,
			Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15,
			Field<T16> field16, Field<T17> field17, Field<T18> field18, Field<T19> field19, Field<T20> field20,
			Field<T21> field21, Field<T22> field22) {
		return delegateContext.mergeInto(table, field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12, field13, field14, field15, field16, field17, field18, field19, field20,
				field21, field22);
	}

	public <R extends Record> MergeKeyStepN<R> mergeInto(Table<R> table, Field<?>... fields) {
		return delegateContext.mergeInto(table, fields);
	}

	public <R extends Record> MergeKeyStepN<R> mergeInto(Table<R> table, Collection<? extends Field<?>> fields) {
		return delegateContext.mergeInto(table, fields);
	}

	public <R extends Record> DeleteQuery<R> deleteQuery(Table<R> table) {
		return delegateContext.deleteQuery(table);
	}

	public <R extends Record> DeleteWhereStep<R> deleteFrom(Table<R> table) {
		return delegateContext.deleteFrom(table);
	}

	public <R extends Record> DeleteWhereStep<R> delete(Table<R> table) {
		return delegateContext.delete(table);
	}

	public Batch batch(Query... queries) {
		return delegateContext.batch(queries);
	}

	public Batch batch(String... queries) {
		return delegateContext.batch(queries);
	}

	public Batch batch(Collection<? extends Query> queries) {
		return delegateContext.batch(queries);
	}

	public BatchBindStep batch(Query query) {
		return delegateContext.batch(query);
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

	public Batch batchStore(UpdatableRecord<?>... records) {
		return delegateContext.batchStore(records);
	}

	public Batch batchStore(Collection<? extends UpdatableRecord<?>> records) {
		return delegateContext.batchStore(records);
	}

	public Batch batchInsert(TableRecord<?>... records) {
		return delegateContext.batchInsert(records);
	}

	public Batch batchInsert(Collection<? extends TableRecord<?>> records) {
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

	public CreateTableAsStep<Record> createTable(String table) {
		return delegateContext.createTable(table);
	}

	public CreateTableAsStep<Record> createTable(Name table) {
		return delegateContext.createTable(table);
	}

	public CreateTableAsStep<Record> createTable(Table<?> table) {
		return delegateContext.createTable(table);
	}

	public CreateTableAsStep<Record> createTemporaryTable(String table) {
		return delegateContext.createTemporaryTable(table);
	}

	public CreateTableAsStep<Record> createTemporaryTable(Name table) {
		return delegateContext.createTemporaryTable(table);
	}

	public CreateTableAsStep<Record> createTemporaryTable(Table<?> table) {
		return delegateContext.createTemporaryTable(table);
	}

	public CreateTableAsStep<Record> createGlobalTemporaryTable(String table) {
		return delegateContext.createGlobalTemporaryTable(table);
	}

	public CreateTableAsStep<Record> createGlobalTemporaryTable(Name table) {
		return delegateContext.createGlobalTemporaryTable(table);
	}

	public CreateTableAsStep<Record> createGlobalTemporaryTable(Table<?> table) {
		return delegateContext.createGlobalTemporaryTable(table);
	}

	public CreateViewAsStep<Record> createView(String view, String... fields) {
		return delegateContext.createView(view, fields);
	}

	public CreateViewAsStep<Record> createView(Name view, Name... fields) {
		return delegateContext.createView(view, fields);
	}

	public CreateViewAsStep<Record> createView(Table<?> view, Field<?>... fields) {
		return delegateContext.createView(view, fields);
	}

	public CreateIndexStep createIndex(String index) {
		return delegateContext.createIndex(index);
	}

	public CreateIndexStep createIndex(Name index) {
		return delegateContext.createIndex(index);
	}

	public CreateSequenceFinalStep createSequence(String sequence) {
		return delegateContext.createSequence(sequence);
	}

	public CreateSequenceFinalStep createSequence(Name sequence) {
		return delegateContext.createSequence(sequence);
	}

	public CreateSequenceFinalStep createSequence(Sequence<?> sequence) {
		return delegateContext.createSequence(sequence);
	}

	public AlterSequenceRestartStep<BigInteger> alterSequence(String sequence) {
		return delegateContext.alterSequence(sequence);
	}

	public AlterSequenceRestartStep<BigInteger> alterSequence(Name sequence) {
		return delegateContext.alterSequence(sequence);
	}

	public <T extends Number> AlterSequenceRestartStep<T> alterSequence(Sequence<T> sequence) {
		return delegateContext.alterSequence(sequence);
	}

	public AlterTableStep alterTable(String table) {
		return delegateContext.alterTable(table);
	}

	public AlterTableStep alterTable(Name table) {
		return delegateContext.alterTable(table);
	}

	public AlterTableStep alterTable(Table<?> table) {
		return delegateContext.alterTable(table);
	}

	public DropViewFinalStep dropView(String view) {
		return delegateContext.dropView(view);
	}

	public DropViewFinalStep dropView(Name view) {
		return delegateContext.dropView(view);
	}

	public DropViewFinalStep dropView(Table<?> view) {
		return delegateContext.dropView(view);
	}

	public DropViewFinalStep dropViewIfExists(String view) {
		return delegateContext.dropViewIfExists(view);
	}

	public DropViewFinalStep dropViewIfExists(Name view) {
		return delegateContext.dropViewIfExists(view);
	}

	public DropViewFinalStep dropViewIfExists(Table<?> view) {
		return delegateContext.dropViewIfExists(view);
	}

	public DropTableStep dropTable(String table) {
		return delegateContext.dropTable(table);
	}

	public DropTableStep dropTable(Name table) {
		return delegateContext.dropTable(table);
	}

	public DropTableStep dropTable(Table<?> table) {
		return delegateContext.dropTable(table);
	}

	public DropTableStep dropTableIfExists(String table) {
		return delegateContext.dropTableIfExists(table);
	}

	public DropTableStep dropTableIfExists(Name table) {
		return delegateContext.dropTableIfExists(table);
	}

	public DropTableStep dropTableIfExists(Table<?> table) {
		return delegateContext.dropTableIfExists(table);
	}

	public DropIndexOnStep dropIndex(String index) {
		return delegateContext.dropIndex(index);
	}

	public DropIndexOnStep dropIndex(Name index) {
		return delegateContext.dropIndex(index);
	}

	public DropIndexOnStep dropIndexIfExists(String index) {
		return delegateContext.dropIndexIfExists(index);
	}

	public DropIndexOnStep dropIndexIfExists(Name index) {
		return delegateContext.dropIndexIfExists(index);
	}

	public DropSequenceFinalStep dropSequence(String sequence) {
		return delegateContext.dropSequence(sequence);
	}

	public DropSequenceFinalStep dropSequence(Name sequence) {
		return delegateContext.dropSequence(sequence);
	}

	public DropSequenceFinalStep dropSequence(Sequence<?> sequence) {
		return delegateContext.dropSequence(sequence);
	}

	public DropSequenceFinalStep dropSequenceIfExists(String sequence) {
		return delegateContext.dropSequenceIfExists(sequence);
	}

	public DropSequenceFinalStep dropSequenceIfExists(Name sequence) {
		return delegateContext.dropSequenceIfExists(sequence);
	}

	public DropSequenceFinalStep dropSequenceIfExists(Sequence<?> sequence) {
		return delegateContext.dropSequenceIfExists(sequence);
	}

	public TruncateIdentityStep<Record> truncate(String table) {
		return delegateContext.truncate(table);
	}

	public TruncateIdentityStep<Record> truncate(Name table) {
		return delegateContext.truncate(table);
	}

	public <R extends Record> TruncateIdentityStep<R> truncate(Table<R> table) {
		return delegateContext.truncate(table);
	}

	public BigInteger lastID() throws DataAccessException {
		return delegateContext.lastID();
	}

	public BigInteger nextval(String sequence) throws DataAccessException {
		return delegateContext.nextval(sequence);
	}

	public <T extends Number> T nextval(Sequence<T> sequence) throws DataAccessException {
		return delegateContext.nextval(sequence);
	}

	public BigInteger currval(String sequence) throws DataAccessException {
		return delegateContext.currval(sequence);
	}

	public <T extends Number> T currval(Sequence<T> sequence) throws DataAccessException {
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

	public Record newRecord(Field<?>... fields) {
		return delegateContext.newRecord(fields);
	}

	public <T1> Record1<T1> newRecord(Field<T1> field1) {
		return delegateContext.newRecord(field1);
	}

	public <T1, T2> Record2<T1, T2> newRecord(Field<T1> field1, Field<T2> field2) {
		return delegateContext.newRecord(field1, field2);
	}

	public <T1, T2, T3> Record3<T1, T2, T3> newRecord(Field<T1> field1, Field<T2> field2, Field<T3> field3) {
		return delegateContext.newRecord(field1, field2, field3);
	}

	public <T1, T2, T3, T4> Record4<T1, T2, T3, T4> newRecord(Field<T1> field1, Field<T2> field2, Field<T3> field3,
			Field<T4> field4) {
		return delegateContext.newRecord(field1, field2, field3, field4);
	}

	public <T1, T2, T3, T4, T5> Record5<T1, T2, T3, T4, T5> newRecord(Field<T1> field1, Field<T2> field2,
			Field<T3> field3, Field<T4> field4, Field<T5> field5) {
		return delegateContext.newRecord(field1, field2, field3, field4, field5);
	}

	public <T1, T2, T3, T4, T5, T6> Record6<T1, T2, T3, T4, T5, T6> newRecord(Field<T1> field1, Field<T2> field2,
			Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6) {
		return delegateContext.newRecord(field1, field2, field3, field4, field5, field6);
	}

	public <T1, T2, T3, T4, T5, T6, T7> Record7<T1, T2, T3, T4, T5, T6, T7> newRecord(Field<T1> field1,
			Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7) {
		return delegateContext.newRecord(field1, field2, field3, field4, field5, field6, field7);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8> Record8<T1, T2, T3, T4, T5, T6, T7, T8> newRecord(Field<T1> field1,
			Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7,
			Field<T8> field8) {
		return delegateContext.newRecord(field1, field2, field3, field4, field5, field6, field7, field8);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9> Record9<T1, T2, T3, T4, T5, T6, T7, T8, T9> newRecord(Field<T1> field1,
			Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7,
			Field<T8> field8, Field<T9> field9) {
		return delegateContext.newRecord(field1, field2, field3, field4, field5, field6, field7, field8, field9);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> Record10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> newRecord(
			Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10) {
		return delegateContext.newRecord(field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> Record11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> newRecord(
			Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11) {
		return delegateContext.newRecord(field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> Record12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> newRecord(
			Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11,
			Field<T12> field12) {
		return delegateContext.newRecord(field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> Record13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> newRecord(
			Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11,
			Field<T12> field12, Field<T13> field13) {
		return delegateContext.newRecord(field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12, field13);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> Record14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> newRecord(
			Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11,
			Field<T12> field12, Field<T13> field13, Field<T14> field14) {
		return delegateContext.newRecord(field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12, field13, field14);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> Record15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> newRecord(
			Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11,
			Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15) {
		return delegateContext.newRecord(field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12, field13, field14, field15);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> Record16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> newRecord(
			Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11,
			Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15, Field<T16> field16) {
		return delegateContext.newRecord(field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12, field13, field14, field15, field16);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> Record17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> newRecord(
			Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11,
			Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15, Field<T16> field16,
			Field<T17> field17) {
		return delegateContext.newRecord(field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12, field13, field14, field15, field16, field17);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18> Record18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18> newRecord(
			Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11,
			Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15, Field<T16> field16,
			Field<T17> field17, Field<T18> field18) {
		return delegateContext.newRecord(field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12, field13, field14, field15, field16, field17, field18);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19> Record19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19> newRecord(
			Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11,
			Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15, Field<T16> field16,
			Field<T17> field17, Field<T18> field18, Field<T19> field19) {
		return delegateContext.newRecord(field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12, field13, field14, field15, field16, field17, field18, field19);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20> Record20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20> newRecord(
			Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11,
			Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15, Field<T16> field16,
			Field<T17> field17, Field<T18> field18, Field<T19> field19, Field<T20> field20) {
		return delegateContext.newRecord(field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12, field13, field14, field15, field16, field17, field18, field19, field20);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21> Record21<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21> newRecord(
			Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11,
			Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15, Field<T16> field16,
			Field<T17> field17, Field<T18> field18, Field<T19> field19, Field<T20> field20, Field<T21> field21) {
		return delegateContext.newRecord(field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12, field13, field14, field15, field16, field17, field18, field19, field20,
				field21);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22> Record22<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22> newRecord(
			Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11,
			Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15, Field<T16> field16,
			Field<T17> field17, Field<T18> field18, Field<T19> field19, Field<T20> field20, Field<T21> field21,
			Field<T22> field22) {
		return delegateContext.newRecord(field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12, field13, field14, field15, field16, field17, field18, field19, field20,
				field21, field22);
	}

	public <R extends Record> Result<R> newResult(Table<R> table) {
		return delegateContext.newResult(table);
	}

	public Result<Record> newResult(Field<?>... fields) {
		return delegateContext.newResult(fields);
	}

	public <T1> Result<Record1<T1>> newResult(Field<T1> field1) {
		return delegateContext.newResult(field1);
	}

	public <T1, T2> Result<Record2<T1, T2>> newResult(Field<T1> field1, Field<T2> field2) {
		return delegateContext.newResult(field1, field2);
	}

	public <T1, T2, T3> Result<Record3<T1, T2, T3>> newResult(Field<T1> field1, Field<T2> field2, Field<T3> field3) {
		return delegateContext.newResult(field1, field2, field3);
	}

	public <T1, T2, T3, T4> Result<Record4<T1, T2, T3, T4>> newResult(Field<T1> field1, Field<T2> field2,
			Field<T3> field3, Field<T4> field4) {
		return delegateContext.newResult(field1, field2, field3, field4);
	}

	public <T1, T2, T3, T4, T5> Result<Record5<T1, T2, T3, T4, T5>> newResult(Field<T1> field1, Field<T2> field2,
			Field<T3> field3, Field<T4> field4, Field<T5> field5) {
		return delegateContext.newResult(field1, field2, field3, field4, field5);
	}

	public <T1, T2, T3, T4, T5, T6> Result<Record6<T1, T2, T3, T4, T5, T6>> newResult(Field<T1> field1,
			Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6) {
		return delegateContext.newResult(field1, field2, field3, field4, field5, field6);
	}

	public <T1, T2, T3, T4, T5, T6, T7> Result<Record7<T1, T2, T3, T4, T5, T6, T7>> newResult(Field<T1> field1,
			Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7) {
		return delegateContext.newResult(field1, field2, field3, field4, field5, field6, field7);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8> Result<Record8<T1, T2, T3, T4, T5, T6, T7, T8>> newResult(Field<T1> field1,
			Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7,
			Field<T8> field8) {
		return delegateContext.newResult(field1, field2, field3, field4, field5, field6, field7, field8);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9> Result<Record9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> newResult(
			Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9) {
		return delegateContext.newResult(field1, field2, field3, field4, field5, field6, field7, field8, field9);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> Result<Record10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>> newResult(
			Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10) {
		return delegateContext.newResult(field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> Result<Record11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>> newResult(
			Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11) {
		return delegateContext.newResult(field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> Result<Record12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>> newResult(
			Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11,
			Field<T12> field12) {
		return delegateContext.newResult(field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> Result<Record13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>> newResult(
			Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11,
			Field<T12> field12, Field<T13> field13) {
		return delegateContext.newResult(field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12, field13);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> Result<Record14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>> newResult(
			Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11,
			Field<T12> field12, Field<T13> field13, Field<T14> field14) {
		return delegateContext.newResult(field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12, field13, field14);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> Result<Record15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>> newResult(
			Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11,
			Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15) {
		return delegateContext.newResult(field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12, field13, field14, field15);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> Result<Record16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>> newResult(
			Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11,
			Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15, Field<T16> field16) {
		return delegateContext.newResult(field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12, field13, field14, field15, field16);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> Result<Record17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>> newResult(
			Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11,
			Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15, Field<T16> field16,
			Field<T17> field17) {
		return delegateContext.newResult(field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12, field13, field14, field15, field16, field17);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18> Result<Record18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>> newResult(
			Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11,
			Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15, Field<T16> field16,
			Field<T17> field17, Field<T18> field18) {
		return delegateContext.newResult(field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12, field13, field14, field15, field16, field17, field18);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19> Result<Record19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>> newResult(
			Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11,
			Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15, Field<T16> field16,
			Field<T17> field17, Field<T18> field18, Field<T19> field19) {
		return delegateContext.newResult(field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12, field13, field14, field15, field16, field17, field18, field19);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20> Result<Record20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>> newResult(
			Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11,
			Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15, Field<T16> field16,
			Field<T17> field17, Field<T18> field18, Field<T19> field19, Field<T20> field20) {
		return delegateContext.newResult(field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12, field13, field14, field15, field16, field17, field18, field19, field20);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21> Result<Record21<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21>> newResult(
			Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11,
			Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15, Field<T16> field16,
			Field<T17> field17, Field<T18> field18, Field<T19> field19, Field<T20> field20, Field<T21> field21) {
		return delegateContext.newResult(field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12, field13, field14, field15, field16, field17, field18, field19, field20,
				field21);
	}

	public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22> Result<Record22<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22>> newResult(
			Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6,
			Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11,
			Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15, Field<T16> field16,
			Field<T17> field17, Field<T18> field18, Field<T19> field19, Field<T20> field20, Field<T21> field21,
			Field<T22> field22) {
		return delegateContext.newResult(field1, field2, field3, field4, field5, field6, field7, field8, field9,
				field10, field11, field12, field13, field14, field15, field16, field17, field18, field19, field20,
				field21, field22);
	}

	public <R extends Record> Result<R> fetch(ResultQuery<R> query) throws DataAccessException {
		return delegateContext.fetch(query);
	}

	public <R extends Record> Cursor<R> fetchLazy(ResultQuery<R> query) throws DataAccessException {
		return delegateContext.fetchLazy(query);
	}

	public <R extends Record> Stream<R> fetchStream(ResultQuery<R> query) throws DataAccessException {
		return delegateContext.fetchStream(query);
	}

	public <R extends Record> Results fetchMany(ResultQuery<R> query) throws DataAccessException {
		return delegateContext.fetchMany(query);
	}

	public <R extends Record> R fetchOne(ResultQuery<R> query) throws DataAccessException, TooManyRowsException {
		return delegateContext.fetchOne(query);
	}

	public <R extends Record> Optional<R> fetchOptional(ResultQuery<R> query)
			throws DataAccessException, TooManyRowsException {
		return delegateContext.fetchOptional(query);
	}

	public <T, R extends Record1<T>> T fetchValue(ResultQuery<R> query)
			throws DataAccessException, TooManyRowsException, InvalidResultException {
		return delegateContext.fetchValue(query);
	}

	public <T> T fetchValue(TableField<?, T> field)
			throws DataAccessException, TooManyRowsException, InvalidResultException {
		return delegateContext.fetchValue(field);
	}

	public <T, R extends Record1<T>> Optional<T> fetchOptionalValue(ResultQuery<R> query)
			throws DataAccessException, TooManyRowsException, InvalidResultException {
		return delegateContext.fetchOptionalValue(query);
	}

	public <T> Optional<T> fetchOptionalValue(TableField<?, T> field)
			throws DataAccessException, TooManyRowsException, InvalidResultException {
		return delegateContext.fetchOptionalValue(field);
	}

	public <T, R extends Record1<T>> List<T> fetchValues(ResultQuery<R> query) throws DataAccessException {
		return delegateContext.fetchValues(query);
	}

	public <T> List<T> fetchValues(TableField<?, T> field) throws DataAccessException {
		return delegateContext.fetchValues(field);
	}

	public int fetchCount(Select<?> query) throws DataAccessException {
		return delegateContext.fetchCount(query);
	}

	public int fetchCount(Table<?> table) throws DataAccessException {
		return delegateContext.fetchCount(table);
	}

	public int fetchCount(Table<?> table, Condition condition) throws DataAccessException {
		return delegateContext.fetchCount(table, condition);
	}

	public boolean fetchExists(Select<?> query) throws DataAccessException {
		return delegateContext.fetchExists(query);
	}

	public boolean fetchExists(Table<?> table) throws DataAccessException {
		return delegateContext.fetchExists(table);
	}

	public boolean fetchExists(Table<?> table, Condition condition) throws DataAccessException {
		return delegateContext.fetchExists(table, condition);
	}

	public int execute(Query query) throws DataAccessException {
		return delegateContext.execute(query);
	}

	public <R extends Record> Result<R> fetch(Table<R> table) throws DataAccessException {
		return delegateContext.fetch(table);
	}

	public <R extends Record> Result<R> fetch(Table<R> table, Condition condition) throws DataAccessException {
		return delegateContext.fetch(table, condition);
	}

	public <R extends Record> R fetchOne(Table<R> table) throws DataAccessException, TooManyRowsException {
		return delegateContext.fetchOne(table);
	}

	public <R extends Record> R fetchOne(Table<R> table, Condition condition)
			throws DataAccessException, TooManyRowsException {
		return delegateContext.fetchOne(table, condition);
	}

	public <R extends Record> Optional<R> fetchOptional(Table<R> table)
			throws DataAccessException, TooManyRowsException {
		return delegateContext.fetchOptional(table);
	}

	public <R extends Record> Optional<R> fetchOptional(Table<R> table, Condition condition)
			throws DataAccessException, TooManyRowsException {
		return delegateContext.fetchOptional(table, condition);
	}

	public <R extends Record> R fetchAny(Table<R> table) throws DataAccessException {
		return delegateContext.fetchAny(table);
	}

	public <R extends Record> R fetchAny(Table<R> table, Condition condition) throws DataAccessException {
		return delegateContext.fetchAny(table, condition);
	}

	public <R extends Record> Cursor<R> fetchLazy(Table<R> table) throws DataAccessException {
		return delegateContext.fetchLazy(table);
	}

	public <R extends Record> Cursor<R> fetchLazy(Table<R> table, Condition condition) throws DataAccessException {
		return delegateContext.fetchLazy(table, condition);
	}

	public <R extends Record> Stream<R> fetchStream(Table<R> table) throws DataAccessException {
		return delegateContext.fetchStream(table);
	}

	public <R extends Record> Stream<R> fetchStream(Table<R> table, Condition condition) throws DataAccessException {
		return delegateContext.fetchStream(table, condition);
	}

	public <R extends TableRecord<R>> int executeInsert(R record) throws DataAccessException {
		return delegateContext.executeInsert(record);
	}

	public <R extends UpdatableRecord<R>> int executeUpdate(R record) throws DataAccessException {
		return delegateContext.executeUpdate(record);
	}

	public <R extends TableRecord<R>, T> int executeUpdate(R record, Condition condition) throws DataAccessException {
		return delegateContext.executeUpdate(record, condition);
	}

	public <R extends UpdatableRecord<R>> int executeDelete(R record) throws DataAccessException {
		return delegateContext.executeDelete(record);
	}

	public <R extends TableRecord<R>, T> int executeDelete(R record, Condition condition) throws DataAccessException {
		return delegateContext.executeDelete(record, condition);
	}

	
}
