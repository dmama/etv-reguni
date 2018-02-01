package ch.vd.unireg.evenement.ech.manager;

import java.util.List;

import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.common.pagination.ParamPagination;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.ech.view.EvenementCivilEchCriteriaView;
import ch.vd.unireg.evenement.ech.view.EvenementCivilEchDetailView;
import ch.vd.unireg.evenement.ech.view.EvenementCivilEchElementListeRechercheView;

/**
 * Classe qui permet de collecter les informations nécessaires à l'affichage
 *
 */
public interface EvenementCivilEchManager {

	/**
	 * Charge la structure EvenementCivilRegPPDetailView en fonction des informations de
	 * l'événement
	 *
	 * @param id ID d'evenement
	 * @return la structure EvenementCivilRegPPDetailView correspondant à l'id
	 *
	 * @throws ch.vd.unireg.adresse.AdressesResolutionException ...
	 * @throws ch.vd.unireg.interfaces.infra.ServiceInfrastructureException ...
	 */
	EvenementCivilEchDetailView get(Long id) throws AdresseException, ServiceInfrastructureException;

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
	boolean recycleEvenementCivil(Long id) throws EvenementCivilException;

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
	List<EvenementCivilEchElementListeRechercheView> find(EvenementCivilEchCriteriaView bean, ParamPagination pagination) throws AdresseException;

	/**
	 * Compte le nombre d'evenements correspondant aux criteres
	 *
	 * @param bean les critères en question
	 * @return le nombre d'évenements correspondant aux critères
	 */
	int count(EvenementCivilEchCriteriaView bean);
}
