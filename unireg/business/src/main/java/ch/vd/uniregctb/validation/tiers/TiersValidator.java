package ch.vd.uniregctb.validation.tiers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.common.AnnulableHelper;
import ch.vd.uniregctb.common.StringRenderer;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.etiquette.EtiquetteTiers;
import ch.vd.uniregctb.tiers.ActiviteEconomique;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.Remarque;
import ch.vd.uniregctb.tiers.RepresentationConventionnelle;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.validation.EntityValidatorImpl;
import ch.vd.uniregctb.validation.ValidationService;

/**
 * Classe de base des validateurs de tiers
 * @param <T>
 */
public abstract class TiersValidator<T extends Tiers> extends EntityValidatorImpl<T> {

	@Override
	public ValidationResults validate(T tiers) {

		final ValidationResults results = new ValidationResults();

		// UNIREG-601 on ignore toutes les erreurs pour un tiers annulé
		if (!tiers.isAnnule()) {
			results.merge(validateAdresses(tiers));
			results.merge(validateFors(tiers));
			results.merge(validateDeclarations(tiers));
			results.merge(validateRapports(tiers));
			results.merge(validateRemarques(tiers));
			results.merge(validateEtiquettes(tiers));
		}

		return results;
	}

	protected ValidationResults validateRapports(T tiers) {

		final ValidationService validationService = getValidationService();
		final ValidationResults results = new ValidationResults();

		// [SIFISC-719] on valide les rapports-entre-tiers pour eux-mêmes
		final Set<RapportEntreTiers> objets = tiers.getRapportsObjet();
		if (objets != null) {
			for (RapportEntreTiers rapport : objets) {
				if (rapport.isAnnule()) {
					continue;
				}
				results.merge(validationService.validate(rapport));
			}
		}

		final Set<RapportEntreTiers> sujets = tiers.getRapportsSujet();
		if (sujets != null) {
			for (RapportEntreTiers rapport : sujets) {
				if (rapport.isAnnule()) {
					continue;
				}
				results.merge(validationService.validate(rapport));
			}
		}

		if (sujets != null) {
			// [SIFISC-719] on s'assure que les rapports de représentation conventionnels ne se chevauchent pas
			checkNonOverlap(sujets, r -> r instanceof RepresentationConventionnelle, results, "rapports de type 'représentation conventionnelle'");

			// de même on s'assure que les établissements principaux ne sont pas plusieurs à un moment donné
			checkNonOverlap(sujets, r -> r instanceof ActiviteEconomique && ((ActiviteEconomique) r).isPrincipal(), results, "rapports de type 'activité économique principale'");
		}

		return results;
	}

	protected static <T extends DateRange & Annulable> void checkNonOverlap(Collection<T> data,
	                                                                        Predicate<? super T> filter,
	                                                                        ValidationResults results,
	                                                                        String description) {
		if (data != null && data.size() > 1) {
			// filtrage des éléments intéressants
			final List<T> concernes = data.stream()
					.filter(AnnulableHelper::nonAnnule)
					.filter(filter)
					.collect(Collectors.toList());

			// vérification des overlaps
			if (concernes.size() > 1) {
				final List<DateRange> intersections = DateRangeHelper.overlaps(concernes);
				if (intersections != null) {
					intersections.stream()
							.map(range -> String.format("La période %s est couverte par plusieurs %s.", DateRangeHelper.toDisplayString(range), description))
							.forEach(results::addError);
				}
			}
		}
	}

	protected static <T extends DateRange & Annulable, K> void checkNonOverlapByGoup(Collection<T> data,
	                                                                                 Predicate<? super T> filter,
	                                                                                 Function<? super T, K> groupKeyExtractor,
	                                                                                 ValidationResults results,
	                                                                                 String description,
	                                                                                 StringRenderer<? super K> keyRenderer) {
		if (data != null && data.size() > 1) {
			// on construit d'abord une map des valeurs regroupées par la clé du groupe
			final Map<K, List<T>> groups = data.stream()
					.filter(AnnulableHelper::nonAnnule)
					.filter(filter)
					.collect(Collectors.toMap(groupKeyExtractor,
					                          Collections::singletonList,
					                          (c1, c2) -> Stream.concat(c1.stream(), c2.stream()).collect(Collectors.toList())));

			// puis on vérifie pour chaque groupe indépendemment
			for (Map.Entry<K, List<T>> entry : groups.entrySet()) {
				final String groupDescription = String.format("%s de type '%s'", description, keyRenderer.toString(entry.getKey()));
				checkNonOverlap(entry.getValue(), x -> true, results, groupDescription);
			}
		}
	}

