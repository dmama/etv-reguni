package ch.vd.unireg.validation.tiers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeAdapterCallback;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.adresse.AdresseCivile;
import ch.vd.unireg.adresse.AdresseTiers;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.documentfiscal.AutreDocumentFiscal;
import ch.vd.unireg.tiers.AllegementFiscal;
import ch.vd.unireg.tiers.AllegementFiscalHelper;
import ch.vd.unireg.tiers.Bouclement;
import ch.vd.unireg.tiers.DonneeCivileEntreprise;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.EtatEntreprise;
import ch.vd.unireg.tiers.FlagEntreprise;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.ForFiscalPrincipalPM;
import ch.vd.unireg.tiers.RegimeFiscal;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.GroupeFlagsEntreprise;
import ch.vd.unireg.type.TypeFlagEntreprise;

public class EntrepriseValidator extends ContribuableImpositionPersonnesMoralesValidator<Entreprise> {

	private static final Function<FlagEntreprise, GroupeFlagsEntreprise> FLAG_GROUP_EXTRACTOR = FlagEntreprise::getGroupe;

	private static final Function<FlagEntreprise, TypeFlagEntreprise> FLAG_TYPE_EXTRACTOR = FlagEntreprise::getType;

	private static final Function<AllegementFiscal, AllegementFiscalHelper.OverlappingKey> ALLEGEMENT_KEY_EXTRACTOR = AllegementFiscalHelper.OverlappingKey::valueOf;

	private static final Function<RegimeFiscal, RegimeFiscal.Portee> REGIME_FISCAL_PORTEE_EXTRACTOR = RegimeFiscal::getPortee;

	private static <K, V> Map<K, List<V>> byKey(Collection<? extends V> source, Function<? super V, ? extends K> keyExtractor) {
		return source.stream()
				.collect(Collectors.toMap(keyExtractor,
				                          Collections::singletonList,
				                          ListUtils::union,
				                          HashMap::new));
	}

	/**
	 * Interface utilisée pour faire en sorte que la validation d'une décision soit faite
	 * en supposant une date du jour spécifique (pour les tests)
	 */
	private interface ReferenceDateAccessor {
		RegDate getReferenceDate();
	}

	private static final ReferenceDateAccessor CURRENT_DATE_ACCESSOR = RegDate::get;

	private static final ThreadLocal<ReferenceDateAccessor> futureBeginDateAccessors = ThreadLocal.withInitial(() -> CURRENT_DATE_ACCESSOR);

	/**
	 * Méthode utilisable dans les tests et qui fait en sorte que la date de "début du futur" soit la date donnée
	 * @param date date qui devra dorénavant être considérée comme la date du jour pour ce qui concerne la validation des décisions
	 */
	public static void setFutureBeginDate(@Nullable final RegDate date) {
		if (date == null) {
			futureBeginDateAccessors.remove();
		}
		else {
			futureBeginDateAccessors.set(() -> date);
		}
	}

	private static ReferenceDateAccessor getFutureBeginDateAccessor() {
		return futureBeginDateAccessors.get();
	}

	protected static RegDate getFutureBeginDate() {
		final ReferenceDateAccessor accessor = getFutureBeginDateAccessor();
		return accessor.getReferenceDate();
	}

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

