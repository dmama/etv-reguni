package ch.vd.uniregctb.role;

import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;

public class RoleServiceImpl implements RoleService {

	private HibernateTemplate hibernateTemplate;

	private ServiceInfrastructureService infraService;

	private TiersDAO tiersDAO;

	private PlatformTransactionManager transactionManager;

	private TiersService tiersService;

	private AdresseService adresseService;

	private ServiceCivilService serviceCivilService;

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public void setServiceCivilService(ServiceCivilService serviceCivilService) {
		this.serviceCivilService = serviceCivilService;
	}

	private ProduireRolesProcessor createProcessor() {
		return new ProduireRolesProcessor(hibernateTemplate, infraService, tiersDAO, transactionManager, adresseService, tiersService, serviceCivilService);
	}

	/**
	 * {@inheritDoc}
	 */
	public ProduireRolesCommunesResults produireRolesPourToutesCommunes(int anneePeriode, int nbThreads, StatusManager status) throws ServiceException {
		final ProduireRolesProcessor processor = createProcessor();
		return processor.runPourToutesCommunes(anneePeriode, nbThreads, status);
	}

	/**
	 * {@inheritDoc}
	 */
	public ProduireRolesCommunesResults produireRolesPourUneCommune(int anneePeriode, int noOfsCommune, int nbThreads, StatusManager status) throws ServiceException {
		final ProduireRolesProcessor processor = createProcessor();
		return processor.runPourUneCommune(anneePeriode, noOfsCommune, nbThreads, status);
	}

	/**
	 * {@inheritDoc}
	 */
	public ProduireRolesOIDsResults produireRolesPourUnOfficeImpot(int anneePeriode, int oid, int nbThreads, StatusManager status) throws ServiceException {
		final ProduireRolesProcessor processor = createProcessor();
		return processor.runPourUnOfficeImpot(anneePeriode, oid, nbThreads, status);
	}

	/**
	 * {@inheritDoc}
	 */
	public ProduireRolesOIDsResults[] produireRolesPourTousOfficesImpot(int anneePeriode, int nbThreads, StatusManager status) throws ServiceException {
		final ProduireRolesProcessor processor = createProcessor();
		return processor.runPourTousOfficesImpot(anneePeriode, nbThreads, status);
	}
}
