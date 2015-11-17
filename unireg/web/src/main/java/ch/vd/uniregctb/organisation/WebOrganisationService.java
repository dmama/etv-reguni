package ch.vd.uniregctb.organisation;


public interface WebOrganisationService {

	/**
	 * Alimente une vue OrganisationView en fonction du numero d'organisation
	 *
	 * @param numeroOrganisation le numéro de l'organisation en question
	 * @return un objet OrganisationView representant l'organisation
	 */
	OrganisationView getOrganisation(Long numeroOrganisation);

}
