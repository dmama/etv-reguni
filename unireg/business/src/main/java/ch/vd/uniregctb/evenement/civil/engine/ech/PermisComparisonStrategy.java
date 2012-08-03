package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.Comparator;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;
import ch.vd.unireg.interfaces.civil.data.Permis;
import ch.vd.uniregctb.common.DataHolder;

/**
 * Comparateur d'individu basé sur les permis de l'individu
 */
public class PermisComparisonStrategy implements IndividuComparisonStrategy {

	private static final String ATTRIBUT = "permis";

	private static final Comparator<Permis> PERMIS_COMPARATOR = new IndividuComparisonHelper.NullableComparator<Permis>(true) {
		@Override
		protected int compareNonNull(@NotNull Permis o1, @NotNull Permis o2) {
			int comparison = NullDateBehavior.EARLIEST.compare(o1.getDateAnnulation(), o2.getDateAnnulation());
			if (comparison == 0) {
				comparison = IndividuComparisonHelper.RANGE_COMPARATOR.compare(o1, o2);
				if (comparison == 0) {
					comparison = o1.getTypePermis().ordinal() - o2.getTypePermis().ordinal();
				}
			}
			return comparison;
		}
	};

	private static final IndividuComparisonHelper.Equalator<Permis> PERMIS_EQUALATOR = new IndividuComparisonHelper.NullableEqualator<Permis>() {
		@Override
		protected boolean areNonNullEqual(@NotNull Permis o1, @NotNull Permis o2) {
			if (o1.getDateAnnulation() != null && o2.getDateAnnulation() != null) {
				return true;
			}
			else if (o1.getDateAnnulation() != null || o2.getDateAnnulation() != null) {
				return false;
			}

			return IndividuComparisonHelper.RANGE_EQUALATOR.areEqual(o1, o2) && o1.getTypePermis() == o2.getTypePermis();
		}
	};

    @Override
	public boolean sansDifferenceFiscalementImportante(IndividuApresEvenement originel, IndividuApresEvenement corrige, @NotNull DataHolder<String> msg) {
	    if (!IndividuComparisonHelper.areContentsEqual(originel.getIndividu().getPermis(), corrige.getIndividu().getPermis(), PERMIS_COMPARATOR, PERMIS_EQUALATOR)) {
		    msg.set(ATTRIBUT);
		    return false;
	    }
	    return true;
    }
}