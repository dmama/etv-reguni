package ch.vd.uniregctb.metier.assujettissement;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Assujettissement de type hors Canton; c'est-à-dire pour un contribuable domicilié dans un canton autre que le canton de vaud, et
 * possédant un ou plusieurs fors secondaires actifs dans une commune vaudoise.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class HorsCanton extends Assujettissement {

	public HorsCanton(Contribuable contribuable, RegDate dateDebut, RegDate dateFin, MotifAssujettissement motifFractDebut, MotifAssujettissement motifFractFin, AssujettissementSurCommuneAnalyzer communeAnalyzer) {
		super(contribuable, dateDebut, dateFin, motifFractDebut, motifFractFin, communeAnalyzer);
	}

	private HorsCanton(HorsCanton source, RegDate dateDebut, RegDate dateFin, MotifAssujettissement motifDebut, MotifAssujettissement motifFin) {
		super(source, dateDebut, dateFin, motifDebut, motifFin);
	}

	private HorsCanton(HorsCanton courant, HorsCanton suivant) {
		super(courant, suivant);
	}

	@Override
	public Assujettissement duplicate(RegDate dateDebut, RegDate dateFin, MotifAssujettissement motifDebut, MotifAssujettissement motifFin) {
		return new HorsCanton(this, dateDebut, dateFin, motifDebut, motifFin);
	}

	@Override
	public TypeAssujettissement getType() {
		return TypeAssujettissement.HORS_CANTON;
	}

	@Override
	protected TypeAutoriteFiscale getTypeAutoriteFiscalePrincipale() {
		return TypeAutoriteFiscale.COMMUNE_HC;
	}

	@Override
	public HorsCanton collate(Assujettissement next) {
		return new HorsCanton(this, (HorsCanton) next);
	}

	@Override
	public String toString() {
		return "HorsCanton(" + getDateDebut() + " - " + getDateFin() + ')';
	}
}
