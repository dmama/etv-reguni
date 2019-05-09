package ch.vd.unireg.etiquette;

import javax.persistence.FlushModeType;
import java.util.Collections;
import java.util.List;

import ch.vd.unireg.common.BaseDAOImpl;

public class EtiquetteDAOImpl extends BaseDAOImpl<Etiquette, Long> implements EtiquetteDAO {

	public EtiquetteDAOImpl() {
		super(Etiquette.class);
	}

	@Override
	public List<Etiquette> getAll(boolean doNotAutoflush) {
		if (!doNotAutoflush) {
			return getAll();
		}
		return find("from Etiquette", Collections.emptyMap(), FlushModeType.COMMIT);
	}

	@Override
	public Etiquette getByCode(String code) {
		final String hql = "from Etiquette where code=:code";
		final List<Etiquette> result = find(hql, Collections.singletonMap("code", code), null);
		if (result == null || result.isEmpty()) {
			return null;
		}
		if (result.size() == 1) {
			return result.get(0);
		}
		throw new IllegalStateException("Plusieurs étiquettes existent avec le code " + code);
	}
}
