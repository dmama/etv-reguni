package ch.vd.unireg.interfaces.civil.data;

/**
 * Type d'une relation civile entre individus
 */
public enum TypeRelationVersIndividu {
	MERE,
	PERE,
	FILS,
	FILLE,
	CONJOINT,
	PARTENAIRE_ENREGISTRE;

	public boolean isEnfant() {
		return this == FILS || this == FILLE;
	}

	public boolean isParent() {
		return this == PERE || this == MERE;
	}

	public boolean isConjointOuPartenaire() {
		return this == CONJOINT || this == PARTENAIRE_ENREGISTRE;
	}
}
