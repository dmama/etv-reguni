package ch.vd.uniregctb.validation.decision;

import ch.vd.uniregctb.tiers.DecisionAci;
import ch.vd.uniregctb.validation.tiers.LocalisationDateeValidator;

/**
 * Classe de  validation des décisions ACI
 */
public class DecisionValidator extends LocalisationDateeValidator<DecisionAci> {

	@Override
	protected Class<DecisionAci> getValidatedClass() {
		return DecisionAci.class;
	}

	@Override
	protected String getEntityCategoryName() {
		return "La décision ACI";
	}
}
