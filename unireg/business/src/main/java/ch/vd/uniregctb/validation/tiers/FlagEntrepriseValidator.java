package ch.vd.uniregctb.validation.tiers;

import ch.vd.registre.base.date.DateConstants;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.tiers.FlagEntreprise;
import ch.vd.uniregctb.validation.EntityValidatorImpl;

public class FlagEntrepriseValidator extends EntityValidatorImpl<FlagEntreprise> {

	@Override
	protected Class<FlagEntreprise> getValidatedClass() {
		return FlagEntreprise.class;
	}

	@Override
	public ValidationResults validate(FlagEntreprise flag) {
		final ValidationResults vr = new ValidationResults();
		if (!flag.isAnnule()) {

			final int minYear = DateConstants.EXTENDED_VALIDITY_RANGE.getDateDebut().year();
			final int maxYear = DateConstants.EXTENDED_VALIDITY_RANGE.getDateFin().year();
			Assert.isTrue(minYear >= 0 || minYear <= 9999);
			Assert.isTrue(maxYear >= 0 || maxYear <= 9999);
			Assert.isTrue(maxYear >= minYear);

			// l'année date de début est obligatoire
			if (flag.getAnneeDebutValidite() == null) {
				vr.addError("L'année de début de validité est obligatoire sur un flag entreprise.");
			}
			else {

				// l'année de début doit être un nombre positif à 4 chiffres (= une année, quoi...)
				if (flag.getAnneeDebutValidite() < minYear || flag.getAnneeDebutValidite() > maxYear) {
					vr.addError("L'année de début de validité d'un flag entreprise doit être comprise entre " + minYear + " et " + maxYear + " (trouvé " + flag.getAnneeDebutValidite() + ").");
				}

				// l'année de fin n'est pas obligatoire, mais si elle est présente, elle doit
				// être postérieure ou égale à l'annee de début
				if (flag.getAnneeFinValidite() != null && flag.getAnneeDebutValidite().compareTo(flag.getAnneeFinValidite()) > 0) {
					vr.addError(String.format("L'année de fin de validité (%d) d'un flag entreprise doit être postérieure ou égale à son année de début de validité (%d).",
					                          flag.getAnneeFinValidite(), flag.getAnneeDebutValidite()));
				}
			}

			// l'année de fin, si présente, doit être un nombre positif de 4 chiffres (= une année, quoi...)
			if (flag.getAnneeFinValidite() != null && (flag.getAnneeFinValidite() < minYear || flag.getAnneeFinValidite() > maxYear)) {
				vr.addError("L'année de fin de validité d'un flag entreprise doit être comprise entre " + minYear + " et " + maxYear + " (trouvé " + flag.getAnneeFinValidite() + ").");
			}

			// le type est obligatoire
			if (flag.getType() == null) {
				vr.addError("Le type de flag entreprise est obligatoire.");
			}
		}
		return vr;
	}
}
