package ch.vd.uniregctb.tiers.manager;

import ch.vd.uniregctb.adresse.AdresseException;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.view.TiersEditView;


/**
 * Service qui fournit les methodes pour editer un tiers
 *
 */
public interface TiersEditManager {

	/**
	 * Charge les informations dans TiersView
	 *
	 * @param numero
	 * @return un objet TiersView
	 * @throws AdressesResolutionException
	 * @throws InfrastructureException
	 */
	@Transactional(readOnly = true)
	public TiersEditView getComplementView(Long numero) throws AdresseException, InfrastructureException;

	/**
	 * Charge les informations dans TiersView
	 *
	 * @param numero
	 * @return un objet TiersView
	 * @throws AdressesResolutionException
	 * @throws InfrastructureException
	 */
	@Transactional(readOnly = true)
	public TiersEditView getDebiteursView(Long numero) throws AdresseException, InfrastructureException;


	/**
	 * Charge les informations dans TiersView
	 *
	 * @param numero
	 * @return un objet TiersView
	 * @throws AdressesResolutionException
	 */
	@Transactional(readOnly = true)
	public TiersEditView getView(Long numero) throws AdresseException, InfrastructureException;

	/**
	 * Rafraichissement de la vue
	 *
	 * @param view
	 * @param numero
	 * @return
	 * @throws AdressesResolutionException
	 */
	@Transactional(readOnly = true)
	public TiersEditView refresh(TiersEditView view, Long numero) throws AdresseException, InfrastructureException;

	/**
	 * Cree une nouvelle instance de TiersView correspondant a une personne
	 *
	 * @return un objet TiersView
	 */
	@Transactional(readOnly = true)
	public abstract TiersEditView creePersonne();

	/**
	 * Cree une nouvelle instance de TiersView correspondant a une organisation
	 *
	 * @return un objet TiersView
	 */
	@Transactional(readOnly = true)
	public abstract TiersEditView creeOrganisation();

	/**
	 * Cree une nouvelle instance de TiersView correspondant a un debiteur
	 *
	 * @return un objet TiersView
	 */
	@Transactional(readOnly = true)
	public abstract TiersEditView creeDebiteur(Long numeroCtbAssocie) throws AdressesResolutionException;

	/**
	 * Sauvegarde du tiers en base et mise a jour de l'indexeur
	 *
	 * @param tiersEditView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public abstract Tiers save(TiersEditView tiersEditView);

	/**
	 * Annule un tiers
	 *
	 * @param numero
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void annulerTiers(Long numero) ;

	/**
	 * Compte le nombre de rapports prestation imposable pour un débiteur
	 * @param numeroDebiteur
	 * @return
	 */
	@Transactional(readOnly = true)
	public int countRapportsPrestationImposable(Long numeroDebiteur, boolean rapportsPrestationHisto);
}
