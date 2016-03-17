package ch.vd.unireg.interfaces.organisation;

import java.util.Map;

import ch.vd.unireg.interfaces.organisation.data.Organisation;

public interface ServiceOrganisationRaw {

	String SERVICE_NAME = "ServiceOrganisation";

	/**
	 * Recherche tous les états d'une organisation.
	 *
	 * @param noOrganisation Identifiant cantonal de l'organisation
	 * @return les données retournées par RCEnt
	 * @throws ServiceOrganisationException
	 */
	Organisation getOrganisationHistory(long noOrganisation) throws ServiceOrganisationException;

	/**
	 * Obtenir un numéro d'organisation à partir d'un numéro de site.
	 *
	 * @param noSite Identifiant cantonal du site.
	 * @return L'identifiant cantonal de l'organisation détenant le site.
	 * @throws ServiceOrganisationException
	 */
	Long getOrganisationPourSite(Long noSite) throws ServiceOrganisationException;


	/**
	 * Recherche les états avant et après de l'événement et contruit ls pseudo historique correspondant.
	 *
	 * @param noEvenement Identifiant de l'événement organisation
	 * @return les données retournées par RCEnt sous forme de map indexée par no cantonal.
	 * @throws ServiceOrganisationException
	 */
	Map<Long, Organisation> getPseudoOrganisationHistory(long noEvenement) throws ServiceOrganisationException;

	/**
	 * Méthode qui permet de tester que le service organisation répond bien. Cette méthode est insensible aux caches.
	 *
	 * @throws ServiceOrganisationException en cas de non-fonctionnement du service organisation
	 */
	void ping() throws ServiceOrganisationException;
}
