package ch.vd.uniregctb.tiers.dao;

import ch.vd.uniregctb.common.BaseDAOImpl;
import ch.vd.uniregctb.tiers.DomicileEtablissement;

public class DomicileEtablissementDAOImpl extends BaseDAOImpl<DomicileEtablissement, Long> implements DomicileEtablissementDAO {

	public DomicileEtablissementDAOImpl() {
		super(DomicileEtablissement.class);
	}
}
