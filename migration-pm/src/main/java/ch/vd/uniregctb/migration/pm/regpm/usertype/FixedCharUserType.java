package ch.vd.uniregctb.migration.pm.regpm.usertype;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;

import ch.vd.shared.hibernate.type.GenericUserType;

public class FixedCharUserType extends GenericUserType implements UserType, ParameterizedType {

	/**
	 * Types SQL.
	 */
	private static final int[] SQL_TYPES = {
			Types.CHAR
	};

	private static final int DEFAULT_LENGTH = 255;

	private int length;

	@Override
	public void setParameterValues(Properties parameters) {
		if (parameters != null) {
			final String propValue = parameters.getProperty("length");
			if (propValue != null) {
				length = Integer.parseInt(propValue);
			}
			else {
				length = DEFAULT_LENGTH;
			}
		}
		else {
			length = DEFAULT_LENGTH;
		}
	}

	@Override
	public int[] sqlTypes() {
		return SQL_TYPES;
	}

	@Override
	public Class<String> returnedClass() {
		return String.class;
	}

	@Override
	public String nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner) throws HibernateException, SQLException {
		final String name = names[0];
		return StringUtils.trimToNull(rs.getString(name));
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session) throws HibernateException, SQLException {
		final String formatString = String.format("%%-%ds", length);
		if (value == null) {
			st.setString(index, String.format(formatString, StringUtils.EMPTY));
		}
		else {
			st.setString(index, String.format(formatString, (String) value));
		}
	}
}
