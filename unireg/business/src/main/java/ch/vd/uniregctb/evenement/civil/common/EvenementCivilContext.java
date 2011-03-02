package ch.vd.uniregctb.evenement.civil.common;

import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.TiersService;

public class EvenementCivilContext {

	private ServiceCivilService serviceCivil;
	private ServiceInfrastructureService serviceInfra;
	private DataEventService dataEventService;
	private boolean refreshCache;
	private TiersService tiersService;

	public EvenementCivilContext(ServiceCivilService serviceCivil, ServiceInfrastructureService serviceInfra, DataEventService dataEventService, TiersService tiersService, boolean refreshCache) {
		this.serviceCivil = serviceCivil;
		this.serviceInfra = serviceInfra;
		this.dataEventService = dataEventService;
		this.tiersService = tiersService;
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

	public final boolean isRefreshCache() {
		return refreshCache;
	}

	public TiersService getTiersService() {
		return tiersService;
	}
}
