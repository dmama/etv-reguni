package ch.vd.unireg.validation.tache;

import ch.vd.unireg.tiers.TacheAnnulationQuestionnaireSNC;

public class TacheAnnulationQuestionnaireSNCValidator extends TacheValidator<TacheAnnulationQuestionnaireSNC> {

	@Override
	protected Class<TacheAnnulationQuestionnaireSNC> getValidatedClass() {
		return TacheAnnulationQuestionnaireSNC.class;
	}
}
