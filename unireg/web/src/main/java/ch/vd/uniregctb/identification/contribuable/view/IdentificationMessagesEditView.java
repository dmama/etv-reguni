package ch.vd.uniregctb.identification.contribuable.view;

import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable.ErreurMessage;
import ch.vd.uniregctb.tiers.view.TiersCriteriaView;

public class IdentificationMessagesEditView extends TiersCriteriaView{

	private static final long serialVersionUID = 4452122137786516242L;

	private ErreurMessage erreurMessage;

	private DemandeIdentificationView demandeIdentificationView;

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

}
