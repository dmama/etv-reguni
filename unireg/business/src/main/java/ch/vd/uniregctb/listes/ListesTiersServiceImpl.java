package ch.vd.uniregctb.listes;

import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.cache.ServiceCivilCacheWarmer;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.listes.listesnominatives.ListesNominativesProcessor;
import ch.vd.uniregctb.listes.listesnominatives.ListesNominativesResults;
import ch.vd.uniregctb.listes.listesnominatives.TypeAdresse;
import ch.vd.uniregctb.listes.suisseoupermiscresident.ListeContribuablesResidentsSansForVaudoisProcessor;
import ch.vd.uniregctb.listes.suisseoupermiscresident.ListeContribuablesResidentsSansForVaudoisResults;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * Implémentation du service de génération des listes nominatives
 */
public class ListesTiersServiceImpl implements ListesTiersService {

	private TiersService tiersService;

	private AdresseService adresseService;

	private HibernateTemplate hibernateTemplate;

	private PlatformTransactionManager transactionManager;

	private TiersDAO tiersDAO;

	private ServiceInfrastructureService infraService;

	private ServiceCivilCacheWarmer serviceCivilCacheWarmer;

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	public void setServiceCivilCacheWarmer(ServiceCivilCacheWarmer serviceCivilCacheWarmer) {
		this.serviceCivilCacheWarmer = serviceCivilCacheWarmer;
	}

	@Override
	public ListesNominativesResults produireListesNominatives(RegDate dateTraitement, int nbThreads, TypeAdresse adressesIncluses, boolean avecContribuablesPP, boolean avecContribuablesPM,
	                                                          boolean avecDebiteurs, StatusManager statusManager) {
		final ListesNominativesProcessor processor = new ListesNominativesProcessor(hibernateTemplate, tiersService, adresseService, transactionManager, tiersDAO, serviceCivilCacheWarmer);
		return processor.run(dateTraitement, nbThreads, adressesIncluses, avecContribuablesPP, avecContribuablesPM, avecDebiteurs, statusManager);
	}

	@Override
	public ListeContribuablesResidentsSansForVaudoisResults produireListeContribuablesSuissesOuPermisCResidentsMaisSansForVd(RegDate dateTraitement, int nbThreads, StatusManager statusManager) {
		final ListeContribuablesResidentsSansForVaudoisProcessor processor = new ListeContribuablesResidentsSansForVaudoisProcessor(hibernateTemplate,
				tiersService, adresseService, transactionManager, tiersDAO, infraService, serviceCivilCacheWarmer);
		return processor.run(dateTraitement, nbThreads, statusManager);
	}
}
