package ch.vd.uniregctb.database;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Date;
import java.sql.Ref;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

import ch.vd.uniregctb.interfaces.service.ServiceTracing;

public class TracingCallableStatement extends TracingPreparedStatement implements CallableStatement {

	private final CallableStatement target;

	public TracingCallableStatement(CallableStatement target, String sql, ServiceTracing tracing) {
		super(target, sql, tracing);
		this.target = target;
	}

	public void registerOutParameter(int parameterIndex, int sqlType) throws SQLException {
		target.registerOutParameter(parameterIndex, sqlType);
	}

	public void registerOutParameter(int parameterIndex, int sqlType, int scale) throws SQLException {
		target.registerOutParameter(parameterIndex, sqlType, scale);
	}

	public boolean wasNull() throws SQLException {
		return target.wasNull();
	}

	public String getString(int parameterIndex) throws SQLException {
		return target.getString(parameterIndex);
	}

	public boolean getBoolean(int parameterIndex) throws SQLException {
		return target.getBoolean(parameterIndex);
	}

	public byte getByte(int parameterIndex) throws SQLException {
		return target.getByte(parameterIndex);
	}

	public short getShort(int parameterIndex) throws SQLException {
		return target.getShort(parameterIndex);
	}

	public int getInt(int parameterIndex) throws SQLException {
		return target.getInt(parameterIndex);
	}

	public long getLong(int parameterIndex) throws SQLException {
		return target.getLong(parameterIndex);
	}

	public float getFloat(int parameterIndex) throws SQLException {
		return target.getFloat(parameterIndex);
	}

	public double getDouble(int parameterIndex) throws SQLException {
		return target.getDouble(parameterIndex);
	}

	public BigDecimal getBigDecimal(int parameterIndex, int scale) throws SQLException {
		//noinspection deprecation
		return target.getBigDecimal(parameterIndex, scale);
	}

	public byte[] getBytes(int parameterIndex) throws SQLException {
		return target.getBytes(parameterIndex);
	}

	public Date getDate(int parameterIndex) throws SQLException {
		return target.getDate(parameterIndex);
	}

	public Time getTime(int parameterIndex) throws SQLException {
		return target.getTime(parameterIndex);
	}

	public Timestamp getTimestamp(int parameterIndex) throws SQLException {
		return target.getTimestamp(parameterIndex);
	}

	public Object getObject(int parameterIndex) throws SQLException {
		return target.getObject(parameterIndex);
	}

	public BigDecimal getBigDecimal(int parameterIndex) throws SQLException {
		return target.getBigDecimal(parameterIndex);
	}

	public Object getObject(int i, Map<String, Class<?>> map) throws SQLException {
		return target.getObject(i, map);
	}

	public Ref getRef(int i) throws SQLException {
		return target.getRef(i);
	}

	public Blob getBlob(int i) throws SQLException {
		return target.getBlob(i);
	}

	public Clob getClob(int i) throws SQLException {
		return target.getClob(i);
	}

	public Array getArray(int i) throws SQLException {
		return target.getArray(i);
	}

	public Date getDate(int parameterIndex, Calendar cal) throws SQLException {
		return target.getDate(parameterIndex, cal);
	}

	public Time getTime(int parameterIndex, Calendar cal) throws SQLException {
		return target.getTime(parameterIndex, cal);
	}

	public Timestamp getTimestamp(int parameterIndex, Calendar cal) throws SQLException {
		return target.getTimestamp(parameterIndex, cal);
	}

	public void registerOutParameter(int paramIndex, int sqlType, String typeName) throws SQLException {
		target.registerOutParameter(paramIndex, sqlType, typeName);
	}

	public void registerOutParameter(String parameterName, int sqlType) throws SQLException {
		target.registerOutParameter(parameterName, sqlType);
	}

	public void registerOutParameter(String parameterName, int sqlType, int scale) throws SQLException {
		target.registerOutParameter(parameterName, sqlType, scale);
	}

	public void registerOutParameter(String parameterName, int sqlType, String typeName) throws SQLException {
		target.registerOutParameter(parameterName, sqlType, typeName);
	}

	public URL getURL(int parameterIndex) throws SQLException {
		return target.getURL(parameterIndex);
	}

	public void setURL(String parameterName, URL val) throws SQLException {
		target.setURL(parameterName, val);
	}

	public void setNull(String parameterName, int sqlType) throws SQLException {
		target.setNull(parameterName, sqlType);
	}

	public void setBoolean(String parameterName, boolean x) throws SQLException {
		target.setBoolean(parameterName, x);
	}

	public void setByte(String parameterName, byte x) throws SQLException {
		target.setByte(parameterName, x);
	}

	public void setShort(String parameterName, short x) throws SQLException {
		target.setShort(parameterName, x);
	}

	public void setInt(String parameterName, int x) throws SQLException {
		target.setInt(parameterName, x);
	}

