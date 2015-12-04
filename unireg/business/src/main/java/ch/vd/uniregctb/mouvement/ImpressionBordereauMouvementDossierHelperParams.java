package ch.vd.uniregctb.mouvement;

public class ImpressionBordereauMouvementDossierHelperParams {

	private final BordereauMouvementDossier bordereau;

	private final String nomOperateur;

	private final String emailOperateur;

	private final String numeroTelephoneOperateur;

	public ImpressionBordereauMouvementDossierHelperParams(BordereauMouvementDossier bordereau, String nomOperateur, String emailOperateur, String numeroTelephoneOperateur) {
		this.bordereau = bordereau;
		this.nomOperateur = nomOperateur;
		this.emailOperateur = emailOperateur;
		this.numeroTelephoneOperateur = numeroTelephoneOperateur;
	}

	public BordereauMouvementDossier getBordereau() {
		return bordereau;
	}

	public String getNomOperateur() {
		return nomOperateur;
	}

	public String getEmailOperateur() {
		return emailOperateur;
	}

	public String getNumeroTelephoneOperateur() {
		return numeroTelephoneOperateur;
	}
}
