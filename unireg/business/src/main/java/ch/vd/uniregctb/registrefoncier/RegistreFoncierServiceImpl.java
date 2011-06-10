package ch.vd.uniregctb.registrefoncier;

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.situationfamille.SituationFamilleService;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;

public class RegistreFoncierServiceImpl implements RegistreFoncierService {

	private static final Logger LOGGER = Logger.getLogger(RegistreFoncierServiceImpl.class);

	/**
	 * DAO permettant d'effectuer une recherche simple en base.
	 */
	private TiersDAO tiersDAO;


	private ServiceCivilService serviceCivilService;

	private SituationFamilleService situationFamilleService;

	private AdresseService adresseService;

	private TiersService tiersService;



	@Override
	public RapprocherCtbResults rapprocherCtbRegistreFoncier(List<ProprietaireFoncier> listeProprietaireFoncier,StatusManager s, RegDate dateTraitement) {
		RapprocherCtbProcessor processor = new RapprocherCtbProcessor(hibernateTemplate, transactionManager,tiersDAO,adresseService, tiersService);
		return processor.run(listeProprietaireFoncier, s, dateTraitement);


	}

	@Override
	public TiersDAO getTiersDAO() {
		return tiersDAO;
	}

	@Override
	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}


	@Override
	public ServiceCivilService getServiceCivilService() {
		return serviceCivilService;
	}

	@Override
	public void setServiceCivilService(ServiceCivilService serviceCivilService) {
		this.serviceCivilService = serviceCivilService;
	}

	public SituationFamilleService getSituationFamilleService() {
		return situationFamilleService;
	}

	public void setSituationFamilleService(SituationFamilleService situationFamilleService) {
		this.situationFamilleService = situationFamilleService;
	}

	public HibernateTemplate getHibernateTemplate() {
		return hibernateTemplate;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public PlatformTransactionManager getTransactionManager() {
		return transactionManager;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	private HibernateTemplate hibernateTemplate;

	private PlatformTransactionManager transactionManager;

	@Override
	public AdresseService getAdresseService() {

		return adresseService;
	}

	@Override
	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;

	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public TiersService getTiersService() {
		return tiersService;
	}

}
