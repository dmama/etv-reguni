package ch.vd.unireg.evenement.civil.engine.ech;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;

public class DateEvenementComparisonStrategy extends DateIndividuComparisonStrategy {

	private static final String ATTRIBUT = "date de l'événement";

	@Nullable
	@Override
	protected RegDate getDate(IndividuApresEvenement individu) {
		return individu.getDateEvenement();
	}

	@NotNull
	@Override
	protected String getNomAttribut() {
		return ATTRIBUT;
	}
}
