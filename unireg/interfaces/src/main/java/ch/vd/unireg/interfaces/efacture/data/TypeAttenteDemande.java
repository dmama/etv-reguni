package ch.vd.unireg.interfaces.efacture.data;

import org.jetbrains.annotations.Nullable;

public enum TypeAttenteDemande {

	EN_ATTENTE_CONTACT(1, "Mise en attente."),
	EN_ATTENTE_SIGNATURE(2, "En attente de confirmation.");

	private final int code;
	private final String description;

	private TypeAttenteDemande(int code, String motif) {
		this.code = code;
		this.description = motif;
	}

	public String getDescription() {
		return description;
	}

	public int getCode() {
		return code;
	}

	@Nullable
	public static TypeAttenteDemande valueOf(@Nullable Integer code) {
		if (code == null) {
			return null;
		}
		for(TypeAttenteDemande t : TypeAttenteDemande.values()) {
			if (code == t.code) {
				return t;
			}
		}
		//TODO afin d'éviter que ca plante, on reçoit des nombres aléatoires de la efacture ; devrait être inutile après la mise en integration
		if (code > 2) {
			return null;
		}
		throw new IllegalArgumentException(code + " n'est pas un TypeAttenteDemande valide");
	}
}
