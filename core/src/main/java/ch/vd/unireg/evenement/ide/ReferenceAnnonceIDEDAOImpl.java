package ch.vd.uniregctb.evenement.ide;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import ch.vd.uniregctb.common.BaseDAOImpl;

/**
 * @author Raphaël Marmier, 2016-08-15, <raphael.marmier@vd.ch>
 */
public class ReferenceAnnonceIDEDAOImpl extends BaseDAOImpl<ReferenceAnnonceIDE, Long> implements ReferenceAnnonceIDEDAO {

	protected ReferenceAnnonceIDEDAOImpl() {
		super(ReferenceAnnonceIDE.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<ReferenceAnnonceIDE> getReferencesAnnonceIDE(long etablissementId) {

		final Session session = getCurrentSession();

		final Criteria crit = session.createCriteria(ReferenceAnnonceIDE.class);
		crit.add(Restrictions.eq("etablissement.numero", etablissementId));
		crit.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		crit.addOrder(Order.asc("etablissement.numero"));

		final List<ReferenceAnnonceIDE> list;
		final FlushMode mode = session.getFlushMode();
		try {
			session.setFlushMode(FlushMode.MANUAL);
			list = crit.list();
		}
		finally {
			session.setFlushMode(mode);
		}

		if (list.isEmpty()) {
			return null;
		}
		else {
			return list;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public ReferenceAnnonceIDE getLastReferenceAnnonceIDE(long etablissementId) {

		final Session session = getCurrentSession();

		final Criteria crit = session.createCriteria(ReferenceAnnonceIDE.class);
		crit.add(Restrictions.eq("etablissement.numero", etablissementId));
		crit.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		crit.addOrder(Order.desc("id"));

		final List<ReferenceAnnonceIDE> list;
		final FlushMode mode = session.getFlushMode();
		try {
			session.setFlushMode(FlushMode.MANUAL);
			list = crit.list();
		}
		finally {
			session.setFlushMode(mode);
		}

		if (list.isEmpty()) {
			return null;
		}
		else {
			return list.get(0);
		}
	}
}
