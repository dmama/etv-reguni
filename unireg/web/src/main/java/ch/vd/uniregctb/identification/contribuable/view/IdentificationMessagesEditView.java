package ch.vd.uniregctb.identification.contribuable.view;

public class IdentificationMessagesEditView {

	private String numeroAVS11;
	private DemandeIdentificationView demandeIdentificationView;
	private Long noCtbIdentifie;

	public DemandeIdentificationView getDemandeIdentificationView() {
		return demandeIdentificationView;
	}

	public void setDemandeIdentificationView(DemandeIdentificationView demandeIdentificationView) {
		this.demandeIdentificationView = demandeIdentificationView;
	}

	public String getNumeroAVS11() {
		return numeroAVS11;
	}

	public void setNumeroAVS11(String numeroAVS11) {
		this.numeroAVS11 = numeroAVS11;
	}

	public Long getNoCtbIdentifie() {
		return noCtbIdentifie;
	}

	public void setNoCtbIdentifie(Long noCtbIdentifie) {
		this.noCtbIdentifie = noCtbIdentifie;
	}
}
