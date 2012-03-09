package ch.vd.uniregctb.evenement.ech.manager;

import java.util.List;

import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.evenement.civil.EvenementCivilCriteria;
import ch.vd.uniregctb.evenement.ech.view.EvenementCivilEchCriteriaView;
import ch.vd.uniregctb.evenement.ech.view.EvenementCivilEchDetailView;
import ch.vd.uniregctb.evenement.ech.view.EvenementCivilEchElementListeRechercheView;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException;

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
	 * @throws ch.vd.uniregctb.adresse.AdressesResolutionException ...
	 * @throws ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException ...
	 */
	public EvenementCivilEchDetailView get(Long id) throws AdresseException, ServiceInfrastructureException;

	/**
	 * Recycle l'evenement civil designe par l'id
	 *
	 * Seulement le premier evenement d'un individu doit pouvoir être recycler
	 *
	 * @param id de l'evt à traiter
	 */
	public void recycleEvenementCivil(Long id);

	/**
	 * Force l'etat de l'evenement à TRAITE
	 *
	 * @param id id de l'evt à forcer
	 */
	public void forceEtatTraite(Long id);

	/**
	 * Recherche des événements correspondant aux critères
	 *
	 * @param bean critères de recherche tel que saisie par l'utilisateur
	 * @param pagination information sur la pagination pour la requete sous-jacente
	 * @return une liste d'evenement pret à afficher
	 * @throws ch.vd.uniregctb.adresse.AdressesResolutionException ...
	 */
	public List<EvenementCivilEchElementListeRechercheView> find(EvenementCivilEchCriteriaView bean, ParamPagination pagination) throws AdresseException;

	/**
	 * Compte le nombre d'evenements correspondant aux criteres
	 *
	 * @param criterion les critères en question
	 * @return le nombre d'évenements correspondant aux critères
	 */
	public int count(EvenementCivilCriteria criterion);

}
