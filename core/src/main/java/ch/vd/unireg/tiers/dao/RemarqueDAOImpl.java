package ch.vd.unireg.tiers.dao;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;

import ch.vd.unireg.common.BaseDAOImpl;
import ch.vd.unireg.tiers.Remarque;

public class RemarqueDAOImpl extends BaseDAOImpl<Remarque, Long> implements RemarqueDAO {

	public RemarqueDAOImpl() {
		super(Remarque.class);
	}

	@Override
	public List<Remarque> getRemarques(final Long tiersId) {
		final Session session = getCurrentSession();
		final Query query = session.createQuery("from Remarque r where r.tiers.id = :tiersId");
		query.setParameter("tiersId", tiersId);
		//noinspection unchecked
		return query.list();
	}
}
