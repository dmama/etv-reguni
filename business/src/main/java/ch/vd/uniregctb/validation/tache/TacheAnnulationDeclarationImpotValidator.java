package ch.vd.uniregctb.validation.tache;

import ch.vd.uniregctb.tiers.TacheAnnulationDeclarationImpot;

public class TacheAnnulationDeclarationImpotValidator extends TacheValidator<TacheAnnulationDeclarationImpot> {

	@Override
	protected Class<TacheAnnulationDeclarationImpot> getValidatedClass() {
		return TacheAnnulationDeclarationImpot.class;
	}
}
