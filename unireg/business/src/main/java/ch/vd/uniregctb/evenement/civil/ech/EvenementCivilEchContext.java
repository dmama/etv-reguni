package ch.vd.uniregctb.evenement.civil.ech;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.rcpers.RcPersClientHelper;
import ch.vd.uniregctb.metier.MetierService;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;

public class EvenementCivilEchContext extends EvenementCivilContext {
	
	private final RcPersClientHelper rcPersClientHelper;

	public EvenementCivilEchContext(ServiceCivilService serviceCivil, ServiceInfrastructureService serviceInfra, DataEventService dataEventService, TiersService tiersService,
	                                GlobalTiersIndexer indexer, MetierService metierService, TiersDAO tiersDAO, AdresseService adresseService, EvenementFiscalService evenementFiscalService,
	                                RcPersClientHelper rcPersClientHelper) {
		super(serviceCivil, serviceInfra, dataEventService, tiersService, indexer, metierService, tiersDAO, adresseService, evenementFiscalService);
		this.rcPersClientHelper = rcPersClientHelper;
	}

	public RcPersClientHelper getRcPersClientHelper() {
		return rcPersClientHelper;
	}
}
