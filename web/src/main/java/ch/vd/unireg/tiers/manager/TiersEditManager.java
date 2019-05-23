package ch.vd.unireg.tiers.manager;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.adresse.AdressesResolutionException;
import ch.vd.unireg.interfaces.infra.InfrastructureException;
import ch.vd.unireg.tiers.view.DebiteurEditView;
import ch.vd.unireg.tiers.view.TiersEditView;
import ch.vd.unireg.type.PeriodiciteDecompte;


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
	TiersEditView getCivilView(Long numero) throws AdresseException, InfrastructureException;

	/**
	 * Charge les informations dans un objet qui servira de view
	 *
	 * @param numero numéro de tiers du débiteur recherché
	 * @return un objet DebiteurEditView
	 * @throws AdressesResolutionException
	 * @throws InfrastructureException
	 */
	@Transactional(readOnly = true)
	DebiteurEditView getDebiteurEditView(Long numero) throws AdresseException, InfrastructureException;

	/**
	 * @param dpiId identifiant technique du débiteur
	 * @param nouvellePeriodicite nouvelle périodicité souhaitée
	 * @param maxDate date maximale (comprise) pour la limitation des dates proposées
	 * @param descendingOnly <code>true</code> si l'utilisateur courant n'a pas le droit d'agrandir les périodicité, <code>false</code> s'il peut le faire (= SuperGRA)
	 * @return une liste de dates possibles pour changer vers la périodicité demandée
	 */
	@Transactional(readOnly = true)
	List<RegDate> getDatesPossiblesPourDebutNouvellePeriodicite(long dpiId, PeriodiciteDecompte nouvellePeriodicite, RegDate maxDate, boolean descendingOnly);

	/**
	 * Charge les informations dans TiersView
	 *
	 * @param numero
	 * @return un objet TiersView
	 * @throws AdressesResolutionException
	 */
	@Transactional(readOnly = true)
	TiersEditView getView(Long numero) throws AdresseException, InfrastructureException;

	/**
	 * Rafraichissement de la vue
	 *
	 * @param view
	 * @param numero
	 * @return
	 * @throws AdressesResolutionException
	 */
	@Transactional(readOnly = true)
	TiersEditView refresh(TiersEditView view, Long numero) throws AdresseException, InfrastructureException;

	/**
	 * Cree une nouvelle instance de TiersView correspondant a une personne
	 *
	 * @return un objet TiersView
	 */
	@Transactional(readOnly = true)
	TiersEditView creePersonne();

	/**
	 * Cree une nouvelle instance de TiersView correspondant a une entreprise
	 *
	 * @return un objet TiersView
	 */
	@Transactional(readOnly = true)
	TiersEditView creeEntreprise();

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
