package ch.vd.uniregctb.migration.pm.regpm.usertype;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;

import ch.vd.shared.hibernate.type.GenericUserType;

public class BooleanYesNoUserType extends GenericUserType implements UserType, ParameterizedType {

	/**
	 * Types SQL.
	 */
	private static final int[] SQL_TYPES = {
			Types.CHAR
	};

	private static final Map<String, Boolean> MAPPING = buildMapping();

	private static Map<String, Boolean> buildMapping() {
		final Map<String, Boolean> map = new HashMap<>();
		map.put("O", Boolean.TRUE);
		map.put("Y", Boolean.TRUE);
		map.put("A", Boolean.TRUE);         // comme "ANNULE"
		map.put("N", Boolean.FALSE);
		return map;
	}

	private Boolean defaultValue;

	@Override
	public void setParameterValues(Properties parameters) {
		if (parameters != null) {
			final String prop = parameters.getProperty("default");
			if (prop != null) {
				defaultValue = Boolean.valueOf(prop);
			}
			else {
				defaultValue = null;
			}
		}
		else {
			defaultValue = null;
		}
	}

	@Override
	public int[] sqlTypes() {
		return SQL_TYPES;
	}

	@Override
	public Class<Boolean> returnedClass() {
		return Boolean.class;
	}

	@Override
	public Boolean nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner) throws HibernateException, SQLException {
		final String name = names[0];
		final String value = rs.getString(name);
		if (value == null) {
			return defaultValue;
		}
		final Boolean res = MAPPING.get(value);
		return res == null ? defaultValue : res;
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session) throws HibernateException, SQLException {
		throw new RuntimeException("Cette conversion ne devrait pas être utilisée (le mainframe est en lecture seule !)");
	}
}
