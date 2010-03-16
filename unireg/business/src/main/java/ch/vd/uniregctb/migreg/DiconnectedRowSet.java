package ch.vd.uniregctb.migreg;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.springframework.jdbc.InvalidResultSetAccessException;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSetMetaData;

import ch.vd.registre.base.utils.Assert;

public class DiconnectedRowSet implements SqlRowSet {

	private static final long serialVersionUID = -3051835710683169220L;

	private ArrayList<HashMap<String, Object>> results = new ArrayList<HashMap<String, Object>>();
	private int position = -1;

	public DiconnectedRowSet(ArrayList<HashMap<String, Object>> res) {
		results = res;
	}

	public boolean next() throws InvalidResultSetAccessException {
		position++;
		return position < results.size();
	}

	public int getInt(String columnName) throws InvalidResultSetAccessException {
		Integer i = (Integer)results.get(position).get(columnName);
		if (i != null) {
			return i;
		}
		return -1;
	}

	public long getLong(String columnName) throws InvalidResultSetAccessException {
		Object o = results.get(position).get(columnName);
		if (o instanceof Integer) {
			o = new Long((Integer)o);
		}
		Long l = (Long)o;
		if (l != null) {
			return l;
		}
		return -1;
	}

	public Date getDate(String columnName) throws InvalidResultSetAccessException {
		return (Date)results.get(position).get(columnName);
	}

	public String getString(String columnName) throws InvalidResultSetAccessException {
		return (String)results.get(position).get(columnName);
	}






	//******************************************************************************

	public boolean absolute(int row) throws InvalidResultSetAccessException {
		Assert.fail();
		return false;
	}

	public void afterLast() throws InvalidResultSetAccessException {
		Assert.fail();
	}

	public void beforeFirst() throws InvalidResultSetAccessException {
		Assert.fail();
	}

	public int findColumn(String columnName) throws InvalidResultSetAccessException {
		Assert.fail();
		return 0;
	}

	public boolean first() throws InvalidResultSetAccessException {
		Assert.fail();
		return false;
	}

	public BigDecimal getBigDecimal(int columnIndex) throws InvalidResultSetAccessException {
		Assert.fail();
		return null;
	}

	public BigDecimal getBigDecimal(String columnName) throws InvalidResultSetAccessException {
		Assert.fail();
		return null;
	}

	public boolean getBoolean(int columnIndex) throws InvalidResultSetAccessException {
		Assert.fail();
		return false;
	}

	public boolean getBoolean(String columnName) throws InvalidResultSetAccessException {
		Assert.fail();
		return false;
	}

	public byte getByte(int columnIndex) throws InvalidResultSetAccessException {
		Assert.fail();
		return 0;
	}

	public byte getByte(String columnName) throws InvalidResultSetAccessException {
		Assert.fail();
		return 0;
	}

	public Date getDate(int columnIndex, Calendar cal) throws InvalidResultSetAccessException {
		Assert.fail();
		return null;
	}

	public Date getDate(int columnIndex) throws InvalidResultSetAccessException {
		Assert.fail();
		return null;
	}

	public Date getDate(String columnName, Calendar cal) throws InvalidResultSetAccessException {
		Assert.fail();
		return null;
	}

	public double getDouble(int columnIndex) throws InvalidResultSetAccessException {
		Assert.fail();
		return 0;
	}

	public double getDouble(String columnName) throws InvalidResultSetAccessException {
		Assert.fail();
		return 0;
	}

	public float getFloat(int columnIndex) throws InvalidResultSetAccessException {
		Assert.fail();
		return 0;
	}

	public float getFloat(String columnName) throws InvalidResultSetAccessException {
		Assert.fail();
		return 0;
	}

	public int getInt(int columnIndex) throws InvalidResultSetAccessException {
		Assert.fail();
		return 0;
	}

	public long getLong(int columnIndex) throws InvalidResultSetAccessException {
		Assert.fail();
		return 0;
	}

	public SqlRowSetMetaData getMetaData() {
		Assert.fail();
		return null;
	}

	@SuppressWarnings("unchecked")
	public Object getObject(int columnIndex, Map map) throws InvalidResultSetAccessException {
		Assert.fail();
		return null;
	}

	public Object getObject(int columnIndex) throws InvalidResultSetAccessException {
		Assert.fail();
		return null;
	}

	@SuppressWarnings("unchecked")
	public Object getObject(String columnName, Map map) throws InvalidResultSetAccessException {
		Assert.fail();
		return null;
	}

	public Object getObject(String columnName) throws InvalidResultSetAccessException {
		Assert.fail();
		return null;
	}

	public int getRow() throws InvalidResultSetAccessException {
		Assert.fail();
		return 0;
	}

	public short getShort(int columnIndex) throws InvalidResultSetAccessException {
		Assert.fail();
		return 0;
	}

	public short getShort(String columnName) throws InvalidResultSetAccessException {
		Assert.fail();
		return 0;
	}

	public String getString(int columnIndex) throws InvalidResultSetAccessException {
		Assert.fail();
		return null;
	}

	public Time getTime(int columnIndex, Calendar cal) throws InvalidResultSetAccessException {
		Assert.fail();
		return null;
	}

	public Time getTime(int columnIndex) throws InvalidResultSetAccessException {
		Assert.fail();
		return null;
	}

	public Time getTime(String columnName, Calendar cal) throws InvalidResultSetAccessException {
		Assert.fail();
		return null;
	}

	public Time getTime(String columnName) throws InvalidResultSetAccessException {
		Assert.fail();
		return null;
	}

	public Timestamp getTimestamp(int columnIndex, Calendar cal) throws InvalidResultSetAccessException {
		Assert.fail();
		return null;
	}

	public Timestamp getTimestamp(int columnIndex) throws InvalidResultSetAccessException {
		Assert.fail();
		return null;
	}

	public Timestamp getTimestamp(String columnName, Calendar cal) throws InvalidResultSetAccessException {
		Assert.fail();
		return null;
	}

	public Timestamp getTimestamp(String columnName) throws InvalidResultSetAccessException {
		Assert.fail();
		return null;
	}

	public boolean isAfterLast() throws InvalidResultSetAccessException {
		Assert.fail();
		return false;
	}

	public boolean isBeforeFirst() throws InvalidResultSetAccessException {
		Assert.fail();
		return false;
	}

	public boolean isFirst() throws InvalidResultSetAccessException {
		Assert.fail();
		return false;
	}

	public boolean isLast() throws InvalidResultSetAccessException {
		Assert.fail();
		return false;
	}

	public boolean last() throws InvalidResultSetAccessException {
		Assert.fail();
		return false;
	}

	public boolean previous() throws InvalidResultSetAccessException {
		Assert.fail();
		return false;
	}

	public boolean relative(int rows) throws InvalidResultSetAccessException {
		Assert.fail();
		return false;
	}

	public boolean wasNull() throws InvalidResultSetAccessException {
		Assert.fail();
		return false;
	}

}
