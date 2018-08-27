package ch.vd.unireg.listes.ear;

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

public class ListeEchangeRenseignementsThreads extends ListesThread<ListeEchangeRenseignementsResults> {

    public ListeEchangeRenseignementsThreads(BlockingQueue<List<Long>> queue, Interruptible interruptible, AtomicInteger compteur, RegDate dateTraitement, int nbThreads, int anneeFiscale, final boolean avecContribuablesPP, final boolean avecContribuablesPM,ServiceCivilCacheWarmer serviceCivilCacheWarmer, TiersService tiersService,
                                             PlatformTransactionManager transactionManager, TiersDAO tiersDAO, HibernateTemplate hibernateTemplate, AssujettissementService assujettissementService,
                                             AdresseService adresseService) {

		super(queue, interruptible, compteur, serviceCivilCacheWarmer, transactionManager, tiersDAO, hibernateTemplate,
		      new ListeEchangeRenseignementsResults(dateTraitement, nbThreads, anneeFiscale,avecContribuablesPP,avecContribuablesPM, tiersService, assujettissementService, adresseService));
	}
}
