package ch.vd.uniregctb.metier.assujettissement;

/**
 * Différents types d'assujettissement
 */
public enum TypeAssujettissement {

	NON_ASSUJETTI("Non assujetti"),
	SOURCE_PURE("Imposition à la source"),
	MIXTE_137_1("Imposition mixte Art. 137 Al. 1"),
	MIXTE_137_2("Imposition mixte Art. 137 Al. 2"),
	VAUDOIS_ORDINAIRE("Imposition ordinaire VD"),
	VAUDOIS_DEPENSE("Imposition d'après la dépense"),
	INDIGENT("Indigent"),
	HORS_SUISSE("Imposition ordinaire HS"),
	HORS_CANTON("Imposition ordinaire HC"),
	DIPLOMATE_SUISSE("Diplomate Suisse");

	private final String description;

	TypeAssujettissement(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
}
