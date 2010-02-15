package ch.vd.uniregctb.situationfamille;

import ch.vd.uniregctb.tiers.SituationFamille;
import ch.vd.uniregctb.type.EtatCivil;

/**
 * Permet d'adapter une situation de famille de personne physique en provenance du fiscal.
 */
public class VueSituationFamillePersonnePhysiqueFiscalAdapter extends VueSituationFamilleFiscalAdapter implements
		VueSituationFamillePersonnePhysique {

	private final SituationFamille situation;

	public VueSituationFamillePersonnePhysiqueFiscalAdapter(SituationFamille situation, Source source) {
		super(situation, source);
		this.situation = situation;
	}

	public EtatCivil getEtatCivil() {
		return situation.getEtatCivil();
	}
}
