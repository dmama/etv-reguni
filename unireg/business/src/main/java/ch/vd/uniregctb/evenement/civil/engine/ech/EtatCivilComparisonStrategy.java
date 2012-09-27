package ch.vd.uniregctb.evenement.civil.engine.ech;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.civil.data.EtatCivil;
import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;
import ch.vd.uniregctb.common.DataHolder;

/**
 * Comparateur d'individu basé sur l'état civil de l'individu
 */
public class EtatCivilComparisonStrategy implements IndividuComparisonStrategy {

	private static final String ATTRIBUT = "état civil";
	private static final String DATES = "dates";

    @Override
	public boolean isFiscalementNeutre(IndividuApresEvenement originel, IndividuApresEvenement corrige, @NotNull DataHolder<String> msg) {
	    // à un instant donné (et juste après un événement répond à cette catégorisation), un individu n'a au plus qu'un seul état civil
	    // -> on peut se baser sur l'état civil "courant"
	    final EtatCivil ecOriginel = originel.getIndividu().getEtatCivilCourant();
	    final EtatCivil ecCorrige = corrige.getIndividu().getEtatCivilCourant();
	    final IndividuComparisonHelper.FieldMonitor monitor = new IndividuComparisonHelper.FieldMonitor();
	    boolean neutre = true;
	    if (ecOriginel != null && ecCorrige != null) {
		    neutre = ecOriginel.getTypeEtatCivil() == ecCorrige.getTypeEtatCivil() &&
				     IndividuComparisonHelper.RANGE_EQUALATOR.areEqual(ecOriginel, ecCorrige, monitor, DATES);
		    if (!neutre) {
			    IndividuComparisonHelper.fillMonitor(monitor, ATTRIBUT);
		    }
	    }
	    else if (ecOriginel != null || ecCorrige != null) {
		    IndividuComparisonHelper.fillMonitorWithApparitionDisparition(ecOriginel == null, monitor, ATTRIBUT);
		    neutre = false;
	    }

	    if (!neutre) {
		    msg.set(IndividuComparisonHelper.buildErrorMessage(monitor));
		}
	    return neutre;
    }
}