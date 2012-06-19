package ch.vd.uniregctb.efacture;

import org.jetbrains.annotations.Nullable;

public enum TypeAttenteEFacture {

	EN_ATTENTE_CONTACT(1, "Mise en attente : l'assujettissement est incohérent avec la e-Facture"),
	EN_ATTENTE_SIGNATURE(2, "En attente de confirmation d'inscription");

	private final Integer code;
	private final String description;

	private TypeAttenteEFacture(Integer code, String motif) {
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
}
