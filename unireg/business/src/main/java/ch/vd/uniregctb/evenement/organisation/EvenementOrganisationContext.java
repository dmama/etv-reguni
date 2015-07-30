package ch.vd.uniregctb.evenement.organisation;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServiceOrganisationService;
import ch.vd.uniregctb.metier.MetierServicePM;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;

public class EvenementOrganisationContext {

	private final ServiceOrganisationService serviceOrganisation;
	private final ServiceInfrastructureService serviceInfra;
	private final DataEventService dataEventService;
	private final TiersService tiersService;
	private final TiersDAO tiersDAO;
	private final GlobalTiersIndexer indexer;
	private final MetierServicePM metierServicePM;
	private final AdresseService adresseService;
	private final EvenementFiscalService evenementFiscalService;
	private final ParametreAppService parametreAppService;

	public EvenementOrganisationContext(ServiceOrganisationService serviceOrganisation, ServiceInfrastructureService serviceInfra, TiersDAO tiersDAO) {
		this.serviceOrganisation = serviceOrganisation;
		this.serviceInfra = serviceInfra;
		this.tiersDAO = tiersDAO;
		this.dataEventService = null;
		this.tiersService = null;
		this.indexer = null;
		this.metierServicePM = null;
		this.adresseService = null;
		this.evenementFiscalService = null;
		this.parametreAppService = null;
	}

	public EvenementOrganisationContext(ServiceOrganisationService serviceOrganisation, ServiceInfrastructureService serviceInfra, DataEventService dataEventService, TiersService tiersService, GlobalTiersIndexer indexer,
	                             MetierServicePM metierServicePM, TiersDAO tiersDAO, AdresseService adresseService, EvenementFiscalService evenementFiscalService, ParametreAppService parametreAppService) {
		this.serviceOrganisation = serviceOrganisation;
		this.serviceInfra = serviceInfra;
		this.dataEventService = dataEventService;
		this.tiersService = tiersService;
		this.indexer = indexer;
		this.metierServicePM = metierServicePM;
		this.tiersDAO = tiersDAO;
		this.adresseService = adresseService;
		this.evenementFiscalService = evenementFiscalService;
		this.parametreAppService = parametreAppService;
	}

	public final ServiceOrganisationService getServiceOrganisation() {
		return serviceOrganisation;
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

	public MetierServicePM getMetierServicePMPM() {
		return metierServicePM;
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
