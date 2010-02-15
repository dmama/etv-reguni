package ch.vd.uniregctb.couple.view;

import java.util.HashMap;
import java.util.Map;

import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Type d'union
 * @author Pavel BLANCO
 *
 */
public enum TypeUnion {

	COUPLE(0, "Union normale entre deux tiers"),
	SEUL(1, "Union où l'un des conjoints est inconnu"),
	RECONCILIATION(2, "Réconciliation"),
	RECONSTITUTION_MENAGE(3, "Reconstitution d'un ménage incomplet"),
	FUSION_MENAGES(4, "Reconstitution d'un ménage à partir de deux ménages communs incomplets"),
	;
	
	private final int id;
	
	private final String description;
	
	private TypeUnion(int id, String description) {
		this.id = id;
		this.description = description;
	}

	public static TypeEvenementCivil valueOf(int code) {
		return typesByCode.get(code);
	}
	
	/**
	 * Map permettant d'accéder au type d'union par son code.
	 */
	private static Map<Integer, TypeEvenementCivil> typesByCode = null;

	static {
		typesByCode = new HashMap<Integer, TypeEvenementCivil>();
		for (TypeEvenementCivil type : TypeEvenementCivil.values()) {
			typesByCode.put(type.getId(), type);
		}
	}
	
	public int getId() {
		return id;
	}
	
	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}
}