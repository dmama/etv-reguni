/**
 *
 */
package ch.vd.uniregctb.type;

public enum TypeContribuable {

	VAUDOIS_ORDINAIRE("ordinaire", true, true),
	VAUDOIS_DEPENSE("à la dépense", true, false),
	HORS_CANTON("hors canton", true, true),
	HORS_SUISSE("hors suisse", true, true),
	/**
	 * Diplomate Suisse basé à l'étranger [UNIREG-1976]
	 */
	DIPLOMATE_SUISSE("diplomate suisse", true, false);

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
