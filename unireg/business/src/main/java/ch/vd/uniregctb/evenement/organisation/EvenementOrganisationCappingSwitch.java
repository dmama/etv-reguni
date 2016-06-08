package ch.vd.uniregctb.evenement.organisation;

import ch.vd.uniregctb.evenement.organisation.engine.translator.NiveauCappingEtat;

public class EvenementOrganisationCappingSwitch implements EvenementOrganisationCappingLevelProvider {

	private NiveauCappingEtat niveauCapping;

	public EvenementOrganisationCappingSwitch(NiveauCappingEtat niveauCapping) {
		this.niveauCapping = niveauCapping;
	}

	@Override
	public NiveauCappingEtat getNiveauCapping() {
		return niveauCapping;
	}

	public void setNiveauCapping(NiveauCappingEtat niveauCapping) {
		this.niveauCapping = niveauCapping;
	}
}
