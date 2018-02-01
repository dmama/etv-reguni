package ch.vd.unireg.metier.assujettissement;

import java.util.List;

import ch.vd.unireg.metier.common.ForFiscalPrincipalContext;
import ch.vd.unireg.metier.common.Fraction;
import ch.vd.unireg.metier.common.FractionSimple;
import ch.vd.unireg.metier.common.Fractionnements;
import ch.vd.unireg.tiers.ForFiscalPrincipalPM;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeAutoriteFiscale;

/**
 * Les fractionnements de l'assujettissement des PM
 */
public class FractionnementsAssujettissementPM extends Fractionnements<ForFiscalPrincipalPM> {

	public FractionnementsAssujettissementPM(List<ForFiscalPrincipalPM> principaux) {
		super(principaux);
	}

	@Override
	protected Fraction isFractionOuverture(ForFiscalPrincipalContext<ForFiscalPrincipalPM> forPrincipal) {

		final ForFiscalPrincipalPM previous = forPrincipal.getPrevious();
		final ForFiscalPrincipalPM current = forPrincipal.getCurrent();

		// de manière générale, un départ ou une arrivée HS doit fractionner l'assujettissement à la date de départ / d'arrivée
		if (AssujettissementPersonnesMoralesCalculator.isDepartOuArriveeHorsSuisse(previous, current)) {
			return new FractionSimple(current.getDateDebut(), current.getMotifOuverture(), null);
		}

		// en attendant un motif spécifique pour la création d'entreprise, on va dire qu'un for principal juste après un
		// trou (= période sans for principal) cause un fractionnement à l'ouverture, sauf bien-sûr le cas de l'arrivée/du départ HC
		if (current.getTypeAutoriteFiscale() != TypeAutoriteFiscale.COMMUNE_HC
				&& !AssujettissementPersonnesMoralesCalculator.isDepartOuArriveeHorsCanton(previous, current)
				&& (previous == null || previous.getDateFin() != current.getDateDebut().getOneDayBefore())) {
			return new FractionSimple(current.getDateDebut(), current.getMotifOuverture(), null);
		}

		return null;
	}

	@Override
	protected Fraction isFractionFermeture(ForFiscalPrincipalContext<ForFiscalPrincipalPM> forPrincipal) {

		final ForFiscalPrincipalPM current = forPrincipal.getCurrent();
		final ForFiscalPrincipalPM next = forPrincipal.getNext();

		// de manière générale, un départ ou une arrivée HS doit fractionner l'assujettissement à la date de départ / d'arrivée
		if (AssujettissementPersonnesMoralesCalculator.isDepartOuArriveeHorsSuisse(current, next)) {
			return new FractionSimple(current.getDateFin().getOneDayAfter(), null, current.getMotifFermeture());
		}

		// en attendant un motif spécifique pour la radiation d'entreprise, on va dire qu'un for principal juste avant un
		// trou (= période sans for principal) cause un fractionnement à la fermeture, sauf bien-sûr le cas de l'arrivée/du départ HC
		// [SIFISC-17850] La faillite ne doit pas arrêter l'assujettissement, qui doit se poursuivre jusqu'à la fin de l'exercice commercial courant
		if (current.getDateFin() != null
				&& current.getTypeAutoriteFiscale() != TypeAutoriteFiscale.COMMUNE_HC
				&& !AssujettissementPersonnesMoralesCalculator.isDepartOuArriveeHorsCanton(current, next)
				&& !(next == null && current.getMotifFermeture() == MotifFor.FAILLITE)
				&& (next == null || next.getDateDebut() != current.getDateFin().getOneDayAfter())) {
			return new FractionSimple(current.getDateFin().getOneDayAfter(), current.getMotifFermeture(), null);
		}

		return null;
	}
}
