package ch.vd.uniregctb.hibernate;

import ch.vd.uniregctb.type.TypeEvenementErreur;

/**
 * Classe de transtypage pour Hibernate : MotifOuverture <--> varchar
 */
public class TypeEvenementErreurUserType extends EnumUserType<TypeEvenementErreur> {

	/**
	 * Constructeur : on passe la classe de l'Enum a EnumUserType
	 */
	public TypeEvenementErreurUserType() {
		super(TypeEvenementErreur.class);
	}

}
