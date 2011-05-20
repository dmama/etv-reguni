package ch.vd.uniregctb.evenement;

import ch.vd.registre.base.dao.GenericDAOImpl;
import ch.vd.uniregctb.tiers.Tiers;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.orm.hibernate3.HibernateCallback;

import java.sql.SQLException;
import java.util.Collection;

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
	@SuppressWarnings("unchecked")
	public Collection<EvenementFiscal> getEvenementFiscals(final Tiers tiers)  {
		return getHibernateTemplate().executeWithNativeSession(new HibernateCallback<Collection<EvenementFiscal>>() {
			public Collection<EvenementFiscal> doInHibernate(Session session) throws HibernateException, SQLException {
				final Criteria criteria = session.createCriteria(EvenementFiscal.class);
				criteria.add(Restrictions.eq("tiers", tiers));
				return criteria.list();
			}
		});
	}
}
