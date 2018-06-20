package ch.vd.unireg.evenement.entreprise.manager;

import java.util.List;

import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.common.pagination.ParamPagination;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseException;
import ch.vd.unireg.evenement.entreprise.view.EvenementEntrepriseCriteriaView;
import ch.vd.unireg.evenement.entreprise.view.EvenementEntrepriseDetailView;
import ch.vd.unireg.evenement.entreprise.view.EvenementEntrepriseElementListeRechercheView;
import ch.vd.unireg.evenement.entreprise.view.EvenementEntrepriseSummaryView;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.TiersException;

/**
 * Classe qui permet de collecter les informations nécessaires à l'affichage
 *
 */
public interface EvenementEntrepriseManager {

	/**
	 * Charge la structure EvenementEntrepriseDetailView en fonction des informations de
	 * l'événement
	 *
	 * @param id ID d'evenement
	 * @return la structure EvenementEntrepriseDetailView correspondant à l'id
	 *
	 * @throws ch.vd.unireg.adresse.AdressesResolutionException ...
	 * @throws ServiceInfrastructureException ...
	 */
	EvenementEntrepriseDetailView get(Long id) throws AdresseException, ServiceInfrastructureException;

	/**
	 * Charge la structure EvenementEntrepriseSummaryView en fonction des informations de
	 * l'événement
	 *
	 * @param id ID d'evenement
	 * @return la structure EvenementEntrepriseSummaryView correspondant à l'id
	 *
	 * @throws ch.vd.unireg.adresse.AdressesResolutionException ...
	 * @throws ServiceInfrastructureException ...
	 */
	EvenementEntrepriseSummaryView getSummary(Long id) throws AdresseException, ServiceInfrastructureException;

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
	boolean recycleEvenementEntreprise(Long id) throws EvenementEntrepriseException;

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
	 * @throws ch.vd.unireg.adresse.AdressesResolutionException ...
	 */
	List<EvenementEntrepriseElementListeRechercheView> find(EvenementEntrepriseCriteriaView bean, ParamPagination pagination) throws AdresseException;

	/**
	 * Compte le nombre d'evenements correspondant aux criteres
	 *
	 * @param bean les critères en question
	 * @return le nombre d'évenements correspondant aux critères
	 */
	int count(EvenementEntrepriseCriteriaView bean);

	/**
	 * Créé "à la main" l'entreprise correspondant à l'événement entreprise. C'est une création simple, avec établissement dans la
	 * mesure du possible.
	 * @param id Le numéro de l'événement
	 * @return L'entreprise créée.
	 */
	Entreprise creerEntreprisePourEvenementEntreprise(Long id) throws TiersException;
}
