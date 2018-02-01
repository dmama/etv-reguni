package ch.vd.unireg.tiers.dao;

import ch.vd.unireg.common.BaseDAOImpl;
import ch.vd.unireg.tiers.DomicileEtablissement;

public class DomicileEtablissementDAOImpl extends BaseDAOImpl<DomicileEtablissement, Long> implements DomicileEtablissementDAO {

	public DomicileEtablissementDAOImpl() {
		super(DomicileEtablissement.class);
	}
}
