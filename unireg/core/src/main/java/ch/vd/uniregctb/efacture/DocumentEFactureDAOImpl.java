package ch.vd.uniregctb.efacture;


import org.hibernate.Query;

import ch.vd.uniregctb.common.BaseDAOImpl;

public class DocumentEFactureDAOImpl extends BaseDAOImpl<DocumentEFacture, Long> implements DocumentEFactureDAO {

	public DocumentEFactureDAOImpl() {
		super(DocumentEFacture.class);
	}

	@Override
	public DocumentEFacture findByTiersEtCleArchivage(long tiersId, String cleArchivage) {
		final String hql = "FROM DocumentEFacture d WHERE d.tiers.id=:tiersId AND d.cleArchivage=:cleArchivage";
		final Query query = getCurrentSession().createQuery(hql);
		query.setParameter("tiersId", tiersId);
		query.setParameter("cleArchivage", cleArchivage);
		return (DocumentEFacture) query.uniqueResult();
	}
}
