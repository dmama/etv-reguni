package ch.vd.unireg.metier.assujettissement;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.type.TypeAutoriteFiscale;

/**
 * Assujettissement de type sourcier pur; c'est-à-dire pour un contribuable imposé à la source, et qui ne remplit donc pas de déclaration
 * d'impôt (par opposition au sourcier mixte).
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class SourcierPur extends Sourcier {

	public SourcierPur(Contribuable contribuable, RegDate dateDebut, RegDate dateFin, MotifAssujettissement motifFractDebut, MotifAssujettissement motifFractFin, TypeAutoriteFiscale typeAutoriteFiscale, AssujettissementSurCommuneAnalyzer communeAnalyzer) {
		super(contribuable, dateDebut, dateFin, motifFractDebut, motifFractFin, typeAutoriteFiscale, communeAnalyzer);
	}

	private SourcierPur(SourcierPur source, RegDate dateDebut, RegDate dateFin, MotifAssujettissement motifDebut, MotifAssujettissement motifFin) {
		super(source, dateDebut, dateFin, motifDebut, motifFin);
	}

	public SourcierPur(SourcierPur courant, SourcierPur suivant) {
		super(courant, suivant);
	}

	@Override
	public Assujettissement duplicate(RegDate dateDebut, RegDate dateFin, MotifAssujettissement motifDebut, MotifAssujettissement motifFin) {
		return new SourcierPur(this, dateDebut, dateFin, motifDebut, motifFin);
	}

	@Override
	public TypeAssujettissement getType() {
		return TypeAssujettissement.SOURCE_PURE;
	}

	@Override
	public SourcierPur collate(Assujettissement next) {
		return new SourcierPur(this, (SourcierPur) next);
	}

	@Override
	public String toString() {
		return "SourcierPur(" + getDateDebut() + " - " + getDateFin() + ')';
	}
}
