package ch.vd.uniregctb.metier.common;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.MotifFor;

/**
 * Représente une fraction simple de l'assujettissement. Précisement, il s'agit d'une fraction qui impacte les fors fiscaux qui commencent ou se terminent <b>à une date précise</b> et dont l'effet de fractionnement
 * s'applique <b>à cette même date</b>.
 * <p/>
 * <i>Exemple :</i> en cas d'arrivée de hors-Suisse d'un contribuable le 5 mai 2011, la fraction est effective le 5 mai 2011 et possède un motif de fractionnement 'arrivée HS'. L'effet de cette
 * fraction et que son assujettissement hors-Suisse prend fin le 4 mai 2011 et son assujettissement vaudois commence le 5 mai 2011.
 */
public class FractionSimple extends Fraction {

	private final DateRange periodeImpact;

	public FractionSimple(@NotNull RegDate date, MotifFor motifOuverture, MotifFor motifFermeture) {
		super(date, motifOuverture, motifFermeture);
		this.periodeImpact = new DateRangeHelper.Range(date, date);
	}

	@Override
	public DateRange getPeriodeImpact() {
		return periodeImpact;
	}
}
