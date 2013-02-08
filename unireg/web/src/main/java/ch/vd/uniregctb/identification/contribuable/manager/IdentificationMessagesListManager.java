package ch.vd.uniregctb.identification.contribuable.manager;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.common.WebParamPagination;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuableCriteria;
import ch.vd.uniregctb.evenement.identification.contribuable.TypeDemande;
import ch.vd.uniregctb.identification.contribuable.view.IdentificationContribuableListCriteria;
import ch.vd.uniregctb.identification.contribuable.view.IdentificationMessagesResultView;

public interface IdentificationMessagesListManager {

	/**
	 * Recherche des identifications correspondant aux critères
	 */
	@Transactional(readOnly = true)
	public List<IdentificationMessagesResultView> find(IdentificationContribuableCriteria bean, WebParamPagination pagination, boolean nonTraiteOnly, boolean archiveOnly,
	                                                   boolean suspenduOnly, TypeDemande... typeDemande)
			throws AdressesResolutionException, ServiceInfrastructureException;

	/**
	 * Cherche et compte les identifications correspondant aux criteres
	 */
	@Transactional(readOnly = true)
	public int count(IdentificationContribuableCriteria criterion, boolean nonTraiteOnly, boolean archiveOnly, boolean suspenduOnly, TypeDemande... typeDemande);


	/**
	 * Suspendre l'identification des messages
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void suspendreIdentificationMessages(IdentificationContribuableListCriteria identificationContribuableListCriteria);

	/**
	 * Soumettre l'identification des messages
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void soumettreIdentificationMessages(IdentificationContribuableListCriteria identificationContribuableListCriteria);

	/**
	 * Alimente la vue
	 */
	public IdentificationContribuableListCriteria getView(String parametreTypeMessage, String parametrePeriode, String parametreEtat);

	/**
	 * Recherche des identifications correspondant seulement à l'état en cours
	 */
	@Transactional(readOnly = true)
	public List<IdentificationMessagesResultView> findEncoursSeul(IdentificationContribuableCriteria bean, WebParamPagination pagination, TypeDemande... typeDemande)
			throws AdressesResolutionException, ServiceInfrastructureException;


	/**
	 * Cherche et compte les identifications correspondant à l'etat en cours
	 */
	@Transactional(readOnly = true)
	public int countEnCoursSeul(IdentificationContribuableCriteria criterion, TypeDemande... typeDemande);


	/**
	 * Re soumettre l'identification des messages qui sont remis "dans le circuit" afin d'être identifié manuellement ou expèrtisé
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void reSoumettreIdentificationMessages(IdentificationContribuableListCriteria bean);

}
