package ch.vd.uniregctb.migration.pm.regpm.usertype;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.usertype.UserType;

import ch.vd.shared.hibernate.type.GenericUserType;

public abstract class EnumIntegerMappingUserType<T extends Enum<T>> extends GenericUserType implements UserType {

	private static final int[] SQL_TYPES = {
			Types.INTEGER
	};

	private final Class<T> enumClazz;
	private final Map<Integer, T> mapping;

	protected EnumIntegerMappingUserType(Class<T> enumClazz, Map<Integer, T> mapping) {
		this.enumClazz = enumClazz;
		this.mapping = mapping;
	}

	@Override
	public int[] sqlTypes() {
		return SQL_TYPES;
	}

	@Override
	public Class<T> returnedClass() {
		return enumClazz;
	}

	@Override
	public T nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner) throws HibernateException, SQLException {
		final String name = names[0];
		final int index = rs.getInt(name);
		if (!rs.wasNull()) {
			final T s = mapping.get(index);
			if (s == null) {
				throw new IllegalArgumentException("Code non supporté (" + index + ") pour mapping sur type " + enumClazz.getName());
			}
			return s;
		}
		return null;
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session) throws HibernateException, SQLException {
		throw new RuntimeException("Cette conversion ne devrait pas être utilisée.");
	}
}