	public void setLong(String parameterName, long x) throws SQLException {
		target.setLong(parameterName, x);
	}

	public void setFloat(String parameterName, float x) throws SQLException {
		target.setFloat(parameterName, x);
	}

	public void setDouble(String parameterName, double x) throws SQLException {
		target.setDouble(parameterName, x);
	}

	public void setBigDecimal(String parameterName, BigDecimal x) throws SQLException {
		target.setBigDecimal(parameterName, x);
	}

	public void setString(String parameterName, String x) throws SQLException {
		target.setString(parameterName, x);
	}

	public void setBytes(String parameterName, byte[] x) throws SQLException {
		target.setBytes(parameterName, x);
	}

	public void setDate(String parameterName, Date x) throws SQLException {
		target.setDate(parameterName, x);
	}

	public void setTime(String parameterName, Time x) throws SQLException {
		target.setTime(parameterName, x);
	}

	public void setTimestamp(String parameterName, Timestamp x) throws SQLException {
		target.setTimestamp(parameterName, x);
	}

	public void setAsciiStream(String parameterName, InputStream x, int length) throws SQLException {
		target.setAsciiStream(parameterName, x, length);
	}

	public void setBinaryStream(String parameterName, InputStream x, int length) throws SQLException {
		target.setBinaryStream(parameterName, x, length);
	}

	public void setObject(String parameterName, Object x, int targetSqlType, int scale) throws SQLException {
		target.setObject(parameterName, x, targetSqlType, scale);
	}

	public void setObject(String parameterName, Object x, int targetSqlType) throws SQLException {
		target.setObject(parameterName, x, targetSqlType);
	}

	public void setObject(String parameterName, Object x) throws SQLException {
		target.setObject(parameterName, x);
	}

	public void setCharacterStream(String parameterName, Reader reader, int length) throws SQLException {
		target.setCharacterStream(parameterName, reader, length);
	}

	public void setDate(String parameterName, Date x, Calendar cal) throws SQLException {
		target.setDate(parameterName, x, cal);
	}

	public void setTime(String parameterName, Time x, Calendar cal) throws SQLException {
		target.setTime(parameterName, x, cal);
	}

	public void setTimestamp(String parameterName, Timestamp x, Calendar cal) throws SQLException {
		target.setTimestamp(parameterName, x, cal);
	}

	public void setNull(String parameterName, int sqlType, String typeName) throws SQLException {
		target.setNull(parameterName, sqlType, typeName);
	}

	public String getString(String parameterName) throws SQLException {
		return target.getString(parameterName);
	}

	public boolean getBoolean(String parameterName) throws SQLException {
		return target.getBoolean(parameterName);
	}

	public byte getByte(String parameterName) throws SQLException {
		return target.getByte(parameterName);
	}

	public short getShort(String parameterName) throws SQLException {
		return target.getShort(parameterName);
	}

	public int getInt(String parameterName) throws SQLException {
		return target.getInt(parameterName);
	}

	public long getLong(String parameterName) throws SQLException {
		return target.getLong(parameterName);
	}

	public float getFloat(String parameterName) throws SQLException {
		return target.getFloat(parameterName);
	}

	public double getDouble(String parameterName) throws SQLException {
		return target.getDouble(parameterName);
	}

	public byte[] getBytes(String parameterName) throws SQLException {
		return target.getBytes(parameterName);
	}

	public Date getDate(String parameterName) throws SQLException {
		return target.getDate(parameterName);
	}

	public Time getTime(String parameterName) throws SQLException {
		return target.getTime(parameterName);
	}

	public Timestamp getTimestamp(String parameterName) throws SQLException {
		return target.getTimestamp(parameterName);
	}

	public Object getObject(String parameterName) throws SQLException {
		return target.getObject(parameterName);
	}

	public BigDecimal getBigDecimal(String parameterName) throws SQLException {
		return target.getBigDecimal(parameterName);
	}

	public Object getObject(String parameterName, Map<String, Class<?>> map) throws SQLException {
		return target.getObject(parameterName, map);
	}

	public Ref getRef(String parameterName) throws SQLException {
		return target.getRef(parameterName);
	}

	public Blob getBlob(String parameterName) throws SQLException {
		return target.getBlob(parameterName);
	}

	public Clob getClob(String parameterName) throws SQLException {
		return target.getClob(parameterName);
	}

	public Array getArray(String parameterName) throws SQLException {
		return target.getArray(parameterName);
	}

	public Date getDate(String parameterName, Calendar cal) throws SQLException {
		return target.getDate(parameterName, cal);
	}

	public Time getTime(String parameterName, Calendar cal) throws SQLException {
		return target.getTime(parameterName, cal);
	}

	public Timestamp getTimestamp(String parameterName, Calendar cal) throws SQLException {
		return target.getTimestamp(parameterName, cal);
	}

	public URL getURL(String parameterName) throws SQLException {
		return target.getURL(parameterName);
	}
}
