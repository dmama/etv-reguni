package ch.vd.unireg.evenement.fiscal;

import java.util.Collection;

import org.hibernate.query.Query;

import ch.vd.unireg.common.BaseDAOImpl;
import ch.vd.unireg.tiers.Tiers;

public class EvenementFiscalDAOImpl extends BaseDAOImpl<EvenementFiscal, Long> implements EvenementFiscalDAO {

	public EvenementFiscalDAOImpl() {
		super(EvenementFiscal.class);
	}

	@Override
	public Collection<EvenementFiscal> getEvenementsFiscaux(Tiers tiers) {
		final Query query = getCurrentSession().createQuery("from EvenementFiscalTiers where tiers = :tiers");
		query.setParameter("tiers", tiers);
		//noinspection unchecked
		return query.list();
	}
}
