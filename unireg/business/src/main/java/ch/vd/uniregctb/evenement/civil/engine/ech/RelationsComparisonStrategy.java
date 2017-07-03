package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.Comparator;

import org.apache.commons.lang3.mutable.Mutable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;
import ch.vd.unireg.interfaces.civil.data.RelationVersIndividu;

/**
 * Comparateur d'individu basé sur les relations (conjoints + filiations) de l'individu
 */
public class RelationsComparisonStrategy implements IndividuComparisonStrategy {

	private static final String ATTRIBUT = "relations";
	private static final String DATES = "dates";
	private static final String CONJOINTS = "conjoints";
	private static final String PARENTS = "parents";

	private static final Comparator<RelationVersIndividu> RELATION_COMPARATOR = Comparator.nullsLast(((Comparator<RelationVersIndividu>) DateRangeComparator::compareRanges)
			                                                                                                 .thenComparingLong(RelationVersIndividu::getNumeroAutreIndividu));

	private static final IndividuComparisonHelper.Equalator<RelationVersIndividu> RELATION_EQUALATOR = new IndividuComparisonHelper.NullableEqualator<RelationVersIndividu>() {
		@Override
		protected boolean areNonNullEqual(@NotNull RelationVersIndividu o1, @NotNull RelationVersIndividu o2, @Nullable IndividuComparisonHelper.FieldMonitor monitor, @Nullable String fieldName) {
			// on se fiche du sexe de l'autre personne (enfant, parent, conjoint..)
			if (!IndividuComparisonHelper.RANGE_EQUALATOR.areEqual(o1, o2, monitor, DATES) || o1.getNumeroAutreIndividu() != o2.getNumeroAutreIndividu()) {
				IndividuComparisonHelper.fillMonitor(monitor, fieldName);
				return false;
			}
			return true;
		}
	};

	@Override
	public boolean isFiscalementNeutre(IndividuApresEvenement originel, IndividuApresEvenement corrige, @NotNull Mutable<String> msg) {
		// les différences de relations sont à chercher dans les conjoints et les filiations
		final IndividuComparisonHelper.FieldMonitor monitor = new IndividuComparisonHelper.FieldMonitor();
		if (!IndividuComparisonHelper.areContentsEqual(originel.getIndividu().getConjoints(), corrige.getIndividu().getConjoints(), RELATION_COMPARATOR, RELATION_EQUALATOR, monitor, CONJOINTS)
				|| !IndividuComparisonHelper.areContentsEqual(originel.getIndividu().getParents(), corrige.getIndividu().getParents(), RELATION_COMPARATOR, RELATION_EQUALATOR, monitor, PARENTS)) {
			IndividuComparisonHelper.fillMonitor(monitor, ATTRIBUT);
			msg.setValue(IndividuComparisonHelper.buildErrorMessage(monitor));
			return false;
		}
		return true;
	}
}