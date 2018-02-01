package ch.vd.unireg.acomptes;

import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.cache.ServiceCivilCacheWarmer;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersService;

public class AcomptesServiceImpl implements AcomptesService {

	private TiersService tiersService;
	private ServiceCivilCacheWarmer serviceCivilCacheWarmer;
	private HibernateTemplate hibernateTemplate;
	private PlatformTransactionManager transactionManager;
	private TiersDAO tiersDAO;
	private AssujettissementService assujettissementService;
	private AdresseService adresseService;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceCivilCacheWarmer(ServiceCivilCacheWarmer serviceCivilCacheWarmer) {
		this.serviceCivilCacheWarmer = serviceCivilCacheWarmer;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setAssujettissementService(AssujettissementService assujettissementService) {
		this.assujettissementService = assujettissementService;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	@Override
	public AcomptesResults produireAcomptes(RegDate dateTraitement, int nbThreads, Integer annee, StatusManager statusManager) {
		final AcomptesProcessor processor = new AcomptesProcessor(hibernateTemplate, tiersService, serviceCivilCacheWarmer, transactionManager, tiersDAO, assujettissementService, adresseService);
		return processor.run(dateTraitement, nbThreads, annee, statusManager);
	}
}
