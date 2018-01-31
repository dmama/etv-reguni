package ch.vd.uniregctb.listes.afc.pm;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.shared.batchtemplate.Interruptible;
import ch.vd.uniregctb.cache.ServiceCivilCacheWarmer;
import ch.vd.uniregctb.common.ListesThread;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.tiers.TiersDAO;

public class ExtractionDonneesRptPMThread extends ListesThread<ExtractionDonneesRptPMResults> {

	public ExtractionDonneesRptPMThread(BlockingQueue<List<Long>> queue, Interruptible interruptible, AtomicInteger compteur, ServiceCivilCacheWarmer serviceCivilCacheWarmer,
	                                    PlatformTransactionManager transactionManager, TiersDAO tiersDAO, HibernateTemplate hibernateTemplate, ExtractionDonneesRptPMResults localResults) {
		super(queue, interruptible, compteur, serviceCivilCacheWarmer, transactionManager, tiersDAO, hibernateTemplate, localResults);
	}

}
