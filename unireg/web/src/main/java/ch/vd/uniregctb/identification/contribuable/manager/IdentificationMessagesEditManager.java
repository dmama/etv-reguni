package ch.vd.uniregctb.identification.contribuable.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable.Etat;
import ch.vd.uniregctb.identification.contribuable.view.DemandeIdentificationView;
import ch.vd.uniregctb.identification.contribuable.view.IdentificationMessagesEditView;

public interface IdentificationMessagesEditManager {

	/**
	 * Alimente la vue
	 * @param id
	 * @return la vue
	 */
	@Transactional(readOnly = true)
	IdentificationMessagesEditView getView(Long id) throws Exception ;

	/**
	 * Alimente le cartouche de demande d'identification
	 * @param id
	 * @return la vue du cartouche
	 */
	@Transactional(readOnly = true)
	DemandeIdentificationView getDemandeIdentificationView (Long id) throws Exception  ;

	/**
	 * Force l'identification du contribuable
	 */
	@Transactional(rollbackFor = Throwable.class)
	void forceIdentification(Long idIdentification, Long idPersonne, Etat etat) throws Exception ;

	/**
	 * Donne à expertiser
	 * @param idIdentification
	 */
	@Transactional(rollbackFor = Throwable.class)
	void expertiser(Long idIdentification);

	/**
	 * Impossible à identifier
	 */
	@Transactional(rollbackFor = Throwable.class)
	void impossibleAIdentifier(IdentificationMessagesEditView bean) throws Exception ;

	/**
	 * Verouille le message pour qu'il ne soit pas traité à double
	 */
	@Transactional(rollbackFor = Throwable.class)
	void verouillerMessage(Long idIdentification) throws Exception;

	/**
	 * deverouille le message après traitement
	 */
	@Transactional(rollbackFor = Throwable.class)
	void deVerouillerMessage(Long idIdentification, boolean byAdmin) throws Exception;

	/**
	 * Indique si le message dont l'id est passé en paramètre est en cours de traitement donc vérouillé
	 * @param idIdentification l'id du message a vérifier
	 * @return true si le message est en cours de traitement par un user false sinon
	 */
	@Transactional(readOnly = true)
	boolean isMessageVerouille(Long idIdentification) throws Exception;

	/**
	 * Relance une identification automatique sur le message en question
	 * @return le numéro du contribuable identifié en cas d'identification réussie (<code>null</code> si le message était déjà traité ou si une nouvelle tentative d'identification n'est pas concluante)
	 */
	@Transactional(rollbackFor = Throwable.class)
	Long relanceIdentificationAuto(long idIdentification) throws Exception;
}
