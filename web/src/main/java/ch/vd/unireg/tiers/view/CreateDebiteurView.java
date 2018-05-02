package ch.vd.unireg.tiers.view;

import ch.vd.unireg.complements.ComplementsEditCommunicationsView;
import ch.vd.unireg.complements.CoordonneesFinancieresEditView;

public class CreateDebiteurView {

	private Long id;
	private Long idCtbAssocie;
	private final DebiteurFiscalView fiscal = new DebiteurFiscalView();
	private final ComplementsEditCommunicationsView complementCommunication = new ComplementsEditCommunicationsView();
	private final CoordonneesFinancieresEditView complementCoordFinanciere = new CoordonneesFinancieresEditView();

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

	public DebiteurFiscalView getFiscal() {
		return fiscal;
	}

	public ComplementsEditCommunicationsView getComplementCommunication() {
		return complementCommunication;
	}

	public CoordonneesFinancieresEditView getComplementCoordFinanciere() {
		return complementCoordFinanciere;
	}
}
