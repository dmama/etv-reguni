package ch.vd.uniregctb.evenement;

import java.util.Collection;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import ch.vd.uniregctb.common.BaseDAOImpl;
import ch.vd.uniregctb.tiers.Tiers;

/**
 * DAO des événements fiscaux..
 *
 * @author Akram BEN AISSI <mailto:akram.ben-aissi@vd.ch>
 *
 */
public class EvenementFiscalDAOImpl extends BaseDAOImpl<EvenementFiscal, Long> implements EvenementFiscalDAO {

	//private static final Logger LOGGER = LoggerFactory.getLogger(EvenementFiscalDAOImpl.class);

	public EvenementFiscalDAOImpl() {
		super(EvenementFiscal.class);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Collection<EvenementFiscal> getEvenementsFiscaux(final Tiers tiers)  {
		final Session session = getCurrentSession();
		final Criteria criteria = session.createCriteria(EvenementFiscal.class);
		criteria.add(Restrictions.eq("tiers", tiers));
		return criteria.list();
	}
}
