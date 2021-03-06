package ch.vd.unireg.identification.contribuable.manager;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.adresse.AdressesResolutionException;
import ch.vd.unireg.common.pagination.WebParamPagination;
import ch.vd.unireg.evenement.identification.contribuable.IdentificationContribuable;
import ch.vd.unireg.evenement.identification.contribuable.IdentificationContribuableCriteria;
import ch.vd.unireg.evenement.identification.contribuable.IdentificationContribuableEtatFilter;
import ch.vd.unireg.evenement.identification.contribuable.TypeDemande;
import ch.vd.unireg.identification.contribuable.view.IdentificationContribuableListCriteria;
import ch.vd.unireg.identification.contribuable.view.IdentificationMessagesResultView;
import ch.vd.unireg.interfaces.infra.InfrastructureException;

public interface IdentificationMessagesListManager {

	/**
	 * Recherche des identifications correspondant aux critères
	 */
	@Transactional(readOnly = true)
	List<IdentificationMessagesResultView> find(IdentificationContribuableCriteria bean, WebParamPagination pagination,
	                                            IdentificationContribuableEtatFilter filter, TypeDemande... typeDemande)
			throws AdressesResolutionException, InfrastructureException;

	/**
	 * Cherche et compte les identifications correspondant aux criteres
	 */
	@Transactional(readOnly = true)
	int count(IdentificationContribuableCriteria criterion, IdentificationContribuableEtatFilter filter, TypeDemande... typeDemande);


	/**
	 * Suspendre l'identification des messages
	 */
	@Transactional(rollbackFor = Throwable.class)
	void suspendreIdentificationMessages(IdentificationContribuableListCriteria identificationContribuableListCriteria);

	/**
	 * Soumettre l'identification des messages
	 */
	@Transactional(rollbackFor = Throwable.class)
	void soumettreIdentificationMessages(IdentificationContribuableListCriteria identificationContribuableListCriteria);

	/**
	 * Alimente la vue
	 */
	IdentificationContribuableListCriteria getView(String parametreTypeMessage, Integer parametrePeriode, IdentificationContribuable.Etat parametreEtat);

	/**
	 * Recherche des identifications correspondant seulement à l'état en cours
	 */
	@Transactional(readOnly = true)
	List<IdentificationMessagesResultView> findEncoursSeul(IdentificationContribuableCriteria bean, WebParamPagination pagination, TypeDemande... typeDemande)
			throws AdressesResolutionException, InfrastructureException;


	/**
	 * Cherche et compte les identifications correspondant à l'etat en cours
	 */
	@Transactional(readOnly = true)
	int countEnCoursSeul(IdentificationContribuableCriteria criterion, TypeDemande... typeDemande);


	/**
	 * Re soumettre l'identification des messages qui sont remis "dans le circuit" afin d'être identifié manuellement ou expèrtisé
	 */
	@Transactional(rollbackFor = Throwable.class)
	void reSoumettreIdentificationMessages(IdentificationContribuableListCriteria bean);

}
