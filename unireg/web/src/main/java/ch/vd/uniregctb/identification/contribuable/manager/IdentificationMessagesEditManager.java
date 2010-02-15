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
	public IdentificationMessagesEditView getView(Long id) throws Exception ;

	/**
	 * Alimente le cartouche de demande d'identification
	 * @param id
	 * @return la vue du cartouche
	 */
	public DemandeIdentificationView getDemandeIdentificationView (Long id) throws Exception  ;

	/**
	 * Force l'identification du contribuable
	 * @param idIdentification
	 * @param idPersonne
	 * @param etat TODO
	 * @throws Exception
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void forceIdentification(Long idIdentification, Long idPersonne, Etat etat) throws Exception ;

	/**
	 * Donne à expertiser
	 * @param idIdentification
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void expertiser(Long idIdentification);

	/**
	 * Impossible à identifier
	 * @param bean TODO
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void impossibleAIdentifier(IdentificationMessagesEditView bean) throws Exception ;

	/**
	 * Verouille le message pour qu'il ne soit pas traité à double
	 */
	@Transactional(rollbackFor = Throwable.class)
	public  void verouillerMessage(Long idIdentification) throws Exception;
	/**
	 * deverouille le message après traitement
	 */
	@Transactional(rollbackFor = Throwable.class)
	public  void deVerouillerMessage(Long idIdentification) throws Exception;
}
