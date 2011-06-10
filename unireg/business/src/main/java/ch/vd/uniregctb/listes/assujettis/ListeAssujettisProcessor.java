package ch.vd.uniregctb.listes.assujettis;

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
import ch.vd.uniregctb.cache.ServiceCivilCacheWarmer;
import ch.vd.uniregctb.common.ListesProcessor;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;

public class ListeAssujettisProcessor extends ListesProcessor<ListeAssujettisResults, ListeAssujettisThreads> {

	private static final Logger LOGGER = Logger.getLogger(ListeAssujettisProcessor.class);

	private final HibernateTemplate hibernateTemplate;
	private final TiersService tiersService;
	private final ServiceCivilCacheWarmer serviceCivilCacheWarmer;
	private final PlatformTransactionManager transactionManager;
	private final TiersDAO tiersDAO;

	public ListeAssujettisProcessor(HibernateTemplate hibernateTemplate, TiersService tiersService, ServiceCivilCacheWarmer serviceCivilCacheWarmer,
	                                PlatformTransactionManager transactionManager, TiersDAO tiersDAO) {
		this.hibernateTemplate = hibernateTemplate;
		this.tiersService = tiersService;
		this.serviceCivilCacheWarmer = serviceCivilCacheWarmer;
		this.transactionManager = transactionManager;
		this.tiersDAO = tiersDAO;
	}

	public ListeAssujettisResults run(RegDate dateTraitement, final int nbThreads, final int anneeFiscale, final boolean avecSourciersPurs,
	                                  final boolean seulementAssujettisFinAnnee, StatusManager s) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		return doRun(dateTraitement, nbThreads, status, hibernateTemplate, new Customizer<ListeAssujettisResults, ListeAssujettisThreads>() {

			@Override
			public Iterator<Long> getIdIterator(Session session) {
				return getIteratorOnCtbs(session, anneeFiscale);
			}

			@Override
			public ListeAssujettisResults createResults(RegDate dateTraitement) {
				return new ListeAssujettisResults(dateTraitement, nbThreads, anneeFiscale, avecSourciersPurs, seulementAssujettisFinAnnee, tiersService);
			}

			@Override
			public ListeAssujettisThreads createThread(LinkedBlockingQueue<List<Long>> queue, RegDate dateTraitement, StatusManager status, AtomicInteger compteur, HibernateTemplate hibernateTemplate) {
				return new ListeAssujettisThreads(queue, status, compteur, dateTraitement, nbThreads, anneeFiscale, avecSourciersPurs, seulementAssujettisFinAnnee,
				                                  serviceCivilCacheWarmer, tiersService, transactionManager, tiersDAO, hibernateTemplate);
			}
		});
	}

	/**
	 * @param session session hibernate à utiliser
	 * @param anneeFiscale période fiscale
	 * @return un itérateur sur tous les contribuables avec un for vaudois actif sur au moins une partie de la période fiscale donnée
	 */
	@SuppressWarnings({"unchecked"})
	private Iterator<Long> getIteratorOnCtbs(Session session, int anneeFiscale) {

		final StringBuilder b = new StringBuilder();
		b.append("SELECT DISTINCT ctb.id FROM Contribuable AS ctb");
		b.append(" INNER JOIN ctb.forsFiscaux AS fors");
		b.append(" WHERE ctb.annulationDate IS NULL");
		b.append(" AND fors.annulationDate IS NULL");
		b.append(" AND fors.typeAutoriteFiscale = 'COMMUNE_OU_FRACTION_VD'");
		b.append(" AND fors.dateDebut <= :finAnnee");
		b.append(" AND (fors.dateFin IS NULL OR fors.dateFin >= :debutAnnee)");
		b.append(" ORDER BY ctb.id ASC");
		final String hql = b.toString();

		final Query query = session.createQuery(hql);
		query.setParameter("debutAnnee", RegDate.get(anneeFiscale, 1, 1).index());
		query.setParameter("finAnnee", RegDate.get(anneeFiscale, 12, 31).index());
		return query.iterate();
	}
}
