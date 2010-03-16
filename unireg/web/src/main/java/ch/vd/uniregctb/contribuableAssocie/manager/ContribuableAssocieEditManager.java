package ch.vd.uniregctb.contribuableAssocie.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.contribuableAssocie.view.ContribuableAssocieEditView;
import ch.vd.uniregctb.contribuableAssocie.view.ContribuableAssocieListView;

public interface ContribuableAssocieEditManager {

	/**
	 * Alimente la vue RapportView
	 *
	 * @param numeroTiers
	 * @param numeroTiersLie
	 * @return une RapportView
	 * @throws AdressesResolutionException
	 */
	public ContribuableAssocieEditView get (Long numeroDebiteur, Long numeroContribuable) throws AdressesResolutionException;

	/**
	 * Persiste le contact impôt source entre le débiteur et le contribuable
	 * @param contribuableAssocieEditView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void save(ContribuableAssocieEditView contribuableAssocieEditView) ;


	/**
	 * Charge l'écran de recherche du contribuable associé
	 *
	 * @param numeroDpi
	 * @return
	 */
	public ContribuableAssocieListView getContribuableList(Long numeroDpi) ;
}
