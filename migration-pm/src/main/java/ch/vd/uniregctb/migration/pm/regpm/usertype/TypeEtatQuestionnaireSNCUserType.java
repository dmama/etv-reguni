package ch.vd.uniregctb.migration.pm.regpm.usertype;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.usertype.UserType;

import ch.vd.shared.hibernate.type.GenericUserType;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeEtatQuestionnaireSNC;

public class TypeEtatQuestionnaireSNCUserType extends GenericUserType implements UserType {

	private static final int[] SQL_TYPES = {
			Types.CHAR
	};

	@Override
	public int[] sqlTypes() {
		return SQL_TYPES;
	}

	@Override
	public Class<RegpmTypeEtatQuestionnaireSNC> returnedClass() {
		return RegpmTypeEtatQuestionnaireSNC.class;
	}

	@Override
	public RegpmTypeEtatQuestionnaireSNC nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner) throws HibernateException, SQLException {
		final String name = names[0];
		final String code = StringUtils.trimToNull(rs.getString(name));
		if (code != null && !rs.wasNull()) {
			return RegpmTypeEtatQuestionnaireSNC.valueOf(code);
		}
		return null;
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session) throws HibernateException, SQLException {
		throw new RuntimeException("Cette conversion ne devrait pas être utilisée.");
	}
}
