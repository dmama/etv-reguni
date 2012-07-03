package ch.vd.unireg.interfaces.efacture.data;

public enum TypeResultatQuittancement {
	QUITTANCEMENT_EN_COURS(true, "Quittancement du contribuable (%s) en cours."),
	QUITTANCEMENT_OK(true, "Quittancement du contribuable (%s) OK."),
	DEJA_INSCRIT(true, "Quittancement du contribuable (%s) OK, Déjà inscrit."),
	CONTRIBUABLE_INEXISTANT(false, "Le contribuable (%s) n’existe pas. Veuillez vérifier le numéro saisi."),
	ETAT_EFACTURE_INCOHERENT(false, "Quittancement impossible. Le contribuable (%s) est dans un état e-facture incohérent"),
	ETAT_FISCAL_INCOHERENT (false, "Quittancement non-autorisé. L'asujettissement du contribuable (%s) est incompatible avec l'e-facture."),
	AUCUNE_DEMANDE_EN_COURS_DE_VALIDATION(false, "Aucune demande en cours de validation pour le contribuable (%s)");


	private boolean ok;
	private String description;

	TypeResultatQuittancement(boolean ok, String description) {
		this.ok = ok;
		this.description = description;
	}

	public String getDescription(long ctbId) {
		return String.format(description, Long.toString(ctbId));
	}

	public boolean isOk() {
		return ok;
	}


}
