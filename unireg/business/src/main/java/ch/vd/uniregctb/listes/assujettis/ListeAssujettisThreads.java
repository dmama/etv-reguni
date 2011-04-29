package ch.vd.uniregctb.listes.assujettis;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.cache.ServiceCivilCacheWarmer;
import ch.vd.uniregctb.common.ListesThread;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;

public class ListeAssujettisThreads extends ListesThread<ListeAssujettisResults> {

    public ListeAssujettisThreads(BlockingQueue<List<Long>> queue, StatusManager status, AtomicInteger compteur, RegDate dateTraitement, int nbThreads, int anneeFiscale,
                                  boolean avecSourciersPurs, boolean seulementAssujettisFinAnnee, ServiceCivilCacheWarmer serviceCivilCacheWarmer, TiersService tiersService,
	                              PlatformTransactionManager transactionManager, TiersDAO tiersDAO, HibernateTemplate hibernateTemplate) {

		super(queue, status, compteur, serviceCivilCacheWarmer, transactionManager, tiersDAO, hibernateTemplate,
		      new ListeAssujettisResults(dateTraitement, nbThreads, anneeFiscale, avecSourciersPurs, seulementAssujettisFinAnnee, tiersService));
	}
}
