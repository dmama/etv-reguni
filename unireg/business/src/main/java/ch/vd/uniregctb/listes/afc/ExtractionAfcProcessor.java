package ch.vd.uniregctb.listes.afc;

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
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * Classe qui génère effectivement les extractions de listes de contribuables pour l'AFC
 */
public class ExtractionAfcProcessor extends ListesProcessor<ExtractionAfcResults, ExtractionAfcThread> {

	public static final Logger LOGGER = Logger.getLogger(ExtractionAfcProcessor.class);

	private final HibernateTemplate hibernateTemplate;
	private final PlatformTransactionManager transactionManager;
	private final TiersService tiersService;
	private final ServiceCivilCacheWarmer serviceCivilCacheWarmer;
	private final ServiceInfrastructureService infraService;
	private final TiersDAO tiersDAO;

	public ExtractionAfcProcessor(HibernateTemplate hibernateTemplate, PlatformTransactionManager transactionManager, TiersService tiersService, ServiceCivilCacheWarmer serviceCivilCacheWarmer,
	                              TiersDAO tiersDAO, ServiceInfrastructureService infraService) {
		this.hibernateTemplate = hibernateTemplate;
		this.transactionManager = transactionManager;
		this.tiersService = tiersService;
		this.serviceCivilCacheWarmer = serviceCivilCacheWarmer;
		this.tiersDAO = tiersDAO;
		this.infraService = infraService;
	}

	public ExtractionAfcResults run(RegDate dateTraitement, final int pf, final TypeExtractionAfc mode, final int nbThreads, StatusManager s) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		return doRun(dateTraitement, nbThreads, status, hibernateTemplate, new Customizer<ExtractionAfcResults, ExtractionAfcThread>() {

			public Iterator<Long> getIdIterator(Session session) {
				return getIdsTiersCandidatsPourExtraction(session, pf);
			}

			public ExtractionAfcResults createResults(RegDate dateTraitement) {
				return new ExtractionAfcResults(dateTraitement, pf, mode, nbThreads, tiersService, infraService);
			}

			public ExtractionAfcThread createThread(LinkedBlockingQueue<List<Long>> queue, RegDate dateTraitement, StatusManager status, AtomicInteger compteur, HibernateTemplate hibernateTemplate) {
				return new ExtractionAfcThread(queue, status, compteur, serviceCivilCacheWarmer, tiersService, infraService, transactionManager, tiersDAO, hibernateTemplate, dateTraitement, pf, mode, nbThreads);
			}
		});
	}

	/**
	 * Tous les contribuables (PP/MC) qui ont un for vaudois ouvert au moins une journée
	 * sur la période fiscale considérée
	 * @param pf période fiscale
	 * @return les numéros des contribuables concernés
	 */
	@SuppressWarnings({"unchecked"})
	private Iterator<Long> getIdsTiersCandidatsPourExtraction(Session session, int pf) {
		final StringBuilder b = new StringBuilder();
		b.append("SELECT DISTINCT ctb.id FROM Contribuable AS ctb");
		b.append(" INNER JOIN ctb.forsFiscaux AS for");
		b.append(" WHERE for.annulationDate IS NULL");
		b.append(" AND for.typeAutoriteFiscale = 'COMMUNE_OU_FRACTION_VD'");
		b.append(" AND for.class IN (ForFiscalPrincipal, ForFiscalSecondaire)");
		b.append(" AND (for.modeImposition IS NULL OR for.modeImposition != 'SOURCE')");
		b.append(" AND for.motifRattachement != 'DIPLOMATE_ETRANGER'");
		b.append(" AND for.dateDebut <= :finPeriode");
		b.append(" AND (for.dateFin IS NULL OR for.dateFin >= :debutPeriode)");
		final String hql = b.toString();

		final Query query = session.createQuery(hql);
		query.setParameter("finPeriode", RegDate.get(pf, 12, 31).index());
		query.setParameter("debutPeriode", RegDate.get(pf, 1, 1).index());
		return query.iterate();
	}
}
