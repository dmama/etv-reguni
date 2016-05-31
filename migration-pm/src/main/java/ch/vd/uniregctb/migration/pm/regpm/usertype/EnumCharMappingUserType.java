package ch.vd.uniregctb.migration.pm.regpm.usertype;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.usertype.UserType;

import ch.vd.shared.hibernate.type.GenericUserType;

public abstract class EnumCharMappingUserType<T extends Enum<T>> extends GenericUserType implements UserType {

	private static final int[] SQL_TYPES = {
			Types.CHAR
	};

	private final Class<T> enumClazz;
	private final Map<String, T> mapping;

	protected EnumCharMappingUserType(Class<T> enumClazz, Map<String, T> mapping) {
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
		final String code = StringUtils.trimToNull(rs.getString(name));
		if (code != null && !rs.wasNull()) {
			final T s = mapping.get(code);
			if (s == null) {
				throw new IllegalArgumentException("Code non supporté (" + code + ") pour mapping sur type " + enumClazz.getName());
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
