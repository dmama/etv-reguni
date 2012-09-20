package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;
import ch.vd.unireg.interfaces.civil.data.Permis;
import ch.vd.unireg.interfaces.civil.data.PermisList;
import ch.vd.uniregctb.common.DataHolder;
import ch.vd.uniregctb.type.TypePermis;

/**
 * Comparateur d'individu basé sur les permis de l'individu
 */
public class PermisComparisonStrategy implements IndividuComparisonStrategy {

	private static final String ATTRIBUT = "permis";

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
	public boolean isFiscalementNeutre(IndividuApresEvenement originel, IndividuApresEvenement corrige, @NotNull DataHolder<String> msg) {

	    final PermisList permis1 = originel.getIndividu().getPermis();
	    final PermisList permis2 = corrige.getIndividu().getPermis();
	    final boolean hasPermit1 = hasPermitAtAll(permis1);
	    final boolean hasPermit2 = hasPermitAtAll(permis2);
	    boolean neutre = true;
	    if (hasPermit1 || hasPermit2) {
		    if (hasPermit1 != hasPermit2) {
			    // information présente d'un côté, mais pas de l'autre...
			    neutre = false;
		    }
		    else {
			    // ici, on a des permis des deux côtés... mais quels sont-ils ?
			    final Permis p1 = getPermisC(permis1);
			    final Permis p2 = getPermisC(permis2);
			    neutre = (p1 == null && p2 == null) || IndividuComparisonHelper.RANGE_EQUALATOR.areEqual(p1, p2);
		    }
	    }
	    if (!neutre) {
		    msg.set(ATTRIBUT);
	    }
	    return neutre;
    }
}