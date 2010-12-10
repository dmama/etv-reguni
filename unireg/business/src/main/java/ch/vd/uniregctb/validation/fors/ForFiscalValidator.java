package ch.vd.uniregctb.validation.fors;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.validation.EntityValidatorImpl;

/**
 * Classe de base pour les validateurs de fors fiscaux
 */
public abstract class ForFiscalValidator<T extends ForFiscal> extends EntityValidatorImpl<T> {

	public ValidationResults validate(T ff) {

		final ValidationResults results = new ValidationResults();

		if (ff.isAnnule()) {
			return results;
		}

		final RegDate dateDebut = ff.getDateDebut();
		final RegDate dateFin = ff.getDateFin();
		final TypeAutoriteFiscale typeAutoriteFiscale = ff.getTypeAutoriteFiscale();
		final Integer numeroOfsAutoriteFiscale = ff.getNumeroOfsAutoriteFiscale();

		// La date de début doit être renseignée
		if (dateDebut == null) {
			results.addError(String.format("Le for %s possède une date de début nulle", ff));
		}
		if (typeAutoriteFiscale == null) {
			results.addError(String.format("Le for %s n'a pas de type d'autorité fiscale", ff));
		}
		if (numeroOfsAutoriteFiscale == null) {
			results.addError(String.format("Le for %s n'a pas d'autorité fiscale renseignée", ff));
		}

		// Date de début doit être avant la date de fin
		// Si "date de début" = "date de fin", c'est un cas OK (for qui dure 1 jour)
		if (dateDebut != null && dateFin != null && dateDebut.isAfter(dateFin)) {
			results.addError(String.format("Le for %s possède une date de début qui est après la date de fin: début = %s fin = %s", ff, RegDateHelper.dateToDisplayString(dateDebut), RegDateHelper.dateToDisplayString(dateFin)));
		}

		// si c'est un for vaudois, il ne doit pas être sur une commune faîtière de fractions de commune
		if (numeroOfsAutoriteFiscale != null) {
			if (typeAutoriteFiscale == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
				if (numeroOfsAutoriteFiscale == 5871 || numeroOfsAutoriteFiscale == 5872 || numeroOfsAutoriteFiscale == 5873) {
					final String message = String.format("Le for fiscal %s ne peut pas être ouvert sur une commune faîtière de fractions de commune (ici OFS %d), une fraction est attendue dans ce cas",
														ff, numeroOfsAutoriteFiscale);
					results.addError(message);
				}
			}
		}

		return results;
	}
}
