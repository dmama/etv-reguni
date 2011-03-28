package ch.vd.uniregctb.rapport.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.common.WebParamPagination;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException;
import ch.vd.uniregctb.rapport.SensRapportEntreTiers;
import ch.vd.uniregctb.rapport.view.RapportView;
import ch.vd.uniregctb.tiers.view.TiersEditView;

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
	 * Construit la vue qui permet d'éditer un rapport.
	 *
	 * @param idRapport   l'id du rapport à éditer
	 * @param editingFrom <i>OBJET</i> si le rapport est édité depuis le tiers objet ou  <i>SUJET</i> si le rapport est édité depuis le tiers sujet.
	 * @return une vue du rapport à éditer
	 * @throws AdresseException s'il y a un problème dans la construction de l'adresse
	 */
	@Transactional(readOnly = true)
	public RapportView get(Long idRapport, SensRapportEntreTiers editingFrom) throws AdresseException;


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
	 * @throws ServiceInfrastructureException
	 */
	@Transactional(readOnly = true)
	public TiersEditView getView(Long numero) throws AdresseException, ServiceInfrastructureException;

	/**
	 * Charge les informations dans TiersView
	 *
	 * @param numero
	 * @param webParamPagination
	 * @param rapportsPrestationHisto
	 * @return un objet TiersView
	 * @throws AdressesResolutionException
	 * @throws ServiceInfrastructureException
	 */
	@Transactional(readOnly = true)
	public TiersEditView getRapportsPrestationView(Long numero, WebParamPagination webParamPagination, boolean rapportsPrestationHisto) throws AdresseException, ServiceInfrastructureException;

}
