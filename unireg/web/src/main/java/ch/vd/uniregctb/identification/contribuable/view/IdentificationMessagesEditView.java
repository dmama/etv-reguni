package ch.vd.uniregctb.identification.contribuable.view;

import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable.ErreurMessage;
import ch.vd.uniregctb.tiers.view.TiersCriteriaView;

public class IdentificationMessagesEditView extends TiersCriteriaView {

	private ErreurMessage erreurMessage;

	private String numeroAVS11;

	private DemandeIdentificationView demandeIdentificationView;

	private Long noCtbIdentifie;

	public DemandeIdentificationView getDemandeIdentificationView() {
		return demandeIdentificationView;
	}

	public void setDemandeIdentificationView(DemandeIdentificationView demandeIdentificationView) {
		this.demandeIdentificationView = demandeIdentificationView;
	}

	public void setErreurMessage(ErreurMessage erreurMessage) {
		this.erreurMessage = erreurMessage;
	}

	public ErreurMessage getErreurMessage() {
		return erreurMessage;
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
