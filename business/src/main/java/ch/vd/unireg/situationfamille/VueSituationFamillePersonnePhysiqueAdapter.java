package ch.vd.uniregctb.situationfamille;

import ch.vd.registre.base.date.RegDate;

/**
 * Permet d'adapter une situation de famille de personne physique en fonction de nouvelles dates de début/fin.
 */
public class VueSituationFamillePersonnePhysiqueAdapter extends VueSituationFamilleAdapter<VueSituationFamillePersonnePhysique> implements VueSituationFamillePersonnePhysique {

	public VueSituationFamillePersonnePhysiqueAdapter(VueSituationFamillePersonnePhysique target, RegDate dateDebut, RegDate dateFin) {
		super(target, dateDebut, dateFin);
	}
}
