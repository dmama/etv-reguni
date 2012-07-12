package ch.vd.unireg.interfaces.efacture.data;

import org.jetbrains.annotations.Nullable;

public enum TypeAttenteDemande {

	PAS_EN_ATTENTE (null, "Pas En Attente"),
	EN_ATTENTE_CONTACT(1, "Mise en attente."),
	EN_ATTENTE_SIGNATURE(2, "En attente de confirmation.");

	private final Integer code;
	private final String description;

	private TypeAttenteDemande(Integer code, String motif) {
		this.code = code;
		this.description = motif;
	}

	public String getDescription() {
		return description;
	}

	@Nullable
	public Integer getCode() {
		return code;
	}

	public static TypeAttenteDemande valueOf (Integer code) {
		if (code == null) {
			return PAS_EN_ATTENTE;
		}
		for(TypeAttenteDemande t : TypeAttenteDemande.values()) {
			if (code.equals(t.code)) {
				return t;
			}
		}
		//TODO afin d'éviter que ca plante, on reçoit des nombres aléatoire de la efacture devrait être inutile après la mise en integration
		if(code > 2){
			return PAS_EN_ATTENTE;

		}
		throw new IllegalArgumentException(code + " n'est pas un TypeAttenteDemande valide");
	}
}
