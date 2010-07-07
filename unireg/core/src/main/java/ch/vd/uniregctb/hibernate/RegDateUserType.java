package ch.vd.uniregctb.hibernate;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;

import ch.vd.registre.base.date.RegDate;

/**
 * Cette classe permet de mapper des dates 'RegDate' dans une colonne de type integer.
 * <p>
 * Le format utilisé est le suivant:
 * <ul>
 * <li>1er février 2006 => 20060201</li>
 * <li>31 décembre 1965 => 19651231</li>
 * <li>9 novembre 1989 => 19891109</li>
 * <li>...</li>
 * </ul>
 * En cas de date partiel, le paramètre 'allowPartial' doit être spécifié à 'true'. Le format est alors le suivant :
 * <ul>
 * <li>février 2006 => 20060200</li>
 * <li>décembre 1965 => 19651200</li>
 * <li>1989 => 19890000</li>
 * <li>2008 => 20080000</li>
 * <li>...</li>
 * </ul>
 *
 * @see RegDate#index()
 * @see RegDate#fromIndex(int, boolean)
 */
public class RegDateUserType extends GenericUserType implements UserType, ParameterizedType {

	private boolean allowPartial;

	/**
	 * Constructeur.
	 */
	public RegDateUserType() {
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
	public int[] sqlTypes() {
		return SQL_TYPES;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.hibernate.usertype.UserType#returnedClass()
	 */
	public Class<?> returnedClass() {
		return RegDate.class;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.hibernate.usertype.UserType#nullSafeGet(java.sql.ResultSet, java.lang.String[], java.lang.Object)
	 */
	public Object nullSafeGet(ResultSet resultSet, String[] names, Object owner) throws HibernateException, SQLException {
		int index = resultSet.getInt(names[0]);
		RegDate result = null;
		if (!resultSet.wasNull()) {
			result = RegDate.fromIndex(index, allowPartial);
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.hibernate.usertype.UserType#nullSafeSet(java.sql.PreparedStatement, java.lang.Object, int)
	 */
	public void nullSafeSet(PreparedStatement preparedStatement, Object value, int index) throws HibernateException, SQLException {
		if (null == value) {
			preparedStatement.setNull(index, Types.INTEGER);
		}
		else {
			final RegDate date = (RegDate) value;
			preparedStatement.setInt(index, date.index());
		}
	}

	public void setParameterValues(Properties parameters) {
		if (parameters != null) {
			final String value = parameters.getProperty("allowPartial");
			allowPartial = Boolean.parseBoolean(value);
		}
		else {
			allowPartial = false;
		}
	}
}
