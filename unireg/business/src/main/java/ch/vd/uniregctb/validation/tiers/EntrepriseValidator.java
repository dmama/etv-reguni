package ch.vd.uniregctb.validation.tiers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeAdapterCallback;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdresseCivile;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.common.AnnulableHelper;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.documentfiscal.AutreDocumentFiscal;
import ch.vd.uniregctb.tiers.AllegementFiscal;
import ch.vd.uniregctb.tiers.AllegementFiscalHelper;
import ch.vd.uniregctb.tiers.Bouclement;
import ch.vd.uniregctb.tiers.DonneeCivileEntreprise;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.EtatEntreprise;
import ch.vd.uniregctb.tiers.FlagEntreprise;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPM;
import ch.vd.uniregctb.tiers.RegimeFiscal;
import ch.vd.uniregctb.type.GenreImpot;

public class EntrepriseValidator extends ContribuableImpositionPersonnesMoralesValidator<Entreprise> {

	@Override
	public ValidationResults validate(Entreprise entreprise) {
		final ValidationResults vr = super.validate(entreprise);
		if (!entreprise.isAnnule()) {
			vr.merge(validateRegimesFiscaux(entreprise));
			vr.merge(validateDonneesCivilesEntreprise(entreprise));
			vr.merge(validateAllegementsFiscaux(entreprise));
			vr.merge(validateBouclements(entreprise));
			vr.merge(validateEtats(entreprise));
			vr.merge(validateFlags(entreprise));
			vr.merge(validateForsEtRegimesFiscaux(entreprise));
			vr.merge(validateAutresDocumentsFiscaux(entreprise));

			// validation de la date de début du premier exercice commercial
			if (entreprise.getDateDebutPremierExerciceCommercial() != null) {
				// elle doit toujours être antérieure au égale à la date de début du premier for
				// principal avec un genre d'impôt 'bénéfice/capital'
				final List<ForFiscalPrincipalPM> forsPrincipaux = entreprise.getForsFiscauxPrincipauxActifsSorted();
				// Il se peut qu'aucun for principal actif n'existe
				if (forsPrincipaux != null) {
					for (ForFiscalPrincipalPM ff : forsPrincipaux) {
						if (ff.getGenreImpot() == GenreImpot.BENEFICE_CAPITAL) {
							// nous venons de trouver le premier for principal IBC
							// (comme nous sommes dans le validateur, il n'est pas exclu que la date de début de ce for soit nulle,,,)
							if (ff.getDateDebut() == null || entreprise.getDateDebutPremierExerciceCommercial().isAfter(ff.getDateDebut())) {
								vr.addError(String.format(
										"La date de début du premier exercice commercial (%s) doit être antérieure ou égale à la date de début du premier for principal IBC (%s) de l'entreprise.",
										RegDateHelper.dateToDisplayString(entreprise.getDateDebutPremierExerciceCommercial()),
										RegDateHelper.dateToDisplayString(ff.getDateDebut())));
							}
							break;
						}
					}
				}
			}
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

		// ... puis entre eux (les flags ne peuvent se chevaucher)
		if (flags.size() > 1) {
			final List<DateRange> overlaps = DateRangeHelper.overlaps(flags);
			if (overlaps != null && !overlaps.isEmpty()) {
				for (DateRange overlap : overlaps) {
					vr.addError(String.format("La période %s est couverte par plusieurs spécificités fiscales", DateRangeHelper.toDisplayString(overlap)));
				}
			}
		}

		return vr;
	}

	protected ValidationResults validateDonneesCivilesEntreprise(Entreprise entreprise) {
		final ValidationResults vr = new ValidationResults();

		// on valide d'abord les données pour elles-mêmes
		final Set<DonneeCivileEntreprise> donnees = entreprise.getDonneesCiviles();
		if (donnees != null && !donnees.isEmpty()) {
			for (DonneeCivileEntreprise d : donnees) {
				vr.merge(getValidationService().validate(d));
			}
		}

		// ... puis entre elles (il ne doit y avoir, à tout moment, au plus qu'une seule instance active de chaque type)
		checkOverlaps(entreprise.getRaisonsSocialesNonAnnuleesTriees(), "raison sociale", vr);
		checkOverlaps(entreprise.getFormesJuridiquesNonAnnuleesTriees(), "forme juridique", vr);
		checkOverlaps(entreprise.getCapitauxNonAnnulesTries(), "capital", vr);

		return vr;
	}

	private static <T extends DonneeCivileEntreprise> void checkOverlaps(List<T> nonAnnulesTries, String libelle, ValidationResults vr) {
		if (nonAnnulesTries.size() > 1) {
			final List<DateRange> overlaps = DateRangeHelper.overlaps(nonAnnulesTries);
			if (overlaps != null && !overlaps.isEmpty()) {
				for (DateRange overlap : overlaps) {
					vr.addError(String.format("La période %s est couverte par plusieurs valeurs de %s",
					                          DateRangeHelper.toDisplayString(overlap),
					                          libelle));
				}
			}
		}
	}

	private static <T extends DonneeCivileEntreprise> void checkContinuous(List<T> nonAnnulesTries, String libelle, ValidationResults vr) {

		if (nonAnnulesTries.size() > 1) {
			final List<DateRange> completeRange = Arrays.<DateRange>asList(
					new DateRangeHelper.Range(nonAnnulesTries.get(0).getDateDebut(), CollectionsUtils.getLastElement(nonAnnulesTries).getDateFin()));

			final List<DateRange> resultingRange = DateRangeHelper.subtract(completeRange, nonAnnulesTries, new DateRangeHelper.AdapterCallback<DateRange>() {
				@Override
				public DateRange adapt(DateRange range, RegDate debut, RegDate fin) {
					return new DateRangeHelper.Range(debut, fin);
				}
			});
			if (resultingRange.size() > 0) {
				for (DateRange holeRange : resultingRange) {
					vr.addError(String.format("Rupture de continuité: période vide %s dans la valeur de %s", DateRangeHelper.toDisplayString(holeRange), libelle));
				}
			}
		}
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
			final Map<AllegementFiscalHelper.OverlappingKey, List<DateRange>> map = new HashMap<>(allegementsFiscaux.size());
			for (AllegementFiscal af : allegementsFiscaux) {
				final AllegementFiscalHelper.OverlappingKey key = AllegementFiscalHelper.OverlappingKey.valueOf(af);
				List<DateRange> liste = map.get(key);
				if (liste == null) {
					liste = new ArrayList<>();
					map.put(key, liste);
				}
				liste.add(af);
			}

			// vérification des chevauchements
			for (Map.Entry<AllegementFiscalHelper.OverlappingKey, List<DateRange>> entry : map.entrySet()) {
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

	protected ValidationResults validateForsEtRegimesFiscaux(Entreprise entreprise) {
		// les périodes de fors IBC doivent être couverts par des régimes fiscaux

		// d'abord allons chercher les périodes de fors IBC
		final List<ForFiscal> fors = entreprise.getForsFiscauxNonAnnules(true);
		final List<DateRange> ibcBrutto = new ArrayList<>(fors.size());
		for (ForFiscal ff : fors) {
			if (ff.getGenreImpot() == GenreImpot.BENEFICE_CAPITAL) {
				ibcBrutto.add(ff);
			}
		}
		final List<DateRange> ibcNetto = DateRangeHelper.merge(ibcBrutto);

		// maintenant, il faut chercher les périodes couvertes par des régimes fiscaux, quels qu'ils soient
		final List<RegimeFiscal> regimes = entreprise.getRegimesFiscauxNonAnnulesTries();
		final ValidationResults vr = new ValidationResults();
		vr.merge(validateForsEtRegimesFiscaux(ibcNetto, regimes, RegimeFiscal.Portee.VD));
		vr.merge(validateForsEtRegimesFiscaux(ibcNetto, regimes, RegimeFiscal.Portee.CH));
		return vr;
	}

	private ValidationResults validateForsEtRegimesFiscaux(List<DateRange> rangesFors, List<RegimeFiscal> allRegimes, RegimeFiscal.Portee portee) {
		final ValidationResults vr = new ValidationResults();
		if (rangesFors != null) {
			final List<RegimeFiscal> pourPortee = new ArrayList<>(allRegimes.size());
			for (RegimeFiscal regime : allRegimes) {
				if (regime.getPortee() == portee) {
					pourPortee.add(regime);
				}
			}
			final List<DateRange> netto = DateRangeHelper.merge(pourPortee);
			final List<DateRange> nonCouverts = DateRangeHelper.subtract(rangesFors, netto, new DateRangeAdapterCallback());
			if (nonCouverts != null && !nonCouverts.isEmpty()) {

				// en fait, on ne s'intéresse qu'aux périodes récentes... (historiquement, les régimes fiscaux n'ont pas
				// forcément été attribués pour les périodes très vieilles...)
				final int premiereAnnee = parametreAppService.getPremierePeriodeFiscalePersonnesMorales();
				final DateRange relevant = new DateRangeHelper.Range(RegDate.get(premiereAnnee, 1, 1), null);

				for (DateRange range : nonCouverts) {
					if (DateRangeHelper.intersect(range, relevant)) {
						vr.addError(String.format("La période %s possède des fors IBC non-annulés mais est exempte de régime fiscal %s%s.",
						                          DateRangeHelper.toDisplayString(range),
						                          portee,
						                          DateRangeHelper.within(range, relevant) ? StringUtils.EMPTY : " (seule la période depuis " + premiereAnnee + " est réellement problématique)"));
					}
				}
			}
		}
		return vr;
	}

	private ValidationResults validateAutresDocumentsFiscaux(Entreprise entreprise) {
		final ValidationResults vr = new ValidationResults();
		final Set<AutreDocumentFiscal> docs = entreprise.getAutresDocumentsFiscaux();
		if (docs != null && !docs.isEmpty()) {
			for (AutreDocumentFiscal doc : docs) {
				vr.merge(getValidationService().validate(doc));
			}
		}
		return vr;
	}

	@Override
	public Class<Entreprise> getValidatedClass() {
		return Entreprise.class;
	}
}
