package ch.vd.unireg.interfaces.efacture.data;

/**
 * Type d'une "demande" au sens e-facture
 */
public enum TypeDemande {
	INSCRIPTION("Inscription"),
	DESINSCRIPTION("Désinscription");

	private final String description;

	private TypeDemande(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
}
