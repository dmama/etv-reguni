package ch.vd.unireg.metier.assujettissement;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.type.TypeAutoriteFiscale;

/**
 * Assujettissement à la dépense; c'est-à-dire pour un contribuable étranger, domicilé dans le canton de Vaud mais sans activité lucrative
 * sur territoire Suisse, et qui a négocié un forfait d'imposition avec l'ACI.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class VaudoisDepense extends Assujettissement {

	public VaudoisDepense(Contribuable contribuable, RegDate dateDebut, RegDate dateFin, MotifAssujettissement motifFractDebut, MotifAssujettissement motifFractFin, AssujettissementSurCommuneAnalyzer communeAnalyzer) {
		super(contribuable, dateDebut, dateFin, motifFractDebut, motifFractFin, communeAnalyzer);
	}

	private VaudoisDepense(VaudoisDepense source, RegDate dateDebut, RegDate dateFin, MotifAssujettissement motifDebut, MotifAssujettissement motifFin) {
		super(source, dateDebut, dateFin, motifDebut, motifFin);
	}

	private VaudoisDepense(VaudoisDepense courant, VaudoisDepense suivant) {
		super(courant, suivant);
	}

	@Override
	public Assujettissement duplicate(RegDate dateDebut, RegDate dateFin, MotifAssujettissement motifDebut, MotifAssujettissement motifFin) {
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
	public VaudoisDepense collate(Assujettissement next) {
		return new VaudoisDepense(this, (VaudoisDepense) next);
	}

	@Override
	public String toString() {
		return "VaudoisDepense(" + getDateDebut() + " - " + getDateFin() + ')';
	}
}
