package ch.vd.unireg.hibernate;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

import ch.vd.unireg.registrefoncier.IdentifiantAffaireRF;

/**
 * Cette classe permet de mapper des IdentifiantAffaireRF dans une colonne de type string.
 */
public class IdentifiantAffaireRFUserType extends GenericUserType implements UserType {

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
		return IdentifiantAffaireRF.class;
	}

	@Override
	public Object nullSafeGet(ResultSet resultSet, String[] names, SharedSessionContractImplementor session, Object owner) throws HibernateException, SQLException {
		String value = resultSet.getString(names[0]);
		IdentifiantAffaireRF result = null;
		if (!resultSet.wasNull()) {
			result = IdentifiantAffaireRF.parse(value);
		}
		return result;
	}

	@Override
	public void nullSafeSet(PreparedStatement preparedStatement, Object value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {
		if (null == value) {
			preparedStatement.setNull(index, Types.VARCHAR);
		}
		else {
			final IdentifiantAffaireRF id = (IdentifiantAffaireRF) value;
			preparedStatement.setString(index, id.toString());
		}
	}
}
