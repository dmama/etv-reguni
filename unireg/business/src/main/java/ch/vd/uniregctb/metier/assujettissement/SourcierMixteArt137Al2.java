package ch.vd.uniregctb.metier.assujettissement;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Assujettissement de type sourcier mixte au sens de l'article 137, alinéa 2 de la LI (revenu élevé)
 */
public class SourcierMixteArt137Al2 extends SourcierMixte {

	public SourcierMixteArt137Al2(Contribuable contribuable, RegDate dateDebut, RegDate dateFin, MotifFor motifFractDebut, MotifFor motifFractFin, TypeAutoriteFiscale typeAutoriteFiscale) {
		super(contribuable, dateDebut, dateFin, motifFractDebut, motifFractFin, typeAutoriteFiscale);
	}

	public SourcierMixteArt137Al2(SourcierMixteArt137Al2 courant, SourcierMixteArt137Al2 suivant) {
		super(courant, suivant);
	}

	@Override
	public String getDescription() {
		return "Imposition mixte Art. 137 Al. 2";
	}

	@Override
	public DateRange collate(DateRange next) {
		return new SourcierMixteArt137Al2(this, (SourcierMixteArt137Al2) next);
	}

	@Override
	public String toString() {
		return "SourcierMixteArt137Al2(" + getDateDebut() + " - " + getDateFin() + ')';
	}
}
