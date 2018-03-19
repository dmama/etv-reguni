package ch.vd.unireg.hibernate;

import ch.vd.unireg.type.EtatEvenementOrganisation;

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