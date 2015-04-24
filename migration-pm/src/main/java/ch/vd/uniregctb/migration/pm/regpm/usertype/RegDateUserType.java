package ch.vd.uniregctb.migration.pm.regpm.usertype;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.usertype.UserType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.shared.hibernate.type.GenericUserType;

public class RegDateUserType extends GenericUserType implements UserType {

	private static final Logger LOGGER = LoggerFactory.getLogger(RegDateUserType.class);

	/**
	 * Types SQL.
	 */
	private static final int[] SQL_TYPES = {
			Types.DATE
	};

	@Override
	public int[] sqlTypes() {
		return SQL_TYPES;
	}

	@Override
	public Class<RegDate> returnedClass() {
		return RegDate.class;
	}

	@Override
	public RegDate nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner) throws HibernateException, SQLException {
		final String name = names[0];
		final Date date = rs.getDate(name);
		RegDate result = null;
		if (!rs.wasNull() && date != null) {
			try {
				result = RegDateHelper.get(date);
			}
			catch (RuntimeException e) {
				LOGGER.error("Impossible de récupérer la valeur de la RegDate =" + date + " name=" + name + " owner=" + owner);
				throw e;
			}
		}
		return result;
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session) throws HibernateException, SQLException {
		if (value == null) {
			st.setNull(index, Types.DATE);
		}
		else {
			st.setDate(index, new Date(((RegDate) value).asJavaDate().getTime()));
		}
	}
}
