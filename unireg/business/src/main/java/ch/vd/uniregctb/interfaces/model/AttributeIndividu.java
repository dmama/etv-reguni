package ch.vd.uniregctb.interfaces.model;

import java.util.ArrayList;
import java.util.List;

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
	CONJOINTS {
		@Override
		public EnumAttributeIndividu toEAI() {
			return null;
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
		final List<EnumAttributeIndividu> list = new ArrayList<EnumAttributeIndividu>();
		for (int i = 0, partiesLength = parties.length; i < partiesLength; i++) {
			EnumAttributeIndividu e = parties[i].toEAI();
			if (e != null) {
				list.add(e);
			}
		}
		return list.toArray(new EnumAttributeIndividu[list.size()]);
	}
}
