package ch.vd.unireg.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

import org.apache.commons.lang3.StringUtils;

import ch.vd.unireg.stats.ServiceTracing;

public class TracingStatement implements Statement {

	private final Statement target;
	protected final ServiceTracing tracing;

	public TracingStatement(Statement target, ServiceTracing tracing) {
		this.target = target;
		this.tracing = tracing;
	}

	@Override
	public ResultSet executeQuery(final String sql) throws SQLException {
		long start = tracing.start();
		try {
			return target.executeQuery(sql);
		}
		finally {
			tracing.end(start, "executeQuery", () -> cap(sql));
		}
	}

	@Override
	public int executeUpdate(final String sql) throws SQLException {
		long start = tracing.start();
		try {
			return target.executeUpdate(sql);
		}
		finally {
			tracing.end(start, "executeUpdate", () -> cap(sql));
		}
	}

	@Override
	public void close() throws SQLException {
		target.close();
	}

	@Override
	public int getMaxFieldSize() throws SQLException {
		return target.getMaxFieldSize();
	}

	@Override
	public void setMaxFieldSize(int max) throws SQLException {
		target.setMaxFieldSize(max);
	}

	@Override
	public int getMaxRows() throws SQLException {
		return target.getMaxRows();
	}

	@Override
	public void setMaxRows(int max) throws SQLException {
		target.setMaxRows(max);
	}

	@Override
	public void setEscapeProcessing(boolean enable) throws SQLException {
		target.setEscapeProcessing(enable);
	}

	@Override
	public int getQueryTimeout() throws SQLException {
		return target.getQueryTimeout();
	}

	@Override
	public void setQueryTimeout(int seconds) throws SQLException {
		target.setQueryTimeout(seconds);
	}

	@Override
	public void cancel() throws SQLException {
		target.cancel();
	}

	@Override
	public SQLWarning getWarnings() throws SQLException {
		return target.getWarnings();
	}

	@Override
	public void clearWarnings() throws SQLException {
		target.clearWarnings();
	}

	@Override
	public void setCursorName(String name) throws SQLException {
		target.setCursorName(name);
	}

	@Override
	public boolean execute(final String sql) throws SQLException {
		long start = tracing.start();
		try {
			return target.execute(sql);
		}
		finally {
			tracing.end(start, "execute", () -> cap(sql));
		}
	}

	@Override
	public ResultSet getResultSet() throws SQLException {
		return target.getResultSet();
	}

	@Override
	public int getUpdateCount() throws SQLException {
		return target.getUpdateCount();
	}

	@Override
	public boolean getMoreResults() throws SQLException {
		return target.getMoreResults();
	}

	@Override
	public void setFetchDirection(int direction) throws SQLException {
		target.setFetchDirection(direction);
	}

	@Override
	public int getFetchDirection() throws SQLException {
		return target.getFetchDirection();
	}

	@Override
	public void setFetchSize(int rows) throws SQLException {
		target.setFetchSize(rows);
	}

	@Override
	public int getFetchSize() throws SQLException {
		return target.getFetchSize();
	}

	@Override
	public int getResultSetConcurrency() throws SQLException {
		return target.getResultSetConcurrency();
	}

	@Override
	public int getResultSetType() throws SQLException {
		return target.getResultSetType();
	}

	@Override
	public void addBatch(String sql) throws SQLException {
		target.addBatch(sql);
	}

	@Override
	public void clearBatch() throws SQLException {
		target.clearBatch();
	}

	@Override
	public int[] executeBatch() throws SQLException {
		long start = tracing.start();
		try {
			return target.executeBatch();
		}
		finally {
			tracing.end(start, "executeBatch", null);
		}
	}

	@Override
	public Connection getConnection() throws SQLException {
		return target.getConnection();
	}

	@Override
	public boolean getMoreResults(int current) throws SQLException {
		return target.getMoreResults(current);
	}

	@Override
	public ResultSet getGeneratedKeys() throws SQLException {
		return target.getGeneratedKeys();
	}

	@Override
	public int executeUpdate(final String sql, int autoGeneratedKeys) throws SQLException {
		long start = tracing.start();
		try {
			return target.executeUpdate(sql, autoGeneratedKeys);
		}
		finally {
			tracing.end(start, "executeUpdate", () -> cap(sql));
		}
	}

	@Override
	public int executeUpdate(final String sql, int[] columnIndexes) throws SQLException {
		long start = tracing.start();
		try {
			return target.executeUpdate(sql, columnIndexes);
		}
		finally {
			tracing.end(start, "executeUpdate", () -> cap(sql));
		}
	}

	@Override
	public int executeUpdate(final String sql, String[] columnNames) throws SQLException {
		long start = tracing.start();
		try {
			return target.executeUpdate(sql, columnNames);
		}
		finally {
			tracing.end(start, "executeUpdate", () -> cap(sql));
		}
	}

	@Override
	public boolean execute(final String sql, int autoGeneratedKeys) throws SQLException {
		long start = tracing.start();
		try {
			return target.execute(sql, autoGeneratedKeys);
		}
		finally {
			tracing.end(start, "execute", () -> cap(sql));
		}
	}

	@Override
	public boolean execute(final String sql, int[] columnIndexes) throws SQLException {
		long start = tracing.start();
		try {
			return target.execute(sql, columnIndexes);
		}
		finally {
			tracing.end(start, "execute", () -> cap(sql));
		}
	}

	@Override
	public boolean execute(final String sql, String[] columnNames) throws SQLException {
		long start = tracing.start();
		try {
			return target.execute(sql, columnNames);
		}
		finally {
			tracing.end(start, "execute", () -> cap(sql));
		}
	}

	/**
	 * Retourne une version limitée aux 100 premiers caractères de la requête spécifiée, autrement le log devient illisible
	 *
	 * @param sql une requête SQL
	 * @return les 100 premiers caractères de la requête
	 */
	protected static String cap(String sql) {
		return StringUtils.abbreviate(sql, 100);
	}

	@Override
	public int getResultSetHoldability() throws SQLException {
		return target.getResultSetHoldability();
	}

	@Override
	public boolean isClosed() throws SQLException {
		return target.isClosed();
	}

	@Override
	public void setPoolable(boolean poolable) throws SQLException {
		target.setPoolable(poolable);
	}

	@Override
	public boolean isPoolable() throws SQLException {
		return target.isPoolable();
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		return target.unwrap(iface);
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return target.isWrapperFor(iface);
	}

	@Override
	public void closeOnCompletion() throws SQLException {
		target.closeOnCompletion();
	}

	@Override
	public boolean isCloseOnCompletion() throws SQLException {
		return target.isCloseOnCompletion();
	}
}