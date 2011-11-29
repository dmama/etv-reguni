package ch.vd.uniregctb.validation.tiers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImposition;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.RepresentationConventionnelle;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeDocument;
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

		// [SIFISC-719] on s'assure que les rapports de représentation conventionnels ne se chevauchent pas
		if (sujets != null) {
			List<RapportEntreTiers> representations = null;
			for (RapportEntreTiers rapport : sujets) {
				if (rapport.isAnnule()) {
					continue;
				}
				if (rapport instanceof RepresentationConventionnelle) {
					if (representations == null) {
						representations = new ArrayList<RapportEntreTiers>();
					}
					representations.add(rapport);
				}
			}

			final List<DateRange> intersections = determineIntersectingRanges(representations);
			if (intersections != null) {
				// génération des messages d'erreur
				for (DateRange range : intersections) {
					results.addError(String.format("La période %s est couverte par plusieurs représentations conventionnelles", DateRangeHelper.toDisplayString(range)));
				}
			}
		}

		return results;
	}

	protected ValidationResults validateDeclarations(T tiers) {

		final ValidationService validationService = getValidationService();
		final ValidationResults results = new ValidationResults();

		final List<Declaration> decls = tiers.getDeclarationsSorted();
		if (decls != null) {
			Declaration last = null;
			for (Declaration d : decls) {
				if (d.isAnnule()) {
					continue;
				}
				// On valide la déclaration pour elle-même
				results.merge(validationService.validate(d));

				// Les plages de validité des déclarations ne doivent pas se chevaucher
				if (last != null && DateRangeHelper.intersect(last, d)) {
					final String message = String.format("La déclaration n°%d %s chevauche la déclaration précédente n°%d %s", d.getId(),
							DateRangeHelper.toString(d), last.getId(), DateRangeHelper.toString(last));
					results.addError(message);
				}
				last = d;
			}

			if (tiers instanceof Contribuable) {
				// [SIFISC-3127] on valide les déclarations d'impôts ordinaires par rapport aux périodes d'imposition théoriques
				final Contribuable ctb = (Contribuable) tiers;
				try {
					final List<PeriodeImposition> periodes = PeriodeImposition.determine(ctb, null);
					for (Declaration d : decls) {
						if (d.isAnnule()) {
							continue;
						}
						if (d instanceof DeclarationImpotOrdinaire) {
							validateDI((DeclarationImpotOrdinaire) d, periodes, results);
						}
					}
				}
				catch (Exception e) {
					results.addWarning("Impossible de calculer les périodes d'imposition", e);
				}
			}
		}

		return results;
	}

	private static void validateDI(DeclarationImpotOrdinaire di, List<PeriodeImposition> periodes, ValidationResults results) {
		boolean intersect = false;
		final TypeDocument typeDocument = di.getModeleDocument().getTypeDocument();
		if (periodes != null) {
			for (PeriodeImposition p : periodes) {
				if (DateRangeHelper.equals(di, p)) {
					intersect = true;
					break;
				}
				else if (DateRangeHelper.intersect(di, p)) {
					intersect = true;
					final String message = String.format("La %s qui va du %s au %s ne correspond pas à la période d'imposition théorique qui va du %s au %s",
							typeDocument.getDescription(), RegDateHelper.dateToDisplayString(di.getDateDebut()), RegDateHelper.dateToDisplayString(di.getDateFin()),
							RegDateHelper.dateToDisplayString(p.getDateDebut()), RegDateHelper.dateToDisplayString(p.getDateFin()));
					results.addWarning(message);
				}
			}
		}
		if (!intersect) {
			final String message = String.format("La %s qui va du %s au %s ne correspond à aucune période d'imposition théorique",
					typeDocument.getDescription(), RegDateHelper.dateToDisplayString(di.getDateDebut()), RegDateHelper.dateToDisplayString(di.getDateFin()));
			results.addWarning(message);
		}
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
		List<AdresseTiers> list = new ArrayList<AdresseTiers>(sorted.size());
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
			for (ForFiscal f : forsFiscaux) {
				results.merge(getValidationService().validate(f));
			}
		}

		return results;
	}

	/**
	 * Détermine et retourne les ranges pour lesquelles deux ou plusieurs des ranges spécifiés s'intersectent.
	 *
	 * @param ranges une liste de ranges
	 * @return les ranges d'intersection; ou <b>null</b> si aucun des ranges spécifiés ne s'intersectent.
	 */
	protected static List<DateRange> determineIntersectingRanges(List<? extends DateRange> ranges) {
		List<DateRange> merge = null;

		if (ranges != null) {
			final int size = ranges.size();
			if (size > 1) {
				final List<DateRange> intersectingRanges = new ArrayList<DateRange>(size);
				for (int i = 0; i < size - 1; ++i) {
					final DateRange mesure = ranges.get(i);
					for (DateRange autreMesure : ranges.subList(i + 1, size)) {
						final DateRange intersection = DateRangeHelper.intersection(mesure, autreMesure);
						if (intersection != null) {
							intersectingRanges.add(intersection);
						}
					}
				}

				if (!intersectingRanges.isEmpty()) {
					Collections.sort(intersectingRanges, new DateRangeComparator<DateRange>());

					merge = new ArrayList<DateRange>(intersectingRanges.size());
					DateRange current = null;
					for (DateRange range : intersectingRanges) {
						if (current == null) {
							current = range;
						}
						else if (DateRangeHelper.intersect(current, range) || DateRangeHelper.isCollatable(current, range)) {
							current = new DateRangeHelper.Range(RegDateHelper.minimum(current.getDateDebut(), range.getDateDebut(), NullDateBehavior.EARLIEST),
									RegDateHelper.maximum(current.getDateFin(), range.getDateFin(), NullDateBehavior.LATEST));
						}
						else {
							merge.add(current);
							current = range;
						}
					}
					merge.add(current);

				}
			}
		}
		return merge;
	}
}
