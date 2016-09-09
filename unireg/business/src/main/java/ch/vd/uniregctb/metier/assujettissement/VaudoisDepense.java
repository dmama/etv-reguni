package ch.vd.uniregctb.metier.assujettissement;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Assujettissement à la dépense; c'est-à-dire pour un contribuable étranger, domicilé dans le canton de Vaud mais sans activité lucrative
 * sur territoire Suisse, et qui a négocié un forfait d'imposition avec l'ACI.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class VaudoisDepense extends Assujettissement {

	public VaudoisDepense(Contribuable contribuable, RegDate dateDebut, RegDate dateFin, MotifFor motifFractDebut, MotifFor motifFractFin, AssujettissementSurCommuneAnalyzer communeAnalyzer) {
		super(contribuable, dateDebut, dateFin, motifFractDebut, motifFractFin, communeAnalyzer);
	}

	private VaudoisDepense(VaudoisDepense source, RegDate dateDebut, RegDate dateFin, MotifFor motifDebut, MotifFor motifFin) {
		super(source, dateDebut, dateFin, motifDebut, motifFin);
	}

	private VaudoisDepense(VaudoisDepense courant, VaudoisDepense suivant) {
		super(courant, suivant);
	}

	@Override
	public Assujettissement duplicate(RegDate dateDebut, RegDate dateFin, MotifFor motifDebut, MotifFor motifFin) {
		return new VaudoisDepense(this, dateDebut, dateFin, motifDebut, motifFin);
	}

	@Override
	public TypeAssujettissement getType() {
		return TypeAssujettissement.VAUDOIS_DEPENSE;
	}

	@Override
	protected TypeAutoriteFiscale getTypeAutoriteFiscalePrincipale() {
		return TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;
	}

	@Override
	public DateRange collate(DateRange next) {
		return new VaudoisDepense(this, (VaudoisDepense) next);
	}

	@Override
	public String toString() {
		return "VaudoisDepense(" + getDateDebut() + " - " + getDateFin() + ')';
	}
}
