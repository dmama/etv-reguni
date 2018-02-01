package ch.vd.unireg.hibernate;

import ch.vd.unireg.type.TypeEvenementCivilEch;

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
