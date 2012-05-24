package ch.vd.uniregctb.evenement.regpp.manager;

import java.util.List;

import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.evenement.civil.EvenementCivilCriteria;
import ch.vd.uniregctb.evenement.regpp.view.EvenementCivilRegPPCriteriaView;
import ch.vd.uniregctb.evenement.regpp.view.EvenementCivilRegPPDetailView;
import ch.vd.uniregctb.evenement.regpp.view.EvenementCivilRegPPElementListeView;

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
	 * @throws ch.vd.unireg.interfaces.infra.ServiceInfrastructureException ...
	 */
	public EvenementCivilRegPPDetailView get(Long id) throws AdresseException, ServiceInfrastructureException;

	/**
	 * Traite un evenement civil designe par l'id
	 *
	 * @param id de l'evt à traiter
	 */
	public void traiteEvenementCivil(Long id);

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
	 * @throws AdressesResolutionException ...
	 */
	public List<EvenementCivilRegPPElementListeView> find(EvenementCivilRegPPCriteriaView bean, ParamPagination pagination) throws AdresseException;

	/**
	 * Compte le nombre d'evenements correspondant aux criteres
	 *
	 * @param criterion les critères en question
	 * @return le nombre d'évenements correspondant aux critères
	 */
	public int count(EvenementCivilCriteria criterion);

}
