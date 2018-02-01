package ch.vd.uniregctb.hibernate;

import ch.vd.uniregctb.type.TypeEvenementCivilEch;

/**
 * Classe de transtypage pour Hibernate : TypeEvenementCivilECH <--> varchar
 */
public class TypeEvenementCivilEchUserType extends EnumUserType<TypeEvenementCivilEch> {

	/**
	 * Constructeur : on passe la classe de l'Enum a EnumUserType
	 */
	public TypeEvenementCivilEchUserType() {
		super(TypeEvenementCivilEch.class);
	}
}
