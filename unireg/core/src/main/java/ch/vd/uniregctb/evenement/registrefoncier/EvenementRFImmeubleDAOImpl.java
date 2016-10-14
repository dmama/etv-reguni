package ch.vd.uniregctb.evenement.registrefoncier;

import ch.vd.uniregctb.common.BaseDAOImpl;

public class EvenementRFImmeubleDAOImpl extends BaseDAOImpl<EvenementRFImmeuble, Long> implements EvenementRFImmeubleDAO {
	protected EvenementRFImmeubleDAOImpl() {
		super(EvenementRFImmeuble.class);
	}
}
