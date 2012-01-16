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
	// FIXME (msi) supprimer cette part
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

	public abstract EnumAttributeIndividu toEAI();

	public static EnumAttributeIndividu[] toEAI(AttributeIndividu[] parties) {
		if (parties == null) {
			return null;
		}
		final EnumAttributeIndividu[] array = new EnumAttributeIndividu[parties.length];
		for (int i = 0, partiesLength = parties.length; i < partiesLength; i++) {
			array[i] = parties[i].toEAI();
		}
		return array;
	}

}
