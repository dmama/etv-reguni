package ch.vd.uniregctb.metier.assujettissement;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Assujettissement de type sourcier mixte au sens de l'article 137, alinéa 2 de la LI (revenu élevé)
 */
public class SourcierMixteArt137Al2 extends SourcierMixte {

	public SourcierMixteArt137Al2(Contribuable contribuable, RegDate dateDebut, RegDate dateFin, MotifAssujettissement motifFractDebut, MotifAssujettissement motifFractFin, TypeAutoriteFiscale typeAutoriteFiscale, AssujettissementSurCommuneAnalyzer communeAnalyzer) {
		super(contribuable, dateDebut, dateFin, motifFractDebut, motifFractFin, typeAutoriteFiscale, communeAnalyzer);
	}

	private SourcierMixteArt137Al2(SourcierMixteArt137Al2 source, RegDate dateDebut, RegDate dateFin, MotifAssujettissement motifDebut, MotifAssujettissement motifFin) {
		super(source, dateDebut, dateFin, motifDebut, motifFin);
	}

	public SourcierMixteArt137Al2(SourcierMixteArt137Al2 courant, SourcierMixteArt137Al2 suivant) {
		super(courant, suivant);
	}

	@Override
	public Assujettissement duplicate(RegDate dateDebut, RegDate dateFin, MotifAssujettissement motifDebut, MotifAssujettissement motifFin) {
		return new SourcierMixteArt137Al2(this, dateDebut, dateFin, motifDebut, motifFin);
	}

	@Override
	public TypeAssujettissement getType() {
		return TypeAssujettissement.MIXTE_137_2;
	}

	@Override
	public SourcierMixteArt137Al2 collate(Assujettissement next) {
		return new SourcierMixteArt137Al2(this, (SourcierMixteArt137Al2) next);
	}

	@Override
	public String toString() {
		return "SourcierMixteArt137Al2(" + getDateDebut() + " - " + getDateFin() + ')';
	}
}
