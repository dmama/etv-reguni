package ch.vd.unireg.listes.afc;

import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.cache.ServiceCivilCacheWarmer;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.listes.afc.pm.ExtractionDonneesRptPMProcessor;
import ch.vd.unireg.listes.afc.pm.ExtractionDonneesRptPMResults;
import ch.vd.unireg.listes.afc.pm.ModeExtraction;
import ch.vd.unireg.listes.afc.pm.VersionWS;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.metier.assujettissement.PeriodeImpositionService;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersService;

/**
 * Service utilisé par l'extraction des listes des données de référence RPT
 */
public class ExtractionDonneesRptServiceImpl implements ExtractionDonneesRptService {

	private HibernateTemplate hibernateTemplate;
	private PlatformTransactionManager transactionManager;
	private TiersService tiersService;
	private ServiceCivilCacheWarmer serviceCivilCacheWarmer;
	private TiersDAO tiersDAO;
	private ServiceInfrastructureService infraService;
	private AssujettissementService assujettissementService;
	private PeriodeImpositionService periodeImpositionService;
	private AdresseService adresseService;

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setServiceCivilCacheWarmer(ServiceCivilCacheWarmer serviceCivilCacheWarmer) {
		this.serviceCivilCacheWarmer = serviceCivilCacheWarmer;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	public void setAssujettissementService(AssujettissementService assujettissementService) {
		this.assujettissementService = assujettissementService;
	}

	public void setPeriodeImpositionService(PeriodeImpositionService periodeImpositionService) {
		this.periodeImpositionService = periodeImpositionService;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	/**
	 * Extrait la liste des données de référence RPT de la période fiscale donnée
	 * @param dateTraitement date d'exécution de l'extraction
	 * @param pf période fiscale de référence
	 * @param mode type d'extraction à effectuer
	 * @param nbThreads degré de parallélisation du traitement
	 * @return extraction
	 */
	@Override
	public ExtractionDonneesRptResults produireExtraction(RegDate dateTraitement, int pf, TypeExtractionDonneesRpt mode, int nbThreads, StatusManager statusManager) {
		final ExtractionDonneesRptProcessor proc = new ExtractionDonneesRptProcessor(hibernateTemplate, transactionManager, tiersService, serviceCivilCacheWarmer, tiersDAO, infraService,
		                                                                             assujettissementService, periodeImpositionService, adresseService);
		return proc.run(dateTraitement, pf, mode, nbThreads, statusManager);
	}

	@Override
	public ExtractionDonneesRptPMResults produireExtractionIBC(RegDate dateTraitement, int pf, VersionWS versionWS, ModeExtraction mode, int nbThreads, StatusManager statusManager) {
		final ExtractionDonneesRptPMProcessor proc = new ExtractionDonneesRptPMProcessor(hibernateTemplate, transactionManager, tiersService, serviceCivilCacheWarmer, tiersDAO, infraService,
		                                                                                 periodeImpositionService, adresseService);
		return proc.run(dateTraitement, pf, mode, versionWS, nbThreads, statusManager);
	}
}
