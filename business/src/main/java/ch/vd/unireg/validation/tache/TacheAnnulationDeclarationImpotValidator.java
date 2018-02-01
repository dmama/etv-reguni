package ch.vd.unireg.validation.tache;

import ch.vd.unireg.tiers.TacheAnnulationDeclarationImpot;

public class TacheAnnulationDeclarationImpotValidator extends TacheValidator<TacheAnnulationDeclarationImpot> {

	@Override
	protected Class<TacheAnnulationDeclarationImpot> getValidatedClass() {
		return TacheAnnulationDeclarationImpot.class;
	}
}
