package ch.vd.uniregctb.hibernate;

import ch.vd.uniregctb.type.EmetteurEvenementOrganisation;

/**
 * Classe de transtypage pour Hibernate : EmetteurEvenementOrganisation <--> varchar
 * 
 * @author Ludovic BERTIN
 */
public class EmetteurEvenementOrganisationUserType extends EnumUserType<EmetteurEvenementOrganisation> {

	/**
	 * Constructeur : on passe la classe de l'Enum a EnumUserType
	 */
	public EmetteurEvenementOrganisationUserType() {
		super(EmetteurEvenementOrganisation.class);
	}

}
