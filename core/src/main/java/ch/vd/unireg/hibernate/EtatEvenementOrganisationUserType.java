package ch.vd.uniregctb.hibernate;

import ch.vd.uniregctb.type.EtatEvenementOrganisation;

/**
 * Classe de transtypage pour Hibernate : EtatEvenementOrganisation <--> varchar
 * 
 * @author Ludovic BERTIN
 */
public class EtatEvenementOrganisationUserType extends EnumUserType<EtatEvenementOrganisation> {

	/**
	 * Constructeur : on passe la classe de l'Enum a EnumUserType
	 */
	public EtatEvenementOrganisationUserType() {
		super(EtatEvenementOrganisation.class);
	}

}
