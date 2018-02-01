package ch.vd.unireg.registrefoncier.dao;

import java.util.List;

import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BaseDAOImpl;
import ch.vd.unireg.registrefoncier.RapprochementRF;
import ch.vd.unireg.registrefoncier.TiersRF;

public class RapprochementRFDAOImpl extends BaseDAOImpl<RapprochementRF, Long> implements RapprochementRFDAO {

	public RapprochementRFDAOImpl() {
		super(RapprochementRF.class);
	}

	@NotNull
	@Override
	public List<RapprochementRF> findByContribuable(long ctbId, boolean noAutoFlush) {
		final Session session = getCurrentSession();
		final FlushMode oldMode = session.getFlushMode();
		if (noAutoFlush) {
			session.setFlushMode(FlushMode.MANUAL);
		}
		try {
			final Query query = session.createQuery("FROM RapprochementRF rrf WHERE rrf.contribuable.id=:ctbId ORDER BY rrf.id");
			query.setParameter("ctbId", ctbId);
			//noinspection unchecked
			return query.list();
		}
		finally {
			session.setFlushMode(oldMode);
		}
	}

	@NotNull
	@Override
	public List<RapprochementRF> findByTiersRF(long tiersRFId, boolean noAutoFlush) {
		final Session session = getCurrentSession();
		final FlushMode oldMode = session.getFlushMode();
		if (noAutoFlush) {
			session.setFlushMode(FlushMode.MANUAL);
		}
		try {
			final Query query = session.createQuery("FROM RapprochementRF rrf WHERE rrf.tiersRF.id=:tiersRFId ORDER BY rrf.id");
			query.setParameter("tiersRFId", tiersRFId);
			//noinspection unchecked
			return query.list();
		}
		finally {
			session.setFlushMode(oldMode);
		}
	}

	@NotNull
	@Override
	public List<TiersRF> findTiersRFSansRapprochement(RegDate dateReference) {
		final Query query;
		if (dateReference == null) {
			query = getCurrentSession().createQuery("FROM TiersRF trf WHERE NOT EXISTS (SELECT 1 FROM RapprochementRF rrf WHERE rrf.tiersRF = trf AND rrf.annulationDate IS NULL)");
		}
		else {
			query = getCurrentSession().createQuery("FROM TiersRF trf WHERE NOT EXISTS (SELECT 1 FROM RapprochementRF rrf WHERE rrf.tiersRF = trf AND rrf.annulationDate IS NULL AND (rrf.dateDebut IS NULL OR rrf.dateDebut <= :dateReference) AND (rrf.dateFin IS NULL OR rrf.dateFin >= :dateReference))");
			query.setParameter("dateReference", dateReference);
		}
		//noinspection unchecked
		return query.list();
	}
}
