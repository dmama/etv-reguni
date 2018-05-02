package ch.vd.unireg.tiers.view;

import ch.vd.unireg.complements.ComplementsEditCommunicationsView;
import ch.vd.unireg.complements.CoordonneesFinancieresEditView;

public class CreateAutreCommunauteView {

	private final AutreCommunauteCivilView civil = new AutreCommunauteCivilView();
	private final ComplementsEditCommunicationsView complementCommunication = new ComplementsEditCommunicationsView();
	private final CoordonneesFinancieresEditView complementCoordFinanciere = new CoordonneesFinancieresEditView();

	public AutreCommunauteCivilView getCivil() {
		return civil;
	}

	public ComplementsEditCommunicationsView getComplementCommunication() {
		return complementCommunication;
	}

	public CoordonneesFinancieresEditView getComplementCoordFinanciere() {
		return complementCoordFinanciere;
	}
}
