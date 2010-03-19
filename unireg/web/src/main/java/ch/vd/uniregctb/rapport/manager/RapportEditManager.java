package ch.vd.uniregctb.rapport.manager;

import ch.vd.uniregctb.adresse.AdresseException;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.common.WebParamPagination;
import ch.vd.uniregctb.rapport.view.RapportView;
import ch.vd.uniregctb.tiers.view.TiersEditView;
import ch.vd.uniregctb.type.SensRapportEntreTiers;

/**
 * Claase offrant les services au controller RapportEditController
 *
 * @author xcifde
 *
 */
public interface RapportEditManager {

	/**
	 * Alimente la vue RapportView
	 *
	 * @param numeroTiers
	 * @param numeroTiersLie
	 * @return une RapportView
	 * @throws AdressesResolutionException
	 */
	@Transactional(readOnly = true)
	public RapportView get (Long numeroTiers, Long numeroTiersLie) throws AdressesResolutionException;

	/**
	 * Alimente la vue RapportView
	 *
	 * @return
	 * @throws AdressesResolutionException
	 */
	@Transactional(readOnly = true)
	public RapportView get (Long idRapport, SensRapportEntreTiers sensRapportEntreTiers) throws AdresseException;


	/**
	 * Persiste le rapport entre tiers
	 * @param rapportView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void save(RapportView rapportView) ;

	/**
	 * Annule le rapport
	 *
	 * @param idRapport
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void annulerRapport(Long idRapport) ;

	/**
	 * Charge les informations dans TiersView
	 *
	 * @param numero
	 * @return un objet TiersView
	 * @throws AdressesResolutionException
	 * @throws InfrastructureException
	 */
	@Transactional(readOnly = true)
	public TiersEditView getView(Long numero) throws AdresseException, InfrastructureException;

	/**
	 * Charge les informations dans TiersView
	 *
	 * @param numero
	 * @param webParamPagination
	 * @return un objet TiersView
	 * @throws AdressesResolutionException
	 * @throws InfrastructureException
	 */
	@Transactional(readOnly = true)
	public TiersEditView getRapportsPrestationView(Long numero, WebParamPagination webParamPagination) throws AdresseException, InfrastructureException;

}
