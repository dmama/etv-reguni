package ch.vd.uniregctb.acomptes;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.cache.ServiceCivilCacheWarmer;
import ch.vd.uniregctb.common.ListesThread;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;

public class AcomptesThread extends ListesThread<AcomptesResults> {

    public AcomptesThread(BlockingQueue<List<Long>> queue, RegDate dateTraitement, int nombreThreads, Integer anneeFiscale, ServiceCivilCacheWarmer serviceCivilCacheWarmer,
                          TiersService tiersService, StatusManager status, AtomicInteger compteur, PlatformTransactionManager transactionManager,
                          TiersDAO tiersDAO, HibernateTemplate hibernateTemplate, AssujettissementService assujettissementService) {

        super(queue, status, compteur, serviceCivilCacheWarmer, transactionManager, tiersDAO, hibernateTemplate,
                new AcomptesResults(dateTraitement, nombreThreads, anneeFiscale, tiersService, assujettissementService));
    }

}
