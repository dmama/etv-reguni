package ch.vd.unireg.evenement.regpp.manager;

import java.util.List;

import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.adresse.AdressesResolutionException;
import ch.vd.unireg.common.pagination.ParamPagination;
import ch.vd.unireg.evenement.civil.EvenementCivilCriteria;
import ch.vd.unireg.evenement.regpp.view.EvenementCivilRegPPCriteriaView;
import ch.vd.unireg.evenement.regpp.view.EvenementCivilRegPPDetailView;
import ch.vd.unireg.evenement.regpp.view.EvenementCivilRegPPElementListeView;
import ch.vd.unireg.interfaces.infra.InfrastructureException;
import ch.vd.unireg.type.TypeEvenementCivil;

/**
 * Classe qui permet de collecter les informations nécessaires à l'affichage
 *
 */
public interface EvenementCivilRegPPManager {

	/**
	 * Charge la structure EvenementCivilRegPPDetailView en fonction des informations de
	 * l'événement
	 *
	 * @param id ID d'evenement
	 * @return la structure EvenementCivilRegPPDetailView correspondant à l'id
	 *
	 * @throws AdressesResolutionException ...
	 * @throws InfrastructureException ...
	 */
	EvenementCivilRegPPDetailView get(Long id) throws AdresseException, InfrastructureException;

	/**
	 * Traite un evenement civil designe par l'id
	 *
	 * @param id de l'evt à traiter
	 */
	void traiteEvenementCivil(Long id);

	/**
	 * Force l'etat de l'evenement à TRAITE
	 *
	 * @param id id de l'evt à forcer
	 */
	void forceEtatTraite(Long id);

	/**
	 * Recherche des événements correspondant aux critères
	 *
	 * @param bean critères de recherche tel que saisie par l'utilisateur
	 * @param pagination information sur la pagination pour la requete sous-jacente
	 * @return une liste d'evenement pret à afficher
	 * @throws AdressesResolutionException ...
	 */
	List<EvenementCivilRegPPElementListeView> find(EvenementCivilRegPPCriteriaView bean, ParamPagination pagination) throws AdresseException;

	/**
	 * Compte le nombre d'evenements correspondant aux criteres
	 *
	 * @param criterion les critères en question
	 * @return le nombre d'évenements correspondant aux critères
	 */
	int count(EvenementCivilCriteria<TypeEvenementCivil> criterion);

}
