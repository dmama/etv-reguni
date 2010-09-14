package ch.vd.uniregctb.interfaces.model;

import ch.vd.registre.civil.model.EnumAttributeIndividu;

/**
 * Enum java 1.5 qui mappe le pseudo-enum {@link ch.vd.registre.civil.model.EnumAttributeIndividu}
 */
public enum AttributeIndividu {

	ORIGINE {
		@Override
		public EnumAttributeIndividu toEAI() {
			return EnumAttributeIndividu.ORIGINE;
		}},
	TUTELLE {
		@Override
		public EnumAttributeIndividu toEAI() {
			return EnumAttributeIndividu.TUTELLE;
		}},
	CONJOINT {
		@Override
		public EnumAttributeIndividu toEAI() {
			return EnumAttributeIndividu.CONJOINT;
		}},
	ENFANTS {
		@Override
		public EnumAttributeIndividu toEAI() {
			return EnumAttributeIndividu.ENFANTS;
		}},
	ADOPTIONS {
		@Override
		public EnumAttributeIndividu toEAI() {
			return EnumAttributeIndividu.ADOPTIONS;
		}},
	ADRESSES {
		@Override
		public EnumAttributeIndividu toEAI() {
			return EnumAttributeIndividu.ADRESSES;
		}},
	PARENTS {
		@Override
		public EnumAttributeIndividu toEAI() {
			return EnumAttributeIndividu.PARENTS;
		}},
	PERMIS {
		@Override
		public EnumAttributeIndividu toEAI() {
			return EnumAttributeIndividu.PERMIS;
		}},
	NATIONALITE {
		@Override
		public EnumAttributeIndividu toEAI() {
			return EnumAttributeIndividu.NATIONALITE;
		}};

	public static AttributeIndividu fromEAI(EnumAttributeIndividu e) {
		if (e == EnumAttributeIndividu.ORIGINE) {
			return ORIGINE;
		}
		else if (e == EnumAttributeIndividu.TUTELLE) {
			return TUTELLE;
		}
		else if (e == EnumAttributeIndividu.CONJOINT) {
			return CONJOINT;
		}
		else if (e == EnumAttributeIndividu.ENFANTS) {
			return ENFANTS;
		}
		else if (e == EnumAttributeIndividu.ADOPTIONS) {
			return ADOPTIONS;
		}
		else if (e == EnumAttributeIndividu.ADRESSES) {
			return ADRESSES;
		}
		else if (e == EnumAttributeIndividu.PARENTS) {
			return PARENTS;
		}
		else if (e == EnumAttributeIndividu.PERMIS) {
			return PERMIS;
		}
		else if (e == EnumAttributeIndividu.NATIONALITE) {
			return NATIONALITE;
		}
		else {
			throw new IllegalArgumentException("Le pseudo-enum EnumAttributeIndividu avec le code [" + e.getCode() + "] est inconnu.");
		}
	}

	public abstract EnumAttributeIndividu toEAI();
}
