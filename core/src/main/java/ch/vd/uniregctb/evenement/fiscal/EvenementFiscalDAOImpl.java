package ch.vd.uniregctb.evenement.fiscal;

import java.util.Collection;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import ch.vd.uniregctb.common.BaseDAOImpl;
import ch.vd.uniregctb.tiers.Tiers;

public class EvenementFiscalDAOImpl extends BaseDAOImpl<EvenementFiscal, Long> implements EvenementFiscalDAO {

	public EvenementFiscalDAOImpl() {
		super(EvenementFiscal.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Collection<EvenementFiscal> getEvenementsFiscaux(Tiers tiers) {
		final Session session = getCurrentSession();
		final Criteria criteria = session.createCriteria(EvenementFiscal.class);
		criteria.add(Restrictions.eq("tiers", tiers));
		return criteria.list();
	}
}
