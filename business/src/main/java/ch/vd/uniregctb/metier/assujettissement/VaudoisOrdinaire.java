package ch.vd.uniregctb.metier.assujettissement;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Assujettissement de type vaudois ordinaire; c'est-à-dire pour un contribuable Suisse ou permis C, domicilié dans le canton de Vaud et
 * exerçant une activité lucrative (dans le canton ou ailleurs). Ce cas représente la majorité des contribuables.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class VaudoisOrdinaire extends Assujettissement {

	public VaudoisOrdinaire(Contribuable contribuable, RegDate dateDebut, RegDate dateFin, MotifAssujettissement motifFractDebut, MotifAssujettissement motifFractFin, AssujettissementSurCommuneAnalyzer communeAnalyzer) {
		super(contribuable, dateDebut, dateFin, motifFractDebut, motifFractFin, communeAnalyzer);
	}

	private VaudoisOrdinaire(VaudoisOrdinaire source, RegDate dateDebut, RegDate dateFin, MotifAssujettissement motifDebut, MotifAssujettissement motifFin) {
		super(source, dateDebut, dateFin, motifDebut, motifFin);
	}

	private VaudoisOrdinaire(VaudoisOrdinaire courant, VaudoisOrdinaire suivant) {
		super(courant, suivant);
	}

	@Override
	public Assujettissement duplicate(RegDate dateDebut, RegDate dateFin, MotifAssujettissement motifDebut, MotifAssujettissement motifFin) {
		return new VaudoisOrdinaire(this, dateDebut, dateFin, motifDebut, motifFin);
	}

	@Override
	public TypeAssujettissement getType() {
		return TypeAssujettissement.VAUDOIS_ORDINAIRE;
	}

	@Override
	protected TypeAutoriteFiscale getTypeAutoriteFiscalePrincipale() {
		return TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;
	}

	@Override
	public VaudoisOrdinaire collate(Assujettissement next) {
		return new VaudoisOrdinaire(this, (VaudoisOrdinaire) next);
	}

	@Override
	public String toString() {
		return "VaudoisOrdinaire(" + getDateDebut() + " - " + getDateFin() + ')';
	}
}
