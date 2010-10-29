package ch.vd.uniregctb.declaration;


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
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.ParallelBatchTransactionTemplate;
import ch.vd.uniregctb.common.StatusManager;

import ch.vd.uniregctb.metier.assujettissement.PeriodeImposition;

import ch.vd.uniregctb.tiers.Contribuable;

public class ListeNoteProcessor {

	private static final int BATCH_SIZE = 100;

	final Logger LOGGER = Logger.getLogger(ListeNoteProcessor.class);

	private final PlatformTransactionManager transactionManager;
	private HibernateTemplate hibernateTemplate;
	private int batchSize = BATCH_SIZE;
	private final ThreadLocal<ListeNoteResults> rapport = new ThreadLocal<ListeNoteResults>();

	public ListeNoteProcessor(HibernateTemplate hibernateTemplate, PlatformTransactionManager transactionManager) {

		this.hibernateTemplate = hibernateTemplate;
		this.transactionManager = transactionManager;


	}


	public ListeNoteResults run(final RegDate dateTraitement, final int annee, int nbThreads, final StatusManager s) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);
		final ListeNoteResults rapportFinal = new ListeNoteResults(dateTraitement,annee);
		status.setMessage("Récupération des contribuables ...");
		final List<Long> ids = recupererContribuables(annee);

		// Reussi les messages par lots
		final ParallelBatchTransactionTemplate<Long, ListeNoteResults>
				template = new ParallelBatchTransactionTemplate<Long, ListeNoteResults>(ids, batchSize, nbThreads, BatchTransactionTemplate.Behavior.REPRISE_AUTOMATIQUE,
				transactionManager, status, hibernateTemplate);
		template.execute(rapportFinal, new BatchTransactionTemplate.BatchCallback<Long, ListeNoteResults>() {

			@Override
			public ListeNoteResults createSubRapport() {
				return new ListeNoteResults(dateTraitement);
			}

			@Override
			public boolean doInTransaction(List<Long> batch, ListeNoteResults r) throws Exception {

				rapport.set(r);
				status.setMessage("Traitement du batch [" + batch.get(0) + "; " + batch.get(batch.size() - 1) + "] ...", percent);

				traiterBatch(batch, annee);
				return true;
			}
		});

		final int count = rapportFinal.listeContribuableAvecNote.size();

		if (status.interrupted()) {
			status.setMessage("La production de la liste de contribuable avec note a été interrompue."
					+ " Nombre de contribuable avec note au moment de l'interruption = " + count);
			rapportFinal.interrompu = true;
		}
		else {
			status.setMessage("La production de la liste de contribuable avec note est terminée."
					+ "Nombre de contribuable parcouru = " + rapportFinal.nbContribuable + ". Nombre de contribuable avec note = " + count + ". Nombre d'erreurs = " + rapportFinal.erreurs.size());
		}

		rapportFinal.end();

		return rapportFinal;

	}


	private void traiterBatch(final List<Long> batch, final int annee) throws Exception {

		// On charge tous les contribuables en vrac (avec préchargement des situations)
		final List<Contribuable> list = (List<Contribuable>) hibernateTemplate.executeWithNativeSession(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException {
				Criteria crit = session.createCriteria(Contribuable.class);
				crit.add(Restrictions.in("id", batch));
				crit.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
				return crit.list();
			}
		});

		for (Contribuable contribuable : list) {
			rapport.get().nbContribuable++;
			List<PeriodeImposition> periodesImposition = PeriodeImposition.determine(contribuable, annee);
			if (periodesImposition != null) {
				for (PeriodeImposition periodeImposition : periodesImposition) {
					if (periodeImposition.isRemplaceeParNote()) {
						rapport.get().addContribuableAvecNote(contribuable);
					}
				}
			}

		}

	}


	private List<Long> recupererContribuables(final int annee) {
		final RegDate debutAnnee = RegDate.get(annee, 1, 1);
		final String queryIdsCtbWithFors = // --------------------------------
				"SELECT DISTINCT                                                                         "
						+ "    cont.id                                                                   "
						+ "FROM                                                                          "
						+ "    Contribuable AS cont                                                      "
						+ "INNER JOIN                                                                    "
						+ "    cont.forsFiscaux AS fors                                                  "
						+ "WHERE                                                                         "
						+ "    cont.annulationDate IS null                                               "
						+ "    AND fors.annulationDate IS null                                           "
						+ "    AND fors.typeAutoriteFiscale = 'COMMUNE_OU_FRACTION_VD'                   "
						+ "    AND (fors.class = ForFiscalPrincipal OR fors.class = ForFiscalSecondaire) "
						+ "    AND (fors.modeImposition IS null OR fors.modeImposition != 'SOURCE')      "
						+ "    AND (fors.dateDebut IS null OR fors.dateDebut <= :finAnnee)               "
						+ "    AND (fors.dateFin IS null OR fors.dateFin >= :debutAnnee)                 " // [UNIREG-1742] for actif n'importe quand dans l'année
						+ "ORDER BY cont.id ASC                                                          ";


		final RegDate finAnnee = RegDate.get(annee, 12, 31);

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		final List<Long> ids = (List<Long>) template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

				final List<Long> idsFors = (List<Long>) hibernateTemplate.executeWithNewSession(new HibernateCallback() {
					public Object doInHibernate(Session session) throws HibernateException {
						Query queryObject = session.createQuery(queryIdsCtbWithFors);
						queryObject.setParameter("debutAnnee", debutAnnee.index());
						queryObject.setParameter("finAnnee", finAnnee.index());
						return queryObject.list();
					}
				});


				return idsFors;
			}
		});

		return ids;
	}
}
