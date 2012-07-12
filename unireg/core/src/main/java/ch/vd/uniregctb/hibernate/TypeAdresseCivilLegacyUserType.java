package ch.vd.uniregctb.hibernate;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;

import ch.vd.uniregctb.type.TypeAdresseCivil;

/**
 * Effectue la traduction d'un attribut de type TypeAdresseCivil (en une colonne VARCHAR, et inversément) en gardant la compatibilité de persistence du type EnumTypeAdresse ('C' = COURRIER, 'P' =
 * PRINCIPALE, 'S' = SECONDAIRE et 'T' = TUTEUR).
 *
 * @see http://www.hibernate.org/265.html
 */
public class TypeAdresseCivilLegacyUserType implements UserType {

	private static final int[] SQL_TYPES = {
			Types.VARCHAR
	};

	@Override
	public Object assemble(Serializable cached, Object owner) throws HibernateException {
		return cached;
	}

	@Override
	public Object deepCopy(Object value) throws HibernateException {
		return value;
	}

	@Override
	public Serializable disassemble(Object value) throws HibernateException {
		return (Serializable) value;
	}

	@Override
	public boolean equals(Object x, Object y) throws HibernateException {
		if (x == y)
			return true;
		if (null == x || null == y)
			return false;
		return x.equals(y);
	}

	@Override
	public int hashCode(Object x) throws HibernateException {
		return x.hashCode();
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public Object nullSafeGet(ResultSet rs, String[] names, Object owner) throws HibernateException, SQLException {
		final String name = rs.getString(names[0]);
		if (rs.wasNull()) {
			return null;
		}
		return TypeAdresseCivil.fromDbValue(name);
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int index) throws HibernateException, SQLException {
		if (null == value) {
			st.setNull(index, Types.VARCHAR);
		}
		else {
			st.setString(index, ((TypeAdresseCivil) value).toDbValue());
		}
	}

	@Override
	public Object replace(Object original, Object target, Object owner) throws HibernateException {
		return original;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class returnedClass() {
		return TypeAdresseCivil.class;
	}

	@Override
	public int[] sqlTypes() {
		return SQL_TYPES;
	}

}