		// ... puis entre eux (en prenant en compte les spécificités de chaque groupe par rapport aux chevauchements)
		if (flags.size() > 1) {
			// répartition en groupes
			final Map<GroupeFlagsEntreprise, List<FlagEntreprise>> flagsParGroupe = byKey(flags, FLAG_GROUP_EXTRACTOR);

			// boucle sur les groupes et, pour ceux qui n'acceptent pas les chevauchement, vérification en conséquence
			for (Map.Entry<GroupeFlagsEntreprise, List<FlagEntreprise>> groupEntry : flagsParGroupe.entrySet()) {
				final List<FlagEntreprise> flagsDansGroupe = groupEntry.getValue();
				if (flagsDansGroupe.size() > 1) {
					// dans un groupe où les éléments doivent être mutuellement exclusifs, il ne doit
					// y avoir aucun chevauchement
					final GroupeFlagsEntreprise groupe = groupEntry.getKey();
					if (groupe.isFlagsMutuellementExclusifs()) {
						final List<DateRange> overlaps = DateRangeHelper.overlaps(flagsDansGroupe);
						if (overlaps != null && !overlaps.isEmpty()) {
							for (DateRange overlap : overlaps) {
								vr.addError(String.format("La période %s est couverte par plusieurs spécificités fiscales du groupe %s",
								                          DateRangeHelper.toDisplayString(overlap),
								                          groupe));
							}
						}
					}

					// dans un groupe où les éléments ne sont pas mutuellement exclusifs, il peut y avoir des chevauchements
					// entre types différents, mais pas sur des entités de même type
					else {
						// il faut dont les regrouper par type
						final Map<TypeFlagEntreprise, List<FlagEntreprise>> flagsParType = byKey(flagsDansGroupe, FLAG_TYPE_EXTRACTOR);
						for (Map.Entry<TypeFlagEntreprise, List<FlagEntreprise>> typeEntry : flagsParType.entrySet()) {
							final List<FlagEntreprise> flagsDeType = typeEntry.getValue();
							if (flagsDeType.size() > 1) {
								final List<DateRange> overlaps = DateRangeHelper.overlaps(flagsDeType);
								if (overlaps != null && !overlaps.isEmpty()) {
									final TypeFlagEntreprise type = typeEntry.getKey();
									for (DateRange overlap : overlaps) {
										vr.addError(String.format("La période %s est couverte par plusieurs spécificités fiscales de type %s",
										                          DateRangeHelper.toDisplayString(overlap),
										                          type));
									}
								}
							}
						}
					}
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
			final Map<RegimeFiscal.Portee, List<RegimeFiscal>> parPortee = byKey(regimesFiscaux, REGIME_FISCAL_PORTEE_EXTRACTOR);

			// 2. pour chacune des portées, on valide qu'il n'y a pas de chevauchements
			for (Map.Entry<RegimeFiscal.Portee, List<RegimeFiscal>> entry : parPortee.entrySet()) {
				final List<DateRange> overlaps = DateRangeHelper.overlaps(entry.getValue());
				if (overlaps != null && !overlaps.isEmpty()) {
					for (DateRange overlap : overlaps) {
						vr.addError(String.format("La période %s est couverte par plusieurs régimes fiscaux de portée %s", DateRangeHelper.toDisplayString(overlap), entry.getKey()));
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
			final Map<AllegementFiscalHelper.OverlappingKey, List<AllegementFiscal>> map = byKey(allegementsFiscaux, ALLEGEMENT_KEY_EXTRACTOR);

			// vérification des chevauchements
			for (Map.Entry<AllegementFiscalHelper.OverlappingKey, List<AllegementFiscal>> entry : map.entrySet()) {
				final List<AllegementFiscal> liste = entry.getValue();
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

				// [SIFISC-19932] on fait une distinction entre le futur proche est le futur plus lointain
				// (la frontière se situant dans 2 ans à partir de maintenant)
				final RegDate futureBeginDate = getFutureBeginDate();
				final RegDate farFutureBeginDate = futureBeginDate.addYears(2);
				final DateRange nearRelevant = new DateRangeHelper.Range(RegDate.get(premiereAnnee, 1, 1), farFutureBeginDate.getOneDayBefore());
				final DateRange farRelevant = new DateRangeHelper.Range(farFutureBeginDate, null);

				for (DateRange range : nonCouverts) {
					if (DateRangeHelper.intersect(range, nearRelevant)) {
						vr.addError(String.format("La période %s possède des fors IBC non-annulés mais est exempte de régime fiscal %s%s.",
						                          DateRangeHelper.toDisplayString(range),
						                          portee,
						                          range.getDateDebut().isAfterOrEqual(nearRelevant.getDateDebut()) ? StringUtils.EMPTY : " (seule la période depuis " + premiereAnnee + " est réellement problématique)"));
					}
					else if (DateRangeHelper.intersect(range, farRelevant)) {
						vr.addWarning(String.format("La période %s possède des fors IBC non-annulés mais est exempte de régime fiscal %s (la période est suffisamment loin dans le futur pour que ceci ne soit qu'un avertissement, mais le problème devrait cependant être corrigé rapidement).",
						                            DateRangeHelper.toDisplayString(range),
						                            portee));
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
