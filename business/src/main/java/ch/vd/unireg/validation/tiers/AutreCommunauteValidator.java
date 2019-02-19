package ch.vd.unireg.validation.tiers;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.adresse.AdresseCivile;
import ch.vd.unireg.adresse.AdressePM;
import ch.vd.unireg.adresse.AdresseTiers;
import ch.vd.unireg.tiers.AutreCommunaute;

public class AutreCommunauteValidator extends ContribuableImpositionPersonnesMoralesValidator<AutreCommunaute> {

	@Override
	@NotNull
	public ValidationResults validate(@NotNull AutreCommunaute communaute) {
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
