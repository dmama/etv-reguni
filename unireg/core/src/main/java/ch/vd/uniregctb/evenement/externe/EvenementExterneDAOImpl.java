package ch.vd.uniregctb.evenement.externe;

import ch.vd.registre.base.dao.GenericDAOImpl;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.springframework.orm.hibernate3.HibernateCallback;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

/**
 * DAO des événements externes.
 *
 * @author xcicfh
 *
 */
public class EvenementExterneDAOImpl extends GenericDAOImpl<EvenementExterne, Long> implements EvenementExterneDAO {

	public EvenementExterneDAOImpl() {
		super(EvenementExterne.class);
	}

	public boolean existe(final String businessId) {
		return (Boolean) getHibernateTemplate().executeWithNativeSession(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException, SQLException {
				Criteria criteria = session.createCriteria(EvenementExterne.class);
				criteria.setProjection(Projections.rowCount());
				criteria.add(Expression.eq("businessId", businessId));
				Integer count = (Integer) criteria.uniqueResult();
				return count > 0;
			}
		});
	}

	@SuppressWarnings("unchecked")
	public Collection<EvenementExterne> getEvenementExternes(final boolean ascending, final EtatEvenementExterne... etatEvenementExternes) {
		return (Collection<EvenementExterne>) getHibernateTemplate().executeWithNativeSession(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException, SQLException {
				Criteria criteria = session.createCriteria(EvenementExterne.class);
				if (ascending) {
					criteria.addOrder(Order.asc("dateEvenement"));
				}
				else {
					criteria.addOrder(Order.desc("dateEvenement"));
				}
				if (etatEvenementExternes != null && etatEvenementExternes.length > 0) {
					criteria.add(Expression.in("etat", etatEvenementExternes));
				}
				return criteria.list();
			}
		});
	}

	@SuppressWarnings({"unchecked"})
	public List<Long> getIdsQuittancesLRToMigrate() {
		return (List<Long>) getHibernateTemplate().executeWithNewSession(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException, SQLException {
				Query q = session.createQuery("select q.id from QuittanceLR q where q.type is null");
				return q.list();
			}
		});
	}
}
