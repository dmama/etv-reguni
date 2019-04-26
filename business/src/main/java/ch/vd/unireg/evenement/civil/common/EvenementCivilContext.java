package ch.vd.unireg.evenement.civil.common;

import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.audit.AuditManager;
import ch.vd.unireg.data.DataEventService;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalService;
import ch.vd.unireg.indexer.tiers.GlobalTiersIndexer;
import ch.vd.unireg.interfaces.service.ServiceCivilService;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.metier.MetierService;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersService;

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
	public final AuditManager audit;


	public EvenementCivilContext(ServiceCivilService serviceCivil, ServiceInfrastructureService serviceInfra, TiersDAO tiersDAO, AuditManager audit) {
		this.serviceCivil = serviceCivil;
		this.serviceInfra = serviceInfra;
		this.tiersDAO = tiersDAO;
		this.audit = audit;
		this.dataEventService = null;
		this.tiersService = null;
		this.indexer = null;
		this.metierService = null;
		this.adresseService = null;
		this.evenementFiscalService = null;
		this.parametreAppService = null;
	}

	public EvenementCivilContext(ServiceCivilService serviceCivil, ServiceInfrastructureService serviceInfra, DataEventService dataEventService, TiersService tiersService, GlobalTiersIndexer indexer,
	                             MetierService metierService, TiersDAO tiersDAO, AdresseService adresseService, EvenementFiscalService evenementFiscalService, ParametreAppService parametreAppService, AuditManager audit) {
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
		this.audit = audit;
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
