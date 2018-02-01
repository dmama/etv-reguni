package ch.vd.unireg.evenement.civil.engine.ech;

import java.util.List;

import org.apache.commons.lang3.mutable.Mutable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;
import ch.vd.unireg.interfaces.civil.data.Permis;
import ch.vd.unireg.interfaces.civil.data.PermisList;
import ch.vd.unireg.type.TypePermis;

/**
 * Comparateur d'individu basé sur les permis de l'individu
 */
public class PermisComparisonStrategy implements IndividuComparisonStrategy {

	private static final String ATTRIBUT = "permis";
	private static final String DATES = "dates";

	@Nullable
	private static Permis getPermisC(List<Permis> list) {
		for (Permis p : list) {
			if (p.getTypePermis() == TypePermis.ETABLISSEMENT && p.getDateAnnulation() == null) {
				return p;
			}
		}
		return null;
	}

	/**
	 * @param list la liste des permis connus
	 * @return <code>true</code> s'il existe au moins un permis non-annulé dans la liste
	 */
	private static boolean hasPermitAtAll(List<Permis> list) {
		if (list != null && list.size() > 0) {
			for (Permis p : list) {
				if (p.getDateAnnulation() == null) {
					return true;
				}
			}
		}
		return false;
	}

    @Override
	public boolean isFiscalementNeutre(IndividuApresEvenement originel, IndividuApresEvenement corrige, @NotNull Mutable<String> msg) {

	    final PermisList permis1 = originel.getIndividu().getPermis();
	    final PermisList permis2 = corrige.getIndividu().getPermis();
	    final boolean hasPermit1 = hasPermitAtAll(permis1);
	    final boolean hasPermit2 = hasPermitAtAll(permis2);
	    final IndividuComparisonHelper.FieldMonitor monitor = new IndividuComparisonHelper.FieldMonitor();
	    boolean neutre = true;
	    if (hasPermit1 || hasPermit2) {
		    if (hasPermit1 != hasPermit2) {
			    // information présente d'un côté, mais pas de l'autre...
			    IndividuComparisonHelper.fillMonitorWithApparitionDisparition(hasPermit2, monitor, ATTRIBUT);
			    neutre = false;
		    }
		    else {
			    // ici, on a des permis des deux côtés... mais quels sont-ils ?
			    final Permis p1 = getPermisC(permis1);
			    final Permis p2 = getPermisC(permis2);
			    if (p1 != null && p2 != null) {
				    neutre = IndividuComparisonHelper.RANGE_EQUALATOR.areEqual(p1, p2, monitor, DATES);
			    }
			    else if (p1 != null || p2 != null) {
				    neutre = false;
			    }
			    if (!neutre) {
				    IndividuComparisonHelper.fillMonitor(monitor, ATTRIBUT);
			    }
		    }

		    if (!neutre) {
			    msg.setValue(IndividuComparisonHelper.buildErrorMessage(monitor));
		    }
	    }
	    return neutre;
    }
}