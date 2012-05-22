package ch.vd.uniregctb.evenement.civil.ech;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.MetierService;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;

// TODO (msi) supprimer ce context spécialisé pour rien
public class EvenementCivilEchContext extends EvenementCivilContext {
	
	public EvenementCivilEchContext(ServiceCivilService serviceCivil, ServiceInfrastructureService serviceInfra, DataEventService dataEventService, TiersService tiersService,
	                                GlobalTiersIndexer indexer, MetierService metierService, TiersDAO tiersDAO, AdresseService adresseService, EvenementFiscalService evenementFiscalService) {
		super(serviceCivil, serviceInfra, dataEventService, tiersService, indexer, metierService, tiersDAO, adresseService, evenementFiscalService);
	}
}