	protected ValidationResults validateDeclarations(T tiers) {

		final ValidationService validationService = getValidationService();
		final ValidationResults results = new ValidationResults();

		final List<Declaration> decls = tiers.getDeclarationsTriees(Declaration.class, false);
		if (!decls.isEmpty()) {

			// validation des déclarations pour elles-mêmes
			decls.stream()
					.map(validationService::validate)
					.forEach(results::merge);

			// on sépare d'abord les déclarations par type (parce que des déclarations de types identitiques
			// ne doivent pas se chevaucher, mais rien n'est dit pour les déclarations de types différents,
			// cf. la réponse à la question "que se passe-t-il au passage de SNC à SA ?", par exemple)
			checkNonOverlapByGoup(decls,
			                      x -> true,
			                      Object::getClass,
			                      results,
			                      "déclarations non-annulées",
			                      Class::getSimpleName);
		}

		return results;
	}

	protected ValidationResults validateAdresses(T tiers) {

		final ValidationResults results = new ValidationResults();

		results.merge(validateTypeAdresses(tiers));
		for (TypeAdresseTiers type : TypeAdresseTiers.values()) {
			results.merge(validateAdresses(tiers.getAdressesTiersSorted(type)));
		}

		return results;
	}

	private ValidationResults validateAdresses(List<AdresseTiers> sorted) {

		if (sorted == null || sorted.isEmpty()) {
			return null;
		}

		// on ignore les adresses annulées
		// [UNIREG-467] on crée une nouvelle liste pour avoir les indexes corrects
		List<AdresseTiers> list = new ArrayList<>(sorted.size());
		for (AdresseTiers a : sorted) {
			if (!a.isAnnule()) {
				list.add(a);
			}
		}

		final ValidationResults results = new ValidationResults();
		final ValidationService validationService = getValidationService();

		RegDate lastDateFin = null;
		RegDate lastDateDebut = null;
		for (int i = 0; i < list.size(); i++) {
			final AdresseTiers adr = list.get(i);
			if (i > 0) {
				if (lastDateFin == null || adr.getDateDebut().isBeforeOrEqual(lastDateFin)) {
					// Overlap
					final String message =
							String.format("L'adresse fiscale numéro %d (type=%s début=%s fin=%s) chevauche l'adresse fiscale numéro %d (type=%s début=%s fin=%s)", i,
									adr.getUsage().name().toLowerCase(), RegDateHelper.dateToDisplayString(lastDateDebut), RegDateHelper.dateToDisplayString(lastDateFin), (i + 1),
									adr.getUsage().name().toLowerCase(), RegDateHelper.dateToDisplayString(adr.getDateDebut()), RegDateHelper.dateToDisplayString(adr.getDateFin()));
					results.addError(message);
				}
			}

			// validation de l'adresse pour elle-même
			results.merge(validationService.validate(adr));

			lastDateDebut = adr.getDateDebut();
			lastDateFin = adr.getDateFin();
		}

		return results;
	}

	protected abstract ValidationResults validateTypeAdresses(T tiers);

	protected ValidationResults validateFors(T tiers) {

		final ValidationResults results = new ValidationResults();

		// On valide tous les fors pour eux-mêmes
		final Set<ForFiscal> forsFiscaux = tiers.getForsFiscaux();
		if (forsFiscaux != null) {
			final List<ForFiscal> forsTries = new ArrayList<>(forsFiscaux);
			forsTries.sort(new DateRangeComparator<>());
			for (ForFiscal f : forsTries) {
				results.merge(getValidationService().validate(f));
			}
		}

		return results;
	}

	protected ValidationResults validateRemarques(T tiers) {
		final ValidationResults vr = new ValidationResults();
		final Set<Remarque> remarques = tiers.getRemarques();
		if (remarques != null) {
			for (Remarque r : remarques) {
				vr.merge(getValidationService().validate(r));
			}
		}
		return vr;
	}

	private ValidationResults validateEtiquettes(T tiers) {
		final ValidationResults vr = new ValidationResults();
		final Set<EtiquetteTiers> etiquettes = tiers.getEtiquettes();
		if (etiquettes != null && !etiquettes.isEmpty()) {

			// chaque étiquette pour elle-même
			final ValidationService validationService = getValidationService();
			etiquettes.stream()
					.map(validationService::validate)
					.forEach(vr::merge);

			// puis les chevauchements (par code d'étiquette)
			if (!vr.hasErrors()) {
				checkNonOverlapByGoup(etiquettes,
				                      x -> true,
				                      etiq -> etiq.getEtiquette().getLibelle(),
				                      vr,
				                      "étiquettes non-annulées",
				                      StringRenderer.DEFAULT);
			}
		}
		return vr;
	}
}
