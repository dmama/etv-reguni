package ch.vd.uniregctb.metier.assujettissement;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.type.MotifFor;

/**
 * Assujettissement de type vaudois ordinaire; c'est-à-dire pour un contribuable Suisse ou permis C, domicilié dans le canton de Vaud et
 * exerçant une activité lucrative (dans le canton ou ailleurs). Ce cas représente la majorité des contribuables.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class VaudoisOrdinaire extends Assujettissement {

	public VaudoisOrdinaire(Contribuable contribuable, RegDate dateDebut, RegDate dateFin, MotifFor motifFractDebut,
			MotifFor motifFractFin, DecompositionFors fors) {
		super(contribuable, dateDebut, dateFin, motifFractDebut, motifFractFin, fors);
	}

	private VaudoisOrdinaire(VaudoisOrdinaire courant, VaudoisOrdinaire suivant) {
		super(courant, suivant);
	}

	@Override
	public String getDescription() {
		return "Imposition ordinaire VD";
	}

	public DateRange collate(DateRange next) {
		return new VaudoisOrdinaire(this, (VaudoisOrdinaire) next);
	}
}
