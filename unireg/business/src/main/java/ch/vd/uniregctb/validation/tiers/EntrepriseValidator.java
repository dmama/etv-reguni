package ch.vd.uniregctb.validation.tiers;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdresseCivile;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.tiers.DonneesRegistreCommerce;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.RegimeFiscal;

public class EntrepriseValidator extends ContribuableImpositionPersonnesMoralesValidator<Entreprise> {

	@Override
	public ValidationResults validate(Entreprise entreprise) {
		final ValidationResults vr = super.validate(entreprise);
		if (!entreprise.isAnnule()) {
			vr.merge(validateRegimesFiscaux(entreprise));
			vr.merge(validateDonneesRegistreCommerce(entreprise));
		}
		return vr;
	}

	@Override
	protected ValidationResults validateTypeAdresses(Entreprise entreprise) {
		final ValidationResults results = new ValidationResults();
		final Set<AdresseTiers> adresses = entreprise.getAdressesTiers();
		if (adresses != null) {
			for (AdresseTiers a : adresses) {
				if (a.isAnnule()) {
					continue;
				}
				if (a instanceof AdresseCivile) {
					results.addError(String.format("L'adresse de type 'personne civile' (numéro=%d, début=%s, fin=%s) n'est pas autorisée sur une entreprise.",
							a.getId(), RegDateHelper.dateToDisplayString(a.getDateDebut()), RegDateHelper.dateToDisplayString(a.getDateFin())));
				}
			}
		}

		return results;
	}

	protected ValidationResults validateDonneesRegistreCommerce(Entreprise entreprise) {
		final ValidationResults vr = new ValidationResults();

		final List<DonneesRegistreCommerce> donnees = entreprise.getDonneesRegistreCommerceNonAnnuleesTriees();

		// on valide d'abord les données pour elles-mêmes
		for (DonneesRegistreCommerce d : donnees) {
			vr.merge(getValidationService().validate(d));
		}

		// ... puis entre elles (il ne doit y avoir, à tout moment, au plus qu'une seule instance active)
		final int size = donnees.size();
		if (size > 1) {
			final List<DateRange> overlaps = DateRangeHelper.overlaps(donnees);
			if (overlaps != null && !overlaps.isEmpty()) {
				for (DateRange overlap : overlaps) {
					vr.addError(String.format("La période %s est couverte par plusieurs ensembles de données RC", DateRangeHelper.toDisplayString(overlap)));
				}
			}
		}

		return vr;
	}

	protected ValidationResults validateRegimesFiscaux(Entreprise entreprise) {
		final ValidationResults vr = new ValidationResults();

		final List<RegimeFiscal> regimesFiscaux = entreprise.getRegimesFiscauxNonAnnulesTries();

		// on valide les régimes fiscaux pour eux-mêmes...
		for (RegimeFiscal rf : regimesFiscaux) {
			vr.merge(getValidationService().validate(rf));
		}

		// ... puis entre eux (il ne doit y avoir, à tout moment, au plus qu'un seul régime fiscal actif d'une portée donnée)
		final int size = regimesFiscaux.size();
		if (size > 1) {

			// 1. on sépare les régimes fiscaux selon leur portée (les listes résultantes restent triées puisque la liste en entrée l'est)
			final Map<RegimeFiscal.Portee, List<RegimeFiscal>> parPortee = new EnumMap<>(RegimeFiscal.Portee.class);
			for (RegimeFiscal regimeFiscal : regimesFiscaux) {
				final RegimeFiscal.Portee portee = regimeFiscal.getPortee();
				final List<RegimeFiscal> liste;
				if (parPortee.containsKey(portee)) {
					liste = parPortee.get(portee);
				}
				else {
					liste = new ArrayList<>(size);
					parPortee.put(portee, liste);
				}
				liste.add(regimeFiscal);
			}

			// 2. pour chacune des portées, on valide qu'il n'y a pas de chevauchements
			for (Map.Entry<RegimeFiscal.Portee, List<RegimeFiscal>> entry : parPortee.entrySet()) {
				final List<DateRange> overlaps = DateRangeHelper.overlaps(entry.getValue());
				if (overlaps != null && !overlaps.isEmpty()) {
					for (DateRange overlap : overlaps) {
						vr.addError(String.format("La période %s est couverte par plusieurs régimes fiscaux %s", DateRangeHelper.toDisplayString(overlap), entry.getKey()));
					}
				}
			}
		}

		return vr;
	}

	@Override
	public Class<Entreprise> getValidatedClass() {
		return Entreprise.class;
	}
}
