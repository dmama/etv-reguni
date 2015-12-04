package ch.vd.uniregctb.migration.pm.regpm.usertype;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.usertype.UserType;

import ch.vd.shared.hibernate.type.GenericUserType;

public class LongZeroIsNullUserType extends GenericUserType implements UserType {

	/**
	 * Types SQL.
	 */
	private static final int[] SQL_TYPES = {
			Types.DECIMAL
	};

	@Override
	public int[] sqlTypes() {
		return SQL_TYPES;
	}

	@Override
	public Class<Long> returnedClass() {
		return Long.class;
	}

	@Override
	public Long nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner) throws HibernateException, SQLException {
		final String name = names[0];
		final BigDecimal decimal = rs.getBigDecimal(name);
		if (decimal == null || rs.wasNull() || BigDecimal.ZERO.compareTo(decimal) == 0) {
			return null;
		}
		return decimal.longValue();
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session) throws HibernateException, SQLException {
		throw new RuntimeException("Should not be used!!!");
	}
}
