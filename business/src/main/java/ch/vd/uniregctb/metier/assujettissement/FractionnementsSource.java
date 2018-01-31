package ch.vd.uniregctb.metier.assujettissement;

import java.util.List;

import ch.vd.uniregctb.metier.common.ForFiscalPrincipalContext;
import ch.vd.uniregctb.metier.common.Fraction;
import ch.vd.uniregctb.metier.common.FractionSimple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPP;
import ch.vd.uniregctb.type.MotifFor;

public class FractionnementsSource extends FractionnementsAssujettissementPP {

	public FractionnementsSource(List<ForFiscalPrincipalPP> principaux) {
		super(principaux);
	}

	@Override
	protected Fraction isFractionOuverture(ForFiscalPrincipalContext<ForFiscalPrincipalPP> forPrincipal) {
		final ForFiscalPrincipalPP previous = forPrincipal.getPrevious();
		final ForFiscalPrincipalPP current = forPrincipal.getCurrent();

		final MotifFor motifOuverture = current.getMotifOuverture();

		Fraction fraction = null;

		if (motifOuverture == MotifFor.VEUVAGE_DECES) {
			// fractionnement systématique à la date d'ouverture pour ce motif
			fraction = new FractionSimple(current.getDateDebut(), motifOuverture, null);
		}
		else if (AssujettissementPersonnesPhysiquesCalculator.isDepartOuArriveeHorsSuisse(previous, current)) {
			// fractionnement en cas d'arrivée hors-Suisse
			fraction = new FractionSimple(current.getDateDebut(), motifOuverture, null);
		}

		checkMotifSurFractionOuverture(forPrincipal, fraction);
		return fraction;
	}

	@Override
	protected Fraction isFractionFermeture(ForFiscalPrincipalContext<ForFiscalPrincipalPP> forPrincipal) {
		final ForFiscalPrincipalPP current = forPrincipal.getCurrent();
		final ForFiscalPrincipalPP next = forPrincipal.getNext();

		if (current.getDateFin() == null) {
			return null;
		}

		final MotifFor motifFermeture = current.getMotifFermeture();

		Fraction fraction = null;

		if (motifFermeture == MotifFor.VEUVAGE_DECES) {
			// fractionnement systématique à la date de fermeture pour ce motif
			fraction = new FractionSimple(current.getDateFin().getOneDayAfter(), null, motifFermeture);
		}
		else if (AssujettissementPersonnesPhysiquesCalculator.isDepartOuArriveeHorsSuisse(current, next)) {
			// fractionnement en cas de départ hors-Suisse
			fraction = new FractionSimple(current.getDateFin().getOneDayAfter(), null, motifFermeture);
		}

		return fraction;
	}
}
