package ch.vd.uniregctb.tiers.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.common.DonneesCivilesException;
import ch.vd.uniregctb.common.WebParamPagination;
import ch.vd.uniregctb.tiers.HistoFlags;
import ch.vd.uniregctb.tiers.view.RapportsPrestationView;
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
	 * @param histoFlags              les flags "histo" demandés
	 * @param modeImpression
	 * @param forsPrincipauxPagines <b>vrai</b> s'il faut paginer les fors principaux (et donc <b>faux</b> si on veut la vue complète)
	 * @param forsSecondairesPagines <b>vrai</b> s'il faut paginer les fors secondaires (et donc <b>faux</b> si on veut la vue complète)
	 * @param autresForsPagines <b>vrai</b> s'il faut paginer les "autres fors fiscaux" (et donc <b>faux</b> si on veut la vue complète)
	 * @param webParamPagination      les informations de pagination  @return un objet TiersVisuView  @Param ctbAssocieHisto <b>vrai</b> s'il faut charger tout l'historique des rapports de contribuable associé
	 * @return les informations de visualisation demandées.
	 * @throws ch.vd.unireg.interfaces.infra.ServiceInfrastructureException
	 *          en cas de problème de connexion au service d'infrastructure.
	 * @throws ch.vd.uniregctb.adresse.AdresseException
	 *          en cas de problème de résolution des adresses
	 */
	@Transactional(readOnly = true)
	TiersVisuView getView(Long numero, HistoFlags histoFlags, boolean modeImpression, boolean forsPrincipauxPagines, boolean forsSecondairesPagines, boolean autresForsPagines, WebParamPagination webParamPagination)
			throws AdresseException, ServiceInfrastructureException, DonneesCivilesException;

	/**
	 * Compte le nombre de rapports prestation imposable pour un débiteur
	 *
	 * @param numeroDebiteur          un numéro de débiteur
	 * @param rapportsPrestationHisto <b>vrai</b> s'il faut charger tout l'historique des rapports de prestation entre débiteur et sourciers
	 * @return le nombre de rapports trouvés
	 */
	@Transactional(readOnly = true)
	int countRapportsPrestationImposable(Long numeroDebiteur, boolean rapportsPrestationHisto);

	@Transactional(readOnly = true)
	void fillRapportsPrestationView(long noDebiteur, RapportsPrestationView view);
}
