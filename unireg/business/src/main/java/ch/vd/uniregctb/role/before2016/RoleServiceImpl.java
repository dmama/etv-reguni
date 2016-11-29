package ch.vd.uniregctb.role.before2016;

import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.validation.ValidationService;

public class RoleServiceImpl implements RoleService {

	private HibernateTemplate hibernateTemplate;
	private ServiceInfrastructureService infraService;
	private TiersDAO tiersDAO;
	private PlatformTransactionManager transactionManager;
	private TiersService tiersService;
	private AdresseService adresseService;
	private ServiceCivilService serviceCivilService;
	private ValidationService validationService;
	private AssujettissementService assujettissementService;

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

	public void setValidationService(ValidationService validationService) {
		this.validationService = validationService;
	}

	public void setAssujettissementService(AssujettissementService assujettissementService) {
		this.assujettissementService = assujettissementService;
	}

	private ProduireRolesProcessor createProcessor() {
		return new ProduireRolesProcessor(hibernateTemplate, infraService, tiersDAO, transactionManager, adresseService, tiersService, serviceCivilService, validationService, assujettissementService);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ProduireRolesPPCommunesResults produireRolesPPPourToutesCommunes(int anneePeriode, int nbThreads, StatusManager status) throws ServiceException {
		final ProduireRolesProcessor processor = createProcessor();
		return processor.runPPPourToutesCommunes(anneePeriode, nbThreads, status);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ProduireRolesPPCommunesResults produireRolesPPPourUneCommune(int anneePeriode, int noOfsCommune, int nbThreads, StatusManager status) throws ServiceException {
		final ProduireRolesProcessor processor = createProcessor();
		return processor.runPPPourUneCommune(anneePeriode, noOfsCommune, nbThreads, status);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ProduireRolesOIDsResults produireRolesPourUnOfficeImpot(int anneePeriode, int oid, int nbThreads, StatusManager status) throws ServiceException {
		final ProduireRolesProcessor processor = createProcessor();
		return processor.runPourUnOfficeImpot(anneePeriode, oid, nbThreads, status);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ProduireRolesOIDsResults[] produireRolesPourTousOfficesImpot(int anneePeriode, int nbThreads, StatusManager status) throws ServiceException {
		final ProduireRolesProcessor processor = createProcessor();
		return processor.runPourTousOfficesImpot(anneePeriode, nbThreads, status);
	}

	@Override
	public ProduireRolesPMCommunesResults produireRolesPMPourToutesCommunes(int anneePeriode, int nbThreads, StatusManager status) throws ServiceException {
		final ProduireRolesProcessor processor = createProcessor();
		return processor.runPMPourToutesCommunes(anneePeriode, nbThreads, status);
	}

	@Override
	public ProduireRolesPMCommunesResults produireRolesPMPourUneCommune(int anneePeriode, int noOfsCommune, int nbThreads, StatusManager status) throws ServiceException {
		final ProduireRolesProcessor processor = createProcessor();
		return processor.runPMPourUneCommune(anneePeriode, noOfsCommune, nbThreads, status);
	}

	@Override
	public ProduireRolesOIPMResults produireRolesPourOfficePersonnesMorales(int anneePeriode, int nbThreads, StatusManager status) throws ServiceException {
		final ProduireRolesProcessor processor = createProcessor();
		return processor.runPourOfficePersonnesMorales(anneePeriode, nbThreads, status);
	}
}
