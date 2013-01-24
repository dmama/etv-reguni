package ch.vd.uniregctb.evenement.civil.engine.ech;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;
import ch.vd.uniregctb.common.DataHolder;
import ch.vd.uniregctb.type.Sexe;

public class SexeComparisonStrategy implements IndividuComparisonStrategy {

	private static final String ATTRIBUTE = "sexe";

	@Override
	public boolean isFiscalementNeutre(IndividuApresEvenement originel, IndividuApresEvenement corrige, @NotNull DataHolder<String> msg) {
		final Sexe origSexe = originel.getIndividu().getSexe();
		final Sexe corSexe = corrige.getIndividu().getSexe();
		final boolean same = origSexe == corSexe;
		if (!same) {
			msg.set(ATTRIBUTE);
		}
		return same;
	}
}
