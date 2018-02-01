package ch.vd.uniregctb.hibernate;

import ch.vd.uniregctb.type.TypeEvenementOrganisation;

/**
 * Classe de transtypage pour Hibernate : TypeEvenementOrganisation <--> varchar
 * 
 * @author Ludovic BERTIN
 */
public class TypeEvenementOrganisationUserType extends EnumUserType<TypeEvenementOrganisation> {

	/**
	 * Constructeur : on passe la classe de l'Enum a EnumUserType
	 */
	public TypeEvenementOrganisationUserType() {
		super(TypeEvenementOrganisation.class);
	}

}
