package ch.vd.uniregctb.hibernate;

import ch.vd.uniregctb.type.TypeAdresseTiers;

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
