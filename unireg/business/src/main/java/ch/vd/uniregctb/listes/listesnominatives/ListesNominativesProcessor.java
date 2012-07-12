package ch.vd.uniregctb.listes.listesnominatives;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.cache.ServiceCivilCacheWarmer;
import ch.vd.uniregctb.common.ListesProcessor;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * Processor pour la génération des listes nominatives
 */
public class ListesNominativesProcessor extends ListesProcessor<ListesNominativesResults, ListesNominativesThread> {

    private final Logger LOGGER = Logger.getLogger(ListesNominativesProcessor.class);

    private final HibernateTemplate hibernateTemplate;

    private final TiersService tiersService;

    private final PlatformTransactionManager transactionManager;

    private final TiersDAO tiersDAO;

    private final AdresseService adresseService;

	private final ServiceCivilCacheWarmer serviceCivilCacheWarmer;

    public ListesNominativesProcessor(HibernateTemplate hibernateTemplate,
                                      TiersService tiersService,
                                      AdresseService adresseService,
                                      PlatformTransactionManager transactionManager,
                                      TiersDAO tiersDAO,
                                      ServiceCivilCacheWarmer serviceCivilCacheWarmer) {
        this.hibernateTemplate = hibernateTemplate;
        this.tiersService = tiersService;
        this.adresseService = adresseService;
        this.transactionManager = transactionManager;
        this.tiersDAO = tiersDAO;
	    this.serviceCivilCacheWarmer = serviceCivilCacheWarmer;
    }

    /**
     * Appel principal de génération des listes nominatives
     */
    public ListesNominativesResults run(RegDate dateTraitement, final int nbThreads, final TypeAdresse adressesIncluses, final boolean avecContribuablesPP, final boolean avecContribuablesPM,
                                        final boolean avecDebiteurs, StatusManager s) {

        final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

        // lance le vrai boulot !
        return doRun(dateTraitement, nbThreads, status, hibernateTemplate, new Customizer<ListesNominativesResults, ListesNominativesThread>() {

            @Override
            public ListesNominativesResults createResults(RegDate dateTraitement) {
                return new ListesNominativesResults(dateTraitement, nbThreads, adressesIncluses, avecContribuablesPP, avecContribuablesPM, avecDebiteurs, tiersService, adresseService);
            }

            @Override
            public ListesNominativesThread createThread(LinkedBlockingQueue<List<Long>> queue, RegDate dateTraitement, StatusManager status,
                                                       AtomicInteger compteur, HibernateTemplate hibernateTemplate) {
                return new ListesNominativesThread(queue,
                                                   dateTraitement,
                                                   nbThreads,
                                                   adressesIncluses,
                                                   avecContribuablesPP,
                                                   avecContribuablesPM,
                                                   avecDebiteurs,
                                                   tiersService,
                                                   adresseService,
                                                   serviceCivilCacheWarmer,
                                                   status,
                                                   compteur,
                                                   transactionManager,
                                                   tiersDAO,
                                                   hibernateTemplate);
            }

            @Override
            public Iterator<Long> getIdIterator(Session session) {
                return createIteratorOnIDsOfTiers(session, avecContribuablesPP, avecContribuablesPM, avecDebiteurs);
            }

        });
    }

	@Override
	protected String getDenominationContribuablesComptes() {
		return "tiers";     // et oui, il n'y a pas que des contribuables
	}

	@SuppressWarnings("unchecked")
    private Iterator<Long> createIteratorOnIDsOfTiers(Session session, boolean avecContribuablesPP, boolean avecContribuablesPM, boolean avecDebiteurs) {
	    if (avecContribuablesPP || avecContribuablesPM || avecDebiteurs) {
		    final String ppPart = "PersonnePhysique, MenageCommun";
		    final String pmPart = "Entreprise";
		    final String debiteurPart = "DebiteurPrestationImposable";

		    final List<String> whereParts = new ArrayList<String>(3);
		    if (avecContribuablesPP) {
			    whereParts.add(ppPart);
		    }
		    if (avecContribuablesPM) {
			    whereParts.add(pmPart);
		    }
		    if (avecDebiteurs) {
			    whereParts.add(debiteurPart);
		    }

		    final StringBuilder b = new StringBuilder();
		    final Iterator<String> wherePartIterator = whereParts.iterator();
		    while (wherePartIterator.hasNext()) {
			    final String part = wherePartIterator.next();
			    b.append(part);
			    if (wherePartIterator.hasNext()) {
				    b.append(", ");
			    }
		    }

		    final String inPart = b.toString();
		    final String queryString = String.format("SELECT tiers.id FROM Tiers AS tiers WHERE tiers.class IN (%s) ORDER BY tiers.id ASC", inPart);
			final Query query = session.createQuery(queryString);
			return query.iterate();
	    }
	    else {
		    return Collections.<Long>emptyList().iterator();
	    }
    }
}
