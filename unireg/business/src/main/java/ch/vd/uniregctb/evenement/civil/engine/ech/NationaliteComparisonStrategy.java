package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.Comparator;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;
import ch.vd.unireg.interfaces.civil.data.Nationalite;
import ch.vd.uniregctb.common.DataHolder;

/**
 * Comparateur d'individu basé sur les nationalités de l'individu
 */
public class NationaliteComparisonStrategy implements IndividuComparisonStrategy {

	private static final String ATTRIBUT = "nationalité";

	private static final Comparator<Nationalite> NATIONALITE_COMPARATOR = new Comparator<Nationalite>() {
		@Override
		public int compare(Nationalite o1, Nationalite o2) {
			int comparison = IndividuComparisonHelper.RANGE_COMPARATOR.compare(o1, o2);
			if (comparison == 0) {
				comparison = Integer.signum(o1.getPays().getNoOFS() - o2.getPays().getNoOFS());
			}
			return comparison;
		}
	};

	private static final IndividuComparisonHelper.Equalator<Nationalite> NATIONALITE_EQUALATOR = new IndividuComparisonHelper.Equalator<Nationalite>() {
		@Override
		public boolean areEqual(Nationalite o1, Nationalite o2) {
			return IndividuComparisonHelper.RANGE_EQUALATOR.areEqual(o1, o2) && o1.getPays().getNoOFS() == o2.getPays().getNoOFS();
		}
	};

	@Override
	public boolean sansDifferenceFiscalementImportante(IndividuApresEvenement originel, IndividuApresEvenement corrige, @NotNull DataHolder<String> msg) {
		// à un instant donné, un individu peut avoir plusieurs nationalités... il faut donc comparer les listes complètes
		if (!IndividuComparisonHelper.areContentsEqual(originel.getIndividu().getNationalites(), corrige.getIndividu().getNationalites(), NATIONALITE_COMPARATOR, NATIONALITE_EQUALATOR)) {
			msg.set(ATTRIBUT);
			return false;
		}
		return true;
	}
}
