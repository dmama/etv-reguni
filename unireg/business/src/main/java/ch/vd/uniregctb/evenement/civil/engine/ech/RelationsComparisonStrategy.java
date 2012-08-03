package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.Comparator;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;
import ch.vd.unireg.interfaces.civil.data.RelationVersIndividu;
import ch.vd.uniregctb.common.DataHolder;

/**
 * Comparateur d'individu basé sur les relations (conjoints + filiations) de l'individu
 */
public class RelationsComparisonStrategy implements IndividuComparisonStrategy {

	private static final String ATTRIBUT = "relations";

	private static final Comparator<RelationVersIndividu> RELATION_COMPARATOR = new IndividuComparisonHelper.NullableComparator<RelationVersIndividu>(true) {
		@Override
		protected int compareNonNull(@NotNull RelationVersIndividu o1, @NotNull RelationVersIndividu o2) {
			int comparison = IndividuComparisonHelper.RANGE_COMPARATOR.compare(o1, o2);
			if (comparison == 0) {
				comparison = Long.signum(o1.getNumeroAutreIndividu() - o2.getNumeroAutreIndividu());
			}
			return comparison;
		}
	};

	private static final IndividuComparisonHelper.Equalator<RelationVersIndividu> RELATION_EQUALATOR = new IndividuComparisonHelper.NullableEqualator<RelationVersIndividu>() {
		@Override
		protected boolean areNonNullEqual(@NotNull RelationVersIndividu o1, @NotNull RelationVersIndividu o2) {
			return IndividuComparisonHelper.RANGE_EQUALATOR.areEqual(o1, o2) && o1.getNumeroAutreIndividu() == o2.getNumeroAutreIndividu();
		}
	};

	@Override
	public boolean sansDifferenceFiscalementImportante(IndividuApresEvenement originel, IndividuApresEvenement corrige, @NotNull DataHolder<String> msg) {
		// les différences de relations sont à chercher dans les conjoints et les filiations
		if (!IndividuComparisonHelper.areContentsEqual(originel.getIndividu().getConjoints(), corrige.getIndividu().getConjoints(), RELATION_COMPARATOR, RELATION_EQUALATOR)
				|| !IndividuComparisonHelper.areContentsEqual(originel.getIndividu().getEnfants(), corrige.getIndividu().getEnfants(), RELATION_COMPARATOR, RELATION_EQUALATOR)
				|| !IndividuComparisonHelper.areContentsEqual(originel.getIndividu().getParents(), corrige.getIndividu().getParents(), RELATION_COMPARATOR, RELATION_EQUALATOR)) {
			msg.set(ATTRIBUT);
			return false;
		}
		return true;
	}
}