package ch.vd.uniregctb.metier.assujettissement;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Assujettissement de type sourcier mixte au sens de l'article 137, alinéa 1 de la LI (fortune ou immeuble)
 */
public class SourcierMixteArt137Al1 extends SourcierMixte {

	public SourcierMixteArt137Al1(Contribuable contribuable, RegDate dateDebut, RegDate dateFin, MotifFor motifFractDebut, MotifFor motifFractFin, TypeAutoriteFiscale typeAutoriteFiscale, AssujettissementSurCommuneAnalyzer communeAnalyzer) {
		super(contribuable, dateDebut, dateFin, motifFractDebut, motifFractFin, typeAutoriteFiscale, communeAnalyzer);
	}

	private SourcierMixteArt137Al1(SourcierMixteArt137Al1 source, RegDate dateDebut, RegDate dateFin, MotifFor motifDebut, MotifFor motifFin) {
		super(source, dateDebut, dateFin, motifDebut, motifFin);
	}

	public SourcierMixteArt137Al1(SourcierMixteArt137Al1 courant, SourcierMixteArt137Al1 suivant) {
		super(courant, suivant);
	}

	@Override
	public Assujettissement duplicate(RegDate dateDebut, RegDate dateFin, MotifFor motifDebut, MotifFor motifFin) {
		return new SourcierMixteArt137Al1(this, dateDebut, dateFin, motifDebut, motifFin);
	}

	@Override
	public TypeAssujettissement getType() {
		return TypeAssujettissement.MIXTE_137_1;
	}

	@Override
	public DateRange collate(DateRange next) {
		return new SourcierMixteArt137Al1(this, (SourcierMixteArt137Al1) next);
	}

	@Override
	public String toString() {
		return "SourcierMixteArt137Al1(" + getDateDebut() + " - " + getDateFin() + ')';
	}
}
