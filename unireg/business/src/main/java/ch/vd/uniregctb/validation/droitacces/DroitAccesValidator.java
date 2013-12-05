package ch.vd.uniregctb.validation.droitacces;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.tiers.DroitAcces;
import ch.vd.uniregctb.validation.EntityValidatorImpl;

public class DroitAccesValidator extends EntityValidatorImpl<DroitAcces> {

	@Override
	protected Class<DroitAcces> getValidatedClass() {
		return DroitAcces.class;
	}

	@Override
	public ValidationResults validate(DroitAcces da) {
		final  ValidationResults vr = new ValidationResults();

		if (!da.isAnnule()) {

			final RegDate dateDebut = da.getDateDebut();
			final RegDate dateFin = da.getDateFin();

			// La date de début doit être renseignée
			if (dateDebut == null) {
				vr.addError(String.format("Le droit d'accès %s possède une date de début nulle", da));
			}

			// Date de début doit être avant ou égale à la date de fin
			if (dateDebut != null && dateFin != null && dateDebut.isAfter(dateFin)) {
				vr.addError(String.format("Le droit d'accès %s possède une date de début qui est après la date de fin: début = %s fin = %s",
						da, RegDateHelper.dateToDisplayString(dateDebut), RegDateHelper.dateToDisplayString(dateFin)));
			}
		}
		return vr;
	}
}
