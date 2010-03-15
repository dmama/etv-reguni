package ch.vd.uniregctb.hibernate;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;

import ch.vd.common.model.EnumTypeAdresse;

/**
 * Effectue la traduction d'un attribut de type EnumTypeAdresse en une colonne VARCHAR, et inversément.
 *
 * @see http://www.hibernate.org/265.html
 */
public class EnumTypeAdresseUserType implements UserType {

	private static final int[] SQL_TYPES = {
		Types.VARCHAR
	};

	public Object assemble(Serializable cached, Object owner) throws HibernateException {
		return cached;
	}

	public Object deepCopy(Object value) throws HibernateException {
		return value;
	}

	public Serializable disassemble(Object value) throws HibernateException {
		return (Serializable) value;
	}

	public boolean equals(Object x, Object y) throws HibernateException {
		if (x == y)
			return true;
		if (null == x || null == y)
			return false;
		return x.equals(y);
	}

	public int hashCode(Object x) throws HibernateException {
		return x.hashCode();
	}

	public boolean isMutable() {
		return false;
	}

	public Object nullSafeGet(ResultSet rs, String[] names, Object owner) throws HibernateException, SQLException {
		final String name = rs.getString(names[0]);
		EnumTypeAdresse result = null;
		if (!rs.wasNull()) {
			result = EnumTypeAdresse.getEnum(name);
		}
		return result;
	}

	public void nullSafeSet(PreparedStatement st, Object value, int index) throws HibernateException, SQLException {
		if (null == value) {
			st.setNull(index, Types.VARCHAR);
		}
		else {
			final EnumTypeAdresse e = (EnumTypeAdresse) value;
			st.setString(index, e.getName());
		}
	}

	public Object replace(Object original, Object target, Object owner) throws HibernateException {
		return original;
	}

	@SuppressWarnings("unchecked")
	public Class returnedClass() {
		return EnumTypeAdresse.class;
	}

	public int[] sqlTypes() {
		return SQL_TYPES;
	}

}
