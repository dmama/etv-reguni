package ch.vd.uniregctb.metier;

import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.ParallelBatchTransactionTemplate;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.TiersService;

public class ComparerForFiscalEtCommuneProcessor {

	private static final int BATCH_SIZE = 100;

	final Logger LOGGER = Logger.getLogger(ComparerForFiscalEtCommuneProcessor.class);

	private final PlatformTransactionManager transactionManager;
	private final HibernateTemplate hibernateTemplate;
	private final AdresseService adresseService;
	private final TiersService tiersService;
	private final ServiceInfrastructureService serviceInfra;
	private static final int batchSize = BATCH_SIZE;
	private final ThreadLocal<ComparerForFiscalEtCommuneResults> rapport = new ThreadLocal<ComparerForFiscalEtCommuneResults>();

	public ComparerForFiscalEtCommuneProcessor(HibernateTemplate hibernateTemplate, PlatformTransactionManager transactionManager, AdresseService aService,
	                                           TiersService tiersService, ServiceInfrastructureService serviceInfra) {
		this.hibernateTemplate = hibernateTemplate;
		this.transactionManager = transactionManager;
		this.adresseService = aService;
		this.tiersService = tiersService;
		this.serviceInfra = serviceInfra;
	}

	public ComparerForFiscalEtCommuneResults run(final RegDate dateTraitement, int nbThreads, final StatusManager s) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);
		final ComparerForFiscalEtCommuneResults rapportFinal = new ComparerForFiscalEtCommuneResults(dateTraitement, tiersService, adresseService);
		status.setMessage("Récupération des contribuables à analyser...");
		final List<Long> ids = recupererContribuableAAnalyser();

		// Reussi les messages par lots
		final ParallelBatchTransactionTemplate<Long, ComparerForFiscalEtCommuneResults>
				template = new ParallelBatchTransactionTemplate<Long, ComparerForFiscalEtCommuneResults>(ids, batchSize, nbThreads, BatchTransactionTemplate.Behavior.REPRISE_AUTOMATIQUE,
																										 transactionManager, status, hibernateTemplate);
		template.execute(rapportFinal, new BatchTransactionTemplate.BatchCallback<Long, ComparerForFiscalEtCommuneResults>() {

			@Override
			public ComparerForFiscalEtCommuneResults createSubRapport() {
				return new ComparerForFiscalEtCommuneResults(dateTraitement, tiersService, adresseService);
			}

			@Override
			public boolean doInTransaction(List<Long> batch, ComparerForFiscalEtCommuneResults r) throws Exception {

				rapport.set(r);
				status.setMessage("Traitement du batch [" + batch.get(0) + "; " + batch.get(batch.size() - 1) + "] ...", percent);

				traiterBatch(batch);
				return true;
			}
		});

		final int count = rapportFinal.listeCommunesDifferentes.size();

		if (status.interrupted()) {
			status.setMessage("La comparaison des For et des communes a été interrompue."
					+ " Nombre de contribuables analysés au moment de l'interruption = " + count);
			rapportFinal.interrompu = true;
		}
		else {
			status.setMessage("La comparaison des For et des communes est terminée."
					+ "Nombre de contribuables analysés = " + rapportFinal.nbCtbTotal + ". Nombre de cas différents = " + count + ". Nombre d'erreurs = " + rapportFinal.erreurs.size());
		}

		rapportFinal.end();

		return rapportFinal;

	}

	private void traiterBatch(final List<Long> batch) throws Exception {

		// On charge tous les contribuables en vrac (avec préchargement des situations)
		final List<Contribuable> list = hibernateTemplate.executeWithNativeSession(new HibernateCallback<List<Contribuable>>() {
			@Override
			public List<Contribuable> doInHibernate(Session session) throws HibernateException {
				final Criteria crit = session.createCriteria(Contribuable.class);
				crit.add(Restrictions.in("id", batch));
				crit.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
				return crit.list();
			}
		});

		for (Contribuable contribuable : list) {
			rapport.get().nbCtbTotal++;
			ForFiscalPrincipal forFiscal = contribuable.getDernierForFiscalPrincipal();
			final Integer numeroAutoriteFiscale = forFiscal.getNumeroOfsAutoriteFiscale();
			Commune communeFor = serviceInfra.getCommuneByNumeroOfsEtendu(numeroAutoriteFiscale, RegDate.get());
			AdresseGenerique adresse = adresseService.getAdresseFiscale(contribuable, TypeAdresseFiscale.DOMICILE, RegDate.get(), false);
			Commune communeAdresse = serviceInfra.getCommuneByAdresse(adresse, RegDate.get());
			if(communeAdresse != null && communeFor!=null){
				if (communeFor.getNoOFS() != communeAdresse.getNoOFS()) {
					rapport.get().addCommunesDifferentes(forFiscal, communeFor.getNomMinuscule(), adresse, communeAdresse.getNomMinuscule());
				}
			}


		}

	}


	private List<Long> recupererContribuableAAnalyser() {

		final String queryMessage =/// --------------------------------
				"SELECT DISTINCT                                                                         "
						+ "    cont.id                                                                   "
						+ "FROM                                                                          "
						+ "    Contribuable AS cont                                                      "
						+ "INNER JOIN                                                                    "
						+ "    cont.forsFiscaux AS fors                                                  "
						+ "WHERE                                                                         "
						+ "    cont.annulationDate IS null                                               "
						+ "    AND fors.annulationDate IS null                                           "
						+ "    AND fors.class = ForFiscalPrincipal                                       "
						+ "    AND fors.dateFin IS null                                                  "
						+ "ORDER BY cont.id ASC                                                          ";


		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		final List<Long> ids = template.execute(new TransactionCallback<List<Long>>() {
			@Override
			public List<Long> doInTransaction(TransactionStatus status) {

				final List<Long> idsMessage = hibernateTemplate.executeWithNewSession(new HibernateCallback<List<Long>>() {
					@Override
					public List<Long> doInHibernate(Session session) throws HibernateException {
						Query queryObject = session.createQuery(queryMessage);
						//noinspection unchecked
						return queryObject.list();
					}
				});
				Collections.sort(idsMessage);
				return idsMessage;
			}

		});
		return ids;
	}
}
