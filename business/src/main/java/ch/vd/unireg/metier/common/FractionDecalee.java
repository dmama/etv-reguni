package ch.vd.unireg.metier.common;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.type.MotifFor;

/**
 * Représente une fraction de l'assujettissement avec un effet décalé. Précisement, il s'agit d'une fraction qui impacte les fors fiscaux qui commencent ou se terminent <b>durant une période</b> et
 * dont l'effet de fractionnement s'applique <b>à une date donnée</b> (= une des bornes de la période d'impact).
 * <p/>
 * <i>Exemple :</i> en cas d'obtention du permis C ou de la nationalité Suisse d'un contribuable sourcier le 5 mai 2011, la période d'impact s'étend du 5 mai au 1er juin 2011 et la fraction est
 * effective le 1 juin 2011. L'effet de cette fraction et que son assujettissement source prend fin le 31 mai 2011 et son assujettissement ordinaire commence le 1 juin 2011.
 */
public class FractionDecalee extends Fraction {

	private final DateRange periodeImpact;

	public FractionDecalee(@NotNull RegDate date, DateRange periodeImpact, MotifFor motifOuverture, MotifFor motifFermeture) {
		super(date, motifOuverture, motifFermeture);
		this.periodeImpact = periodeImpact;
	}

	@Override
	public DateRange getPeriodeImpact() {
		return periodeImpact;
	}
}
