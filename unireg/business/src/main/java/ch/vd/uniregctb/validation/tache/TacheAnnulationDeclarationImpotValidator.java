package ch.vd.uniregctb.validation.tache;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.tiers.TacheAnnulationDeclarationImpot;

public class TacheAnnulationDeclarationImpotValidator extends TacheValidator<TacheAnnulationDeclarationImpot> {

	@Override
	protected Class<TacheAnnulationDeclarationImpot> getValidatedClass() {
		return TacheAnnulationDeclarationImpot.class;
	}

	@Override
	public ValidationResults validate(TacheAnnulationDeclarationImpot tache) {
		final ValidationResults vr = super.validate(tache);
		if (!tache.isAnnule()) {
			if (tache.getDeclarationImpotOrdinaire() == null) {
				vr.addError("La déclaration associée à la tâche d'annulation ne peut pas être nulle.");
			}
		}
		return vr;
	}
}
