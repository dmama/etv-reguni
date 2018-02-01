package ch.vd.unireg.metier.assujettissement;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.type.TypeAutoriteFiscale;

/**
 * Assujettissement de type hors Suisse; c'est-à-dire pour un contribuable domicilié dans un pays étranger, et possédant un ou plusieurs
 * fors secondaires actifs dans une commune vaudoise.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class HorsSuisse extends Assujettissement {

	public HorsSuisse(Contribuable contribuable, RegDate dateDebut, RegDate dateFin, MotifAssujettissement motifFractDebut, MotifAssujettissement motifFractFin, AssujettissementSurCommuneAnalyzer communeAnalyzer) {
		super(contribuable, dateDebut, dateFin, motifFractDebut, motifFractFin, communeAnalyzer);
	}

	private HorsSuisse(HorsSuisse source, RegDate dateDebut, RegDate dateFin, MotifAssujettissement motifDebut, MotifAssujettissement motifFin) {
		super(source, dateDebut, dateFin, motifDebut, motifFin);
	}

	private HorsSuisse(HorsSuisse courant, HorsSuisse suivant) {
		super(courant, suivant);
	}

	@Override
	public Assujettissement duplicate(RegDate dateDebut, RegDate dateFin, MotifAssujettissement motifDebut, MotifAssujettissement motifFin) {
		return new HorsSuisse(this, dateDebut, dateFin, motifDebut, motifFin);
	}

	@Override
	public TypeAssujettissement getType() {
		return TypeAssujettissement.HORS_SUISSE;
	}

	@Override
	protected TypeAutoriteFiscale getTypeAutoriteFiscalePrincipale() {
		return TypeAutoriteFiscale.PAYS_HS;
	}

	@Override
	public HorsSuisse collate(Assujettissement next) {
		return new HorsSuisse(this, (HorsSuisse) next);
	}

	@Override
	public String toString() {
		return "HorsSuisse(" + getDateDebut() + " - " + getDateFin() + ')';
	}
}
