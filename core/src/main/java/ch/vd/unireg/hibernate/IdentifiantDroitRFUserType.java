package ch.vd.unireg.hibernate;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

import ch.vd.unireg.registrefoncier.IdentifiantDroitRF;

/**
 * Cette classe permet de mapper des IdentifiantDroitRF dans une colonne de type string.
 */
public class IdentifiantDroitRFUserType extends GenericUserType implements UserType {

	public IdentifiantDroitRFUserType() {
	}

	/**
	 * Types SQL.
	 */
	private static final int[] SQL_TYPES = {
			Types.VARCHAR
	};

	@Override
	public int[] sqlTypes() {
		return SQL_TYPES;
	}

	@Override
	public Class<?> returnedClass() {
		return IdentifiantDroitRF.class;
	}

	@Override
	public Object nullSafeGet(ResultSet resultSet, String[] names, SharedSessionContractImplementor session, Object owner) throws HibernateException, SQLException {
		String value = resultSet.getString(names[0]);
		IdentifiantDroitRF result = null;
		if (!resultSet.wasNull()) {
			result = IdentifiantDroitRF.parse(value);
		}
		return result;
	}

	@Override
	public void nullSafeSet(PreparedStatement preparedStatement, Object value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {
		if (null == value) {
			preparedStatement.setNull(index, Types.VARCHAR);
		}
		else {
			final IdentifiantDroitRF id = (IdentifiantDroitRF) value;
			preparedStatement.setString(index, id.toString());
		}
	}
}
