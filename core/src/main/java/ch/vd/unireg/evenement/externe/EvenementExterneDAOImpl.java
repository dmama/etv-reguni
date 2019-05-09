package ch.vd.unireg.evenement.externe;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.Collection;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.query.Query;

import ch.vd.unireg.common.BaseDAOImpl;

/**
 * DAO des événements externes.
 */
public class EvenementExterneDAOImpl extends BaseDAOImpl<EvenementExterne, Long> implements EvenementExterneDAO {

	public EvenementExterneDAOImpl() {
		super(EvenementExterne.class);
	}

	@Override
	public boolean existe(final String businessId) {
		final Query query = getCurrentSession().createQuery("select count(*) from EvenementExterne where businessId = :businessId");
		query.setParameter("businessId", businessId);
		final int count = ((Number) query.uniqueResult()).intValue();
		return count > 0;
	}

	@Override
	public Collection<EvenementExterne> getEvenementExternes(final boolean ascending, final EtatEvenementExterne... etatEvenementExternes) {
		final Session session = getCurrentSession();
		final CriteriaBuilder builder = session.getCriteriaBuilder();

		final CriteriaQuery<EvenementExterne> query = builder.createQuery(EvenementExterne.class);
		final Root<EvenementExterne> root = query.from(EvenementExterne.class);

		if (ascending) {
			query.orderBy(builder.asc(root.get("dateEvenement")));
		}
		else {
			query.orderBy(builder.desc(root.get("dateEvenement")));
		}

		if (etatEvenementExternes != null && etatEvenementExternes.length > 0) {
			query.where(root.get("etat").in((Object[]) etatEvenementExternes));
		}

		return session.createQuery(query).list();
	}

	@Override
	public List<Long> getIdsQuittancesLRToMigrate() {
		return find("select q.id from QuittanceLR q where q.type is null", null);
	}
}
