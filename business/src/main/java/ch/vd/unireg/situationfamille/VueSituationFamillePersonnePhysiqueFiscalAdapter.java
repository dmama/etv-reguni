package ch.vd.unireg.situationfamille;

import ch.vd.unireg.tiers.SituationFamille;
import ch.vd.unireg.type.EtatCivil;

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

	@Override
	public EtatCivil getEtatCivil() {
		return situation.getEtatCivil();
	}
}
