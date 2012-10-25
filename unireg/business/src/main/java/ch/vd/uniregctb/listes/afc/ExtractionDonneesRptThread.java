package ch.vd.uniregctb.listes.afc;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.uniregctb.cache.ServiceCivilCacheWarmer;
import ch.vd.uniregctb.common.ListesThread;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.tiers.TiersDAO;

public class ExtractionDonneesRptThread extends ListesThread<ExtractionDonneesRptResults> {

	public ExtractionDonneesRptThread(BlockingQueue<List<Long>> queue, StatusManager status, AtomicInteger compteur, ServiceCivilCacheWarmer serviceCivilCacheWarmer,
	                                  PlatformTransactionManager transactionManager, TiersDAO tiersDAO, HibernateTemplate hibernateTemplate, ExtractionDonneesRptResults localResults) {
		super(queue, status, compteur, serviceCivilCacheWarmer, transactionManager, tiersDAO, hibernateTemplate, localResults);
	}
}
