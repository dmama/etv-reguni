package ch.vd.uniregctb.metier.assujettissement;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Assujettissement de type indigent; c'est-à-dire pour un contribuable domicilié dans une commune vaudoise, mais n'ayant pas de revenu ni
 * fortune personnelle et au bénéfice d'une décision de l'ACI l'exemptant de déclaration d'impôt.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class Indigent extends Assujettissement {

	public Indigent(Contribuable contribuable, RegDate dateDebut, RegDate dateFin, MotifFor motifFractDebut, MotifFor motifFractFin, AssujettissementSurCommuneAnalyzer communeAnalyzer) {
		super(contribuable, dateDebut, dateFin, motifFractDebut, motifFractFin, communeAnalyzer);
	}

	private Indigent(Indigent source, RegDate dateDebut, RegDate dateFin, MotifFor motifDebut, MotifFor motifFin) {
		super(source, dateDebut, dateFin, motifDebut, motifFin);
	}

	private Indigent(Indigent courant, Indigent suivant) {
		super(courant, suivant);
	}

	@Override
	public Assujettissement duplicate(RegDate dateDebut, RegDate dateFin, MotifFor motifDebut, MotifFor motifFin) {
		return new Indigent(this, dateDebut, dateFin, motifDebut, motifFin);
	}

	@Override
	public TypeAssujettissement getType() {
		return TypeAssujettissement.INDIGENT;
	}

	@Override
	protected TypeAutoriteFiscale getTypeAutoriteFiscalePrincipale() {
		return TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;
	}

	@Override
	public DateRange collate(DateRange next) {
		return new Indigent(this, (Indigent) next);
	}

	@Override
	public String toString() {
		return "Indigent(" + getDateDebut() + " - " + getDateFin() + ')';
	}
}
