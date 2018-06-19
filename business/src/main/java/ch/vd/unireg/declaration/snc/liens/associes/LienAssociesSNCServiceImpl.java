package ch.vd.unireg.declaration.snc.liens.associes;

import java.util.List;

import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.tiers.TiersService;

public class LienAssociesSNCServiceImpl implements LienAssociesSNCService {

	private PlatformTransactionManager transactionManager;
	private HibernateTemplate hibernateTemplate;
	private TiersService tiersService;


	@Override
	public LienAssociesSNCEnMasseImporterResults importLienAssociesSNCEnMasse(List<DonneesLienAssocieEtSNC> rapportEntreTiersSnc, final RegDate dateTraitement, StatusManager statusManager) {
		final ImportLienAssociesSNCEnMasseProcessor processor = new ImportLienAssociesSNCEnMasseProcessor(transactionManager, hibernateTemplate, tiersService);
		return processor.run(rapportEntreTiersSnc, dateTraitement, statusManager);
	}

	public PlatformTransactionManager getTransactionManager() {
		return transactionManager;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public HibernateTemplate getHibernateTemplate() {
		return hibernateTemplate;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public TiersService getTiersService() {
		return tiersService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}
}
