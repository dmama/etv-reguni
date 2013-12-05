package ch.vd.uniregctb.metier.assujettissement;

import java.util.List;

import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.type.MotifFor;

public class FractionnementsSource extends Fractionnements {

	public FractionnementsSource(List<ForFiscalPrincipal> principaux) {
		super(principaux);
	}

	@Override
	protected Fraction isFractionOuverture(ForFiscalPrincipalContext forPrincipal) {
		final ForFiscalPrincipal previous = forPrincipal.previous;
		final ForFiscalPrincipal current = forPrincipal.current;

		final MotifFor motifOuverture = current.getMotifOuverture();

		Fraction fraction = null;

		if (motifOuverture == MotifFor.VEUVAGE_DECES) {
			// fractionnement systématique à la date d'ouverture pour ce motif
			fraction = new FractionSimple(current.getDateDebut(), motifOuverture, null);
		}
		else if (AssujettissementServiceImpl.isDepartOuArriveeHorsSuisse(previous, current)) {
			// fractionnement en cas d'arrivée hors-Suisse
			fraction = new FractionSimple(current.getDateDebut(), motifOuverture, null);
		}

		return fraction;
	}

	@Override
	protected Fraction isFractionFermeture(ForFiscalPrincipalContext forPrincipal) {
		final ForFiscalPrincipal current = forPrincipal.current;
		final ForFiscalPrincipal next = forPrincipal.next;

		if (current.getDateFin() == null) {
			return null;
		}

		final MotifFor motifFermeture = current.getMotifFermeture();

		Fraction fraction = null;

		if (motifFermeture == MotifFor.VEUVAGE_DECES) {
			// fractionnement systématique à la date de fermeture pour ce motif
			fraction = new FractionSimple(current.getDateFin().getOneDayAfter(), null, motifFermeture);
		}
		else if (AssujettissementServiceImpl.isDepartOuArriveeHorsSuisse(current, next)) {
			// fractionnement en cas de départ hors-Suisse
			fraction = new FractionSimple(current.getDateFin().getOneDayAfter(), null, motifFermeture);
		}

		return fraction;
	}
}
