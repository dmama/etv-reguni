package ch.vd.uniregctb.migration.pm.regpm.usertype;


import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.usertype.UserType;

import ch.vd.shared.hibernate.type.GenericUserType;

public class MutationTimestampUserType extends GenericUserType implements UserType {

	private static final int[] SQL_TYPES = { Types.DATE, Types.TIME };

	@Override
	public int[] sqlTypes() {
		return SQL_TYPES;
	}

	@Override
	public Class<Timestamp> returnedClass() {
		return Timestamp.class;
	}

	@Override
	public Timestamp nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner) throws HibernateException, SQLException {
		final String nameDate = names[0];
		final String nameTime = names[1];
		final Time time = rs.getTime(nameTime);
		final Date date = rs.getDate(nameDate);
		if (time == null || date == null) {
			return null;
		}

		final Calendar dateCal = Calendar.getInstance();
		dateCal.setTime(date);
		final Calendar timeCal = Calendar.getInstance();
		timeCal.setTime(time);

		dateCal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY));
		dateCal.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE));
		dateCal.set(Calendar.SECOND, timeCal.get(Calendar.SECOND));
		dateCal.set(Calendar.MILLISECOND, timeCal.get(Calendar.MILLISECOND));

		final long tsTime = dateCal.getTimeInMillis();
		return new Timestamp(tsTime);
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session) throws HibernateException, SQLException {
		throw new RuntimeException("Cette conversion ne devrait pas être utilisée dans ce sens !");
	}
}
