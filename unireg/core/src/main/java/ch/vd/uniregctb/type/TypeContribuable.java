/**
 *
 */
package ch.vd.uniregctb.type;

public enum TypeContribuable {

	VAUDOIS_ORDINAIRE("ordinaire", true, true),
	VAUDOIS_DEPENSE("à la dépense", true, false),
	HORS_CANTON("hors canton", true, true),
	HORS_SUISSE("hors Suisse", true, true),

	/**
	 * Diplomate Suisse basé à l'étranger [UNIREG-1976]
	 */
	DIPLOMATE_SUISSE("diplomate suisse", true, false),

	/**
	 * Fondation placée sous la coupe de l'autorité surveillance des fondations (LIASF) [SIFISC-18113]
	 */
	UTILITE_PUBLIQUE("utilité publique (LIASF)", false, true);

	private final String description;
	private final boolean pp;
	private final boolean pm;

	TypeContribuable(String description, boolean pp, boolean pm) {
		this.description = description;
		this.pp = pp;
		this.pm = pm;
	}

	/**
	 * @return une description textuelle du type de contribuable
	 */
	public String description() {
		return description;
	}

	/**
	 * @return <code>true</code> si ce type est utilisable pour les PP
	 */
	public boolean isUsedForPP() {
		return pp;
	}

	/**
	 * @return <code>true</code> si ce type est utilisable pour les PM
	 */
	public boolean isUsedForPM() {
		return pm;
	}
}
