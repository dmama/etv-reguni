package ch.vd.unireg.hibernate;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.usertype.UserType;

import ch.vd.unireg.type.DayMonth;

/**
 * Cette classe permet de mapper des dates 'DayMonth' dans une colonne de type integer.
 * <p>
 * Le format utilisé est le suivant:
 * <ul>
 * <li>1er février => 201</li>
 * <li>31 décembre => 1231</li>
 * <li>9 novembre => 1109</li>
 * <li>...</li>
 * </ul>
 *
 * @see ch.vd.unireg.type.DayMonth#index()
 * @see ch.vd.unireg.type.DayMonth#fromIndex(int)
 */
@SuppressWarnings("UnusedDeclaration")
public class DayMonthUserType extends GenericUserType implements UserType {

	/**
	 * Constructeur.
	 */
	public DayMonthUserType() {
	}

	/**
	 * Types SQL.
	 */
	private static final int[] SQL_TYPES = {
		Types.INTEGER
	};

	/*
	 * (non-Javadoc)
	 *
	 * @see org.hibernate.usertype.UserType#sqlTypes()
	 */
	@Override
	public int[] sqlTypes() {
		return SQL_TYPES;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.hibernate.usertype.UserType#returnedClass()
	 */
	@Override
	public Class<?> returnedClass() {
		return DayMonth.class;
	}

	@Override
	public Object nullSafeGet(ResultSet resultSet, String[] names, SessionImplementor session, Object owner) throws HibernateException, SQLException {
		int index = resultSet.getInt(names[0]);
		DayMonth result = null;
		if (!resultSet.wasNull()) {
			result = DayMonth.fromIndex(index);
		}
		return result;
	}

	@Override
	public void nullSafeSet(PreparedStatement preparedStatement, Object value, int index, SessionImplementor session) throws HibernateException, SQLException {
		if (null == value) {
			preparedStatement.setNull(index, Types.INTEGER);
		}
		else {
			final DayMonth dm = (DayMonth) value;
			preparedStatement.setInt(index, dm.index());
		}
	}
}
