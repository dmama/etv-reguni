package ch.vd.uniregctb.database;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;

import ch.vd.uniregctb.interfaces.service.ServiceTracing;

/**
 * Connection de débugging qui stocke la callstack de l'appelant à l'ouverture.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class TracingConnection implements Connection {

	private final ServiceTracing tracing;
	private final Connection target;

	public TracingConnection(Connection target, ServiceTracing tracing) {
		this.target = target;
		this.tracing = tracing;
	}

	public void clearWarnings() throws SQLException {
		target.clearWarnings();
	}

	public void close() throws SQLException {
		target.close();
	}

	public void commit() throws SQLException {
		target.commit();
	}

	public Statement createStatement() throws SQLException {
		return new TracingStatement(target.createStatement(), tracing);
	}

	public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
		return new TracingStatement(target.createStatement(resultSetType, resultSetConcurrency), tracing);
	}

	public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
		return new TracingStatement(target.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability), tracing);
	}

	public boolean getAutoCommit() throws SQLException {
		return target.getAutoCommit();
	}

	public String getCatalog() throws SQLException {
		return target.getCatalog();
	}

	public int getHoldability() throws SQLException {
		return target.getHoldability();
	}

	public DatabaseMetaData getMetaData() throws SQLException {
		return target.getMetaData();
	}

	public int getTransactionIsolation() throws SQLException {
		return target.getTransactionIsolation();
	}

	public Map<String, Class<?>> getTypeMap() throws SQLException {
		return target.getTypeMap();
	}

	public SQLWarning getWarnings() throws SQLException {
		return target.getWarnings();
	}

	public boolean isClosed() throws SQLException {
		return target.isClosed();
	}

	public boolean isReadOnly() throws SQLException {
		return target.isReadOnly();
	}

	public String nativeSQL(String sql) throws SQLException {
		return target.nativeSQL(sql);
	}

	public CallableStatement prepareCall(String sql) throws SQLException {
		return new TracingCallableStatement(target.prepareCall(sql), sql, tracing);
	}

	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
		return new TracingCallableStatement(target.prepareCall(sql, resultSetType, resultSetConcurrency), sql, tracing);
	}

	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
		return new TracingCallableStatement(target.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability), sql, tracing);
	}

	public PreparedStatement prepareStatement(String sql) throws SQLException {
		return new TracingPreparedStatement(target.prepareStatement(sql), sql, tracing);
	}

	public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
		return new TracingPreparedStatement(target.prepareStatement(sql, autoGeneratedKeys), sql, tracing);
	}

	public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
		return new TracingPreparedStatement(target.prepareStatement(sql, columnIndexes), sql, tracing);
	}

	public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
		return new TracingPreparedStatement(target.prepareStatement(sql, columnNames), sql, tracing);
	}

	@Override
	public Clob createClob() throws SQLException {
		return target.createClob();
	}

	@Override
	public Blob createBlob() throws SQLException {
		return target.createBlob();
	}

	@Override
	public NClob createNClob() throws SQLException {
		return target.createNClob();
	}

	@Override
	public SQLXML createSQLXML() throws SQLException {
		return target.createSQLXML();
	}

	@Override
	public boolean isValid(int timeout) throws SQLException {
		return target.isValid(timeout);
	}

	@Override
	public void setClientInfo(String name, String value) throws SQLClientInfoException {
		target.setClientInfo(name, value);
	}

	@Override
	public void setClientInfo(Properties properties) throws SQLClientInfoException {
		target.setClientInfo(properties);
	}

	@Override
	public String getClientInfo(String name) throws SQLException {
		return target.getClientInfo(name);
	}

	@Override
	public Properties getClientInfo() throws SQLException {
		return target.getClientInfo();
	}

	@Override
	public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
		return target.createArrayOf(typeName, elements);
	}

	@Override
	public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
		return target.createStruct(typeName, attributes);
	}

	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
		return new TracingPreparedStatement(target.prepareStatement(sql, resultSetType, resultSetConcurrency), sql, tracing);
	}

	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
		return new TracingPreparedStatement(target.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability), sql, tracing);
	}

	public void releaseSavepoint(Savepoint savepoint) throws SQLException {
		target.releaseSavepoint(savepoint);
	}

	public void rollback() throws SQLException {
		target.rollback();
	}

	public void rollback(Savepoint savepoint) throws SQLException {
		target.rollback(savepoint);
	}

	public void setAutoCommit(boolean autoCommit) throws SQLException {
		target.setAutoCommit(autoCommit);
	}

	public void setCatalog(String catalog) throws SQLException {
		target.setCatalog(catalog);
	}

	public void setHoldability(int holdability) throws SQLException {
		target.setHoldability(holdability);
	}

	public void setReadOnly(boolean readOnly) throws SQLException {
		target.setReadOnly(readOnly);
	}

	public Savepoint setSavepoint() throws SQLException {
		return target.setSavepoint();
	}

	public Savepoint setSavepoint(String name) throws SQLException {
		return target.setSavepoint(name);
	}

	public void setTransactionIsolation(int level) throws SQLException {
		target.setTransactionIsolation(level);
	}

	public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
		target.setTypeMap(map);
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		return target.unwrap(iface);
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return target.isWrapperFor(iface);
	}
}