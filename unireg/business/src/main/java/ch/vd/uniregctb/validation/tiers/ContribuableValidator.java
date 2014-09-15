package ch.vd.uniregctb.validation.tiers;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DecisionAci;
import ch.vd.uniregctb.tiers.ForDebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForFiscalAutreElementImposable;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.ForsParType;
import ch.vd.uniregctb.tiers.SituationFamille;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.validation.ValidationService;

/**
 * Validateur qui se préoccupe de la partie Contribuable d'un tiers contribuable
 */
public abstract class ContribuableValidator<T extends Contribuable> extends TiersValidator<T> {

	@Override
	public ValidationResults validate(T ctb) {
		final ValidationResults vr = super.validate(ctb);
		if (!ctb.isAnnule()) {
			vr.merge(validateSituationsFamille(ctb));
			vr.merge(validateDecisions(ctb));
		}
		return vr;
	}

	@Override
	protected ValidationResults validateFors(T ctb) {
		final ValidationResults vr = super.validateFors(ctb);

		final ForsParType fors = ctb.getForsParType(true /* triés par ordre chronologique */);

		// Les plages de validité des fors principaux ne doivent pas se chevaucher
		ForFiscalPrincipal lastFor = null;
		for (ForFiscalPrincipal ffp : fors.principaux) {
			if (lastFor != null && DateRangeHelper.intersect(lastFor, ffp)) {
				vr.addError(String.format("Le for principal qui commence le %s chevauche le for précédent", RegDateHelper.dateToDisplayString(ffp.getDateDebut())));
			}
			lastFor = ffp;
		}

		// [SIFISC-57] pour les fors fiscaux principaux HC/HS avec le mode d'imposition "source", il est anormal d'avoir des fors secondaires
		for (ForFiscalPrincipal ffp : fors.principaux) {
			if (ffp.getTypeAutoriteFiscale() != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
				final String typeName = ffp.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_HC ? "hors-canton" : "hors-Suisse";
				if (ffp.getModeImposition() == ModeImposition.SOURCE) {
					final List<DateRange> intersections = DateRangeHelper.intersections(ffp, fors.secondaires);
					if (intersections != null) {
						vr.addWarning(String.format("Le mode d'imposition \"source\" du for principal %s " +
								"qui commence le %s est anormal en présence de fors secondaires", typeName, RegDateHelper.dateToDisplayString(ffp.getDateDebut())));
					}
				}
			}
		}

		// Pour chaque for secondaire il doit exister un for principal valide
		for (ForFiscalSecondaire fs : fors.secondaires) {
			if (!Contribuable.existForPrincipal(fors.principaux, fs.getDateDebut(), fs.getDateFin())) {
				String msg = String.format("Il n'y a pas de for principal pour accompagner le for secondaire qui commence le %s", RegDateHelper.dateToDisplayString(fs.getDateDebut()));
				if (fs.getDateFin() != null) {
					msg += String.format(" et se termine le %s", RegDateHelper.dateToDisplayString(fs.getDateFin()));
				}
				vr.addError(msg);
			}
		}

		// Pour chaque for autre élément imposable il doit exister un for principal valide
		for (ForFiscalAutreElementImposable fs : fors.autreElementImpot) {
			if (!Contribuable.existForPrincipal(fors.principaux, fs.getDateDebut(), fs.getDateFin())) {
				String msg = String.format("Il n'y a pas de for principal pour accompagner le for autre élément imposable qui commence le %s", RegDateHelper.dateToDisplayString(fs.getDateDebut()));
				if (fs.getDateFin() != null) {
					msg += String.format(" et se termine le %s", RegDateHelper.dateToDisplayString(fs.getDateFin()));
				}
				vr.addError(msg);
			}
		}

		// Les for DPI ne sont pas autorisés
		for (ForDebiteurPrestationImposable fpdi : fors.dpis) {
			vr.addError("Le for " + fpdi + " n'est pas un type de for autorisé sur un contribuable.");
		}

		return vr;

	}

	private ValidationResults validateSituationsFamille(Contribuable ctb) {

		final ValidationResults results = new ValidationResults();

		final List<SituationFamille> situationsSorted = ctb.getSituationsFamilleSorted();
		if (situationsSorted != null) {

			// On valide toutes les situations de familles pour elles-mêmes
			final ValidationService validationService = getValidationService();
			for (SituationFamille s : situationsSorted) {
				results.merge(validationService.validate(s));
			}

			// Les plages de validité des situations de familles ne doivent pas se chevaucher
			SituationFamille lastSituation = null;
			for (SituationFamille situation : situationsSorted) {
				if (lastSituation != null && DateRangeHelper.intersect(lastSituation, situation)) {
					results.addError(String.format("La situation de famille qui commence le %s chevauche la situation précédente", RegDateHelper.dateToDisplayString(situation.getDateDebut())));
				}
				lastSituation = situation;
			}
		}

		return results;
	}


	private ValidationResults validateDecisions(Contribuable ctb) {

		final ValidationResults results = new ValidationResults();
		if (ctb.getDecisionsAci() == null) {
			return results;
		}

		final List<DecisionAci> decisionAcis = new ArrayList<>(ctb.getDecisionsAci());
		// On valide toutes les décisions
		final ValidationService validationService = getValidationService();
		for (DecisionAci d : decisionAcis) {
			results.merge(validationService.validate(d));
		}

		return results;
	}
}
