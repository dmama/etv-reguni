package ch.vd.unireg.interfaces.entreprise.data;

/**
 * Types de demandes d'annonce (entreprises).
 *
 * @author Raphaël Marmier, 2016-08-19, <raphael.marmier@vd.ch>
 */
public enum TypeAnnonce {
	CREATION("Création"),
	MUTATION("Modification"),
	RADIATION("Radiation"),
	REACTIVATION("Réactivation");

	private final String libelle;

	TypeAnnonce(String libelle) {
		this.libelle = libelle;	
	}

	public String getLibelle() {
		return libelle;
	}
}
