package ch.vd.unireg.hibernate;

import ch.vd.unireg.type.TypeAdresseTiers;

/**
 * Classe de transtypage pour Hibernate : TypeAdresseTiers <--> varchar
 * 
 * @author Ludovic BERTIN
 */
public class TypeAdresseTiersUserType extends EnumUserType<TypeAdresseTiers> {

	/**
	 * Constructeur : on passe la classe de l'Enum a EnumUserType
	 */
	public TypeAdresseTiersUserType() {
		super(TypeAdresseTiers.class);
	}

}
