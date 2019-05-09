package ch.vd.unireg.hibernate;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

import ch.vd.unireg.type.delai.Delai;

/**
 * Cette classe permet de mapper un délai {@link Delai} dans une colonne de type string.
 */
@SuppressWarnings("UnusedDeclaration")
public class DelaiUserType extends GenericUserType implements UserType {

	private static final int[] SQL_TYPES = {
			Types.VARCHAR
	};

	@Override
	public int[] sqlTypes() {
		return SQL_TYPES;
	}

	@Override
	public Class<?> returnedClass() {
		return Delai.class;
	}

	@Override
	public Object nullSafeGet(ResultSet resultSet, String[] names, SharedSessionContractImplementor session, Object owner) throws HibernateException, SQLException {
		final String string = resultSet.getString(names[0]);
		if (resultSet.wasNull() || StringUtils.isBlank(string)) {
			return null;
		}
		return Delai.fromString(string);
	}

	@Override
	public void nullSafeSet(PreparedStatement preparedStatement, Object value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {
		if (null == value) {
			preparedStatement.setNull(index, Types.VARCHAR);
		}
		else {
			final Delai delai = (Delai) value;
			preparedStatement.setString(index, delai.toString());
		}
	}
}
