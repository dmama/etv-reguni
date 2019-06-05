package ch.vd.unireg.registrefoncier.dao;

import org.hibernate.query.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.common.BaseDAOImpl;
import ch.vd.unireg.registrefoncier.CommuneRF;
import ch.vd.unireg.registrefoncier.key.CommuneRFKey;

public class CommuneRFDAOImpl extends BaseDAOImpl<CommuneRF, Long> implements CommuneRFDAO {
	protected CommuneRFDAOImpl() {
		super(CommuneRF.class);
	}

	@Nullable
	@Override
	public CommuneRF findActive(@NotNull CommuneRFKey key) {
		CommuneRF commune = null;
		if (key.getNoOfs() != null) {
			// on essaie d'abord avec le numéro Ofs
			final Query query = getCurrentSession().createQuery("from CommuneRF where noOfs = :noOfs and dateFin is null");
			query.setParameter("noOfs", key.getNoOfs());
			commune = (CommuneRF) query.uniqueResult();
		}
		if (commune == null && key.getNoRF() != null) {
			// on essaie ensuite avec le numéro RF
			final Query query = getCurrentSession().createQuery("from CommuneRF where noRf = :noRf and dateFin is null");
			query.setParameter("noRf", key.getNoRF());
			commune = (CommuneRF) query.uniqueResult();
		}
		return commune;
	}
}
