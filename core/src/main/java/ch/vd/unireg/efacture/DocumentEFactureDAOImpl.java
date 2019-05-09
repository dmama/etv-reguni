package ch.vd.unireg.efacture;


import org.hibernate.query.Query;

import ch.vd.unireg.common.BaseDAOImpl;

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
