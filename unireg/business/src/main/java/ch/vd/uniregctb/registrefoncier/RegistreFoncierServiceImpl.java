package ch.vd.uniregctb.registrefoncier;

import java.util.List;

import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;

public class RegistreFoncierServiceImpl implements RegistreFoncierService {

	/**
	 * DAO permettant d'effectuer une recherche simple en base.
	 */
	private TiersDAO tiersDAO;

	private ServiceCivilService serviceCivilService;

	private AdresseService adresseService;

	private TiersService tiersService;

	@Override
	public RapprocherCtbResults rapprocherCtbRegistreFoncier(List<ProprietaireFoncier> listeProprietaireFoncier, StatusManager s, RegDate dateTraitement, int nbThreads) {
		final RapprocherCtbProcessor processor = new RapprocherCtbProcessor(hibernateTemplate, transactionManager, tiersDAO, adresseService, tiersService, serviceCivilService);
		return processor.run(listeProprietaireFoncier, s, dateTraitement, nbThreads);
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setServiceCivilService(ServiceCivilService serviceCivilService) {
		this.serviceCivilService = serviceCivilService;
	}

	private HibernateTemplate hibernateTemplate;

	private PlatformTransactionManager transactionManager;

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}
}
