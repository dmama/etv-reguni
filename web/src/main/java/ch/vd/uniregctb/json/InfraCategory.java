package ch.vd.uniregctb.json;

/**
 * Catégories disponibles pour l'auto-completion des champs d'infrastructures.
 */
public enum InfraCategory {
	RUE("rue"),
	LOCALITE("localite"),
	COMMUNE("commune"),
	COMMUNE_VD("communeVD"),
	COMMUNE_HC("communeHC"),
	ETAT("etat"),
	TERRITOIRE("territoire"),
	COLLECTIVITE_ADMINISTRATIVE("collectiviteAdministrative"),
	JUSTICES_DE_PAIX("justicePaix"),
	OFFICES_IMPOT("officeImpot");

	InfraCategory(String tag) {
		this.tag = tag;
	}

	private final String tag;

	public String getTag() {
		return tag;
	}
}
