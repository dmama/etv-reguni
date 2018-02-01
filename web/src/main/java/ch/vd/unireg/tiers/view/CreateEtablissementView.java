package ch.vd.unireg.tiers.view;

import ch.vd.unireg.complements.ComplementsEditCommunicationsView;
import ch.vd.unireg.complements.ComplementsEditCoordonneesFinancieresView;

public class CreateEtablissementView {

	private Long id;
	private Long idCtbAssocie;
	private EtablissementCivilView civil = new EtablissementCivilView();
	private final ComplementsEditCommunicationsView complementCommunication = new ComplementsEditCommunicationsView();
	private final ComplementsEditCoordonneesFinancieresView complementCoordFinanciere = new ComplementsEditCoordonneesFinancieresView();

	public CreateEtablissementView() {
	}

	public CreateEtablissementView(String raisonSociale) {
		this.civil = new EtablissementCivilView(raisonSociale);
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getIdCtbAssocie() {
		return idCtbAssocie;
	}

	public void setIdCtbAssocie(Long idCtbAssocie) {
		this.idCtbAssocie = idCtbAssocie;
	}

	public EtablissementCivilView getCivil() {
		return civil;
	}

	public ComplementsEditCommunicationsView getComplementCommunication() {
		return complementCommunication;
	}

	public ComplementsEditCoordonneesFinancieresView getComplementCoordFinanciere() {
		return complementCoordFinanciere;
	}
}
