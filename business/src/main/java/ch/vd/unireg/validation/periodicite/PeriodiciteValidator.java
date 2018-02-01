package ch.vd.unireg.validation.periodicite;

import ch.vd.registre.base.validation.ValidationHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.declaration.Periodicite;
import ch.vd.unireg.type.PeriodiciteDecompte;
import ch.vd.unireg.validation.EntityValidatorImpl;

public class PeriodiciteValidator extends EntityValidatorImpl<Periodicite> {

	@Override
	protected Class<Periodicite> getValidatedClass() {
		return Periodicite.class;
	}

	@Override
	public ValidationResults validate(Periodicite periodicite) {
		return validatePeriodicite(periodicite);
	}

	public static ValidationResults validatePeriodicite(Periodicite periodicite) {
		final ValidationResults results = new ValidationResults();

		if (!periodicite.isAnnule()) {
			ValidationHelper.validate(periodicite, false, true, results);

			final PeriodiciteDecompte periodiciteDecompte = periodicite.getPeriodiciteDecompte();
			if (periodiciteDecompte == null) {
				results.addError("La périodicité de décompte doit être renseignée.");
			}
			if (periodiciteDecompte == PeriodiciteDecompte.UNIQUE && periodicite.getPeriodeDecompte() == null) {
				results.addError("La période de décompte doit être renseignée lorsque la périodicité de décompte est UNIQUE.");
			}
		}
		return results;
	}
}
