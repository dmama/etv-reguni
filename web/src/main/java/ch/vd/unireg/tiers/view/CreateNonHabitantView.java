package ch.vd.unireg.tiers.view;

import ch.vd.unireg.complements.ComplementsEditCommunicationsView;
import ch.vd.unireg.complements.CoordonneesFinancieresEditView;

public class CreateNonHabitantView {

	private final NonHabitantCivilView civil = new NonHabitantCivilView();
	private final ComplementsEditCommunicationsView complementCommunication = new ComplementsEditCommunicationsView();
	private final CoordonneesFinancieresEditView complementCoordFinanciere = new CoordonneesFinancieresEditView();

	public NonHabitantCivilView getCivil() {
		return civil;
	}

	public ComplementsEditCommunicationsView getComplementCommunication() {
		return complementCommunication;
	}

	public CoordonneesFinancieresEditView getComplementCoordFinanciere() {
		return complementCoordFinanciere;
	}
}
