package ch.vd.uniregctb.evenement.civil.common;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.MetierService;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;

public class EvenementCivilContext {

	private ServiceCivilService serviceCivil;
	private ServiceInfrastructureService serviceInfra;
	private DataEventService dataEventService;
	private boolean refreshCache;
	private TiersService tiersService;
	private TiersDAO tiersDAO;
	private GlobalTiersIndexer indexer;
	private MetierService metierService;
	private AdresseService adresseService;

	public EvenementCivilContext(ServiceCivilService serviceCivil, ServiceInfrastructureService serviceInfra, DataEventService dataEventService, TiersService tiersService, GlobalTiersIndexer indexer,
	                             MetierService metierService, TiersDAO tiersDAO, AdresseService adresseService, boolean refreshCache) {
		this.serviceCivil = serviceCivil;
		this.serviceInfra = serviceInfra;
		this.dataEventService = dataEventService;
		this.tiersService = tiersService;
		this.indexer = indexer;
		this.metierService = metierService;
		this.tiersDAO = tiersDAO;
		this.adresseService = adresseService;
		this.refreshCache = refreshCache;
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

	public final boolean isRefreshCache() {
		return refreshCache;
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
}
