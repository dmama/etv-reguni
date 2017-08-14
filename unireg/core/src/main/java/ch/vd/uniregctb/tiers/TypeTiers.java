package ch.vd.uniregctb.tiers;

/**
 * Types de tiers concrets existants.
 */
public enum TypeTiers {
	PERSONNE_PHYSIQUE("Personne physique"),
	MENAGE_COMMUN("Ménage commun"),
	ENTREPRISE("Entreprise"),
	ETABLISSEMENT("Etablissement"),
	COLLECTIVITE_ADMINISTRATIVE("Collectivité administrative"),
	AUTRE_COMMUNAUTE("Autre communauté"),
	DEBITEUR_PRESTATION_IMPOSABLE("Débiteur prestation imposable");

	private final String description;

	TypeTiers(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
}
