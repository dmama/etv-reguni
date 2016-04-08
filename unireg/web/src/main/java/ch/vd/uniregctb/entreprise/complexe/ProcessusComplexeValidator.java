package ch.vd.uniregctb.entreprise.complexe;

import ch.vd.uniregctb.common.DelegatingValidator;
import ch.vd.uniregctb.tiers.validator.TiersCriteriaValidator;
import ch.vd.uniregctb.tiers.view.TiersCriteriaView;

public class ProcessusComplexeValidator extends DelegatingValidator {

	public ProcessusComplexeValidator() {
		// recherche de tiers
		addSubValidator(TiersCriteriaView.class, new TiersCriteriaValidator());

		// faillite
		addSubValidator(FailliteView.class, new FailliteViewValidator());
	}
}
