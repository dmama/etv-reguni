package ch.vd.unireg.hibernate;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.usertype.UserType;

public abstract class JsonUserType<T> extends GenericUserType implements UserType {

	private final Class<T> clazz;
	private final ObjectMapper mapper = new ObjectMapper();
	private static final int[] SQL_TYPES = { Types.VARCHAR };

	protected JsonUserType(Class<T> clazz) {
		this.clazz = clazz;
	}

	@Override
	public int[] sqlTypes() {
		return SQL_TYPES;
	}

	@Override
	public Class<T> returnedClass() {
		return clazz;
	}

	@Override
	public T nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner) throws HibernateException, SQLException {
		final String str = rs.getString(names[0]);
		if (StringUtils.isNotBlank(str) && !rs.wasNull()) {
			try {
				return mapper.readValue(str, new TypeReference<T>() {});
			}
			catch (IOException e) {
				throw new HibernateException(e);
			}
		}
		return null;
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session) throws HibernateException, SQLException {
		if (value == null) {
			st.setNull(index, Types.VARCHAR);
		}
		else {
			try {
				final String str = mapper.writeValueAsString(value);
				st.setString(index, str);
			}
			catch (IOException e) {
				throw new HibernateException(e);
			}
		}
	}
}
