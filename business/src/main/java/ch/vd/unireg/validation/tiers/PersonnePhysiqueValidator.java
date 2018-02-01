package ch.vd.unireg.validation.tiers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.adresse.AdresseCivile;
import ch.vd.unireg.adresse.AdressePM;
import ch.vd.unireg.adresse.AdresseTiers;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.tiers.AppartenanceMenage;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.Heritage;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.tiers.RepresentationLegale;
import ch.vd.unireg.type.TypeRapportEntreTiers;

public class PersonnePhysiqueValidator extends ContribuableImpositionPersonnesPhysiquesValidator<PersonnePhysique> {

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
			final String prenom = pp.getPrenomUsuel();

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
		final List<RapportEntreTiers> rapportsMenages = new ArrayList<>();
		final Set<RapportEntreTiers> rapports = pp.getRapportsSujet();
		if (rapports != null) {
			for (RapportEntreTiers r : rapports) {
				if (!r.isAnnule() && TypeRapportEntreTiers.APPARTENANCE_MENAGE == r.getType()) {
					rapportsMenages.add(r);
				}
			}
		}
		rapportsMenages.sort(new DateRangeComparator<>());
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

		final Set<RapportEntreTiers> rapportsSujet = tiers.getRapportsSujet();
		final Set<RapportEntreTiers> rapportsObjet = tiers.getRapportsObjet();

		// [SIFISC-124] Les mesures de tutelles sont mutuellement exclusives (= on ne peut pas en avoir plusieurs actives à un moment donné)
		if (rapportsSujet != null && !rapportsSujet.isEmpty()) {
			List<RapportEntreTiers> mesures = null;
			for (RapportEntreTiers r : rapportsSujet) {
				if (!r.isAnnule() && r instanceof RepresentationLegale) {
					if (mesures == null) {
						mesures = new ArrayList<>();
					}
					mesures.add(r);
				}
			}

			final List<DateRange> intersections = DateRangeHelper.overlaps(mesures);
			if (intersections != null) {
				// génération des messages d'erreur
				for (DateRange range : intersections) {
					results.addError(String.format("La période %s est couverte par plusieurs mesures tutélaires", DateRangeHelper.toDisplayString(range)));
				}
			}
		}

		// [SIFISC-2533] Une personne physique ne peut pas appartenir à plusieurs ménages-communs en même temps
		if (rapportsSujet != null && !rapportsSujet.isEmpty()) {
			List<AppartenanceMenage> menages = null;
			for (RapportEntreTiers r : rapportsSujet) {
				if (!r.isAnnule() && r instanceof AppartenanceMenage) {
					if (menages == null) {
						menages = new ArrayList<>();
					}
					menages.add((AppartenanceMenage) r);
				}
			}

			final List<DateRange> intersections = DateRangeHelper.overlaps(menages);
			if (intersections != null) {
				// génération des messages d'erreur
				for (DateRange range : intersections) {
					results.addError(String.format("La personne physique appartient à plusieurs ménages communs sur la période %s", DateRangeHelper.toDisplayString(range)));
				}
			}
		}

		// [SIFISC-25655] Deux personnes physiques ne peuvent avoir plus d'un lien d'héritage qui les lie
		if (rapportsSujet != null && !rapportsSujet.isEmpty()) {
			// l'héritage ne peut venir plusieurs fois du même défunt... (à cause de l'élection de principal, on peut se retrouver avec
			// plusieurs liens d'héritage vers le même héritier, mais ils ne doivent pas se chevaucher)

			// map, par id de défunt lié-> liste des liens d'héritage
			final Map<Long, List<Heritage>> map = rapportsSujet.stream()
					.filter(AnnulableHelper::nonAnnule)
					.filter(Heritage.class::isInstance)
					.map(Heritage.class::cast)
					.collect(Collectors.toMap(RapportEntreTiers::getObjetId, Collections::singletonList, ListUtils::union));
			for (Map.Entry<Long, List<Heritage>> entry : map.entrySet()) {
				final List<DateRange> intersections = DateRangeHelper.overlaps(entry.getValue());
				if (intersections != null) {
					// génération des messages d'erreur
					for (DateRange range : intersections) {
						results.addError(String.format("La personne physique %s possède des liens d'héritage vers le défunt %s qui se chevauchent sur la période %s",
						                               FormatNumeroHelper.numeroCTBToDisplay(tiers.getNumero()),
						                               FormatNumeroHelper.numeroCTBToDisplay(entry.getKey()),
						                               DateRangeHelper.toDisplayString(range)));
					}
				}
			}
		}

		if (rapportsObjet != null && !rapportsObjet.isEmpty()) {
			// on ne peut léguer plusieurs fois au même héritier... (à cause de l'élection de principal, on peut se retrouver avec
			// plusieurs liens d'héritage vers le même héritier, mais ils ne doivent pas se chevaucher)

			// map, par id d'héritier lié -> liste des liens d'héritage
			final Map<Long, List<Heritage>> map = rapportsObjet.stream()
					.filter(AnnulableHelper::nonAnnule)
					.filter(Heritage.class::isInstance)
					.map(Heritage.class::cast)
					.collect(Collectors.toMap(RapportEntreTiers::getSujetId, Collections::singletonList, ListUtils::union));

			for (Map.Entry<Long, List<Heritage>> entry : map.entrySet()) {
				final List<DateRange> intersections = DateRangeHelper.overlaps(entry.getValue());
				if (intersections != null) {
					// génération des messages d'erreur
					for (DateRange range : intersections) {
						results.addError(String.format("La personne physique %s possède des liens d'héritage vers l'héritier %s qui se chevauchent sur la période %s",
						                               FormatNumeroHelper.numeroCTBToDisplay(tiers.getNumero()),
						                               FormatNumeroHelper.numeroCTBToDisplay(entry.getKey()),
						                               DateRangeHelper.toDisplayString(range)));
					}
				}
			}
		}

		if (rapportsObjet != null && !rapportsObjet.isEmpty()) {

			final List<Heritage> heritages = rapportsObjet.stream()
					.filter(AnnulableHelper::nonAnnule)
					.filter(Heritage.class::isInstance)
					.map(Heritage.class::cast)
					.collect(Collectors.toList());

			if (!heritages.isEmpty()) {

				final List<Heritage> principaux = heritages.stream()
						.filter(h -> h.getPrincipalCommunaute() != null && h.getPrincipalCommunaute())
						.sorted(new DateRangeComparator<>())
						.collect(Collectors.toList());

				// [SIFISC-24999] Les héritages où l'héritier est élu principal sont mutuellement exclusifs (= on ne peut pas en avoir plusieurs actifs à un moment donné)
				final List<DateRange> intersections = DateRangeHelper.overlaps(principaux);
				if (intersections != null) {
					// génération des messages d'erreur
					for (DateRange range : intersections) {
						results.addError(String.format("La période %s est couverte par plusieurs héritages où l'héritier est considéré comme le principal de la communauté d'héritiers", DateRangeHelper.toDisplayString(range)));
					}
				}

				// [SIFISC-24999] Il doit toujours y avoir un principal à tout moment pendant la période de validité de l'héritage
				final DateRange rangeHeritage = DateRangeHelper.getOverallRange(heritages);
				if (!DateRangeHelper.isFullyCovered(rangeHeritage, principaux)) {
					results.addError(String.format("La période de validité de l'héritage %s ne possède pas des héritiers désignés comme principaux en continu", DateRangeHelper.toDisplayString(rangeHeritage)));
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
