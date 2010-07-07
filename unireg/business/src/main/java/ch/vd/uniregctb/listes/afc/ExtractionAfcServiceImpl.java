package ch.vd.uniregctb.listes.afc;

import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * Service utilisé par l'extraction des listes AFC
 */
public class ExtractionAfcServiceImpl implements ExtractionAfcService {

	private HibernateTemplate hibernateTemplate;

	private PlatformTransactionManager transactionManager;

	private TiersService tiersService;

	private ServiceCivilService serviceCivilService;

	private TiersDAO tiersDAO;

	private ServiceInfrastructureService infraService;

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setServiceCivilService(ServiceCivilService serviceCivilService) {
		this.serviceCivilService = serviceCivilService;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	/**
	 * Extrait la liste AFC de la période fiscale donnée
	 * @param dateTraitement date d'exécution de l'extraction
	 * @param pf période fiscale de référence
	 * @param mode type d'extraction à effectuer
	 * @param nbThreads degré de parallélisation du traitement
	 * @return extraction
	 */
	public ExtractionAfcResults produireExtraction(RegDate dateTraitement, int pf, TypeExtractionAfc mode, int nbThreads, StatusManager statusManager) {
		final ExtractionAfcProcessor proc = new ExtractionAfcProcessor(hibernateTemplate, transactionManager, tiersService, serviceCivilService, tiersDAO, infraService);
		return proc.run(dateTraitement, pf, mode, nbThreads, statusManager);
	}
}
