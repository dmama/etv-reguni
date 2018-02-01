package ch.vd.unireg.reqdes;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import ch.vd.unireg.common.BaseDAOImpl;

public class EvenementReqDesDAOImpl extends BaseDAOImpl<EvenementReqDes, Long> implements EvenementReqDesDAO {

	public EvenementReqDesDAOImpl() {
		super(EvenementReqDes.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<EvenementReqDes> findByNumeroMinute(String noMinute, String visaNotaire) {
		final Criteria criteria = getCurrentSession().createCriteria(getPersistentClass());
		criteria.add(Restrictions.eq("numeroMinute", noMinute));
		criteria.add(Restrictions.eq("notaire.visa", visaNotaire));
		return criteria.list();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<EvenementReqDes> findByNoAffaire(long noAffaire) {
		final Criteria criteria = getCurrentSession().createCriteria(getPersistentClass());
		criteria.add(Restrictions.eq("noAffaire", noAffaire));
		return criteria.list();
	}
}
