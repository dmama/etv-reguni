package ch.vd.uniregctb.identification.contribuable.manager;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.common.WebParamPagination;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuableCriteria;
import ch.vd.uniregctb.identification.contribuable.view.IdentificationMessagesListView;
import ch.vd.uniregctb.identification.contribuable.view.IdentificationMessagesResultView;

public interface IdentificationMessagesListManager {

	/**
	 * Recherche des identifications correspondant aux critères
	 * @param bean
	 * @param pagination
	 * @param nonTraiteOnly TODO
	 * @param archiveOnly TODO
	 * @param nonTraiterAndSuspendu TODO
	 * @return
	 * @throws AdressesResolutionException
	 * @throws InfrastructureException
	 */
	@Transactional(readOnly = true)
	public List<IdentificationMessagesResultView> find(IdentificationContribuableCriteria bean, WebParamPagination pagination, boolean nonTraiteOnly, boolean archiveOnly,boolean nonTraiterAndSuspendu)
	throws AdressesResolutionException, InfrastructureException ;

	/**
	 * Cherche et compte les identifications correspondant aux criteres
	 * @param criterion
	 * @param nonTraiteOnly TODO
	 * @param archiveOnly TODO
	 * @param nonTraiterAndSuspendu TODO
	 * @return
	 */
	@Transactional(readOnly = true)
	public int count(IdentificationContribuableCriteria criterion, boolean nonTraiteOnly, boolean archiveOnly,boolean nonTraiterAndSuspendu) ;

	/**
	 * Suspendre l'identification des messages
	 *
	 * @param identificationMessagesListView
	 * @throws InfrastructureException
	 * @throws EditiqueException
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void suspendreIdentificationMessages(IdentificationMessagesListView identificationMessagesListView)  ;

	/**
	 * Soumettre l'identification des messages
	 *
	 * @param identificationMessagesListView
	 * @throws InfrastructureException
	 * @throws EditiqueException
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void soumettreIdentificationMessages(IdentificationMessagesListView identificationMessagesListView)  ;

	/**
	 * Alimente la vue
	 * @param parametreTypeMessage TODO
	 * @param parametrePeriode TODO
	 * @param parametreEtat TODO
	 *
	 * @return
	 */
	public IdentificationMessagesListView getView(String parametreTypeMessage, String parametrePeriode, String parametreEtat) ;

	/**
	 * Recherche des identifications correspondant seulement à l'état en cours
	 * @param bean
	 * @param pagination
	 * @return
	 * @throws AdressesResolutionException
	 * @throws InfrastructureException
	 */
	public List<IdentificationMessagesResultView> findEncoursSeul(IdentificationContribuableCriteria bean, WebParamPagination pagination)
			throws AdressesResolutionException, InfrastructureException;



	/**
	 * Cherche et compte les identifications correspondant à l'etat en cours
	 * @param criterion
	 * @return
	 */
	@Transactional(readOnly = true)
	public int countEnCoursSeul(IdentificationContribuableCriteria criterion);


	/**
	 * Re soumettre l'identification des messages qui sont remis "dans le circuit" afin d'être identifié manuellement
	 * ou expèrtisé
	 *
	 * @param identificationMessagesListView
	 * @throws InfrastructureException
	 * @throws EditiqueException
	 */

	@Transactional(rollbackFor = Throwable.class)
	public void ResoumettreIdentificationMessages(IdentificationMessagesListView bean);

}
