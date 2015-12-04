package ch.vd.uniregctb.validation.adresse;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.validation.EntityValidatorImpl;

public abstract class AdresseTiersValidator<T extends AdresseTiers> extends EntityValidatorImpl<T> {

	@Override
	public ValidationResults validate(T adr) {
		final ValidationResults vr = new ValidationResults();
		if (!adr.isAnnule()) {

			// L'usage doit être renseigné
			if (adr.getUsage() == null) {
				vr.addError(String.format("L'adresse %s possède un usage nul", adr));
			}

			// La date de début doit être renseignée
			if (adr.getDateDebut() == null) {
				vr.addError(String.format("L'adresse %s possède une date de début nulle", adr));
			}

			// Date de début doit être avant ou égale à la date de fin
			final RegDate dateDebut = adr.getDateDebut();
			final RegDate dateFin = adr.getDateFin();
			if (dateDebut != null && dateFin != null && dateDebut.isAfter(dateFin)) {
				vr.addError(String.format("L'adresse %s possède une date de début qui est après la date de fin: début = %s fin = %s",
						adr, RegDateHelper.dateToDisplayString(dateDebut), RegDateHelper.dateToDisplayString(dateFin)));
			}
		}

		return vr;
	}
}
