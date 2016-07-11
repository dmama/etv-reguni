package ch.vd.uniregctb.migration.pm.engine;

import ch.vd.unireg.wsclient.rcpers.RcPersClient;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionService;
import ch.vd.uniregctb.metier.bouclement.BouclementService;
import ch.vd.uniregctb.migration.pm.communes.FractionsCommuneProvider;
import ch.vd.uniregctb.migration.pm.communes.FusionCommunesProvider;
import ch.vd.uniregctb.migration.pm.engine.helpers.AdresseHelper;
import ch.vd.uniregctb.migration.pm.engine.helpers.DateHelper;
import ch.vd.uniregctb.migration.pm.engine.helpers.DoublonProvider;
import ch.vd.uniregctb.migration.pm.engine.helpers.OrganisationServiceAccessor;
import ch.vd.uniregctb.migration.pm.engine.helpers.RegimeFiscalHelper;
import ch.vd.uniregctb.migration.pm.indexeur.NonHabitantIndex;
import ch.vd.uniregctb.migration.pm.store.UniregStore;
import ch.vd.uniregctb.migration.pm.utils.DatesParticulieres;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.dao.RemarqueDAO;

public class MigrationContexte {

	private final UniregStore uniregStore;
	private final ActivityManager activityManager;
	private final AppariementsMultiplesManager appariementsMultiplesManager;
	private final ServiceInfrastructureService infraService;
	private final FusionCommunesProvider fusionCommunesProvider;
	private final FractionsCommuneProvider fractionsCommuneProvider;
	private final DateHelper dateHelper;
	private final DatesParticulieres datesParticulieres;
	private final AdresseHelper adresseHelper;
	private final BouclementService bouclementService;
	private final AssujettissementService assujettissementService;
	private final PeriodeImpositionService periodeImpositionService;
	private final ParametreAppService parametreAppService;
	private final OrganisationServiceAccessor organisationService;
	private final DoublonProvider doublonProvider;
	private final RegimeFiscalHelper regimeFiscalHelper;
	private final TiersService tiersService;
	private final TiersDAO tiersDAO;
	private final RemarqueDAO remarqueDAO;
	private final RcPersClient rcpersClient;
	private final NonHabitantIndex nonHabitantIndex;

	public MigrationContexte(UniregStore uniregStore, ActivityManager activityManager, AppariementsMultiplesManager appariementsMultiplesManager, ServiceInfrastructureService infraService, FusionCommunesProvider fusionCommunesProvider,
	                         FractionsCommuneProvider fractionsCommuneProvider, DateHelper dateHelper, DatesParticulieres datesParticulieres, AdresseHelper adresseHelper, BouclementService bouclementService,
	                         AssujettissementService assujettissementService, PeriodeImpositionService periodeImpositionService, ParametreAppService parametreAppService, OrganisationServiceAccessor organisationService,
	                         DoublonProvider doublonProvider, RegimeFiscalHelper regimeFiscalHelper, TiersService tiersService, TiersDAO tiersDAO, RemarqueDAO remarqueDAO, RcPersClient rcpersClient, NonHabitantIndex nonHabitantIndex) {
		this.uniregStore = uniregStore;
		this.activityManager = activityManager;
		this.appariementsMultiplesManager = appariementsMultiplesManager;
		this.infraService = infraService;
		this.fusionCommunesProvider = fusionCommunesProvider;
		this.fractionsCommuneProvider = fractionsCommuneProvider;
		this.dateHelper = dateHelper;
		this.datesParticulieres = datesParticulieres;
		this.adresseHelper = adresseHelper;
		this.bouclementService = bouclementService;
		this.assujettissementService = assujettissementService;
		this.periodeImpositionService = periodeImpositionService;
		this.parametreAppService = parametreAppService;
		this.organisationService = organisationService;
		this.doublonProvider = doublonProvider;
		this.regimeFiscalHelper = regimeFiscalHelper;
		this.tiersService = tiersService;
		this.tiersDAO = tiersDAO;
		this.remarqueDAO = remarqueDAO;
		this.rcpersClient = rcpersClient;
		this.nonHabitantIndex = nonHabitantIndex;
	}

	public UniregStore getUniregStore() {
		return uniregStore;
	}

	public ActivityManager getActivityManager() {
		return activityManager;
	}

	public AppariementsMultiplesManager getAppariementsMultiplesManager() {
		return appariementsMultiplesManager;
	}

	public ServiceInfrastructureService getInfraService() {
		return infraService;
	}

	public FusionCommunesProvider getFusionCommunesProvider() {
		return fusionCommunesProvider;
	}

	public FractionsCommuneProvider getFractionsCommuneProvider() {
		return fractionsCommuneProvider;
	}

	public DateHelper getDateHelper() {
		return dateHelper;
	}

	public DatesParticulieres getDatesParticulieres() {
		return datesParticulieres;
	}

	public AdresseHelper getAdresseHelper() {
		return adresseHelper;
	}

	public BouclementService getBouclementService() {
		return bouclementService;
	}

	public AssujettissementService getAssujettissementService() {
		return assujettissementService;
	}

	public PeriodeImpositionService getPeriodeImpositionService() {
		return periodeImpositionService;
	}

	public ParametreAppService getParametreAppService() {
		return parametreAppService;
	}

	public OrganisationServiceAccessor getOrganisationService() {
		return organisationService;
	}

	public DoublonProvider getDoublonProvider() {
		return doublonProvider;
	}

	public RegimeFiscalHelper getRegimeFiscalHelper() {
		return regimeFiscalHelper;
	}

	public TiersService getTiersService() {
		return tiersService;
	}

	public TiersDAO getTiersDAO() {
		return tiersDAO;
	}

	public RemarqueDAO getRemarqueDAO() {
		return remarqueDAO;
	}

	public RcPersClient getRcpersClient() {
		return rcpersClient;
	}

	public NonHabitantIndex getNonHabitantIndex() {
		return nonHabitantIndex;
	}
}
