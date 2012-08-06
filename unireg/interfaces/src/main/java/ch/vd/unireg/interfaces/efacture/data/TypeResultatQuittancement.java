package ch.vd.unireg.interfaces.efacture.data;

public enum TypeResultatQuittancement {

	QUITTANCEMENT_EN_COURS(true, "Quittancement du contribuable %s en cours."),
	QUITTANCEMENT_OK(true, "Le contribuable %s est maintenant inscrit à la e-facture."),
	DEJA_INSCRIT(true, "Le contribuable %s est déjà inscrit à la e-facture."),
	CONTRIBUABLE_INEXISTANT(false, "Le contribuable %s n’existe pas. Veuillez vérifier le numéro saisi."),
	ETAT_EFACTURE_INCOHERENT(false, "Quittancement impossible. Le contribuable %s est dans un état e-facture incompatible avec un quittancement."),
	ETAT_FISCAL_INCOHERENT (false, "Quittancement non-autorisé. L'assujettissement du contribuable %s est incompatible avec la e-facture."),
	AUCUNE_DEMANDE_EN_ATTENTE_SIGNATURE(false, "Aucune demande en cours de validation n'est en attente de signature pour le contribuable %s.");

	private final boolean ok;
	private final String description;

	private TypeResultatQuittancement(boolean ok, String description) {
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
