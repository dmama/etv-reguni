package ch.vd.uniregctb.evenement.externe;

import java.util.Collection;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ch.vd.uniregctb.common.BaseDAOImpl;

/**
 * DAO des événements externes.
 *
 * @author xcicfh
 *
 */
public class EvenementExterneDAOImpl extends BaseDAOImpl<EvenementExterne, Long> implements EvenementExterneDAO {

	public EvenementExterneDAOImpl() {
		super(EvenementExterne.class);
	}

	@Override
	public boolean existe(final String businessId) {
		final Session session = getCurrentSession();
		final Criteria criteria = session.createCriteria(EvenementExterne.class);
		criteria.setProjection(Projections.rowCount());
		criteria.add(Restrictions.eq("businessId", businessId));
		final int count = ((Number) criteria.uniqueResult()).intValue();
		return count > 0;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Collection<EvenementExterne> getEvenementExternes(final boolean ascending, final EtatEvenementExterne... etatEvenementExternes) {
		final Session session = getCurrentSession();
		final Criteria criteria = session.createCriteria(EvenementExterne.class);
		if (ascending) {
			criteria.addOrder(Order.asc("dateEvenement"));
		}
		else {
			criteria.addOrder(Order.desc("dateEvenement"));
		}
		if (etatEvenementExternes != null && etatEvenementExternes.length > 0) {
			criteria.add(Restrictions.in("etat", etatEvenementExternes));
		}
		return criteria.list();
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public List<Long> getIdsQuittancesLRToMigrate() {
		return find("select q.id from QuittanceLR q where q.type is null", null);
	}
}
