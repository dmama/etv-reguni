package ch.vd.uniregctb.migration.pm.regpm.usertype;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.usertype.UserType;

import ch.vd.shared.hibernate.type.GenericUserType;
import ch.vd.uniregctb.migration.pm.regpm.RegpmFormeFusion;

public class FormeFusionUserType extends GenericUserType implements UserType {

	private static final int[] SQL_TYPES = {
			Types.CHAR
	};

	private static final Map<String, RegpmFormeFusion> MAPPING = buildMapping();

	private static Map<String, RegpmFormeFusion> buildMapping() {
		final Map<String, RegpmFormeFusion> map = new HashMap<>();
		map.put("A", RegpmFormeFusion.FORME_A);
		map.put("B", RegpmFormeFusion.FORME_B);
		map.put("C", RegpmFormeFusion.FORME_C);
		return map;
	}

	@Override
	public int[] sqlTypes() {
		return SQL_TYPES;
	}

	@Override
	public Class<RegpmFormeFusion> returnedClass() {
		return RegpmFormeFusion.class;
	}

	@Override
	public RegpmFormeFusion nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner) throws HibernateException, SQLException {
		final String name = names[0];
		final String code = StringUtils.trimToNull(rs.getString(name));
		if (code != null && !rs.wasNull()) {
			final RegpmFormeFusion s = MAPPING.get(code);
			if (s == null) {
				throw new IllegalArgumentException("Code non supporté : " + code);
			}
			return s;
		}
		return null;
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session) throws HibernateException, SQLException {
		throw new RuntimeException("Cette conversion ne devrait pas être utilisée.");
	}
}
