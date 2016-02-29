package ch.vd.uniregctb.evenement.civil.engine.ech;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;

/**
 * Stratégie de comparaison de deux individus basée sur la date de décès
 */
public class DateDecesComparisonStrategy extends DateIndividuComparisonStrategy {

	private static final String ATTRIBUT = "date de décès";

	@Override
	protected RegDate getDate(IndividuApresEvenement individu) {
		return individu.getIndividu().getDateDeces();
	}

	@NotNull
	@Override
	protected String getNomAttribut() {
		return ATTRIBUT;
	}
}
