package ch.vd.uniregctb.evenement.externe;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.dao.GenericDAOImpl;

/**
 * DAO des événements externes.
 *
 * @author xcicfh
 *
 */
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
public class EvenementExterneDAOImpl extends GenericDAOImpl<EvenementExterne, Long> implements EvenementExterneDAO {

	public EvenementExterneDAOImpl() {
		super(EvenementExterne.class);
	}

	public boolean existe(final String correlationId) {
		return (Boolean) getHibernateTemplate().executeWithNativeSession(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException, SQLException {
				Criteria criteria = session.createCriteria(EvenementExterne.class);
				criteria.setProjection(Projections.rowCount());
				criteria.add(Expression.eq("correlationId", correlationId));
				Integer count = (Integer) criteria.uniqueResult();
				return count.intValue() > 0;
			}
		});
	}

	/**
	 * {@inheritDoc}
	 */
	public EvenementExterne creerEvenementExterne(String text, String correlationId) {
		EvenementExterne evenementExterne = new EvenementExterne();
		evenementExterne.setDateEvenement(new Date());
		evenementExterne.setEtat(EtatEvenementExterne.NON_TRAITE);
		evenementExterne.setMessage(text);
		evenementExterne.setCorrelationId(correlationId);
		return evenementExterne;
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Throwable.class)
	public EvenementExterne save(EvenementExterne ev) {
		return super.save(ev);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Throwable.class)
	public void traceEvenementEnError(long id, String errorMessage) {
		EvenementExterne evenementExterne = get(id);
		evenementExterne = changeEtat(evenementExterne, EtatEvenementExterne.ERREUR);
		evenementExterne.setErrorMessage(errorMessage);
		evenementExterne.setDateTraitement(new Date());
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Throwable.class)
	public void traceEvenementTraite(long id) {
		EvenementExterne evenementExterne = get(id);
		changeEtat(evenementExterne, EtatEvenementExterne.TRAITE);
		evenementExterne.setDateTraitement(new Date());
	}

	/**
	 *
	 * @param evenementExterne
	 * @param etat
	 */
	private EvenementExterne changeEtat(EvenementExterne evenementExterne, EtatEvenementExterne etat) {
		evenementExterne.setEtat(etat);
		return evenementExterne;
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

}
