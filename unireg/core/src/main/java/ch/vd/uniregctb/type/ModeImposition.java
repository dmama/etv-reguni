package ch.vd.uniregctb.type;

/**
 * Longueur de colonne : 11
 */
public enum ModeImposition {

	ORDINAIRE("Ordinaire", "Imposition ordinaire", false, true),
	SOURCE("Source", "Imposition à la source", true, false),
	DEPENSE("Dépense", "Imposition d'après la dépense", false, true),
	MIXTE_137_1("Mixte 137 Al 1", "Imposition mixte", true, true),
	MIXTE_137_2("Mixte 137 Al 2", "Imposition mixte", true, true),
	INDIGENT("Indigent", "Imposition ordinaire", false, true);

	private final String texte;
	private final String texteEnrichi;
	private final boolean source;
	private final boolean role;

	ModeImposition(String format, String formatEnrichi, boolean source, boolean role) {
		this.texte = format;
		this.texteEnrichi = formatEnrichi;
		this.source = source;
		this.role = role;
	}

	public String texte() {
		return texte;
	}

	public String texteEnrichi() {
		return texteEnrichi;
	}

	public boolean isSource() {
		return source;
	}

	public boolean isRole() {
		return role;
	}
}
