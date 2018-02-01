package ch.vd.unireg.tiers.view;

import ch.vd.unireg.complements.ComplementsEditCommunicationsView;
import ch.vd.unireg.complements.ComplementsEditCoordonneesFinancieresView;

public class CreateAutreCommunauteView {

	private final AutreCommunauteCivilView civil = new AutreCommunauteCivilView();
	private final ComplementsEditCommunicationsView complementCommunication = new ComplementsEditCommunicationsView();
	private final ComplementsEditCoordonneesFinancieresView complementCoordFinanciere = new ComplementsEditCoordonneesFinancieresView();

	public AutreCommunauteCivilView getCivil() {
		return civil;
	}

	public ComplementsEditCommunicationsView getComplementCommunication() {
		return complementCommunication;
	}

	public ComplementsEditCoordonneesFinancieresView getComplementCoordFinanciere() {
		return complementCoordFinanciere;
	}
}
