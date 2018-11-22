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

import ch.vd.unireg.type.delai.Delai;

/**
 * Cette classe permet de mapper une liste de délais {@link ch.vd.unireg.type.delai.Delai} dans une colonne de type string.
 */
@SuppressWarnings("UnusedDeclaration")
public class DelaiListUserType extends GenericUserType implements UserType {

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
		return Arrays.stream(string.split("[,]"))
				.filter(StringUtils::isNotBlank)
				.map(String::trim)
				.map(Delai::fromString)
				.collect(Collectors.toList());
	}

	@Override
	public void nullSafeSet(PreparedStatement preparedStatement, Object value, int index, SessionImplementor session) throws HibernateException, SQLException {
		//noinspection unchecked
		final List<Delai> list = (List<Delai>) value;
		if (list == null || list.isEmpty()) {
			preparedStatement.setNull(index, Types.VARCHAR);
		}
		else {
			final String string = list.stream()
					.map(Delai::toString)
					.collect(Collectors.joining(", "));
			preparedStatement.setString(index, string);
		}
	}
}
