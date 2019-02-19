package ch.vd.unireg.validation.tache;

import org.jetbrains.annotations.NotNull;

import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.tiers.TacheEnvoiDeclarationImpotPP;

public class TacheEnvoiDeclarationImpotPPValidator extends TacheEnvoiDeclarationImpotValidator<TacheEnvoiDeclarationImpotPP> {

	@Override
	protected Class<TacheEnvoiDeclarationImpotPP> getValidatedClass() {
		return TacheEnvoiDeclarationImpotPP.class;
	}

	@Override
	@NotNull
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
