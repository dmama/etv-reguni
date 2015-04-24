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
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeEtatEntreprise;

public class TypeEtatEntrepriseUserType extends GenericUserType implements UserType {

	private static final int[] SQL_TYPES = {
			Types.CHAR
	};

	private static final Map<String, RegpmTypeEtatEntreprise> MAPPING = buildMapping();

	private static Map<String, RegpmTypeEtatEntreprise> buildMapping() {
		final Map<String, RegpmTypeEtatEntreprise> map = new HashMap<>();
		map.put("01", RegpmTypeEtatEntreprise.INSCRITE_AU_RC);
		map.put("02", RegpmTypeEtatEntreprise.EN_LIQUIDATION);
		map.put("03", RegpmTypeEtatEntreprise.EN_SUSPENS_FAILLITE);
		map.put("04", RegpmTypeEtatEntreprise.EN_FAILLITE);
		map.put("05", RegpmTypeEtatEntreprise.ABSORBEE);
		map.put("06", RegpmTypeEtatEntreprise.RADIEE_DU_RC);
		map.put("07", RegpmTypeEtatEntreprise.FONDEE);
		map.put("08", RegpmTypeEtatEntreprise.DISSOUTE);
		return map;
	}

	@Override
	public int[] sqlTypes() {
		return SQL_TYPES;
	}

	@Override
	public Class<RegpmTypeEtatEntreprise> returnedClass() {
		return RegpmTypeEtatEntreprise.class;
	}

	@Override
	public RegpmTypeEtatEntreprise nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner) throws HibernateException, SQLException {
		final String name = names[0];
		final String code = StringUtils.trimToNull(rs.getString(name));
		if (code != null && !rs.wasNull()) {
			final RegpmTypeEtatEntreprise s = MAPPING.get(code);
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
