package ch.vd.uniregctb.validation.tiers;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdresseCivile;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.common.AnnulableHelper;
import ch.vd.uniregctb.tiers.AllegementFiscal;
import ch.vd.uniregctb.tiers.Bouclement;
import ch.vd.uniregctb.tiers.CapitalEntreprise;
import ch.vd.uniregctb.tiers.DonneesRegistreCommerce;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.EtatEntreprise;
import ch.vd.uniregctb.tiers.FlagEntreprise;
import ch.vd.uniregctb.tiers.RegimeFiscal;

public class EntrepriseValidator extends ContribuableImpositionPersonnesMoralesValidator<Entreprise> {

	@Override
	public ValidationResults validate(Entreprise entreprise) {
		final ValidationResults vr = super.validate(entreprise);
		if (!entreprise.isAnnule()) {
			vr.merge(validateRegimesFiscaux(entreprise));
			vr.merge(validateDonneesRegistreCommerce(entreprise));
			vr.merge(validateCapitaux(entreprise));
			vr.merge(validateAllegementsFiscaux(entreprise));
			vr.merge(validateBouclements(entreprise));
			vr.merge(validateEtats(entreprise));
			vr.merge(validateFlags(entreprise));
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

	protected ValidationResults validateEtats(Entreprise entreprise) {
		final ValidationResults vr = new ValidationResults();
		final List<EtatEntreprise> etats = AnnulableHelper.sansElementsAnnules(entreprise.getEtats());

		// on valide les états pour eux-mêmes ...
		for (EtatEntreprise etat : etats) {
			vr.merge(getValidationService().validate(etat));
		}

		return vr;
	}

	protected ValidationResults validateFlags(Entreprise entreprise) {
		final ValidationResults vr = new ValidationResults();
		final List<FlagEntreprise> flags = AnnulableHelper.sansElementsAnnules(entreprise.getFlags());

		// on valide les flags par eux-mêmes
		for (FlagEntreprise flag : flags) {
			vr.merge(getValidationService().validate(flag));
		}

		// TODO [SIPM][FlagEntreprise] pour l'instant, on n'a aucune règle concernant les éventuelles incompatibilités entre flags...

		return vr;
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

	protected ValidationResults validateCapitaux(Entreprise entreprise) {
		final ValidationResults vr = new ValidationResults();

		final List<CapitalEntreprise> capitaux = entreprise.getCapitauxNonAnnulesTries();

		// on valide d'abord les données pour elles-mêmes
		for (CapitalEntreprise d : capitaux) {
			vr.merge(getValidationService().validate(d));
		}

		// ... puis entre elles (il ne doit y avoir, à tout moment, au plus qu'une seule instance active)
		final int size = capitaux.size();
		if (size > 1) {
			final List<DateRange> overlaps = DateRangeHelper.overlaps(capitaux);
			if (overlaps != null && !overlaps.isEmpty()) {
				for (DateRange overlap : overlaps) {
					vr.addError(String.format("La période %s est couverte par plusieurs données de capital", DateRangeHelper.toDisplayString(overlap)));
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

				// [SIFISC-17375] dans les écrans super-gra, on peut être dans des cas où la portée n'a pas encore été assignée (-> NPE à l'insertion dans la map)
				if (portee != null) {
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

	/**
	 * Classe interne utilisée pour déterminer si des allègements fiscaux similaires ne se chevauchent pas...
	 */
	private static final class AllegementFiscalKey {

		private final AllegementFiscal.TypeImpot typeImpot;
		private final AllegementFiscal.TypeCollectivite typeCollectivite;
		private final Integer noOfsCommune;

		public AllegementFiscalKey(AllegementFiscal.TypeImpot typeImpot, AllegementFiscal.TypeCollectivite typeCollectivite, Integer noOfsCommune) {
			this.typeImpot = typeImpot;
			this.typeCollectivite = typeCollectivite;
			this.noOfsCommune = (typeCollectivite == AllegementFiscal.TypeCollectivite.COMMUNE ? noOfsCommune : null);
		}

		public AllegementFiscalKey(AllegementFiscal allegementFiscal) {
			this(allegementFiscal.getTypeImpot(), allegementFiscal.getTypeCollectivite(), allegementFiscal.getNoOfsCommune());
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final AllegementFiscalKey that = (AllegementFiscalKey) o;

			if (typeImpot != that.typeImpot) return false;
			if (typeCollectivite != that.typeCollectivite) return false;
			return !(noOfsCommune != null ? !noOfsCommune.equals(that.noOfsCommune) : that.noOfsCommune != null);
		}

		@Override
		public int hashCode() {
			int result = typeImpot != null ? typeImpot.hashCode() : 0;
			result = 31 * result + (typeCollectivite != null ? typeCollectivite.hashCode() : 0);
			result = 31 * result + (noOfsCommune != null ? noOfsCommune.hashCode() : 0);
			return result;
		}

		@Override
		public String toString() {
			if (typeImpot == null && typeCollectivite == null) {
				return "allègement universel";
			}

			final StringBuilder b = new StringBuilder("allègement");
			if (typeImpot != null) {
				b.append(" ").append(typeImpot);
			}
			if (typeCollectivite != null) {
				b.append(" ").append(typeCollectivite);
				if (noOfsCommune != null) {
					b.append(" (").append(noOfsCommune).append(")");
				}
			}
			return b.toString();
		}
	}

	protected ValidationResults validateAllegementsFiscaux(Entreprise entreprise) {
		final ValidationResults vr = new ValidationResults();

		final List<AllegementFiscal> allegementsFiscaux = AnnulableHelper.sansElementsAnnules(entreprise.getAllegementsFiscaux());

		// on valide les allègements fiscaux pour eux-mêmes...
		for (AllegementFiscal af : allegementsFiscaux) {
			vr.merge(getValidationService().validate(af));
		}

		// puis on valide que deux allègements fiscaux sur le même sujet ne se chevauchent pas...
		if (!allegementsFiscaux.isEmpty()) {
			// "même sujet" est défini par la clé AllegementFiscalKey
			final Map<AllegementFiscalKey, List<DateRange>> map = new HashMap<>(allegementsFiscaux.size());
			for (AllegementFiscal af : allegementsFiscaux) {
				final AllegementFiscalKey key = new AllegementFiscalKey(af);
				List<DateRange> liste = map.get(key);
				if (liste == null) {
					liste = new ArrayList<>();
					map.put(key, liste);
				}
				liste.add(af);
			}

			// vérification des chevauchements
			for (Map.Entry<AllegementFiscalKey, List<DateRange>> entry : map.entrySet()) {
				final List<DateRange> liste = entry.getValue();
				if (liste.size() > 1) {
					final List<DateRange> overlaps = DateRangeHelper.overlaps(liste);
					if (overlaps != null && !overlaps.isEmpty()) {
						for (DateRange overlap : overlaps) {
							vr.addError(String.format("La période %s est couverte par plusieurs allègements fiscaux de type '%s'.",
							                          DateRangeHelper.toDisplayString(overlap),
							                          entry.getKey()));
						}
					}
				}
			}
		}

		return vr;
	}

	protected ValidationResults validateBouclements(Entreprise entreprise) {
		final ValidationResults vr = new ValidationResults();
		if (entreprise.getBouclements() != null) {
			for (Bouclement bouclement : entreprise.getBouclements()) {
				vr.merge(getValidationService().validate(bouclement));
			}
		}
		return vr;
	}

	@Override
	public Class<Entreprise> getValidatedClass() {
		return Entreprise.class;
	}
}
