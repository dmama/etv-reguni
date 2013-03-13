package ch.vd.uniregctb.metier.assujettissement;

import java.util.List;

import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.type.MotifFor;

public class FractionnementsSource extends Fractionnements {

	public FractionnementsSource(List<ForFiscalPrincipal> principaux) {
		super(principaux);
	}

	@Override
	protected boolean isFractionOuverture(ForFiscalPrincipalContext forPrincipal) {
		final ForFiscalPrincipal previous = forPrincipal.previous;
		final ForFiscalPrincipal current = forPrincipal.current;

		final MotifFor motifOuverture = current.getMotifOuverture();

		boolean fraction = false;

		if (motifOuverture == MotifFor.VEUVAGE_DECES) {
			// fractionnement systématique à la date d'ouverture pour ce motif
			fraction = true;
		}
		else if (AssujettissementServiceImpl.isDepartOuArriveeHorsSuisse(previous, current)) {
			// fractionnement en cas d'arrivée hors-Suisse
			fraction = true;
		}

		return fraction;
	}

	@Override
	protected boolean isFractionFermeture(ForFiscalPrincipalContext forPrincipal) {
		final ForFiscalPrincipal current = forPrincipal.current;
		final ForFiscalPrincipal next = forPrincipal.next;

		if (current.getDateFin() == null) {
			return false;
		}

		final MotifFor motifFermeture = current.getMotifFermeture();

		boolean fraction = false;

		if (motifFermeture == MotifFor.VEUVAGE_DECES) {
			// fractionnement systématique à la date de fermeture pour ce motif
			fraction = true;
		}
		else if (AssujettissementServiceImpl.isDepartOuArriveeHorsSuisse(current, next)) {
			// fractionnement en cas de départ hors-Suisse
			fraction = true;
		}

		return fraction;
	}
}
