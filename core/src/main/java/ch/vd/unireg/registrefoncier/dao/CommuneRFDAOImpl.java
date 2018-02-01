package ch.vd.unireg.registrefoncier.dao;

import org.hibernate.Query;
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
	public CommuneRF findActive(@NotNull CommuneRFKey communeRFKey) {
		final Query query = getCurrentSession().createQuery("from CommuneRF where noRf = :noRf and dateFin is null");
		query.setParameter("noRf", communeRFKey.getNoRF());
		return (CommuneRF) query.uniqueResult();
	}
}
