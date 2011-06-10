package ch.vd.uniregctb.validation.tiers;

import java.util.Set;

import org.apache.commons.lang.StringUtils;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdresseCivile;
import ch.vd.uniregctb.adresse.AdressePM;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.tiers.AutreCommunaute;

public class AutreCommunauteValidator extends ContribuableValidator<AutreCommunaute> {

	@Override
	public ValidationResults validate(AutreCommunaute communaute) {
		final ValidationResults vr = super.validate(communaute);
		if (!communaute.isAnnule()) {
			final String nom = communaute.getNom();
			if (StringUtils.isBlank(nom)) {
				vr.addError("Le nom est un attribut obligatoire");
			}
		}
		return vr;
	}

	@Override
	protected ValidationResults validateTypeAdresses(AutreCommunaute communaute) {

		final ValidationResults results = new ValidationResults();

		final Set<AdresseTiers> adresses = communaute.getAdressesTiers();
		if (adresses != null) {
			for (AdresseTiers a : adresses) {
				if (a.isAnnule()) {
					continue;
				}
				if (a instanceof AdressePM) {
					results.addError(String.format("L'adresse de type 'personne morale' (numéro=%d, début=%s, fin=%s) n'est pas autorisée sur une autre communauté.",
							a.getId(), RegDateHelper.dateToDisplayString(a.getDateDebut()), RegDateHelper.dateToDisplayString(a.getDateFin())));
				}
				else if (a instanceof AdresseCivile) {
					results.addError(String.format("L'adresse de type 'personne civile' (numéro=%d, début=%s, fin=%s) n'est pas autorisée sur une autre communauté.",
							a.getId(), RegDateHelper.dateToDisplayString(a.getDateDebut()), RegDateHelper.dateToDisplayString(a.getDateFin())));
				}
			}
		}

		return results;
	}

	@Override
	public Class<AutreCommunaute> getValidatedClass() {
		return AutreCommunaute.class;
	}
}
