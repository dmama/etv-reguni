package ch.vd.unireg.validation.tache;

import org.jetbrains.annotations.NotNull;

import ch.vd.shared.validation.ValidationHelper;
import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.tiers.TacheEnvoiQuestionnaireSNC;

public class TacheEnvoiQuestionnaireSNCValidator extends TacheEnvoiDocumentValidator<TacheEnvoiQuestionnaireSNC> {

	@Override
	protected Class<TacheEnvoiQuestionnaireSNC> getValidatedClass() {
		return TacheEnvoiQuestionnaireSNC.class;
	}

	@NotNull
	@Override
	public ValidationResults validate(@NotNull TacheEnvoiQuestionnaireSNC tache) {
		final ValidationResults vr = super.validate(tache);
		if (!tache.isAnnule()) {
			ValidationHelper.validate(tache, false, false, vr);
		}
		return vr;
	}
}
