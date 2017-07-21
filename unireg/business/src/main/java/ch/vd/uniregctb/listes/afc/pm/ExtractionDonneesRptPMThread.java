package ch.vd.uniregctb.listes.afc.pm;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.cache.ServiceCivilCacheWarmer;
import ch.vd.uniregctb.common.ListesThread;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.TiersDAO;

public class ExtractionDonneesRptPMThread extends ListesThread<ExtractionDonneesRptPMResults> {

	public ExtractionDonneesRptPMThread(BlockingQueue<List<Long>> queue, StatusManager status, AtomicInteger compteur, ServiceCivilCacheWarmer serviceCivilCacheWarmer,
	                                    PlatformTransactionManager transactionManager, TiersDAO tiersDAO, HibernateTemplate hibernateTemplate, ExtractionDonneesRptPMResults localResults) {
		super(queue, status, compteur, serviceCivilCacheWarmer, transactionManager, tiersDAO, hibernateTemplate, localResults);
	}

	@Override
	protected void traiteContribuable(Contribuable ctb) throws Exception {



		super.traiteContribuable(ctb);
	}
}
