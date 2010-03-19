package ch.vd.uniregctb.activation.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.activation.TiersActivationListController;
import ch.vd.uniregctb.activation.view.TiersActivationListView;

public class TiersActivationListManagerImpl implements TiersActivationListManager{

	/**
	 * Alimente la vue TiersActivationListView
	 *
	 * @param activation
	 * @return
	 */
	public TiersActivationListView get(String activation) {
		TiersActivationListView bean;
		bean = new TiersActivationListView();
		bean.setTypeRechercheDuNom(TiersActivationListView.TypeRecherche.EST_EXACTEMENT);
		if (TiersActivationListController.ACTIVATION_ANNULATION_VALUE.equals(activation)) {
			bean.setInclureTiersAnnules(false);
			bean.setTiersAnnulesSeulement(false);
		}
		else if (TiersActivationListController.ACTIVATION_REACTIVATION_VALUE.equals(activation)) {
			bean.setInclureTiersAnnules(true);
			bean.setTiersAnnulesSeulement(true);
		}
		bean.setTypeTiers(TiersActivationListView.TypeTiers.TIERS);
		return bean;
	}

}
