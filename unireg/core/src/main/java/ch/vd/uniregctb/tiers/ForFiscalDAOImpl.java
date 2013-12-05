package ch.vd.uniregctb.tiers;

import ch.vd.registre.base.dao.GenericDAOImpl;

public class ForFiscalDAOImpl extends GenericDAOImpl<ForFiscal, Long> implements ForFiscalDAO {

	public ForFiscalDAOImpl() {
		super(ForFiscal.class);
	}

}
