package ch.vd.uniregctb.listes.listesnominatives;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.ListesProcessor;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

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

	private final ServiceCivilService serviceCivilService;

    public ListesNominativesProcessor(HibernateTemplate hibernateTemplate,
                                      TiersService tiersService,
                                      AdresseService adresseService,
                                      PlatformTransactionManager transactionManager,
                                      TiersDAO tiersDAO,
                                      ServiceCivilService serviceCivilService) {
        this.hibernateTemplate = hibernateTemplate;
        this.tiersService = tiersService;
        this.adresseService = adresseService;
        this.transactionManager = transactionManager;
        this.tiersDAO = tiersDAO;
	    this.serviceCivilService = serviceCivilService;
    }

    /**
     * Appel principal de génération des listes nominatives
     */
    public ListesNominativesResults run(RegDate dateTraitement, final int nbThreads, final TypeAdresse adressesIncluses, final boolean avecContribuables, final boolean avecDebiteurs, StatusManager s) {

        final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

        // lance le vrai boulot !
        return doRun(dateTraitement, nbThreads, status, hibernateTemplate, new Customizer<ListesNominativesResults, ListesNominativesThread>() {

            public ListesNominativesResults createResults(RegDate dateTraitement) {
                return new ListesNominativesResults(dateTraitement, nbThreads, adressesIncluses, avecContribuables, avecDebiteurs, tiersService, adresseService, serviceCivilService);
            }

            public ListesNominativesThread createTread(LinkedBlockingQueue<List<Long>> queue, RegDate dateTraitement, StatusManager status,
                                                       AtomicInteger compteur, HibernateTemplate hibernateTemplate) {
                return new ListesNominativesThread(queue,
                        dateTraitement,
		                nbThreads, 
                        adressesIncluses,
		                avecContribuables,
		                avecDebiteurs,
		                tiersService,
                        adresseService,
		                serviceCivilService,
		                status,
                        compteur,
                        transactionManager,
                        tiersDAO,
                        hibernateTemplate);
            }

            public Iterator<Long> getIdIterator(Session session) {
                return createIteratorOnIDsOfTiers(session, avecContribuables, avecDebiteurs);
            }

        });
    }

    @SuppressWarnings("unchecked")
    private Iterator<Long> createIteratorOnIDsOfTiers(Session session, boolean avecContribuables, boolean avecDebiteurs) {
	    if (avecContribuables || avecDebiteurs) {
		    final String contribuablePart = "PersonnePhysique, MenageCommun";
		    final String debiteurPart = "DebiteurPrestationImposable";

		    final String inPart;
		    if (avecContribuables && avecDebiteurs) {
			    inPart = String.format("%s, %s", contribuablePart, debiteurPart);
		    }
		    else if (avecContribuables) {
			    inPart = contribuablePart;
		    }
		    else {
			    inPart = debiteurPart;
		    }
		    final String queryString = String.format("SELECT tiers.id FROM Tiers AS tiers WHERE tiers.class IN (%s) ORDER BY tiers.id ASC", inPart);
			final Query query = session.createQuery(queryString);
			return query.iterate();
	    }
	    else {
		    return Collections.<Long>emptyList().iterator();
	    }
    }
}
