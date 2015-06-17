package ch.vd.unireg.interfaces.organisation;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;

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
	 * Recherche l'état d'un établissement aujourd'hui.
	 *
	 * NOTE: La structure renvoiée est bien celle d'une organisation mais qui ne contient QUE
	 * l'établissement demandé.
	 * @param cantonalId
	 * @return
	 * @throws ServiceOrganisationException
	 */
	SiteOrganisation getLocation(Long cantonalId) throws ServiceOrganisationException;

	/**
	 * Recherche l'état d'un établissement à la date indiquée.
	 *
	 * NOTE: La structure renvoiée est bien celle d'une organisation mais qui ne contient QUE
	 * l'établissement demandé.
	 * @param cantonalId
	 * @param date
	 * @return
	 * @throws ServiceOrganisationException
	 */
	SiteOrganisation getLocation(Long cantonalId, RegDate date) throws ServiceOrganisationException;

	/**
	 * Recherche tous les états d'un établissement.
	 *
	 * NOTE: La structure renvoiée est bien celle d'une organisation mais qui ne contient QUE
	 * l'établissement demandé.
	 * @param cantonalId
	 * @return
	 * @throws ServiceOrganisationException
	 */
	SiteOrganisation getLocationHistory(Long cantonalId) throws ServiceOrganisationException;

}
