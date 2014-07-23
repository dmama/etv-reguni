package ch.vd.uniregctb.rapport.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.common.TiersNotFoundException;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.rapport.view.RapportListView;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.manager.TiersManager;


/**
 * Classe offrant les services à RapportListController
 *
 * @author xcifde
 *
 */
public class RapportListManagerImpl extends TiersManager implements RapportListManager {

	/**
	 * Alimente la vue RapportListView (cas ou numero selectionne)
	 *
	 * @param numero
	 * @return une vue RapportListView
	 */
	@Override
	@Transactional(readOnly = true)
	public RapportListView get(Long numero) {
		Tiers tiers = tiersService.getTiers(numero);

		if (tiers == null) {
			throw new TiersNotFoundException(numero);
		}


		RapportListView rapportListView = new RapportListView();
		//gestion des droits
		rapportListView.setAllowed(checkDroitEdit(tiers));

		if(rapportListView.isAllowed()){
			TiersGeneralView tiersGeneralView = tiersGeneralManager.getTiers(tiers, true);
			rapportListView.setTiers(tiersGeneralView);
		}
		return rapportListView;
	}
}
