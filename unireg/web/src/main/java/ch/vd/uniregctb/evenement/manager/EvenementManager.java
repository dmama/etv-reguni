package ch.vd.uniregctb.evenement.manager;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPPCriteria;
import ch.vd.uniregctb.evenement.view.EvenementCivilView;
import ch.vd.uniregctb.evenement.view.EvenementCriteriaView;
import ch.vd.uniregctb.evenement.view.EvenementView;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException;

/**
 * Classe qui permet de collecter les informations nécessaires à l'affichage
 *
 * @author xcifde
 *
 */
public interface EvenementManager {

	/**
	 * Charge la structure EvenementView en fonction des informations de
	 * l'événement
	 *
	 * @param id ID d'evenement
	 * @return
	 * @throws AdressesResolutionException
	 */
	@Transactional(readOnly = true)
	public EvenementView get(Long id) throws AdresseException, ServiceInfrastructureException;

	/**
	 * Traite un evenement civil designe par l'id
	 *
	 * @param id
	 */
	public void traiteEvenementCivil(Long id);

	/**
	 * Force l'etat de l'evenement à TRAITE
	 *
	 * @param id
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void forceEtatTraite(Long id);

	/**
	 * Recherche des événements correspondant aux critères
	 * @param bean
	 * @param pagination
	 * @return
	 * @throws AdressesResolutionException
	 */
	@Transactional(readOnly = true)
	public List<EvenementCivilView> find(EvenementCriteriaView bean, ParamPagination pagination) throws AdresseException;

	/**
	 * Cherche et compte les evenements correspondant aux criteres
	 * @param criterion
	 * @return
	 */
	@Transactional(readOnly = true)
	public int count(EvenementCivilRegPPCriteria criterion);

}
