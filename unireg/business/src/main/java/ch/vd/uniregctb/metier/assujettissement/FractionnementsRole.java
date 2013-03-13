package ch.vd.uniregctb.metier.assujettissement;

import java.util.List;

import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;

public class FractionnementsRole extends Fractionnements {

	public FractionnementsRole(List<ForFiscalPrincipal> principaux) {
		super(principaux);
	}

	@Override
	protected boolean isFractionOuverture(ForFiscalPrincipalContext forPrincipal) {
		final ForFiscalPrincipal previous = forPrincipal.previous;
		final ForFiscalPrincipal current = forPrincipal.current;
		final ForFiscalPrincipal next = forPrincipal.next;

		final MotifFor motifOuverture = current.getMotifOuverture();

		boolean fraction = false;

		if (motifOuverture == MotifFor.VEUVAGE_DECES) {
			// fractionnement systématique à la date d'ouverture pour ce motif
			fraction = true;
		}
		else if (AssujettissementServiceImpl.isDepartOuArriveeHorsSuisse(previous, current) &&
				AssujettissementServiceImpl.isDepartDepuisOuArriveeVersVaud(current, previous) &&
				!AssujettissementServiceImpl.isDepartHCApresArriveHSMemeAnnee(current, next)) {
			// De manière générale, les transitions Suisse <-> Hors-Suisse provoquent des fractionnements
			// [UNIREG-1742] le départ hors-Suisse depuis hors-canton ne doit pas fractionner la période d'assujettissement (car le rattachement économique n'est pas interrompu)
			// [UNIREG-2759] l'arrivée de hors-Suisse ne doit pas fractionner si le for se ferme dans la même année avec un départ hors-canton
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
		else if (AssujettissementServiceImpl.isDepartOuArriveeHorsSuisse(current, next) &&
				AssujettissementServiceImpl.isDepartDepuisOuArriveeVersVaud(current, next) &&
				!AssujettissementServiceImpl.isDepartHCApresArriveHSMemeAnnee(next, forPrincipal.nextnext)) {
			// De manière générale, les transitions Suisse <-> Hors-Suisse provoquent des fractionnements
			// [UNIREG-1742] le départ hors-Suisse depuis hors-canton ne doit pas fractionner la période d'assujettissement (car le rattachement économique n'est pas interrompu)
			// [UNIREG-2759] l'arrivée de hors-Suisse ne doit pas fractionner si le for se ferme dans la même année avec un départ hors-canton
			fraction = true;
		}
		else if (AssujettissementServiceImpl.isDepartDansHorsCanton(current, next) && current.getModeImposition() == ModeImposition.MIXTE_137_2) {
			// [SIFISC-7281] le départ hors-canton d'un sourcier mixte 137 al2 doit fractionner la période d'assujettissement
			fraction = true;
		}

		return fraction;
	}
}
