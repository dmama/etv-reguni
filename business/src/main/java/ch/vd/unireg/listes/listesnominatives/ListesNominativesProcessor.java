package ch.vd.unireg.listes.listesnominatives;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.Interruptible;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.cache.ServiceCivilCacheWarmer;
import ch.vd.unireg.common.ListesProcessor;
import ch.vd.unireg.common.LoggingStatusManager;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersService;

/**
 * Processor pour la génération des listes nominatives
 */
public class ListesNominativesProcessor extends ListesProcessor<ListesNominativesResults, ListesNominativesThread> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ListesNominativesProcessor.class);

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
	public ListesNominativesResults run(final int nbThreads, final TypeAdresse adressesIncluses, final boolean avecContribuablesPP, final boolean avecContribuablesPM, Set<Long> tiersList, RegDate dateTraitement,
	                                    final boolean avecDebiteurs, StatusManager s) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		// lance le vrai boulot !
		return doRun(dateTraitement, nbThreads, status, hibernateTemplate, new Customizer<ListesNominativesResults, ListesNominativesThread>() {

			@Override
			public ListesNominativesResults createResults(RegDate dateTraitement) {
				return new ListesNominativesResults(dateTraitement, nbThreads, adressesIncluses, avecContribuablesPP, avecContribuablesPM, avecDebiteurs, tiersList, tiersService, adresseService);
			}

			@Override
			public ListesNominativesThread createThread(LinkedBlockingQueue<List<Long>> queue, RegDate dateTraitement, Interruptible interruptible,
			                                            AtomicInteger compteur, HibernateTemplate hibernateTemplate) {
				return new ListesNominativesThread(queue,
				                                   dateTraitement,
				                                   nbThreads,
				                                   adressesIncluses,
				                                   avecContribuablesPP,
				                                   avecContribuablesPM,
				                                   avecDebiteurs,
				                                   tiersList,
				                                   tiersService,
				                                   adresseService,
				                                   serviceCivilCacheWarmer,
				                                   interruptible,
				                                   compteur,
				                                   transactionManager,
				                                   tiersDAO, hibernateTemplate);
			}

			@Override
			public Iterator<Long> getIdIterator(Session session) {
				return createIteratorOnIDsOfTiers(avecContribuablesPP, avecContribuablesPM, avecDebiteurs, tiersList, session);
			}

		});
	}

	@Override
	protected String getDenominationContribuablesComptes() {
		return "tiers";     // et oui, il n'y a pas que des contribuables
	}

	@SuppressWarnings("unchecked")
	private Iterator<Long> createIteratorOnIDsOfTiers(boolean avecContribuablesPP, boolean avecContribuablesPM, boolean avecDebiteurs, Set<Long> listTiers, Session session) {
		if (avecContribuablesPP || avecContribuablesPM || avecDebiteurs) {
			final String populationPP = PersonnePhysique.class.getSimpleName() + ", " + MenageCommun.class.getSimpleName();
			final String populationPM = Entreprise.class.getSimpleName();
			final String debiteurPart = DebiteurPrestationImposable.class.getSimpleName();

			final List<String> whereParts = new ArrayList<>(3);
			if (avecContribuablesPP) {
				whereParts.add(populationPP);
			}
			if (avecContribuablesPM) {
				whereParts.add(populationPM);
			}
			if (avecDebiteurs) {
				whereParts.add(debiteurPart);
			}

			// on découpe par lot de 500 car la clause 'in' est limité à 1000 avec Oracle (on prend un peu de marge)
			final List<List<Long>> partitions = ListUtils.partition(new ArrayList<>(listTiers), 500);
			final StringBuilder sb = new StringBuilder();

			final ListIterator<List<Long>> iterator = partitions.listIterator();
			while (iterator.hasNext()) {
				final List<Long> partition = iterator.next();
				sb.append("tiers.id")
						.append(" IN ")
						.append("(")
						.append(partition.stream()
								        .map(String::valueOf)
								        .collect(Collectors.joining(",")))
						.append(")");
				if (iterator.hasNext()) {
					sb.append(" OR ");
				}
			}

			final String inPopulationCriteria = String.join(", ", whereParts);

			final String queryString;
			if (StringUtils.isBlank(sb.toString())) {
				queryString = String.format("SELECT tiers.id FROM Tiers AS tiers WHERE tiers.class IN (%s)  ORDER BY tiers.id ASC", inPopulationCriteria);
			}
			else {
				queryString = String.format("SELECT tiers.id FROM Tiers AS tiers WHERE tiers.class IN (%s) AND (%s) ORDER BY tiers.id ASC", inPopulationCriteria, sb.toString());
			}

			final Query query = session.createQuery(queryString);
			return query.iterate();
		}
		else {
			return Collections.<Long>emptyList().iterator();
		}
	}
}
