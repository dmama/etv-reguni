package ch.vd.uniregctb.metier.common;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.MotifFor;

public class FractionContrariante extends FractionSimple {

	public FractionContrariante(@NotNull RegDate date, MotifFor motifOuverture, MotifFor motifFermeture) {
		super(date, motifOuverture, motifFermeture);
	}
}
