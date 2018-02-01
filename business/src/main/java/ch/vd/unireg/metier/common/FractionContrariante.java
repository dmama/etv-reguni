package ch.vd.unireg.metier.common;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.type.MotifFor;

public class FractionContrariante extends FractionSimple {

	public FractionContrariante(@NotNull RegDate date, MotifFor motifOuverture, MotifFor motifFermeture) {
		super(date, motifOuverture, motifFermeture);
	}
}
