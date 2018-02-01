package ch.vd.unireg.hibernate;

import ch.vd.unireg.type.TypeEvenementErreur;

/**
 * Classe de transtypage pour Hibernate : TypeEvenementErreurUserType <--> varchar
 */
public class TypeEvenementErreurUserType extends EnumUserType<TypeEvenementErreur> {

	/**
	 * Constructeur : on passe la classe de l'Enum a EnumUserType
	 */
	public TypeEvenementErreurUserType() {
		super(TypeEvenementErreur.class);
	}

}
