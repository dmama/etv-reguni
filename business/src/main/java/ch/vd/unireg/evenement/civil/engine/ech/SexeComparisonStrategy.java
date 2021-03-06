package ch.vd.unireg.evenement.civil.engine.ech;

import org.apache.commons.lang3.mutable.Mutable;
import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;
import ch.vd.unireg.type.Sexe;

public class SexeComparisonStrategy implements IndividuComparisonStrategy {

	private static final String ATTRIBUTE = "sexe";

	@Override
	public boolean isFiscalementNeutre(IndividuApresEvenement originel, IndividuApresEvenement corrige, @NotNull Mutable<String> msg) {
		final Sexe origSexe = originel.getIndividu().getSexe();
		final Sexe corSexe = corrige.getIndividu().getSexe();
		final boolean same = origSexe == corSexe;
		if (!same) {
			msg.setValue(ATTRIBUTE);
		}
		return same;
	}
}
