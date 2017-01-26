package ch.vd.unireg.interfaces.infra.data;

/**
 * @author Raphaël Marmier, 2017-03-10, <raphael.marmier@vd.ch>
 */
public enum CategorieEntrepriseFidor {
	PM("Personne morale"),
	APM("Autre personne morale"),
	SP("Société de personnes"),
	INDET("Indéterminée"),
	AUTRE("Autre");

	private final String description;

	CategorieEntrepriseFidor(String description) {
		this.description = description;
	}

	public static CategorieEntrepriseFidor fromCode(String code) {
		switch (code) {
		case "PM": return PM;
		case "APM": return APM;
		case "SP": return SP;
		case "EN_ATTENTE_DETERMINATION": return INDET;
		default: return AUTRE;
		}
	}

	public String getDescription() {
		return description;
	}
}
