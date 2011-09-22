package ch.vd.uniregctb.listes.afc;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.jetbrains.annotations.Nullable;
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
 * Classe qui génère effectivement les extractions de listes de contribuables pour les données de référence RPT
 */
public class ExtractionDonneesRptProcessor extends ListesProcessor<ExtractionDonneesRptResults, ExtractionDonneesRptThread> {

	public static final Logger LOGGER = Logger.getLogger(ExtractionDonneesRptProcessor.class);

	private final HibernateTemplate hibernateTemplate;
	private final PlatformTransactionManager transactionManager;
	private final TiersService tiersService;
	private final ServiceCivilCacheWarmer serviceCivilCacheWarmer;
	private final ServiceInfrastructureService infraService;
	private final TiersDAO tiersDAO;

	public ExtractionDonneesRptProcessor(HibernateTemplate hibernateTemplate, PlatformTransactionManager transactionManager, TiersService tiersService, ServiceCivilCacheWarmer serviceCivilCacheWarmer,
	                                     TiersDAO tiersDAO, ServiceInfrastructureService infraService) {
		this.hibernateTemplate = hibernateTemplate;
		this.transactionManager = transactionManager;
		this.tiersService = tiersService;
		this.serviceCivilCacheWarmer = serviceCivilCacheWarmer;
		this.tiersDAO = tiersDAO;
		this.infraService = infraService;
	}

	public ExtractionDonneesRptResults run(RegDate dateTraitement, int pf, TypeExtractionDonneesRpt mode, int nbThreads, @Nullable StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);
		switch (mode) {
			case FORTUNE:
				return runFortune(dateTraitement, pf, nbThreads, status);
			case REVENU_SOURCE_PURE:
				return runRevenuSourcePure(dateTraitement, pf, nbThreads, status);
			case REVENU_ORDINAIRE:
				return runRevenuOrdinaire(dateTraitement, pf, nbThreads, status);
			default:
				throw new IllegalArgumentException("Mode inconnu : " + mode);
		}
	}

	/**
	 * Classe de base des customizers spécifiques à chacun des modes de fonctionnement
	 */
	private abstract class ExtractionDonneesRptBaseCustomizer implements Customizer<ExtractionDonneesRptResults, ExtractionDonneesRptThread> {
		@Override
		public ExtractionDonneesRptThread createThread(LinkedBlockingQueue<List<Long>> queue, RegDate dateTraitement, StatusManager status, AtomicInteger compteur, HibernateTemplate hibernateTemplate) {
			final ExtractionDonneesRptResults localResults = createResults(dateTraitement);
			return new ExtractionDonneesRptThread(queue, status, compteur, serviceCivilCacheWarmer, transactionManager, tiersDAO, hibernateTemplate, localResults);
		}
	}

	private ExtractionDonneesRptResults runFortune(RegDate dateTraitement, final int pf, final int nbThreads, StatusManager s) {
		return doRun(dateTraitement, nbThreads, s, hibernateTemplate, new ExtractionDonneesRptBaseCustomizer() {
			@Override
			public Iterator<Long> getIdIterator(Session session) {
				return getIdsTiersCandidatsPourExtractionFortune(session, pf);
			}

			@Override
			public ExtractionDonneesRptResults createResults(RegDate dateTraitement) {
				return new ExtractionDonneesRptFortuneResults(dateTraitement, pf, nbThreads, tiersService, infraService);
			}
		});
	}

	private ExtractionDonneesRptResults runRevenuSourcePure(RegDate dateTraitement, final int pf, final int nbThreads, StatusManager s) {
		return doRun(dateTraitement, nbThreads, s, hibernateTemplate, new ExtractionDonneesRptBaseCustomizer() {
			@Override
			public Iterator<Long> getIdIterator(Session session) {
				return getIdsTiersCandidatsPourExtractionRevenuSourcePure(session, pf);
			}

			@Override
			public ExtractionDonneesRptResults createResults(RegDate dateTraitement) {
				return new ExtractionDonneesRptRevenuSourcePureResults(dateTraitement, pf, nbThreads, tiersService, infraService);
			}
		});
	}


	private ExtractionDonneesRptResults runRevenuOrdinaire(RegDate dateTraitement, final int pf, final int nbThreads, StatusManager s) {
		return doRun(dateTraitement, nbThreads, s, hibernateTemplate, new ExtractionDonneesRptBaseCustomizer() {
			@Override
			public Iterator<Long> getIdIterator(Session session) {
				return getIdsTiersCandidatsPourExtractionRevenuOrdinaire(session, pf);
			}

			@Override
			public ExtractionDonneesRptResults createResults(RegDate dateTraitement) {
				return new ExtractionDonneesRptRevenuOrdinaireResults(dateTraitement, pf, nbThreads, tiersService, infraService);
			}
		});
	}

	/**
	 * Tous les contribuables (PP/MC) qui ont un for vaudois ouvert au moins une journée
	 * sur la période fiscale considérée
	 * @param session session Hibernate dans laquelle la requête en base peut être lancée
	 * @param pf période fiscale
	 * @return les numéros des contribuables concernés
	 */
	@SuppressWarnings({"unchecked"})
	private Iterator<Long> getIdsContribuablesAvecForOrdinaireValideUnJourAuMoins(Session session, int pf) {
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

	private Iterator<Long> getIdsTiersCandidatsPourExtractionFortune(Session session, int pf) {
		return getIdsContribuablesAvecForOrdinaireValideUnJourAuMoins(session, pf);
	}

	private Iterator<Long> getIdsTiersCandidatsPourExtractionRevenuOrdinaire(Session session, int pf) {
		return getIdsContribuablesAvecForOrdinaireValideUnJourAuMoins(session, pf);
	}
	/**
	 * Tous les contribuables (PP/MC) qui ont un for principal source pure actif au moins un jour
	 * sur la période fiscale considérée
	 * @param session session Hibernate dans laquelle la requête en base peut être lancée
	 * @param pf période fiscale
	 * @return les numéros des contribuables concernés
	 */
	@SuppressWarnings({"unchecked"})
	private Iterator<Long> getIdsTiersCandidatsPourExtractionRevenuSourcePure(Session session, int pf) {
		final StringBuilder b = new StringBuilder();
		b.append("SELECT DISTINCT ctb.id FROM Contribuable AS ctb");
		b.append(" INNER JOIN ctb.forsFiscaux AS for");
		b.append(" WHERE for.annulationDate IS NULL");
		b.append(" AND for.class = ForFiscalPrincipal");
		b.append(" AND for.modeImposition = 'SOURCE'");
		b.append(" AND for.dateDebut <= :finPeriode");
		b.append(" AND (for.dateFin IS NULL OR for.dateFin >= :debutPeriode)");
		final String hql = b.toString();

		final Query query = session.createQuery(hql);
		query.setParameter("finPeriode", RegDate.get(pf, 12, 31).index());
		query.setParameter("debutPeriode", RegDate.get(pf, 1, 1).index());
		return query.iterate();
	}
}
