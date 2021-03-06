package ch.vd.unireg.hibernate;

import ch.vd.unireg.type.TypeAdressePM;

/**
 * Classe de transtypage pour Hibernate : TypeAdressePM <--> varchar
 * 
 * @author Ludovic BERTIN
 */
public class TypeAdressePMUserType extends EnumUserType<TypeAdressePM> {

	/**
	 * Constructeur : on passe la classe de l'Enum a EnumUserType
	 */
	public TypeAdressePMUserType() {
		super(TypeAdressePM.class);
	}

}
