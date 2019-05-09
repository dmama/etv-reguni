package ch.vd.unireg.listes.afc.pm;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.NotImplementedException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.jetbrains.annotations.Nullable;
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
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.metier.assujettissement.PeriodeImpositionService;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersService;

/**
 * Classe qui génère effectivement les extractions de listes de contribuables pour les données de référence RPT PM
 */
public class ExtractionDonneesRptPMProcessor extends ListesProcessor<ExtractionDonneesRptPMResults, ExtractionDonneesRptPMThread> {

	public static final Logger LOGGER = LoggerFactory.getLogger(ExtractionDonneesRptPMProcessor.class);

	private final HibernateTemplate hibernateTemplate;
	private final PlatformTransactionManager transactionManager;
	private final TiersService tiersService;
	private final ServiceCivilCacheWarmer serviceCivilCacheWarmer;
	private final ServiceInfrastructureService infraService;
	private final TiersDAO tiersDAO;
	private final PeriodeImpositionService periodeImpositionService;
	private final AdresseService adresseService;

	public ExtractionDonneesRptPMProcessor(HibernateTemplate hibernateTemplate, PlatformTransactionManager transactionManager, TiersService tiersService, ServiceCivilCacheWarmer serviceCivilCacheWarmer,
	                                       TiersDAO tiersDAO, ServiceInfrastructureService infraService,
	                                       PeriodeImpositionService periodeImpositionService, AdresseService adresseService) {
		this.hibernateTemplate = hibernateTemplate;
		this.transactionManager = transactionManager;
		this.tiersService = tiersService;
		this.serviceCivilCacheWarmer = serviceCivilCacheWarmer;
		this.tiersDAO = tiersDAO;
		this.infraService = infraService;
		this.periodeImpositionService = periodeImpositionService;
		this.adresseService = adresseService;
	}

	public ExtractionDonneesRptPMResults run(RegDate dateTraitement, int pf, ModeExtraction mode, VersionWS versionWs, int nbThreads, @Nullable StatusManager s) {
		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);
		if (mode == ModeExtraction.BENEFICE) {
			return runBenefice(dateTraitement, pf, versionWs, nbThreads, status);
		}
		else {
			throw new NotImplementedException("Mode d'extraction " + mode + " non-implémenté");
		}
	}

	/**
	 * Classe de base des customizers spécifiques à chacun des modes de fonctionnement
	 */
	private abstract class ExtractionDonneesRptBaseCustomizer implements Customizer<ExtractionDonneesRptPMResults, ExtractionDonneesRptPMThread> {
		@Override
		public ExtractionDonneesRptPMThread createThread(LinkedBlockingQueue<List<Long>> queue, RegDate dateTraitement, Interruptible interruptible, AtomicInteger compteur, HibernateTemplate hibernateTemplate) {
			final ExtractionDonneesRptPMResults localResults = createResults(dateTraitement);
			return new ExtractionDonneesRptPMThread(queue, interruptible, compteur, serviceCivilCacheWarmer, transactionManager, tiersDAO, hibernateTemplate, localResults);
		}
	}


	private ExtractionDonneesRptPMResults runBenefice(RegDate dateTraitement, int pf, VersionWS versionWs, int nbThreads, StatusManager s) {
		return doRun(dateTraitement, nbThreads, s, hibernateTemplate, new ExtractionDonneesRptBaseCustomizer() {
			@Override
			public Iterator<Long> getIdIterator(Session session) {
				return getIdsContribuablesPMAvecForActifSurPf(session, pf);
			}

			@Override
			public ExtractionDonneesRptPMResults createResults(RegDate dateTraitement) {
				return new ExtractionDonneesRptPMResults(dateTraitement, pf, ModeExtraction.BENEFICE, versionWs, nbThreads, tiersService, infraService, periodeImpositionService, adresseService);
			}
		});
	}

	/**
	 * Retourne toutes les entreprises avec un for vaudois actif sur la période
	 */
	@SuppressWarnings("unchecked")
	private Iterator<Long> getIdsContribuablesPMAvecForActifSurPf(Session session, int pf) {
		//Mettre la pf à 2 ans en arrière permet de recuperer les entreprises n'ayant plus de for sur la période N mais qui potentiellement
		//pourraient encore avoir des périodes d'impostion qui existent encore au dela de la fermeture grace à l'existence d'un exercice commercial
		//	Iterator<Long> iter = list.iterator();
		final StringBuilder b = new StringBuilder();
		b.append("SELECT DISTINCT ctb.id FROM Entreprise AS ctb");
		b.append(" INNER JOIN ctb.forsFiscaux AS for");
		b.append(" WHERE for.annulationDate IS NULL");
		b.append(" AND for.typeAutoriteFiscale = 'COMMUNE_OU_FRACTION_VD'");
		b.append(" AND type(for) IN (ForFiscalPrincipalPM, ForFiscalSecondaire)");
		b.append(" AND (for.genreImpot = 'BENEFICE_CAPITAL')");
		b.append(" AND for.dateDebut <= :finPeriode");
		b.append(" AND (for.dateFin IS NULL OR for.dateFin >= :debutPeriode)");
		final String hql = b.toString();

		//On utilise comme periode de départ la période N-2
		final RegDate debutPeriodePrisEnCompte = RegDate.get(pf, 1, 1).addYears(-2);
		final Query query = session.createQuery(hql);
		query.setParameter("finPeriode", RegDate.get(pf, 12, 31));

		query.setParameter("debutPeriode", debutPeriodePrisEnCompte);
		return query.iterate();
	}
}
