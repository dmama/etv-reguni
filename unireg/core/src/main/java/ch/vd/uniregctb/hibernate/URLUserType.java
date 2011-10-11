package ch.vd.uniregctb.hibernate;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;

/**
 * Cette classe permet de mapper des URLs dans une colonne de type string.
 */
public class URLUserType extends GenericUserType implements UserType {

	private boolean allowPartial;

	public URLUserType() {
	}

	private static final int[] SQL_TYPES = {
			Types.VARCHAR
	};

	@Override
	public int[] sqlTypes() {
		return SQL_TYPES;
	}

	@Override
	public Class<?> returnedClass() {
		return URL.class;
	}

	@Override
	public Object nullSafeGet(ResultSet resultSet, String[] names, Object owner) throws HibernateException, SQLException {
		String spec = resultSet.getString(names[0]);
		URL result = null;
		if (!resultSet.wasNull()) {
			try {
				result = new URL(spec);
			}
			catch (MalformedURLException e) {
				throw new HibernateException("La string [" + spec + "] ne représente pas une URL valide", e);
			}
		}
		return result;
	}

	@Override
	public void nullSafeSet(PreparedStatement preparedStatement, Object value, int index) throws HibernateException, SQLException {
		if (null == value) {
			preparedStatement.setNull(index, Types.VARCHAR);
		}
		else {
			final URL url = (URL) value;
			preparedStatement.setString(index, url.toString());
		}
	}
}
