package ch.vd.unireg.interfaces.service;

import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.model.AdressesCivilesHisto;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationException;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationRaw;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDEQuery;
import ch.vd.unireg.interfaces.organisation.data.BaseAnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.ServiceOrganisationEvent;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;

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
	 * Recherche les données de l'événement, en particulier des états avant et après pour chaque organisation touchée.
	 *
	 * L'objet retourné contient, en plus de la pseudo histoire correspondant à chaque organisation, les
	 * métadonnées éventuellement disponibles (RC et FOSC).
	 *
	 * @param noEvenement Identifiant de l'événement organisation
	 * @return les données de l'événement sous forme de map indexée par no cantonal.
	 * @throws ServiceOrganisationException
	 */
	Map<Long, ServiceOrganisationEvent> getOrganisationEvent(long noEvenement) throws ServiceOrganisationException;

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
	AdressesCivilesHisto getAdressesOrganisationHisto(long noOrganisation) throws ServiceOrganisationException;

	/**
	 * @param noSite l'identifiant cantonal d'un site
	 * @return l'historique des adresses de ce site; ou <b>null</b> si le site n'existe pas.
	 * @throws ServiceOrganisationException en cas de souci
	 */
	@Nullable
	AdressesCivilesHisto getAdressesSiteOrganisationHisto(long noSite) throws ServiceOrganisationException;

	/**
	 * Obtenir le contenu et le statut d'une annonce à l'IDE.
	 * <p>
	 *     Attention: RCEnt ne connait pas nécessairement une annonce qu'on lui a envoyé, du fait du caractère asynchrone de la
	 *     transmition par l'esb.
	 * </p>
	 *
	 * @param numero le numéro de l'annonce recherchée
	 * @return l'annonce à l'IDE, ou null si RCEnt ne connait pas d'annonce pour ce numéro.
	 * @throws ServiceOrganisationException en cas de problème d'accès ou de cohérence des données retournées.
	 */
	@Nullable
	AnnonceIDE getAnnonceIDE(long numero) throws ServiceOrganisationException;

	/**
	 * Recherche des demandes d'annonces à l'IDE.
	 *
	 * @param query          les critères de recherche des annonces
	 * @param order          l'ordre de tri demandé pour les résultats
	 * @param pageNumber     le numéro de page demandée (0-based)
	 * @param resultsPerPage le nombre d'éléments par page
	 * @return une page avec les annonces correspondantes
	 */
	@NotNull
	Page<AnnonceIDE> findAnnoncesIDE(@NotNull AnnonceIDEQuery query, @Nullable Sort.Order order, int pageNumber, int resultsPerPage) throws ServiceOrganisationException;

	/**
	 * Demander la validation d'une annonce à l'IDE par le registre civil avant l'envoi.
	 * @param annonceIDE l'annonce candidate
	 * @return le statut résultant contenant les éventuelles erreurs rapportées par le service civil.
	 */
	BaseAnnonceIDE.Statut validerAnnonceIDE(BaseAnnonceIDE annonceIDE) throws ServiceOrganisationException;

	@NotNull
	String createOrganisationDescription(Organisation organisation, RegDate date);

	String afficheAttributsSite(@Nullable SiteOrganisation site, @Nullable RegDate date);
}
