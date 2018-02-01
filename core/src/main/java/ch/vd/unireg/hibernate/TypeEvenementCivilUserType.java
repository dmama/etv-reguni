package ch.vd.unireg.hibernate;

import ch.vd.unireg.type.TypeEvenementCivil;

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
