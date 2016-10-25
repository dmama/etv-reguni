package ch.vd.uniregctb.evenement.registrefoncier;

import ch.vd.uniregctb.common.BaseDAOImpl;

public class EvenementRFMutationDAOImpl extends BaseDAOImpl<EvenementRFMutation, Long> implements EvenementRFMutationDAO {
	protected EvenementRFMutationDAOImpl() {
		super(EvenementRFMutation.class);
	}
}
