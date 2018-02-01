package ch.vd.unireg.hibernate;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.apache.commons.io.IOUtils;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.usertype.UserType;

/**
 * User-type hibernate qui mappe une chaîne de caractères dans un CLOB SQL
 */
public class StringAsClobUserType extends GenericUserType implements UserType {

	/**
	 * Types SQL.
	 */
	private static final int[] SQL_TYPES = {Types.CLOB};

	@Override
	public int[] sqlTypes() {
		return SQL_TYPES;
	}

	@Override
	public Class returnedClass() {
		return String.class;
	}

	@Override
	public Object nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner) throws HibernateException, SQLException {
		try (Reader reader = rs.getCharacterStream(names[0])) {
			if (rs.wasNull()) {
				return null;
			}

			final StringWriter writer = new StringWriter();
			IOUtils.copy(reader, writer);
			return writer.toString();
		}
		catch (IOException e) {
			throw new HibernateException(e);
		}
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session) throws HibernateException, SQLException {
		if (value != null) {
			final String string = (String) value;
			st.setCharacterStream(index, new StringReader(string), string.length());
		}
		else {
			st.setNull(index, Types.CLOB);
		}
	}
}
