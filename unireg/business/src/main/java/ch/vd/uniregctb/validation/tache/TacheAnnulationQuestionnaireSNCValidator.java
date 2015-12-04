package ch.vd.uniregctb.validation.tache;

import ch.vd.uniregctb.tiers.TacheAnnulationQuestionnaireSNC;

public class TacheAnnulationQuestionnaireSNCValidator extends TacheValidator<TacheAnnulationQuestionnaireSNC> {

	@Override
	protected Class<TacheAnnulationQuestionnaireSNC> getValidatedClass() {
		return TacheAnnulationQuestionnaireSNC.class;
	}
}
