package ch.vd.unireg.hibernate;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.usertype.UserType;

import ch.vd.unireg.type.DayMonth;

/**
 * Cette classe permet de mapper une liste des dates 'DayMonth' dans une colonne de type string.
 * <p>
 * Le format utilisé est le suivant:
 * <ul>
 * <li>1er février => 0201</li>
 * <li>31 décembre => 1231</li>
 * <li>9 novembre => 1109</li>
 * <li>1er février, 9 novembre => 0201, 1109</li>
 * <li>1er février, 9 novembre, 31 décembre => 0201, 1109, 1231</li>
 * <li>...</li>
 * </ul>
 */
@SuppressWarnings("UnusedDeclaration")
public class DayMonthListUserType extends GenericUserType implements UserType {

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
		return List.class;
	}

	@Override
	public Object nullSafeGet(ResultSet resultSet, String[] names, SessionImplementor session, Object owner) throws HibernateException, SQLException {
		final String string = resultSet.getString(names[0]);
		if (resultSet.wasNull()) {
			return Collections.emptyList();
		}
		return Arrays.stream(string.split("[ ,]"))
				.filter(StringUtils::isNotBlank)
				.map(DayMonth::fromString)
				.collect(Collectors.toList());
	}

	@Override
	public void nullSafeSet(PreparedStatement preparedStatement, Object value, int index, SessionImplementor session) throws HibernateException, SQLException {
		//noinspection unchecked
		final List<DayMonth> list = (List<DayMonth>) value;
		if (list == null || list.isEmpty()) {
			preparedStatement.setNull(index, Types.VARCHAR);
		}
		else {
			final String string = list.stream()
					.map(DayMonth::toString)
					.collect(Collectors.joining(", "));
			preparedStatement.setString(index, string);
		}
	}
}
