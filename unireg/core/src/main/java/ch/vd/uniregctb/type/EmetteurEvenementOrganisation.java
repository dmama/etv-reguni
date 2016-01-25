package ch.vd.uniregctb.type;

import java.util.HashMap;
import java.util.Map;

/**
 * ch.vd.evd0022.v1:senderIdentificationType
 *
 * @author Raphaël Marmier, 2015-07-14
 */
public enum EmetteurEvenementOrganisation {

	AUTRE(0, "Autre"),
	IDE(1, "Registre IDE de l'OFS"),
	REE(2, "Registre des entreprises et des établissements"),
	FOSC(3, "Feuille officielle suisse du commerce");

	/**
	 * Code technique du type d'événement.
	 */
	private final int id;

	/**
	 * Description textuelle du type de l'événement
	 */
	private final String description;

	/**
	 * Map permettant d'accéder à un type d'événement par son code.
	 */
	private static final Map<Integer, EmetteurEvenementOrganisation> typesByCode;

	static {
		typesByCode = new HashMap<>();
		for (EmetteurEvenementOrganisation type : EmetteurEvenementOrganisation.values()) {
			typesByCode.put(type.getId(), type);
		}
	}

	/**
	 * Un type d'événement est construit avec son code.
	 *
	 * @param i   code identifiant le type d'événement
	 * @param description description du type d'événement
	 */
	EmetteurEvenementOrganisation(int i, String description) {
		this.id = i;
		this.description = description;
	}

	/**
	 * Retourne le code technique du type d'événement.
	 * @return code technique du type d'événement
	 */
	public int getId() {
		return id;
	}

	public String getName() {
		return name();
	}

	/**
	 * Retourne le type d'événement correspondant à un code donné.
	 * @param code  le code de l'événement
	 * @return le type d'événement correspondant à un code donné, null si le code n'a pas été trouvé.
	 */
	public static EmetteurEvenementOrganisation valueOf(int code) {
		return typesByCode.get(code);
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	public String getFullDescription() {
		return description + " (" + id + ')';
	}
}
