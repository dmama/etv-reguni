package ch.vd.uniregctb.interfaces.service;

import java.util.Map;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationException;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationRaw;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.uniregctb.interfaces.model.AdressesCivilesHistoriques;

public interface ServiceOrganisationService {

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
	 * Recherche les états avant et après de l'événement et contruit la pseudo histoire correspondante.
	 *
	 * @param noEvenement Identifiant de l'événement organisation
	 * @return les données retournées par RCEnt sous forme de map indexée par no cantonal.
	 * @throws ServiceOrganisationException
	 */
	Map<Long, Organisation> getPseudoOrganisationHistory(long noEvenement) throws ServiceOrganisationException;

	/**
	 * @param noide numéro IDE (sous la forme sans point ni tiret)
	 * @return les identifiants de l'organisation et de son site qui correspondent à ce numéro IDE
	 * @throws ServiceOrganisationException en cas de souci quelque part
	 */
	ServiceOrganisationRaw.Identifiers getOrganisationByNoIde(String noide) throws ServiceOrganisationException;

	/**
	 * Obtenir un numéro d'organisation à partir d'un numéro de site.
	 *
	 * @param noSite Identifiant cantonal du site.
	 * @return L'identifiant cantonal de l'organisation détenant le site.
	 * @throws ServiceOrganisationException
	 */
	Long getOrganisationPourSite(Long noSite) throws ServiceOrganisationException;

	/**
	 * @param noOrganisation l'identifiant cantonal d'une organisation
	 * @return l'historique des adresses de cette organisation
	 * @throws ServiceOrganisationException en cas de souci
	 */
	AdressesCivilesHistoriques getAdressesOrganisationHisto(long noOrganisation) throws ServiceOrganisationException;

	/**
	 * @param noSite l'identifiant cantonal d'un site
	 * @return l'historique des adresses de ce site
	 * @throws ServiceOrganisationException en cas de souci
	 */
	AdressesCivilesHistoriques getAdressesSiteOrganisationHisto(long noSite) throws ServiceOrganisationException;

	@NotNull
	String createOrganisationDescription(Organisation organisation, RegDate date);
}
