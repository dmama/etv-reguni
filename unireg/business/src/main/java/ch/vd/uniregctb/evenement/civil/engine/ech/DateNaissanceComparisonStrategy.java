package ch.vd.uniregctb.evenement.civil.engine.ech;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;

/**
 * Stratégie de comparaison de deux individus basée sur la date de naissance
 */
public class DateNaissanceComparisonStrategy extends DateIndividuComparisonStrategy {

	private static final String ATTRIBUT = "date de naissance";

	@Override
	protected RegDate getDate(IndividuApresEvenement individu) {
		return individu.getIndividu().getDateNaissance();
	}

	@NotNull
	@Override
	protected String getNomAttribut() {
		return ATTRIBUT;
	}
}
