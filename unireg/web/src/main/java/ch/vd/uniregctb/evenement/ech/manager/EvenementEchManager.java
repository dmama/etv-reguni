package ch.vd.uniregctb.evenement.ech.manager;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.evenement.civil.EvenementCivilCriteria;
import ch.vd.uniregctb.evenement.ech.view.EvenementCivilEchView;
import ch.vd.uniregctb.evenement.ech.view.EvenementEchCriteriaView;
import ch.vd.uniregctb.evenement.ech.view.EvenementEchView;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException;

/**
 * Classe qui permet de collecter les informations nécessaires à l'affichage
 *
 */
public interface EvenementEchManager {

	/**
	 * Charge la structure EvenementView en fonction des informations de
	 * l'événement
	 *
	 * @param id ID d'evenement
	 * @return la structure EvenementView correspondant à l'id
	 *
	 * @throws ch.vd.uniregctb.adresse.AdressesResolutionException ...
	 * @throws ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException ...
	 */
	@Transactional(readOnly = true)
	public EvenementEchView get(Long id) throws AdresseException, ServiceInfrastructureException;

	/**
	 * Traite un evenement civil designe par l'id
	 *
	 * Seulement le premier evenement d'un individu doit pouvoir être recycler
	 *
	 * @param id de l'evt à traiter
	 */
	public void traiteEvenementCivil(Long id);

	/**
	 * Force l'etat de l'evenement à TRAITE
	 *
	 * Seulement le premier evenement d'un individu doit pouvoir être forcer
	 *
	 * @param id id de l'evt à forcer
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void forceEtatTraite(Long id);

	/**
	 * Recherche des événements correspondant aux critères
	 *
	 * @param bean critères de recherche tel que saisie par l'utilisateur
	 * @param pagination information sur la pagination pour la requete sous-jacente
	 * @return une liste d'evenement pret à afficher
	 * @throws ch.vd.uniregctb.adresse.AdressesResolutionException ...
	 */
	@Transactional(readOnly = true)
	public List<EvenementCivilEchView> find(EvenementEchCriteriaView bean, ParamPagination pagination) throws AdresseException;

	/**
	 * Compte le nombre d'evenements correspondant aux criteres
	 *
	 * @param criterion les critères en question
	 * @return le nombre d'évenements correspondant aux critères
	 */
	@Transactional(readOnly = true)
	public int count(EvenementCivilCriteria criterion);

}
