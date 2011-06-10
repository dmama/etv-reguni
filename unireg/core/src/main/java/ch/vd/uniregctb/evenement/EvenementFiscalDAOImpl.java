package ch.vd.uniregctb.evenement;

import java.sql.SQLException;
import java.util.Collection;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.orm.hibernate3.HibernateCallback;

import ch.vd.registre.base.dao.GenericDAOImpl;
import ch.vd.uniregctb.tiers.Tiers;

/**
 * DAO des événements fiscaux..
 *
 * @author Akram BEN AISSI <mailto:akram.ben-aissi@vd.ch>
 *
 */
public class EvenementFiscalDAOImpl extends GenericDAOImpl<EvenementFiscal, Long> implements EvenementFiscalDAO {

	//private static final Logger LOGGER = Logger.getLogger(EvenementFiscalDAOImpl.class);

	public EvenementFiscalDAOImpl() {
		super(EvenementFiscal.class);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Collection<EvenementFiscal> getEvenementFiscals(final Tiers tiers)  {
		return getHibernateTemplate().executeWithNativeSession(new HibernateCallback<Collection<EvenementFiscal>>() {
			@Override
			public Collection<EvenementFiscal> doInHibernate(Session session) throws HibernateException, SQLException {
				final Criteria criteria = session.createCriteria(EvenementFiscal.class);
				criteria.add(Restrictions.eq("tiers", tiers));
				return criteria.list();
			}
		});
	}
}
