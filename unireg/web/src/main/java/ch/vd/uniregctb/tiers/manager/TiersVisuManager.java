package ch.vd.uniregctb.tiers.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.common.WebParamPagination;
import ch.vd.uniregctb.tiers.view.TiersVisuView;

/**
 * Service qui fournit les methodes pour visualiser un tiers
 *
 * @author xcifde
 *
 */
public interface TiersVisuManager {

	/**
	 * Charge les informations dans TiersVisuView
	 *
	 * @param numero
	 * @param adresseActive
	 * @return un objet TiersVisuView
	 * @throws AdressesResolutionException
	 */
	public TiersVisuView getView(Long numero, boolean adresseActive, WebParamPagination webParamPagination) throws AdressesResolutionException, InfrastructureException;

	/**
	 * Annule un tiers
	 *
	 * @param numero
	 * @param user
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void annulerTiers(Long numero) ;

	/**
	 * Compte le nombre de rapports prestation imposable pour un débiteur
	 * @param numeroDebiteur
	 * @return
	 */
	public int countRapportsPrestationImposable(Long numeroDebiteur);

}
