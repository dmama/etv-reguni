package ch.vd.unireg.validation.tache;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.tiers.TacheEnvoiDeclarationImpotPP;

public class TacheEnvoiDeclarationImpotPPValidator extends TacheEnvoiDeclarationImpotValidator<TacheEnvoiDeclarationImpotPP> {

	@Override
	protected Class<TacheEnvoiDeclarationImpotPP> getValidatedClass() {
		return TacheEnvoiDeclarationImpotPP.class;
	}

	@Override
	public ValidationResults validate(TacheEnvoiDeclarationImpotPP tache) {
		final ValidationResults vr = super.validate(tache);
		if (!tache.isAnnule()) {
			if (tache.getTypeContribuable() == null) {
				vr.addError("Le type de contribuable ne peut pas être nul.");
			}
		}
		return vr;
	}
}
