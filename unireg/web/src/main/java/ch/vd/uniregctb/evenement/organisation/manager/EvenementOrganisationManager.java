package ch.vd.uniregctb.evenement.organisation.manager;

import java.util.List;

import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.view.EvenementOrganisationCriteriaView;
import ch.vd.uniregctb.evenement.organisation.view.EvenementOrganisationDetailView;
import ch.vd.uniregctb.evenement.organisation.view.EvenementOrganisationElementListeRechercheView;
import ch.vd.uniregctb.evenement.organisation.view.EvenementOrganisationSummaryView;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.TiersException;

/**
 * Classe qui permet de collecter les informations nécessaires à l'affichage
 *
 */
public interface EvenementOrganisationManager {

	/**
	 * Charge la structure EvenementOrganisationDetailView en fonction des informations de
	 * l'événement
	 *
	 * @param id ID d'evenement
	 * @return la structure EvenementOrganisationDetailView correspondant à l'id
	 *
	 * @throws ch.vd.uniregctb.adresse.AdressesResolutionException ...
	 * @throws ServiceInfrastructureException ...
	 */
	EvenementOrganisationDetailView get(Long id) throws AdresseException, ServiceInfrastructureException;

	/**
	 * Charge la structure EvenementOrganisationSummaryView en fonction des informations de
	 * l'événement
	 *
	 * @param id ID d'evenement
	 * @return la structure EvenementOrganisationSummaryView correspondant à l'id
	 *
	 * @throws ch.vd.uniregctb.adresse.AdressesResolutionException ...
	 * @throws ServiceInfrastructureException ...
	 */
	EvenementOrganisationSummaryView getSummary(Long id) throws AdresseException, ServiceInfrastructureException;

	/**
	 * Recycle l'evenement civil designe par l'id
	 *
	 * Seulement le premier evenement d'un individu doit pouvoir être recycler
	 *
	 * @param id de l'evt à traiter
	 *
	 * @return true si l'evenement a été recyclé avant la sortie de la méthode (le traitement est asynchrone)
	 * false si l'evenement est toujours en attente de traitement une fois sortie de la méthode
	 */
	boolean recycleEvenementOrganisation(Long id) throws EvenementOrganisationException;

	/**
	 * Force l'etat de l'evenement à TRAITE
	 *
	 * @param id id de l'evt à forcer
	 */
	void forceEvenement(Long id);

	/**
	 * Recherche des événements correspondant aux critères
	 *
	 * @param bean critères de recherche tel que saisie par l'utilisateur
	 * @param pagination information sur la pagination pour la requete sous-jacente
	 * @return une liste d'evenement pret à afficher
	 * @throws ch.vd.uniregctb.adresse.AdressesResolutionException ...
	 */
	List<EvenementOrganisationElementListeRechercheView> find(EvenementOrganisationCriteriaView bean, ParamPagination pagination) throws AdresseException;

	/**
	 * Compte le nombre d'evenements correspondant aux criteres
	 *
	 * @param bean les critères en question
	 * @return le nombre d'évenements correspondant aux critères
	 */
	int count(EvenementOrganisationCriteriaView bean);

	/**
	 * Créé "à la main" l'entreprise correspondant à l'événement organisation. C'est une création simple, avec établissement dans la
	 * mesure du possible.
	 * @param id Le numéro de l'événement
	 * @return L'entreprise créée.
	 */
	Entreprise creerEntreprisePourEvenementOrganisation(Long id) throws TiersException;
}
