package ch.vd.uniregctb.acomptes;

import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;

public class AcomptesServiceImpl implements AcomptesService {

	private TiersService tiersService;

	private AdresseService adresseService;

	private HibernateTemplate hibernateTemplate;

	private PlatformTransactionManager transactionManager;

	private TiersDAO tiersDAO;

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

	public AcomptesResults produireAcomptes(RegDate dateTraitement, int nbThreads, Integer annee, StatusManager statusManager) {
		final AcomptesProcessor processor = new AcomptesProcessor(hibernateTemplate, tiersService, adresseService, transactionManager, tiersDAO);
		return processor.run(dateTraitement, nbThreads, annee, statusManager);
	}

}
