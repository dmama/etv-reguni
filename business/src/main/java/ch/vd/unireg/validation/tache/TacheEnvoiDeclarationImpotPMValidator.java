package ch.vd.unireg.validation.tache;

import ch.vd.unireg.tiers.TacheEnvoiDeclarationImpotPM;

public class TacheEnvoiDeclarationImpotPMValidator extends TacheEnvoiDeclarationImpotValidator<TacheEnvoiDeclarationImpotPM> {

	@Override
	protected Class<TacheEnvoiDeclarationImpotPM> getValidatedClass() {
		return TacheEnvoiDeclarationImpotPM.class;
	}

}
