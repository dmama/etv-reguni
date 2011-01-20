package ch.vd.uniregctb.hibernate;

import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Classe de transtypage pour Hibernate : TypeAutoriteFiscale <--> varchar
 * 
 * @author Ludovic BERTIN
 */
public class TypeAutoriteFiscaleUserType extends EnumUserType<TypeAutoriteFiscale> {

	/**
	 * Constructeur : on passe la classe de l'Enum a EnumUserType
	 */
	public TypeAutoriteFiscaleUserType() {
		super(TypeAutoriteFiscale.class);
	}

}
