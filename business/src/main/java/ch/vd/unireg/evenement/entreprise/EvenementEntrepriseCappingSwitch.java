package ch.vd.unireg.evenement.entreprise;

import ch.vd.unireg.evenement.entreprise.engine.translator.NiveauCappingEtat;

public class EvenementEntrepriseCappingSwitch implements EvenementEntrepriseCappingLevelProvider {

	private NiveauCappingEtat niveauCapping;

	public EvenementEntrepriseCappingSwitch(NiveauCappingEtat niveauCapping) {
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
