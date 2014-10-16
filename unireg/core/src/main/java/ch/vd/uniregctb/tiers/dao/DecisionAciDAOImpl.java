package ch.vd.uniregctb.tiers.dao;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;

import ch.vd.uniregctb.common.BaseDAOImpl;
import ch.vd.uniregctb.tiers.DecisionAci;

public class DecisionAciDAOImpl extends BaseDAOImpl<DecisionAci, Long> implements DecisionAciDAO {
	protected DecisionAciDAOImpl() {
		super(DecisionAci.class);
	}

	/**
	 * $ Retourne les décisions aci associés à un tiers
	 *
	 * @param tiersId numéro du tiers pour qui on veut récupérer les décision aci
	 * @return la liste des décisions aci
	 */
	@Override
	public List<DecisionAci> getDecisionsAci(Long tiersId) {
		final Session session = getCurrentSession();
		final Query query = session.createQuery("from DecisionAci d where d.tiers.id = :tiersId");
		query.setParameter("tiersId", tiersId);
		//noinspection unchecked
		return query.list();
	}
}
