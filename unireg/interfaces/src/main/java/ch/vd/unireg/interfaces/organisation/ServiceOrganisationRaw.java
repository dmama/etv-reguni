package ch.vd.unireg.interfaces.organisation;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.Organisation;

public interface ServiceOrganisationRaw {

	/**
	 * Recherche l'état d'organisation aujourd'hui.
	 *
	 * @param cantonalId Identifiant cantonal de l'organisation
	 * @return les données retournées par RCEnt
	 * @throws ServiceOrganisationException
	 */
	Organisation getOrganisation(long cantonalId) throws ServiceOrganisationException;

	/**
	 * Recherche l'état d'une organisation à la date indiquée.
	 *
	 * @param cantonalId Identifiant cantonal de l'organisation
	 * @param date la date. Optionel. Comportement par défaut de RCEnt si null.
	 * @return les données retournées par RCEnt
	 * @throws ServiceOrganisationException
	 */
	Organisation getOrganisation(long cantonalId, RegDate date) throws ServiceOrganisationException;

	/**
	 * Recherche tous les états d'une organisation.
	 *
	 * @param cantonalId Identifiant cantonal de l'organisation
	 * @return les données retournées par RCEnt
	 * @throws ServiceOrganisationException
	 */
	Organisation getOrganisationHistory(long cantonalId) throws ServiceOrganisationException;

	/**
	 * Obtenir un numéro d'organisation à partir d'un numéro de site.
	 *
	 * @param noSite Identifiant cantonal du site.
	 * @return L'identifiant cantonal de l'organisation détenant le site.
	 * @throws ServiceOrganisationException
	 */
	Long getOrganisationPourSite(Long noSite) throws ServiceOrganisationException;
}
