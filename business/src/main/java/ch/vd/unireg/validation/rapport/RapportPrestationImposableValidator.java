package ch.vd.unireg.validation.rapport;

import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.tiers.RapportPrestationImposable;

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

	/**
	 * [SIFISC-16659] Les rapports de travail peuvent être fermés dans le futur (tout comme le for du débiteur, en fait...)
	 */
	@Override
	protected boolean isDateFinFutureAllowed() {
		return true;
	}
}
