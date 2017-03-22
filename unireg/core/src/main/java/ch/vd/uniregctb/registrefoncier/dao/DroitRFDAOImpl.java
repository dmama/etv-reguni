package ch.vd.uniregctb.registrefoncier.dao;

import java.util.List;

import org.hibernate.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.common.BaseDAOImpl;
import ch.vd.uniregctb.registrefoncier.DroitRF;
import ch.vd.uniregctb.registrefoncier.key.DroitRFKey;

public class DroitRFDAOImpl extends BaseDAOImpl<DroitRF, Long> implements DroitRFDAO {

	protected DroitRFDAOImpl() {
		super(DroitRF.class);
	}

	@Nullable
	@Override
	public DroitRF findActive(@NotNull DroitRFKey key) {
		final Query query = getCurrentSession().createQuery("from DroitRF where masterIdRF = :masterIdRF and dateFin is null");
		query.setParameter("masterIdRF", key.getMasterIdRF());
		return (DroitRF) query.uniqueResult();
	}

	@SuppressWarnings("unchecked")
	@NotNull
	@Override
	public List<DroitRF> findForAyantDroit(long ayantDroitId, boolean fetchSituationsImmeuble) {
		final StringBuilder b = new StringBuilder();
		b.append("SELECT dt FROM DroitRF dt");
		if (fetchSituationsImmeuble) {
			b.append(" INNER JOIN FETCH dt.immeuble AS imm");
			b.append(" LEFT OUTER JOIN FETCH imm.situations");
		}
		b.append(" WHERE dt.ayantDroit.id = :ayantDroitId");

		final Query query = getCurrentSession().createQuery(b.toString());
		query.setParameter("ayantDroitId", ayantDroitId);
		return query.list();
	}
}
