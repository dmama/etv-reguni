package ch.vd.uniregctb.efacture;

import ch.vd.evd0025.v1.RegistrationRequestHistoryEntry;
import ch.vd.evd0025.v1.RegistrationRequestStatus;
import ch.vd.evd0025.v1.RegistrationRequestWithHistory;
import ch.vd.registre.base.utils.Assert;


public class DemandeValidationInscriptionAvecHistorique
		extends DemandeValidationInscription {


	private final TypeAttenteEFacture typeAttente;
	private final boolean enCours;

	public DemandeValidationInscriptionAvecHistorique(RegistrationRequestWithHistory request) {
		super(request);
		Assert.isTrue(request.getRegistrationRequestHistoryEntry().size() > 0,
				"Une DemandeValidationInscriptionAvecHistorique ne peut pas être construite si la RegistrationRequestWithHistory source n'a pas au moins une entrée dans son historique");

		// Le statut actuel est à la fin de l'historique
		RegistrationRequestHistoryEntry rrhe = request.getRegistrationRequestHistoryEntry().get(request.getRegistrationRequestHistoryEntry().size() - 1);
		enCours = rrhe.getStatus() == RegistrationRequestStatus.VALIDATION_EN_COURS;
		typeAttente = TypeAttenteEFacture.valueOf(rrhe.getReasonCode());
	}

	public boolean isEnCoursDeTraitement () {
		return enCours && typeAttente != TypeAttenteEFacture.PAS_EN_ATTENTE;
	}
}
