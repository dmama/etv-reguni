package ch.vd.uniregctb.validation.tiers;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.MovingWindow;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DecisionAci;
import ch.vd.uniregctb.tiers.ForDebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForFiscalAutreElementImposable;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.ForsParType;
import ch.vd.uniregctb.validation.ValidationService;

/**
 * Validateur qui se préoccupe de la partie Contribuable d'un tiers contribuable
 */
public abstract class ContribuableValidator<T extends Contribuable> extends TiersValidator<T> {

	@Override
	public ValidationResults validate(T ctb) {
		final ValidationResults vr = super.validate(ctb);
		if (!ctb.isAnnule()) {
			vr.merge(validateDecisions(ctb));
		}
		return vr;
	}

	@NotNull
	private static <T> List<T> neverNull(List<T> list) {
		return list == null ? Collections.<T>emptyList() : list;
	}

	@Override
	protected ValidationResults validateFors(T ctb) {
		final ValidationResults vr = super.validateFors(ctb);

		// les plages de validité des fors principaux ne doivent pas se chevaucher
		final List<? extends ForFiscalPrincipal> forsPrincipaux = neverNull(ctb.getForsFiscauxPrincipauxActifsSorted());
		final MovingWindow<ForFiscalPrincipal> movingWindow = new MovingWindow<>(forsPrincipaux);
		while (movingWindow.hasNext()) {
			final MovingWindow.Snapshot<ForFiscalPrincipal> snapshot = movingWindow.next();
			final ForFiscalPrincipal current = snapshot.getCurrent();
			final ForFiscalPrincipal next = snapshot.getNext();
			if (current != null && next != null && DateRangeHelper.intersect(current, next)) {
				vr.addError(String.format("Le for principal qui commence le %s chevauche le for précédent", RegDateHelper.dateToDisplayString(next.getDateDebut())));
			}
		}

		final ForsParType fors = ctb.getForsParType(true);

		// Pour chaque for secondaire il doit exister un for principal valide
		for (ForFiscalSecondaire fs : fors.secondaires) {
			if (!Contribuable.existForPrincipal(forsPrincipaux, fs.getDateDebut(), fs.getDateFin())) {
				String msg = String.format("Il n'y a pas de for principal pour accompagner le for secondaire qui commence le %s", RegDateHelper.dateToDisplayString(fs.getDateDebut()));
				if (fs.getDateFin() != null) {
					msg += String.format(" et se termine le %s", RegDateHelper.dateToDisplayString(fs.getDateFin()));
				}
				vr.addError(msg);
			}
		}

		// Pour chaque for autre élément imposable il doit exister un for principal valide
		for (ForFiscalAutreElementImposable fs : fors.autreElementImpot) {
			if (!Contribuable.existForPrincipal(forsPrincipaux, fs.getDateDebut(), fs.getDateFin())) {
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

	private ValidationResults validateDecisions(Contribuable ctb) {

		final ValidationResults results = new ValidationResults();
		if (ctb.getDecisionsAci() == null) {
			return results;
		}

		final Set<DecisionAci> decisionAcis = ctb.getDecisionsAci();
		// On valide toutes les décisions
		final ValidationService validationService = getValidationService();
		for (DecisionAci d : decisionAcis) {
			results.merge(validationService.validate(d));
		}

		return results;
	}
}
