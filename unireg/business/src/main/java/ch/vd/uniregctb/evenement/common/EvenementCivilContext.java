package ch.vd.uniregctb.evenement.common;

import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

public class EvenementCivilContext {

	private ServiceCivilService serviceCivil;
	private ServiceInfrastructureService serviceInfra;
	private DataEventService dataEventService;
	private boolean refreshCache;

	public EvenementCivilContext(ServiceCivilService serviceCivil, ServiceInfrastructureService serviceInfra, DataEventService dataEventService, boolean refreshCache) {
		this.serviceCivil = serviceCivil;
		this.serviceInfra = serviceInfra;
		this.dataEventService = dataEventService;
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
}
