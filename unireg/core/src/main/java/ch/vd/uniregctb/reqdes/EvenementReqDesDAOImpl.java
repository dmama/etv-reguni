package ch.vd.uniregctb.reqdes;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import ch.vd.uniregctb.common.BaseDAOImpl;

public class EvenementReqDesDAOImpl extends BaseDAOImpl<EvenementReqDes, Long> implements EvenementReqDesDAO {

	public EvenementReqDesDAOImpl() {
		super(EvenementReqDes.class);
	}

	@Override
	public EvenementReqDes findByNumeroMinute(long noMinute) {
		final Criteria criteria = getCurrentSession().createCriteria(getPersistentClass());
		criteria.add(Restrictions.eq("numeroMinute", noMinute));
		return (EvenementReqDes) criteria.uniqueResult();
	}
}
