package ch.vd.uniregctb.validation.tache;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.tiers.TacheEnvoiDeclarationImpot;

public class TacheEnvoiDeclarationImpotValidator extends TacheValidator<TacheEnvoiDeclarationImpot> {

	@Override
	protected Class<TacheEnvoiDeclarationImpot> getValidatedClass() {
		return TacheEnvoiDeclarationImpot.class;
	}

	@Override
	public ValidationResults validate(TacheEnvoiDeclarationImpot tache) {
		final ValidationResults vr = super.validate(tache);
		if (!tache.isAnnule()) {
			DateRangeHelper.validate(tache, false, false, vr);

			if (tache.getTypeContribuable() == null) {
				vr.addError("Le type de contribuable ne peut pas être nul.");
			}

			if (tache.getTypeDocument() == null) {
				vr.addError("Le type de document ne peut pas être nul.");
			}
		}
		return vr;
	}
}
