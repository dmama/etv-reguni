package ch.vd.uniregctb.hibernate;

import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Classe de transtypage pour Hibernate : TypeEvenementCivil <--> varchar
 * 
 * @author Ludovic BERTIN
 */
public class TypeEvenementCivilUserType extends EnumUserType<TypeEvenementCivil> {

	/**
	 * Constructeur : on passe la classe de l'Enum a EnumUserType
	 */
	public TypeEvenementCivilUserType() {
		super(TypeEvenementCivil.class);
	}

}
