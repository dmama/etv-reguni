package ch.vd.uniregctb.evenement.organisation;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServiceOrganisationService;
import ch.vd.uniregctb.metier.MetierServicePM;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.regimefiscal.RegimeFiscalService;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.rattrapage.appariement.AppariementService;

public class EvenementOrganisationContext {

	private final ServiceOrganisationService serviceOrganisation;
	private final EvenementOrganisationService evenementOrganisationService;
	private final ServiceInfrastructureService serviceInfra;
	private final RegimeFiscalService regimeFiscalService;
	private final DataEventService dataEventService;
	private final TiersService tiersService;
	private final TiersDAO tiersDAO;
	private final GlobalTiersIndexer indexer;
	private final MetierServicePM metierServicePM;
	private final AdresseService adresseService;
	private final EvenementFiscalService evenementFiscalService;
	private final AssujettissementService assujettissementService;
	private final AppariementService appariementService;
	private final ParametreAppService parametreAppService;

	public EvenementOrganisationContext(ServiceOrganisationService serviceOrganisation, ServiceInfrastructureService serviceInfra, RegimeFiscalService regimeFiscalService, TiersDAO tiersDAO) {
		this.serviceOrganisation = serviceOrganisation;
		this.regimeFiscalService = regimeFiscalService;
		this.evenementOrganisationService = null;
		this.serviceInfra = serviceInfra;
		this.tiersDAO = tiersDAO;
		this.dataEventService = null;
		this.tiersService = null;
		this.indexer = null;
		this.metierServicePM = null;
		this.adresseService = null;
		this.evenementFiscalService = null;
		this.assujettissementService = null;
		this.appariementService = null;
		this.parametreAppService = null;
	}

	public EvenementOrganisationContext(ServiceOrganisationService serviceOrganisation, EvenementOrganisationService evenementOrganisationService, ServiceInfrastructureService serviceInfra,
	                                    RegimeFiscalService regimeFiscalService, DataEventService dataEventService, TiersService tiersService, GlobalTiersIndexer indexer,
	                                    MetierServicePM metierServicePM, TiersDAO tiersDAO, AdresseService adresseService, EvenementFiscalService evenementFiscalService,
	                                    AssujettissementService assujettissementService,
	                                    AppariementService appariementService, ParametreAppService parametreAppService) {
		this.serviceOrganisation = serviceOrganisation;
		this.evenementOrganisationService = evenementOrganisationService;
		this.serviceInfra = serviceInfra;
		this.regimeFiscalService = regimeFiscalService;
		this.dataEventService = dataEventService;
		this.tiersService = tiersService;
		this.indexer = indexer;
		this.metierServicePM = metierServicePM;
		this.tiersDAO = tiersDAO;
		this.adresseService = adresseService;
		this.evenementFiscalService = evenementFiscalService;
		this.assujettissementService = assujettissementService;
		this.appariementService = appariementService;
		this.parametreAppService = parametreAppService;
	}

	public final ServiceOrganisationService getServiceOrganisation() {
		return serviceOrganisation;
	}

	public EvenementOrganisationService getEvenementOrganisationService() {
		return evenementOrganisationService;
	}

	public final ServiceInfrastructureService getServiceInfra() {
		return serviceInfra;
	}

	public RegimeFiscalService getRegimeFiscalService() {
		return regimeFiscalService;
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

	public MetierServicePM getMetierServicePM() {
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

	public AssujettissementService getAssujettissementService() {
		return assujettissementService;
	}

	public AppariementService getAppariementService() {
		return appariementService;
	}

	public ParametreAppService getParametreAppService() {
		return parametreAppService;
	}
}
