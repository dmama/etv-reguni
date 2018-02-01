package ch.vd.unireg.validation.tiers;

import java.util.Set;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.adresse.AdresseCivile;
import ch.vd.unireg.adresse.AdressePM;
import ch.vd.unireg.adresse.AdresseTiers;
import ch.vd.unireg.tiers.CollectiviteAdministrative;

public class CollectiviteAdministrativeValidator extends ContribuableValidator<CollectiviteAdministrative> {

	@Override
	protected ValidationResults validateTypeAdresses(CollectiviteAdministrative ca) {

		final ValidationResults results = new ValidationResults();

		final Set<AdresseTiers> adresses = ca.getAdressesTiers();
		if (adresses != null) {
			for (AdresseTiers a : adresses) {
				if (a.isAnnule()) {
					continue;
				}
				if (a instanceof AdressePM) {
					results.addError(String.format("L'adresse de type 'personne morale' (numéro=%d, début=%s, fin=%s) n'est pas autorisée sur une collectivité administrative.",
							a.getId(), RegDateHelper.dateToDisplayString(a.getDateDebut()), RegDateHelper.dateToDisplayString(a.getDateFin())));
				}
				else if (a instanceof AdresseCivile) {
					results.addError(String.format("L'adresse de type 'personne civile' (numéro=%d, début=%s, fin=%s) n'est pas autorisée sur une collectivité administrative.",
							a.getId(), RegDateHelper.dateToDisplayString(a.getDateDebut()), RegDateHelper.dateToDisplayString(a.getDateFin())));
				}
			}
		}

		return results;
	}

	@Override
	public Class<CollectiviteAdministrative> getValidatedClass() {
		return CollectiviteAdministrative.class;
	}
}
