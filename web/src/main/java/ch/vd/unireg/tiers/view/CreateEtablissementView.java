package ch.vd.unireg.tiers.view;

import ch.vd.unireg.complements.ComplementsEditCommunicationsView;
import ch.vd.unireg.complements.CoordonneesFinancieresEditView;

public class CreateEtablissementView {

	private Long id;
	private Long idCtbAssocie;
	private EtablissementCivilView civil = new EtablissementCivilView();
	private final ComplementsEditCommunicationsView complementCommunication = new ComplementsEditCommunicationsView();
	private final CoordonneesFinancieresEditView complementCoordFinanciere = new CoordonneesFinancieresEditView();

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

	public CoordonneesFinancieresEditView getComplementCoordFinanciere() {
		return complementCoordFinanciere;
	}
}
