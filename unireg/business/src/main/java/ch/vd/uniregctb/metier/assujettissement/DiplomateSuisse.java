package ch.vd.uniregctb.metier.assujettissement;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Période d'assujettissement de type diplomate Suisse; c'est-à-dire pour un diplomate de nationalité Suisse, basé à l'étranger mais
 * rattaché à une commune vaudoise (pour imposition à l'IFD).
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class DiplomateSuisse extends Assujettissement {

	public DiplomateSuisse(Contribuable contribuable, RegDate dateDebut, RegDate dateFin, MotifAssujettissement motifFractDebut, MotifAssujettissement motifFractFin, AssujettissementSurCommuneAnalyzer communeAnalyzer) {
		super(contribuable, dateDebut, dateFin, motifFractDebut, motifFractFin, communeAnalyzer);
	}

	private DiplomateSuisse(DiplomateSuisse source, RegDate dateDebut, RegDate dateFin, MotifAssujettissement motifDebut, MotifAssujettissement motifFin) {
		super(source, dateDebut, dateFin, motifDebut, motifFin);
	}

	private DiplomateSuisse(DiplomateSuisse courant, DiplomateSuisse suivant) {
		super(courant, suivant);
	}

	@Override
	public Assujettissement duplicate(RegDate dateDebut, RegDate dateFin, MotifAssujettissement motifDebut, MotifAssujettissement motifFin) {
		return new DiplomateSuisse(this, dateDebut, dateFin, motifDebut, motifFin);
	}

	@Override
	public TypeAssujettissement getType() {
		return TypeAssujettissement.DIPLOMATE_SUISSE;
	}

	@Override
	protected TypeAutoriteFiscale getTypeAutoriteFiscalePrincipale() {
		return TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;
	}

	@Override
	public DiplomateSuisse collate(Assujettissement next) {
		return new DiplomateSuisse(this, (DiplomateSuisse) next);
	}

	@Override
	public String toString() {
		return "DiplomateSuisse(" + getDateDebut() + " - " + getDateFin() + ')';
	}
}
