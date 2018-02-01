package ch.vd.uniregctb.validation.tache;

import ch.vd.uniregctb.tiers.TacheEnvoiDeclarationImpotPM;

public class TacheEnvoiDeclarationImpotPMValidator extends TacheEnvoiDeclarationImpotValidator<TacheEnvoiDeclarationImpotPM> {

	@Override
	protected Class<TacheEnvoiDeclarationImpotPM> getValidatedClass() {
		return TacheEnvoiDeclarationImpotPM.class;
	}

}
