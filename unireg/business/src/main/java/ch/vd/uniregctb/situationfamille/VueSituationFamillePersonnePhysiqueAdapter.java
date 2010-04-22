package ch.vd.uniregctb.situationfamille;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.EtatCivil;

/**
 * Permet d'adapter une situation de famille de personne physique en fonction de nouvelles dates de début/fin.
 */
public class VueSituationFamillePersonnePhysiqueAdapter extends VueSituationFamilleAdapter implements VueSituationFamillePersonnePhysique {

	private final VueSituationFamillePersonnePhysique target;

	public VueSituationFamillePersonnePhysiqueAdapter(VueSituationFamillePersonnePhysique target, RegDate dateDebut, RegDate dateFin) {
		super(target, dateDebut, dateFin);
		this.target = target;
	}

	public EtatCivil getEtatCivil() {
		return target.getEtatCivil();
	}
}
