package ch.vd.uniregctb.metier.assujettissement;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Assujettissement de type hors Suisse; c'est-à-dire pour un contribuable domicilié dans un pays étranger, et possédant un ou plusieurs
 * fors secondaires actifs dans une commune vaudoise.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class HorsSuisse extends Assujettissement {

	public HorsSuisse(Contribuable contribuable, RegDate dateDebut, RegDate dateFin, MotifFor motifFractDebut, MotifFor motifFractFin) {
		super(contribuable, dateDebut, dateFin, motifFractDebut, motifFractFin);
	}

	private HorsSuisse(HorsSuisse courant, HorsSuisse suivant) {
		super(courant, suivant);
	}

	@Override
	public Assujettissement duplicate(RegDate dateDebut, RegDate dateFin, MotifFor motifDebut, MotifFor motifFin) {
		return new HorsSuisse(getContribuable(), dateDebut, dateFin, motifDebut, motifFin);
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
	public DateRange collate(DateRange next) {
		return new HorsSuisse(this, (HorsSuisse) next);
	}

	@Override
	public String toString() {
		return "HorsSuisse(" + getDateDebut() + " - " + getDateFin() + ')';
	}
}
