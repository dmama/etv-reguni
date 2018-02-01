package ch.vd.unireg.tiers.view;

import ch.vd.unireg.complements.ComplementsEditCommunicationsView;
import ch.vd.unireg.complements.ComplementsEditCoordonneesFinancieresView;

public class CreateEntrepriseView {

	private final EntrepriseCivilView civil = new EntrepriseCivilView();
	private final ComplementsEditCommunicationsView complementCommunication = new ComplementsEditCommunicationsView();
	private final ComplementsEditCoordonneesFinancieresView complementCoordFinanciere = new ComplementsEditCoordonneesFinancieresView();

	public EntrepriseCivilView getCivil() {
		return civil;
	}

	public ComplementsEditCommunicationsView getComplementCommunication() {
		return complementCommunication;
	}

	public ComplementsEditCoordonneesFinancieresView getComplementCoordFinanciere() {
		return complementCoordFinanciere;
	}
}
