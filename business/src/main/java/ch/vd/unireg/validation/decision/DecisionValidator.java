package ch.vd.unireg.validation.decision;

import ch.vd.unireg.tiers.DecisionAci;
import ch.vd.unireg.validation.tiers.LocalisationDateeValidator;

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
