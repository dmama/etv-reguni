package ch.vd.uniregctb.tiers.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.view.AdresseView;
import ch.vd.uniregctb.tiers.view.TiersEditView;

/**
 * Service offrant les methodes permettant de gerer le controller TiersAdresseController
 *
 * @author xcifde
 *
 */
public interface AdresseManager {

	public abstract AdresseService getAdresseService();

	public abstract void setAdresseService(AdresseService adresseService);

	public abstract TiersService getTiersService();

	public abstract void setTiersService(TiersService tiersService);

	/**
	 * Alimente la vue AdresseView pour une adresse existante
	 *
	 * @param id
	 * @return
	 */
	public abstract AdresseView getAdresseView(Long id);

	/**
	 * Cree une nouvelle vue AdresseView pour une nouvelle adresse
	 *
	 * @param id
	 * @return
	 */
	public abstract AdresseView create(Long numeroCtb);

	/**
	 * Sauvegarde de l'adresse en base de donnees
	 *
	 * @param adresseView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public abstract void save(AdresseView adresseView);

	/**
	 * Annule une adresse
	 *
	 * @param idAdresse
	 */
	public void annulerAdresse(Long idAdresse);

	/**
	 * Sauvegarde d'une reprise d'adresse
	 *
	 * @param adresseView
	 */
	public abstract void saveReprise(AdresseView adresseView);

	/**
	 * Charge les informations dans TiersView
	 *
	 * @param numero
	 * @return un objet TiersView
	 * @throws AdressesResolutionException
	 * @throws InfrastructureException
	 */
	public TiersEditView getView(Long numero) throws AdressesResolutionException, InfrastructureException ;

}
