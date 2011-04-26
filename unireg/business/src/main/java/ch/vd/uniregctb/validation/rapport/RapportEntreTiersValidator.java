package ch.vd.uniregctb.validation.rapport;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.validation.EntityValidatorImpl;

public abstract class RapportEntreTiersValidator<T extends RapportEntreTiers> extends EntityValidatorImpl<T> {

	public ValidationResults validate(T ret) {
		final ValidationResults vr = new ValidationResults();

		if (!ret.isAnnule()) {

			final RegDate dateDebut = ret.getDateDebut();
			final RegDate dateFin = ret.getDateFin();

			// La date de début doit être renseignée
			if (dateDebut == null) {
				vr.addError(String.format("Le rapport-entre-tiers %s possède une date de début nulle", ret));
			}

			// Date de début doit être avant (ou égale) la date de fin
			if (dateDebut != null && dateFin != null && dateDebut.isAfter(dateFin)) {
				vr.addError(String.format("Le rapport-entre-tiers %s possède une date de début qui est après la date de fin: début = %s fin = %s",
						ret, RegDateHelper.dateToDisplayString(dateDebut), RegDateHelper.dateToDisplayString(dateFin)));
			}
		}

		return vr;
	}
}
