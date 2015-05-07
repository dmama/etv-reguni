package ch.vd.uniregctb.validation.tiers;

import java.util.List;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPP;
import ch.vd.uniregctb.tiers.ForsParType;
import ch.vd.uniregctb.tiers.SituationFamille;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.validation.ValidationService;

public abstract class ContribuableImpositionPersonnesPhysiquesValidator<T extends ContribuableImpositionPersonnesPhysiques> extends ContribuableValidator<T> {

	@Override
	public ValidationResults validate(T ctb) {
		final ValidationResults vr = super.validate(ctb);
		if (!ctb.isAnnule()) {
			vr.merge(validateSituationsFamille(ctb));
		}
		return vr;
	}

	private ValidationResults validateSituationsFamille(ContribuableImpositionPersonnesPhysiques ctb) {

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

	@Override
	protected ValidationResults validateFors(T ctb) {
		final ValidationResults vr = super.validateFors(ctb);

		final ForsParType fors = ctb.getForsParType(true);

		// [SIFISC-57] pour les fors fiscaux principaux HC/HS avec le mode d'imposition "source", il est anormal d'avoir des fors secondaires
		// [SIFISC-13774] règle également appliquée aux fors principaux vaudois
		for (ForFiscalPrincipalPP ffp : fors.principauxPP) {
			if (ffp.getModeImposition() == ModeImposition.SOURCE) {
				if (DateRangeHelper.intersect(ffp, fors.secondaires)) {
					vr.addWarning(String.format("Le mode d'imposition \"source\" du for principal qui commence le %s est anormal en présence de fors secondaires",
					                            RegDateHelper.dateToDisplayString(ffp.getDateDebut())));
				}
			}
		}

		return vr;
	}
}
