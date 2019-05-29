package ch.vd.unireg.listes;

import java.util.Set;

import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.cache.ServiceCivilCacheWarmer;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.listes.listesnominatives.ListesNominativesProcessor;
import ch.vd.unireg.listes.listesnominatives.ListesNominativesResults;
import ch.vd.unireg.listes.listesnominatives.TypeAdresse;
import ch.vd.unireg.listes.suisseoupermiscresident.ListeContribuablesResidentsSansForVaudoisProcessor;
import ch.vd.unireg.listes.suisseoupermiscresident.ListeContribuablesResidentsSansForVaudoisResults;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.utils.UniregModeHelper;

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
	private UniregModeHelper uniregModeHelper;

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

	public void setUniregModeHelper(UniregModeHelper uniregModeHelper) {
		this.uniregModeHelper = uniregModeHelper;
	}

	@Override
	public ListesNominativesResults produireListesNominatives(int nbThreads, TypeAdresse adressesIncluses, boolean avecContribuablesPP, boolean avecContribuablesPM, RegDate dateTraitement,
	                                                          boolean avecDebiteurs, StatusManager statusManager, Set<Long> tiersList) {
		final ListesNominativesProcessor processor = new ListesNominativesProcessor(hibernateTemplate, tiersService, adresseService, transactionManager, tiersDAO, serviceCivilCacheWarmer, uniregModeHelper);
		return processor.run(nbThreads, adressesIncluses, avecContribuablesPP, avecContribuablesPM, tiersList, dateTraitement, avecDebiteurs, statusManager);
	}

	@Override
	public ListeContribuablesResidentsSansForVaudoisResults produireListeContribuablesSuissesOuPermisCResidentsMaisSansForVd(RegDate dateTraitement, int nbThreads, StatusManager statusManager) {
		final ListeContribuablesResidentsSansForVaudoisProcessor processor = new ListeContribuablesResidentsSansForVaudoisProcessor(hibernateTemplate,
		                                                                                                                            tiersService, adresseService, transactionManager, tiersDAO, infraService, serviceCivilCacheWarmer);
		return processor.run(dateTraitement, nbThreads, statusManager);
	}
}
