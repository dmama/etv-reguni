package ch.vd.unireg.listes.afc;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.shared.batchtemplate.Interruptible;
import ch.vd.unireg.cache.ServiceCivilCacheWarmer;
import ch.vd.unireg.common.ListesThread;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.tiers.TiersDAO;

public class ExtractionDonneesRptThread extends ListesThread<ExtractionDonneesRptResults> {

	public ExtractionDonneesRptThread(BlockingQueue<List<Long>> queue, Interruptible interruptible, AtomicInteger compteur, ServiceCivilCacheWarmer serviceCivilCacheWarmer,
	                                  PlatformTransactionManager transactionManager, TiersDAO tiersDAO, HibernateTemplate hibernateTemplate, ExtractionDonneesRptResults localResults) {
		super(queue, interruptible, compteur, serviceCivilCacheWarmer, transactionManager, tiersDAO, hibernateTemplate, localResults);
	}
}
