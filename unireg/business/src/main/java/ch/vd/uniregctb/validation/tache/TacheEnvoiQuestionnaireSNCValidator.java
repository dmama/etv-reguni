package ch.vd.uniregctb.validation.tache;

import ch.vd.registre.base.validation.ValidationHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.tiers.TacheEnvoiQuestionnaireSNC;

public class TacheEnvoiQuestionnaireSNCValidator extends TacheEnvoiDocumentValidator<TacheEnvoiQuestionnaireSNC> {

	@Override
	protected Class<TacheEnvoiQuestionnaireSNC> getValidatedClass() {
		return TacheEnvoiQuestionnaireSNC.class;
	}

	@Override
	public ValidationResults validate(TacheEnvoiQuestionnaireSNC tache) {
		final ValidationResults vr = super.validate(tache);
		if (!tache.isAnnule()) {
			ValidationHelper.validate(tache, false, false, vr);
		}
		return vr;
	}
}
