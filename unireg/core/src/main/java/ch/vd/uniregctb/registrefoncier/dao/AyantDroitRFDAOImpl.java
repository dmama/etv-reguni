package ch.vd.uniregctb.registrefoncier.dao;

import org.hibernate.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.common.BaseDAOImpl;
import ch.vd.uniregctb.registrefoncier.AyantDroitRF;
import ch.vd.uniregctb.registrefoncier.key.AyantDroitRFKey;

public class AyantDroitRFDAOImpl extends BaseDAOImpl<AyantDroitRF, Long> implements AyantDroitRFDAO {
	protected AyantDroitRFDAOImpl() {
		super(AyantDroitRF.class);
	}

	@Nullable
	@Override
	public AyantDroitRF find(@NotNull AyantDroitRFKey key) {
		final Query query = getCurrentSession().createQuery("from AyantDroitRF where idRF = :idRF");
		query.setParameter("idRF", key.getIdRF());
		return (AyantDroitRF) query.uniqueResult();
	}
}
