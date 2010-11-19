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
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.ParallelBatchTransactionTemplate;
import ch.vd.uniregctb.common.StatusManager;

import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImposition;

import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.ForsParType;
import ch.vd.uniregctb.tiers.TiersService;

public class ListeNoteProcessor {

	private static final int BATCH_SIZE = 100;

	final Logger LOGGER = Logger.getLogger(ListeNoteProcessor.class);

	private final PlatformTransactionManager transactionManager;
	private HibernateTemplate hibernateTemplate;
	private TiersService tiersService;
	private AdresseService adresseService;
	private ServiceInfrastructureService infraService;
	private int batchSize = BATCH_SIZE;
	private final ThreadLocal<ListeNoteResults> rapport = new ThreadLocal<ListeNoteResults>();

	public ListeNoteProcessor(HibernateTemplate hibernateTemplate, PlatformTransactionManager transactionManager, TiersService tiersService, AdresseService adresseService,
	                          ServiceInfrastructureService infraService) {

		this.hibernateTemplate = hibernateTemplate;
		this.transactionManager = transactionManager;
		this.tiersService = tiersService;
		this.adresseService = adresseService;
		this.infraService = infraService;
	}


	public ListeNoteResults run(final RegDate dateTraitement, final int annee, int nbThreads, final StatusManager s) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);
		final ListeNoteResults rapportFinal = new ListeNoteResults(dateTraitement, annee);
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
			//Absence de for secondaire ouvert
			final RegDate dateFinSecondaire = getDateFermetureForSecondaire(contribuable);
			if (dateFinSecondaire != null) {
				if (tiersService.isHorsCanton(contribuable, dateFinSecondaire)) {
					rapport.get().nbContribuable++;
					rapport.get().addContribuableAvecNote(new ListeNoteResults.InfoContribuableAvecNote(
							contribuable, dateFinSecondaire, adresseService, tiersService, infraService));


				}

			}


		}


	}


	/**
	 * Permet de recherche la date de fin du for secondaire fermé Si un for secondaire ouvert existe, alors on renvoie null
	 *
	 * @param contribuable à analyser
	 * @return la date de fin du for secondaire ou null si il existe un for secondaire ouvert
	 */
	private RegDate getDateFermetureForSecondaire(Contribuable contribuable) {
		ForsParType forsParType = contribuable.getForsParType(true);
		List<ForFiscalSecondaire> forsSecondaires = forsParType.secondaires;
		RegDate dateFin = null;
		for (ForFiscalSecondaire forsSecondaire : forsSecondaires) {
			if (forsSecondaire.getDateFin() == null) {
				return null;
			}
			dateFin = forsSecondaire.getDateFin();

		}
		return dateFin;
	}


	private List<Long> recupererContribuables(final int annee) {
		final RegDate debutAnnee = RegDate.get(annee, 1, 1);

		//Identifier les contribuables ayant vendu leur dernier immeuble ou cessé leur dernière activité indépendante dans une PF donnée
		final String queryIdsCtbForsSecondaire = // --------------------------------
				"SELECT DISTINCT                                                                         "
						+ "    cont.id                                                                   "
						+ "FROM                                                                          "
						+ "    Contribuable AS cont                                                      "
						+ "INNER JOIN                                                                    "
						+ "    cont.forsFiscaux AS fors                                                  "
						+ "WHERE                                                                         "
						+ "        cont.annulationDate IS null                                           "
						+ "    AND fors.annulationDate IS null                                           "
						+ "    AND fors.class = ForFiscalSecondaire                                      "
						+ "    AND fors.motifFermeture IN ('VENTE_IMMOBILIER', 'FIN_EXPLOITATION')       "
						+ "    AND fors.dateFin IS NOT null                                              "
						+ "    AND fors.dateFin >= :debutAnnee                                           "
						+ "    AND fors.dateFin <= :finAnnee                                             "
						+ "ORDER BY cont.id ASC                                                          ";


		final RegDate finAnnee = RegDate.get(annee, 12, 31);

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		final List<Long> ids = (List<Long>) template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

				final List<Long> idsFors = (List<Long>) hibernateTemplate.executeWithNewSession(new HibernateCallback() {
					public Object doInHibernate(Session session) throws HibernateException {
						Query queryObject = session.createQuery(queryIdsCtbForsSecondaire);
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
