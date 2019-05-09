package ch.vd.unireg.evenement.ide;

import javax.persistence.FlushModeType;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.query.Query;

import ch.vd.unireg.common.BaseDAOImpl;

/**
 * @author Raphaël Marmier, 2016-08-15, <raphael.marmier@vd.ch>
 */
public class ReferenceAnnonceIDEDAOImpl extends BaseDAOImpl<ReferenceAnnonceIDE, Long> implements ReferenceAnnonceIDEDAO {

	protected ReferenceAnnonceIDEDAOImpl() {
		super(ReferenceAnnonceIDE.class);
	}

	@Override
	public List<ReferenceAnnonceIDE> getReferencesAnnonceIDE(long etablissementId) {

		final Session session = getCurrentSession();

		final List<ReferenceAnnonceIDE> list;
		final FlushModeType mode = session.getFlushMode();
		try {
			session.setFlushMode(FlushModeType.COMMIT);
			final Query query = session.createQuery("select distinct r from ReferenceAnnonceIDE r where r.etablissement.numero = :etablissementId order by r.etablissement.numero");
			query.setParameter("etablissementId", etablissementId);
			//noinspection unchecked
			list = query.list();
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

		final List<ReferenceAnnonceIDE> list;
		final FlushModeType mode = session.getFlushMode();
		try {
			session.setFlushMode(FlushModeType.COMMIT);
			final Query query = session.createQuery("select distinct r from ReferenceAnnonceIDE r where r.etablissement.numero = :etablissementId order by id desc");
			query.setParameter("etablissementId", etablissementId);
			list = query.list();
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
