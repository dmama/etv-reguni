package ch.vd.uniregctb.validation.tiers;

import java.util.Set;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdresseCivile;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.tiers.Etablissement;

public class EtablissementValidator extends ContribuableValidator<Etablissement> {

	@Override
	protected ValidationResults validateTypeAdresses(Etablissement etb) {
		final ValidationResults results = new ValidationResults();

		final Set<AdresseTiers> adresses = etb.getAdressesTiers();
		if (adresses != null) {
			for (AdresseTiers a : adresses) {
				if (a.isAnnule()) {
					continue;
				}
				if (a instanceof AdresseCivile) {
					results.addError(String.format("L'adresse de type 'personne civile' (numéro=%d, début=%s, fin=%s) n'est pas autorisée sur un établissement.",
							a.getId(), RegDateHelper.dateToDisplayString(a.getDateDebut()), RegDateHelper.dateToDisplayString(a.getDateFin())));
				}
			}
		}

		return results;
	}

	@Override
	public Class<Etablissement> getValidatedClass() {
		return Etablissement.class;
	}
}
