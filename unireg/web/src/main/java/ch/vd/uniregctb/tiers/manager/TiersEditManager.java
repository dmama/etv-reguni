package ch.vd.uniregctb.tiers.manager;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.view.DebiteurEditView;
import ch.vd.uniregctb.tiers.view.TiersEditView;
import ch.vd.uniregctb.type.PeriodiciteDecompte;


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
	 * @throws ServiceInfrastructureException
	 */
	@Transactional(readOnly = true)
	TiersEditView getCivilView(Long numero) throws AdresseException, ServiceInfrastructureException;

	/**
	 * Charge les informations dans un objet qui servira de view
	 *
	 * @param numero numéro de tiers du débiteur recherché
	 * @return un objet DebiteurEditView
	 * @throws AdressesResolutionException
	 * @throws ServiceInfrastructureException
	 */
	@Transactional(readOnly = true)
	DebiteurEditView getDebiteurEditView(Long numero) throws AdresseException, ServiceInfrastructureException;

	/**
	 *
	 * @param dpiId
	 * @param nouvellePeriodicite
	 * @param maxDate
	 * @return
	 */
	@Transactional(readOnly = true)
	List<RegDate> getDatesPossiblesPourDebutNouvellePeriodicite(long dpiId, PeriodiciteDecompte nouvellePeriodicite, RegDate maxDate);

	/**
	 * Charge les informations dans TiersView
	 *
	 * @param numero
	 * @return un objet TiersView
	 * @throws AdressesResolutionException
	 */
	@Transactional(readOnly = true)
	TiersEditView getView(Long numero) throws AdresseException, ServiceInfrastructureException;

	/**
	 * Rafraichissement de la vue
	 *
	 * @param view
	 * @param numero
	 * @return
	 * @throws AdressesResolutionException
	 */
	@Transactional(readOnly = true)
	TiersEditView refresh(TiersEditView view, Long numero) throws AdresseException, ServiceInfrastructureException;

	/**
	 * Cree une nouvelle instance de TiersView correspondant a une personne
	 *
	 * @return un objet TiersView
	 */
	@Transactional(readOnly = true)
	TiersEditView creePersonne();

	/**
	 * Cree une nouvelle instance de TiersView correspondant a une organisation
	 *
	 * @return un objet TiersView
	 */
	@Transactional(readOnly = true)
	TiersEditView creeOrganisation();

	/**
	 * Cree une nouvelle instance de TiersView correspondant a un debiteur
	 *
	 * @return un objet TiersView
	 */
	@Transactional(readOnly = true)
	TiersEditView creeDebiteur(Long numeroCtbAssocie) throws AdressesResolutionException;

	/**
	 * Sauvegarde du tiers en base et mise a jour de l'indexeur
	 *
	 * @param tiersEditView
	 */
	@Transactional(rollbackFor = Throwable.class)
	Tiers save(TiersEditView tiersEditView);

	/**
	 * Sauvegarde du débiteur IS
	 * @param view paramètres connus dans le débiteur IS
	 */
	@Transactional(rollbackFor = Throwable.class)
	void save(DebiteurEditView view);

	/**
	 * Annule un tiers
	 *
	 * @param numero
	 */
	@Transactional(rollbackFor = Throwable.class)
	void annulerTiers(Long numero) ;

	/**
	 * Compte le nombre de rapports prestation imposable pour un débiteur
	 * @param numeroDebiteur
	 * @return
	 */
	@Transactional(readOnly = true)
	int countRapportsPrestationImposable(Long numeroDebiteur, boolean rapportsPrestationHisto);
}
