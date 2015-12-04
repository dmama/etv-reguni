package ch.vd.uniregctb.validation.rapport;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.tiers.RapportPrestationImposable;

public class RapportPrestationImposableValidator extends RapportEntreTiersValidator<RapportPrestationImposable> {

	@Override
	protected Class<RapportPrestationImposable> getValidatedClass() {
		return RapportPrestationImposable.class;
	}

	@Override
	protected void verificationClasses(ValidationResults vr, RapportPrestationImposable ret) {
		// [SIFISC-12245] si c'est pour ne tout de façon pas tester les classes de chaque côté de la relation, autant ne pas aller chercher les tiers
		// (qui peuvent être très nombreux, ce qui prends alors beaucoup de temps)
	}
}
