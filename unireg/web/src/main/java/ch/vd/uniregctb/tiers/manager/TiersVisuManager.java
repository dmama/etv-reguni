package ch.vd.uniregctb.tiers.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.uniregctb.adresse.AdresseException;
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
	 * Charge les informations de visualisation d'un tiers
	 *
	 * @param numero                  le numéro du tiers dont on veut afficher le détails
	 * @param adressesHisto           <b>vrai</b> s'il faut charger tout l'historique des adresses
	 * @param rapportsPrestationHisto <b>vrai</b> s'il faut charger tout l'historique des rapports de prestation entre débiteur et sourciers
	 * @param webParamPagination      les informations de pagination  @return un objet TiersVisuView
	 * @return les informations de visualisation demandées.
	 * @throws ch.vd.infrastructure.service.InfrastructureException
	 *          en cas de problème de connexion au service d'infrastructure.
	 * @throws ch.vd.uniregctb.adresse.AdresseException
	 *          en cas de problème de résolution des adresses
	 */
	@Transactional(readOnly = true)
	public TiersVisuView getView(Long numero, boolean adressesHisto, boolean rapportsPrestationHisto, WebParamPagination webParamPagination) throws AdresseException, InfrastructureException;

	/**
	 * Annule un tiers
	 *
	 * @param numero un numéro de tiers à annuler
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void annulerTiers(Long numero) ;

	/**
	 * Compte le nombre de rapports prestation imposable pour un débiteur
	 *
	 * @param numeroDebiteur          un numéro de débiteur
	 * @param rapportsPrestationHisto <b>vrai</b> s'il faut charger tout l'historique des rapports de prestation entre débiteur et sourciers
	 * @return le nombre de rapports trouvés
	 */
	@Transactional(readOnly = true)
	public int countRapportsPrestationImposable(Long numeroDebiteur, boolean rapportsPrestationHisto);
}
