package ch.vd.uniregctb.tiers.manager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.uniregctb.tiers.TiersCriteria.TypeTiers;
import ch.vd.uniregctb.tiers.view.TiersCriteriaView;

/**
 * Classe permettant la gestion du controller TiersListController
 *
 * @author xcifde
 *
 */
public interface TiersListManager {

	/**
	 * Initialise les champs avec les valeurs des paramètres
	 *
	 * @param tiersCriteriaView
	 * @param typeTiers
	 * @param numero
	 * @param nomRaison
	 * @param localiteOuPays
	 * @param noOfsFor
	 * @param dateNaissance
	 * @param numeroAssureSocial
	 */
	public void initFieldsWithParams(	TiersCriteriaView tiersCriteriaView,
										TypeTiers typeTiers,
										String numero,
										String nomRaison,
										String localiteOuPays,
										String noOfsFor,
										RegDate dateNaissance,
										String numeroAssureSocial)
									throws NumberFormatException, ServiceInfrastructureException;

}
