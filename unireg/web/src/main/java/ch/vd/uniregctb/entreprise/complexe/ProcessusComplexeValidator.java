package ch.vd.uniregctb.entreprise.complexe;

import ch.vd.uniregctb.common.DelegatingValidator;
import ch.vd.uniregctb.tiers.validator.TiersCriteriaValidator;
import ch.vd.uniregctb.tiers.view.TiersCriteriaView;

public class ProcessusComplexeValidator extends DelegatingValidator {

	public ProcessusComplexeValidator() {
		// recherche de tiers
		addSubValidator(TiersCriteriaView.class, new TiersCriteriaValidator());

		addSubValidator(FailliteView.class, new FailliteViewValidator());                       // faillite
		addSubValidator(DemenagementSiegeView.class, new DemenagementSiegeViewValidator());     // déménagement de siège
		addSubValidator(FinActiviteView.class, new FinActiviteViewValidator());                 // fin d'activité
		addSubValidator(FusionEntreprisesView.class, new FusionEntreprisesViewValidator());     // fusion d'entreprises
		addSubValidator(ScissionEntrepriseView.class, new ScissionEntrepriseViewValidator());   // scission d'entreprise

		addSubValidator(AnnulationDemenagementSiegeView.class, new AnnulationDemenagementSiegeViewValidator());     // annulation de déménagement de siège
	}
}
