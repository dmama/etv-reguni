package ch.vd.uniregctb.tiers.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException;
import ch.vd.uniregctb.tiers.view.AdresseView;
import ch.vd.uniregctb.tiers.view.TiersEditView;

/**
 * Service offrant les methodes permettant de gerer le controller TiersAdresseController
 *
 * @author xcifde
 *
 */
public interface AdresseManager {

	/**
	 * Alimente la vue AdresseView pour une adresse existante
	 *
	 * @param id
	 * @return
	 */
	@Transactional(readOnly = true)
	public abstract AdresseView getAdresseView(Long id);


	public AdresseView getAdresseView(TiersEditView tiers,Long numero);



	/**
	 * Cree une nouvelle vue AdresseView pour une nouvelle adresse
	 *
	 * @param id
	 * @return
	 */
	@Transactional(readOnly = true)
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
	@Transactional(rollbackFor = Throwable.class)
	public void annulerAdresse(Long idAdresse);


/**
	 * ferme une adresse
	 *
	 * @param bean
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void fermerAdresse(AdresseView bean);

	/**
	 * Sauvegarde d'une reprise d'adresse
	 *
	 * @param adresseView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public abstract void saveReprise(AdresseView adresseView);

	/**
	 * Charge les informations dans TiersView
	 *
	 * @param numero
	 * @return un objet TiersView
	 * @throws AdressesResolutionException
	 * @throws ServiceInfrastructureException
	 */
	@Transactional(readOnly = true)
	public TiersEditView getView(Long numero) throws ServiceInfrastructureException, AdresseException;

}
