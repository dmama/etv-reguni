package ch.vd.uniregctb.validation.tiers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdresseCivile;
import ch.vd.uniregctb.adresse.AdressePM;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.RepresentationLegale;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public class PersonnePhysiqueValidator extends ContribuableValidator<PersonnePhysique> {

	private static final Pattern NOM_PRENOM_PATTERN = Pattern.compile("[']?[A-Za-zÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿŒœŠšŸŽž]['A-Za-zÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿŒœŠšŸŽž. /-]*");
	private static final Pattern NOM_PRENOM_ESPACES = Pattern.compile("([^ ]+ )*[^ ]+");

	@Override
	public ValidationResults validate(PersonnePhysique pp) {

		final ValidationResults vr = super.validate(pp);

		if (pp.isAnnule()) {
			return vr;
		}

		final Boolean habitant = pp.getHabitant();
		final Long numeroIndividu = pp.getNumeroIndividu();
		if (habitant == null) {
			vr.addError("La personne physique doit être habitant ou non habitant");
		}
		else if (habitant && (numeroIndividu == null || numeroIndividu <= 0L)) {
			vr.addError("Le numéro d'individu du registre civil est un attribut obligatoire pour un habitant");
		}
		else if (!habitant) {

			final String nom = pp.getNom();
			final String prenom = pp.getPrenom();

			// nom : obligatoire
			if (StringUtils.isBlank(nom)) {
				vr.addError("Le nom est un attribut obligatoire pour un non-habitant");
			}
			else {
				if (!NOM_PRENOM_PATTERN.matcher(nom).matches()) {
					vr.addWarning("Le nom du non-habitant contient au moins un caractère invalide");
				}
				if (!NOM_PRENOM_ESPACES.matcher(nom).matches()) {
					vr.addWarning("Le nom du non-habitant contient un groupe de plusieurs espaces consécutifs ou un espace final");
				}
			}

			// prénom : facultatif, donc on laisse passer empty, mais pas des espaces seuls
			if (!StringUtils.isEmpty(prenom)) {
				if (!NOM_PRENOM_PATTERN.matcher(prenom).matches()) {
					vr.addWarning("Le prénom du non-habitant contient au moins un caractère invalide");
				}
				if (!NOM_PRENOM_ESPACES.matcher(prenom).matches()) {
					vr.addWarning("Le prénom du non-habitant contient un groupe de plusieurs espaces consécutifs ou un espace final");
				}
			}
		}

		return vr;
	}

	@Override
	protected ValidationResults validateFors(PersonnePhysique pp) {

		final ValidationResults vr = super.validateFors(pp);

		/*
		 * On n'autorise pas la présence de fors durant la ou les périodes d'appartenance à un couple
		 */
		// Détermine les périodes de validités ininterrompues du ménage commun
		final List<RapportEntreTiers> rapportsMenages = new ArrayList<RapportEntreTiers>();
		final Set<RapportEntreTiers> rapports = pp.getRapportsSujet();
		if (rapports != null) {
			for (RapportEntreTiers r : rapports) {
				if (!r.isAnnule() && TypeRapportEntreTiers.APPARTENANCE_MENAGE == r.getType()) {
					rapportsMenages.add(r);
				}
			}
		}
		Collections.sort(rapportsMenages, new DateRangeComparator<RapportEntreTiers>());
		final List<DateRange> periodes = DateRangeHelper.collateRange(rapportsMenages);

		// Vérifie que chaque for est entièrement compris à l'extérieur d'une période de validité
		final Set<ForFiscal> fors = pp.getForsFiscaux();
		if (fors != null) {
			for (ForFiscal f : fors) {
				if (f.isAnnule()) {
					continue;
				}
				if (DateRangeHelper.intersect(f, periodes)) {
					vr.addError(String.format("Le for fiscal [%s] ne peut pas exister alors que le tiers [%d] appartient à un ménage-commun", f, pp.getNumero()));
				}
			}
		}

		return vr;
	}

	@Override
	protected ValidationResults validateTypeAdresses(PersonnePhysique pp) {

		final ValidationResults results = new ValidationResults();
		final Set<AdresseTiers> adresses = pp.getAdressesTiers();
		if (adresses != null) {
			for (AdresseTiers a : adresses) {
				if (a.isAnnule()) {
					continue;
				}
				if (a instanceof AdressePM) {
					results.addError("L'adresse de type 'personne morale' (numéro=" + a.getId() + ", début=" + a.getDateDebut() + ", fin="
							+ a.getDateFin() + ") n'est pas autorisée sur une personne physique.");
				}
				else if (pp.getHabitant() != null && !pp.getHabitant() && a instanceof AdresseCivile && a.getDateFin() == null) {
					results.addError("L'adresse de type 'personne civile' (numéro=" + a.getId() + ", début=" + a.getDateDebut() + ", fin="
							+ a.getDateFin() + ") n'est pas autorisée sur un non-habitant.");
				}
			}
		}

		return results;
	}

	@Override
	protected ValidationResults validateRapports(PersonnePhysique tiers) {

		final ValidationResults results = super.validateRapports(tiers);

		// [SIFISC-124] Les mesures de tutelles sont mutuellement exclusives (= on ne peut pas en avoir plusieurs actives à un moment donné)
		if (tiers.getRapportsSujet() != null) {
			List<RapportEntreTiers> mesures = null;
			for (RapportEntreTiers r : tiers.getRapportsSujet()) {
				if (!r.isAnnule() && r instanceof RepresentationLegale) {
					if (mesures == null) {
						mesures = new ArrayList<RapportEntreTiers>();
					}
					mesures.add(r);
				}
			}
			if (mesures != null) {
				final int size = mesures.size();
				if (size > 1) {
					final List<DateRange> intersectingRanges = new ArrayList<DateRange>(size);
					for (int i = 0 ; i < size - 1 ; ++ i) {
						final DateRange mesure = mesures.get(i);
						for (DateRange autreMesure : mesures.subList(i + 1, size)) {
							final DateRange intersection = DateRangeHelper.intersection(mesure, autreMesure);
							if (intersection != null) {
								intersectingRanges.add(intersection);
							}
						}
					}

					if (intersectingRanges.size() > 0) {
						Collections.sort(intersectingRanges, new DateRangeComparator<DateRange>());

						final List<DateRange> merge = new ArrayList<DateRange>(intersectingRanges.size());
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

						// génération des messages d'erreur
						for (DateRange range : merge) {
							results.addError(String.format("La période %s est couverte par plusieurs mesures tutélaires", DateRangeHelper.toDisplayString(range)));
						}
					}
				}
			}
		}

		return results;
	}

	@Override
	public Class<PersonnePhysique> getValidatedClass() {
		return PersonnePhysique.class;
	}
}
