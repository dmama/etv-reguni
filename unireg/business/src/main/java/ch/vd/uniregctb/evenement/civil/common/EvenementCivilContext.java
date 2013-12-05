package ch.vd.uniregctb.evenement.civil.common;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.MetierService;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;

public class EvenementCivilContext {

	private final ServiceCivilService serviceCivil;
	private final ServiceInfrastructureService serviceInfra;
	private final DataEventService dataEventService;
	private final TiersService tiersService;
	private final TiersDAO tiersDAO;
	private final GlobalTiersIndexer indexer;
	private final MetierService metierService;
	private final AdresseService adresseService;
	private final EvenementFiscalService evenementFiscalService;
	private final ParametreAppService parametreAppService;

	public EvenementCivilContext(ServiceCivilService serviceCivil, ServiceInfrastructureService serviceInfra, TiersDAO tiersDAO) {
		this.serviceCivil = serviceCivil;
		this.serviceInfra = serviceInfra;
		this.tiersDAO = tiersDAO;
		this.dataEventService = null;
		this.tiersService = null;
		this.indexer = null;
		this.metierService = null;
		this.adresseService = null;
		this.evenementFiscalService = null;
		this.parametreAppService = null;
	}

	public EvenementCivilContext(ServiceCivilService serviceCivil, ServiceInfrastructureService serviceInfra, DataEventService dataEventService, TiersService tiersService, GlobalTiersIndexer indexer,
	                             MetierService metierService, TiersDAO tiersDAO, AdresseService adresseService, EvenementFiscalService evenementFiscalService, ParametreAppService parametreAppService) {
		this.serviceCivil = serviceCivil;
		this.serviceInfra = serviceInfra;
		this.dataEventService = dataEventService;
		this.tiersService = tiersService;
		this.indexer = indexer;
		this.metierService = metierService;
		this.tiersDAO = tiersDAO;
		this.adresseService = adresseService;
		this.evenementFiscalService = evenementFiscalService;
		this.parametreAppService = parametreAppService;
	}

	public final ServiceCivilService getServiceCivil() {
		return serviceCivil;
	}

	public final ServiceInfrastructureService getServiceInfra() {
		return serviceInfra;
	}

	public final DataEventService getDataEventService() {
		return dataEventService;
	}

	public GlobalTiersIndexer getIndexer() {
		return indexer;
	}

	public TiersService getTiersService() {
		return tiersService;
	}

	public MetierService getMetierService() {
		return metierService;
	}

	public TiersDAO getTiersDAO() {
		return tiersDAO;
	}

	public AdresseService getAdresseService() {
		return adresseService;
	}

	public EvenementFiscalService getEvenementFiscalService() {
		return evenementFiscalService;
	}

	public ParametreAppService getParametreAppService() {
		return parametreAppService;
	}
}
