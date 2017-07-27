package ch.vd.uniregctb.validation.tiers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdresseCivile;
import ch.vd.uniregctb.adresse.AdressePM;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.common.AnnulableHelper;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.tiers.AppartenanceMenage;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.Heritage;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.RepresentationLegale;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

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
			// l'héritage ne peut venir plusieurs fois du même défunt...
			// map, par id de défunt lié, du nombre d'occurrences de lien non-annulé
			final Map<Long, Integer> idsDefunts = rapportsSujet.stream()
					.filter(AnnulableHelper::nonAnnule)
					.filter(Heritage.class::isInstance)
					.map(RapportEntreTiers::getObjetId)
					.filter(Objects::nonNull)               // blindage contre le rapport incomplet (écrans supergra ?)
					.collect(Collectors.toMap(Function.identity(), id -> 1, (nb1, nb2) -> nb1 + nb2));
			for (Map.Entry<Long, Integer> entry : idsDefunts.entrySet()) {
				if (entry.getValue() > 1) {
					results.addError(String.format("La personne physique %s possède plusieurs liens d'héritage vers le défunt %s",
					                               FormatNumeroHelper.numeroCTBToDisplay(tiers.getNumero()),
					                               FormatNumeroHelper.numeroCTBToDisplay(entry.getKey())));
				}
			}
		}
		if (rapportsObjet != null && !rapportsObjet.isEmpty()) {
			// on ne peut léguer plusieurs fois au même héritier...
			// map, par id d'héritier lié, du nombre d'occurrences de lien non-annulé
			final Map<Long, Integer> idsHeritiers = rapportsObjet.stream()
					.filter(AnnulableHelper::nonAnnule)
					.filter(Heritage.class::isInstance)
					.map(RapportEntreTiers::getSujetId)
					.filter(Objects::nonNull)           // blindage contre le rapport incomplet (écrans supergra ?)
					.collect(Collectors.toMap(Function.identity(), id -> 1, (nb1, nb2) -> nb1 + nb2));
			for (Map.Entry<Long, Integer> entry : idsHeritiers.entrySet()) {
				if (entry.getValue() > 1) {
					results.addError(String.format("La personne physique %s possède plusieurs liens d'héritage vers l'héritier %s",
					                               FormatNumeroHelper.numeroCTBToDisplay(tiers.getNumero()),
					                               FormatNumeroHelper.numeroCTBToDisplay(entry.getKey())));
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
