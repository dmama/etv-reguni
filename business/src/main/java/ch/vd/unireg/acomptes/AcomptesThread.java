package ch.vd.unireg.acomptes;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.Interruptible;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.cache.ServiceCivilCacheWarmer;
import ch.vd.unireg.common.ListesThread;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersService;

public class AcomptesThread extends ListesThread<AcomptesResults> {

    public AcomptesThread(BlockingQueue<List<Long>> queue, RegDate dateTraitement, int nombreThreads, Integer anneeFiscale, ServiceCivilCacheWarmer serviceCivilCacheWarmer,
                          TiersService tiersService, Interruptible interruptible, AtomicInteger compteur, PlatformTransactionManager transactionManager,
                          TiersDAO tiersDAO, HibernateTemplate hibernateTemplate, AssujettissementService assujettissementService, AdresseService adresseService) {

        super(queue, interruptible, compteur, serviceCivilCacheWarmer, transactionManager, tiersDAO, hibernateTemplate,
                new AcomptesResults(dateTraitement, nombreThreads, anneeFiscale, tiersService, assujettissementService, adresseService));
    }

}
