package ch.vd.unireg.reqdes;

import java.util.List;

import org.hibernate.query.Query;

import ch.vd.unireg.common.BaseDAOImpl;

public class EvenementReqDesDAOImpl extends BaseDAOImpl<EvenementReqDes, Long> implements EvenementReqDesDAO {

	public EvenementReqDesDAOImpl() {
		super(EvenementReqDes.class);
	}

	@Override
	public List<EvenementReqDes> findByNumeroMinute(String noMinute, String visaNotaire) {
		final Query query = getCurrentSession().createQuery("from EvenementReqDes where numeroMinute = :numeroMinute and notaire.visa = :visa");
		query.setParameter("numeroMinute", noMinute);
		query.setParameter("visa", visaNotaire);
		//noinspection unchecked
		return query.list();
	}

	@Override
	public List<EvenementReqDes> findByNoAffaire(long noAffaire) {
		final Query query = getCurrentSession().createQuery("from EvenementReqDes where noAffaire = :noAffaire");
		query.setParameter("noAffaire", noAffaire);
		//noinspection unchecked
		return query.list();
	}
}
