package ch.vd.uniregctb.entreprise.complexe;

import ch.vd.uniregctb.common.DelegatingValidator;

public class ProcessusComplexeValidator extends DelegatingValidator {

	public ProcessusComplexeValidator() {
		addSubValidator(FailliteView.class, new FailliteViewValidator());                       // faillite
		addSubValidator(DemenagementSiegeView.class, new DemenagementSiegeViewValidator());     // déménagement de siège
		addSubValidator(FinActiviteView.class, new FinActiviteViewValidator());                 // fin d'activité
		addSubValidator(FusionEntreprisesView.class, new FusionEntreprisesViewValidator());     // fusion d'entreprises
		addSubValidator(ScissionEntrepriseView.class, new ScissionEntrepriseViewValidator());   // scission d'entreprise
		addSubValidator(TransfertPatrimoineView.class, new TransfertPatrimoineViewValidator()); // transfert de patrimoine
		addSubValidator(ReinscriptionRCView.class, new ReinscriptionRCViewValidator());         // ré-inscription au RC
		addSubValidator(RequisitionRadiationRCView.class, new RequisitionRadiationRCViewValidator());   // réquisition de radiation du RC

		addSubValidator(AnnulationDemenagementSiegeView.class, new AnnulationDemenagementSiegeViewValidator());     // annulation de déménagement de siège
	}
}
